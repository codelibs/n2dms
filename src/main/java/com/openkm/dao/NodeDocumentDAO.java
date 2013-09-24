/**
 * OpenKM, Open Document Management System (http://www.openkm.com)
 * Copyright (c) 2006-2013 Paco Avila & Josep Llort
 * 
 * No bytes were intentionally harmed during the development of this application.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.openkm.dao;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.openkm.automation.AutomationManager;
import com.openkm.automation.AutomationUtils;
import com.openkm.bean.Permission;
import com.openkm.cache.UserItemsManager;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.LockException;
import com.openkm.core.PathNotFoundException;
import com.openkm.dao.bean.AutomationRule;
import com.openkm.dao.bean.NodeBase;
import com.openkm.dao.bean.NodeDocument;
import com.openkm.dao.bean.NodeDocumentVersion;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.dao.bean.NodeLock;
import com.openkm.extractor.RegisteredExtractors;
import com.openkm.extractor.TextExtractorWork;
import com.openkm.module.common.CommonGeneralModule;
import com.openkm.module.db.stuff.FsDataStore;
import com.openkm.module.db.stuff.LockHelper;
import com.openkm.module.db.stuff.SecurityHelper;
import com.openkm.module.jcr.stuff.JCRUtils;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.FormatUtil;
import com.openkm.util.PathUtils;
import com.openkm.util.UserActivity;
import com.openkm.vernum.VersionNumerationAdapter;
import com.openkm.vernum.VersionNumerationFactory;

public class NodeDocumentDAO {
    private static Logger log = LoggerFactory.getLogger(NodeDocumentDAO.class);

    private static NodeDocumentDAO single = new NodeDocumentDAO();

    private NodeDocumentDAO() {
    }

    public static NodeDocumentDAO getInstance() {
        return single;
    }

    /**
     * Create document and first version
     */
    public NodeDocumentVersion create(final NodeDocument nDoc,
            final InputStream is, final long size)
            throws PathNotFoundException, AccessDeniedException,
            ItemExistsException, DatabaseException, IOException {
        log.debug("create({}, {}, {})", new Object[] { nDoc, is, size });
        Session session = null;
        Transaction tx = null;
        final NodeDocumentVersion newDocVer = new NodeDocumentVersion();

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            final NodeBase parentNode = (NodeBase) session.load(NodeBase.class,
                    nDoc.getParent());
            SecurityHelper.checkRead(parentNode);
            SecurityHelper.checkWrite(parentNode);

            // Check for same document name in same folder
            NodeBaseDAO.getInstance().checkItemExistence(session,
                    nDoc.getParent(), nDoc.getName());

            // Create first document version
            final VersionNumerationAdapter verNumAdapter = VersionNumerationFactory
                    .getVersionNumerationAdapter();
            newDocVer.setUuid(UUID.randomUUID().toString());
            newDocVer.setParent(nDoc.getUuid());
            newDocVer.setName(verNumAdapter.getInitialVersionNumber());
            newDocVer.setAuthor(nDoc.getAuthor());
            newDocVer.setCurrent(true);
            newDocVer.setCreated(nDoc.getCreated());
            newDocVer.setSize(size);
            newDocVer.setMimeType(nDoc.getMimeType());

            // Persist file in datastore
            FsDataStore.persist(newDocVer, is);

            session.save(nDoc);
            session.save(newDocVer);
            HibernateUtil.commit(tx);

            log.debug("create: {}", newDocVer);
            return newDocVer;
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final AccessDeniedException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final ItemExistsException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);

            // What happen when create fails? This datastore file should be deleted!
            FsDataStore.delete(newDocVer.getUuid());
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Find all documents
     */
    @SuppressWarnings("unchecked")
    public List<NodeDocument> findAll() throws DatabaseException {
        log.debug("findAll()");
        Session session = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            final Query q = session.createQuery("from NodeDocument nd");
            final List<NodeDocument> ret = q.list();

            // Security Check
            SecurityHelper.pruneNodeList(ret);

            initialize(ret);
            log.debug("findAll: {}", ret);
            return ret;
        } catch (final HibernateException e) {
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Find by parent
     */
    @SuppressWarnings("unchecked")
    public List<NodeDocument> findByParent(final String parentUuid)
            throws PathNotFoundException, DatabaseException {
        log.debug("findByParent({})", parentUuid);
        final String qs = "from NodeDocument nd where nd.parent=:parent order by nd.name";
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            if (!Config.ROOT_NODE_UUID.equals(parentUuid)) {
                final NodeBase parentNode = (NodeBase) session.load(
                        NodeBase.class, parentUuid);
                SecurityHelper.checkRead(parentNode);
            }

            final Query q = session.createQuery(qs);
            q.setString("parent", parentUuid);
            final List<NodeDocument> ret = q.list();

            // Security Check
            SecurityHelper.pruneNodeList(ret);

            initialize(ret);
            HibernateUtil.commit(tx);
            log.debug("findByParent: {}", ret);
            return ret;
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Find by pk
     */
    public NodeDocument findByPk(final String uuid)
            throws PathNotFoundException, DatabaseException {
        return findByPk(uuid, false);
    }

    /**
     * Find by pk and optionally initialize node property groups
     */
    public NodeDocument findByPk(final String uuid, final boolean initPropGroups)
            throws PathNotFoundException, DatabaseException {
        log.debug("findByPk({}, {})", uuid, initPropGroups);
        final String qs = "from NodeDocument nd where nd.uuid=:uuid";
        Session session = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            final Query q = session.createQuery(qs);
            q.setString("uuid", uuid);
            final NodeDocument nDoc = (NodeDocument) q.setMaxResults(1)
                    .uniqueResult();

            if (nDoc == null) {
                throw new PathNotFoundException(uuid);
            }

            // Security Check
            SecurityHelper.checkRead(nDoc);

            initialize(nDoc, initPropGroups);
            log.debug("findByPk: {}", nDoc);
            return nDoc;
        } catch (final HibernateException e) {
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Search nodes by category
     */
    @SuppressWarnings("unchecked")
    public List<NodeDocument> findByCategory(final String catUuid)
            throws PathNotFoundException, DatabaseException {
        log.debug("findByCategory({})", catUuid);
        final String qs = "from NodeDocument nd where :category in elements(nd.categories) order by nd.name";
        List<NodeDocument> ret = new ArrayList<NodeDocument>();
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            final NodeBase catNode = (NodeBase) session.load(NodeBase.class,
                    catUuid);
            SecurityHelper.checkRead(catNode);

            final Query q = session.createQuery(qs);
            q.setString("category", catUuid);
            ret = q.list();

            // Security Check
            SecurityHelper.pruneNodeList(ret);

            initialize(ret);
            HibernateUtil.commit(tx);
            log.debug("findByCategory: {}", ret);
            return ret;
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Search nodes by keyword
     */
    @SuppressWarnings("unchecked")
    public List<NodeDocument> findByKeyword(final String keyword)
            throws DatabaseException {
        log.debug("findByKeyword({})", keyword);
        final String qs = "from NodeDocument nd where :keyword in elements(nd.keywords) order by nd.name";
        List<NodeDocument> ret = new ArrayList<NodeDocument>();
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            final Query q = session.createQuery(qs);
            q.setString("keyword", keyword);
            ret = q.list();

            // Security Check
            SecurityHelper.pruneNodeList(ret);

            initialize(ret);
            HibernateUtil.commit(tx);
            log.debug("findByKeyword: {}", ret);
            return ret;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Search nodes by property value
     */
    @SuppressWarnings("unchecked")
    public List<NodeDocument> findByPropertyValue(final String group,
            final String property, final String value) throws DatabaseException {
        log.debug("findByPropertyValue({}, {}, {})", property, value);
        final String qs = "select nb from NodeDocument nb join nb.properties nbp where nbp.group=:group and nbp.name=:property and nbp.value like :value";
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            final Query q = session.createQuery(qs);
            q.setString("group", group);
            q.setString("property", property);
            q.setString("value", "%" + value + "%");
            final List<NodeDocument> ret = q.list();

            // Security Check
            SecurityHelper.pruneNodeList(ret);

            initialize(ret);
            HibernateUtil.commit(tx);
            log.debug("findByPropertyValue: {}", ret);
            return ret;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Search nodes from a given parent recursively
     */
    public List<NodeDocument> findFromParent(final String parentUuid)
            throws DatabaseException {
        log.debug("findFromParent({}})", parentUuid);
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            final List<NodeDocument> ret = findFromParentHelper(session,
                    parentUuid);

            HibernateUtil.commit(tx);
            log.debug("findFromParent: {}", ret);
            return ret;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    @SuppressWarnings("unchecked")
    private List<NodeDocument> findFromParentHelper(final Session session,
            final String parentUuid) throws DatabaseException,
            HibernateException {
        final List<NodeDocument> nodeList = new ArrayList<NodeDocument>();
        final String qs = "from NodeBase n where n.parent=:parent";
        final Query q = session.createQuery(qs);
        q.setString("parent", parentUuid);
        final List<NodeBase> nodes = q.list();

        for (final NodeBase nBase : nodes) {
            if (SecurityHelper.getAccessManager().isGranted(nBase,
                    Permission.READ)) {
                if (nBase instanceof NodeFolder) {
                    nodeList.addAll(findFromParentHelper(session,
                            nBase.getUuid()));
                } else if (nBase instanceof NodeDocument) {
                    final NodeDocument nDoc = (NodeDocument) nBase;
                    initialize(nDoc, false);
                    nodeList.add(nDoc);
                }
            }
        }

        return nodeList;
    }

    /**
     * Check if folder has children
     */
    @SuppressWarnings("unchecked")
    public boolean hasChildren(final String parentUuid)
            throws PathNotFoundException, DatabaseException {
        log.debug("hasChildren({})", parentUuid);
        final String qs = "from NodeDocument nd where nd.parent=:parent";
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            if (!Config.ROOT_NODE_UUID.equals(parentUuid)) {
                final NodeBase parentNode = (NodeBase) session.load(
                        NodeBase.class, parentUuid);
                SecurityHelper.checkRead(parentNode);
            }

            final Query q = session.createQuery(qs);
            q.setString("parent", parentUuid);
            final List<NodeFolder> nodeList = q.list();

            // Security Check
            SecurityHelper.pruneNodeList(nodeList);

            final boolean ret = !nodeList.isEmpty();
            HibernateUtil.commit(tx);
            log.debug("hasChildren: {}", ret);
            return ret;
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Rename document
     */
    public NodeDocument rename(final String uuid, final String newName)
            throws PathNotFoundException, AccessDeniedException,
            ItemExistsException, LockException, DatabaseException {
        log.debug("rename({}, {})", uuid, newName);
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            final NodeBase parentNode = NodeBaseDAO.getInstance()
                    .getParentNode(session, uuid);
            SecurityHelper.checkRead(parentNode);
            SecurityHelper.checkWrite(parentNode);
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            SecurityHelper.checkRead(nDoc);
            SecurityHelper.checkWrite(nDoc);

            // Lock Check
            LockHelper.checkWriteLock(nDoc);

            // Check for same folder name in same parent
            NodeBaseDAO.getInstance().checkItemExistence(session,
                    nDoc.getParent(), newName);

            nDoc.setName(newName);
            session.update(nDoc);
            initialize(nDoc, false);
            HibernateUtil.commit(tx);
            log.debug("rename: {}", nDoc);
            return nDoc;
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final AccessDeniedException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final LockException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final ItemExistsException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Move document
     */
    public void move(final String uuid, final String dstUuid)
            throws PathNotFoundException, AccessDeniedException,
            ItemExistsException, LockException, DatabaseException {
        log.debug("move({}, {})", uuid, dstUuid);
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            final NodeFolder nDstFld = (NodeFolder) session.load(
                    NodeFolder.class, dstUuid);
            SecurityHelper.checkRead(nDstFld);
            SecurityHelper.checkWrite(nDstFld);
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            SecurityHelper.checkRead(nDoc);
            SecurityHelper.checkWrite(nDoc);

            // Lock Check
            LockHelper.checkWriteLock(nDoc);

            // Check for same folder name in same parent
            NodeBaseDAO.getInstance().checkItemExistence(session, dstUuid,
                    nDoc.getName());

            // Check if context changes
            if (!nDstFld.getContext().equals(nDoc.getContext())) {
                nDoc.setContext(nDstFld.getContext());
            }

            nDoc.setParent(dstUuid);
            session.update(nDoc);
            HibernateUtil.commit(tx);
            log.debug("move: void");
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final AccessDeniedException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final LockException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final ItemExistsException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Delete document
     */
    public void delete(final String name, final String uuid,
            final String trashUuid) throws PathNotFoundException,
            AccessDeniedException, LockException, DatabaseException {
        log.debug("delete({}, {}, {})", new Object[] { name, uuid, trashUuid });
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            final NodeFolder nTrashFld = (NodeFolder) session.load(
                    NodeFolder.class, trashUuid);
            SecurityHelper.checkRead(nTrashFld);
            SecurityHelper.checkWrite(nTrashFld);
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            SecurityHelper.checkRead(nDoc);
            SecurityHelper.checkWrite(nDoc);

            // Lock Check
            LockHelper.checkWriteLock(nDoc);

            // Test if already exists a document with the same name in the trash
            final String fileName = com.openkm.util.FileUtils.getFileName(name);
            final String fileExtension = com.openkm.util.FileUtils
                    .getFileExtension(name);
            String testName = name;

            for (int i = 1; NodeBaseDAO.getInstance().testItemExistence(
                    session, trashUuid, testName); i++) {
                // log.info("Trying with: {}", testName);
                testName = fileName + " (" + i + ")." + fileExtension;
            }

            nDoc.setContext(nTrashFld.getContext());
            nDoc.setParent(trashUuid);
            nDoc.setName(testName);
            session.update(nDoc);
            HibernateUtil.commit(tx);
            log.debug("delete: void");
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final AccessDeniedException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final LockException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Checkout
     */
    public void checkout(final String user, final String uuid)
            throws PathNotFoundException, AccessDeniedException, LockException,
            DatabaseException {
        log.debug("checkout({})", uuid);
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            SecurityHelper.checkRead(nDoc);
            SecurityHelper.checkWrite(nDoc);

            nDoc.setCheckedOut(true);
            lock(session, user, nDoc);
            session.update(nDoc);
            HibernateUtil.commit(tx);
            log.debug("checkout: void");
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final AccessDeniedException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final LockException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Cancel checkout
     */
    public void cancelCheckout(final String user, final String uuid,
            final boolean force) throws PathNotFoundException,
            AccessDeniedException, LockException, DatabaseException {
        log.debug("cancelCheckout({}, {}, {})", new Object[] { user, uuid,
                force });
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            SecurityHelper.checkRead(nDoc);
            SecurityHelper.checkWrite(nDoc);

            nDoc.setCheckedOut(false);
            unlock(session, user, nDoc, force);
            session.update(nDoc);
            HibernateUtil.commit(tx);
            log.debug("cancelCheckout: void");
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final AccessDeniedException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final LockException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Test for checked out status
     */
    public boolean isCheckedOut(final String uuid)
            throws PathNotFoundException, DatabaseException {
        log.debug("isCheckedOut({})", uuid);
        Session session = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // Security Check
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            SecurityHelper.checkRead(nDoc);

            final boolean checkedOut = nDoc.isCheckedOut();
            log.debug("isCheckedOut: {}", checkedOut);
            return checkedOut;
        } catch (final PathNotFoundException e) {
            throw e;
        } catch (final HibernateException e) {
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Purge in depth
     */
    public void purge(final String uuid) throws PathNotFoundException,
            AccessDeniedException, LockException, DatabaseException,
            IOException {
        log.debug("purge({})", uuid);
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            purgeHelper(session, nDoc);
            HibernateUtil.commit(tx);
            log.debug("purge: void");
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final AccessDeniedException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final LockException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final IOException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Purge in depth helper
     * 
     * @see com.openkm.dao.NodeFolderDAO.purgeHelper(Session, NodeFolder, boolean)
     * @see com.openkm.dao.NodeMailDAO.purgeHelper(Session, NodeMail)
     */
    @SuppressWarnings("unchecked")
    public void purgeHelper(final Session session, final String parentUuid)
            throws PathNotFoundException, AccessDeniedException, LockException,
            IOException, DatabaseException, HibernateException {
        final String qs = "from NodeDocument nd where nd.parent=:parent";
        final Query q = session.createQuery(qs);
        q.setString("parent", parentUuid);
        final List<NodeDocument> listAttachments = q.list();

        for (final NodeDocument nDocument : listAttachments) {
            purgeHelper(session, nDocument);
        }
    }

    /**
     * Purge in depth helper
     */
    private void purgeHelper(final Session session, final NodeDocument nDocument)
            throws PathNotFoundException, AccessDeniedException, LockException,
            IOException, DatabaseException, HibernateException {
        final String path = NodeBaseDAO.getInstance().getPathFromUuid(session,
                nDocument.getUuid());
        final String user = PrincipalUtils.getUser();
        final String author = nDocument.getAuthor();

        // Security Check
        SecurityHelper.checkRead(nDocument);
        SecurityHelper.checkDelete(nDocument);

        // Lock Check
        LockHelper.checkWriteLock(nDocument);

        // Remove pdf & preview from cache
        CommonGeneralModule.cleanPreviewCache(nDocument.getUuid());

        // Delete children document versions
        NodeDocumentVersionDAO.getInstance().purgeHelper(session,
                nDocument.getUuid());

        // Delete children notes
        NodeNoteDAO.getInstance().purgeHelper(session, nDocument.getUuid());

        // Delete bookmarks
        BookmarkDAO.purgeBookmarksByNode(nDocument.getUuid());

        // Delete the node itself
        session.delete(nDocument);

        // Update user items size
        if (Config.USER_ITEM_CACHE) {
            UserItemsManager.decDocuments(author, 1);
        }

        // Activity log
        UserActivity.log(user, "PURGE_DOCUMENT", nDocument.getUuid(), path,
                null);
    }

    /**
     * Lock node
     */
    public NodeLock lock(final String user, final String uuid)
            throws PathNotFoundException, AccessDeniedException, LockException,
            DatabaseException {
        log.debug("lock({}, {})", user, uuid);
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            SecurityHelper.checkRead(nDoc);
            SecurityHelper.checkWrite(nDoc);

            lock(session, user, nDoc);
            session.update(nDoc);
            HibernateUtil.commit(tx);
            log.debug("lock: {}", nDoc.getLock());
            return nDoc.getLock();
        } catch (final LockException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Lock node
     */
    public void lock(final Session session, final String user,
            final NodeDocument nDoc) throws HibernateException, LockException {
        if (!nDoc.isLocked()) {
            final String token = JCRUtils.getLockToken(nDoc.getUuid());
            final NodeLock nLock = new NodeLock();
            nLock.setToken(token);
            nLock.setOwner(user);
            nLock.setCreated(Calendar.getInstance());
            nDoc.setLock(nLock);
            nDoc.setLocked(true);
        } else {
            throw new LockException("Node already locked");
        }
    }

    /**
     * Unlock node
     */
    public void unlock(final String user, final String uuid, final boolean force)
            throws PathNotFoundException, AccessDeniedException,
            DatabaseException, LockException {
        log.debug("unlock({}, {}, {})", new Object[] { user, uuid, force });
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Security Check
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            SecurityHelper.checkRead(nDoc);
            SecurityHelper.checkWrite(nDoc);

            unlock(session, user, nDoc, force);
            session.update(nDoc);
            HibernateUtil.commit(tx);
            log.debug("unlock: void");
        } catch (final LockException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Unlock node
     */
    public void unlock(final Session session, final String user,
            final NodeDocument nDoc, final boolean force)
            throws HibernateException, LockException {
        if (nDoc.isLocked()) {
            if (force || user.equals(nDoc.getLock().getOwner())) {
                nDoc.setLock(null);
                nDoc.setLocked(false);
            } else {
                throw new LockException("Node not locked by user");
            }
        } else {
            throw new LockException("Node not locked");
        }
    }

    /**
     * Test for locked status
     */
    public boolean isLocked(final String uuid) throws PathNotFoundException,
            DatabaseException {
        log.debug("isLocked({})", uuid);
        Session session = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // Security Check
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            SecurityHelper.checkRead(nDoc);

            final boolean locked = nDoc.isLocked();
            log.debug("isLocked: {}", locked);
            return locked;
        } catch (final HibernateException e) {
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Test for locked status
     */
    public NodeLock getLock(final String uuid) throws PathNotFoundException,
            LockException, DatabaseException {
        log.debug("getLock({})", uuid);
        Session session = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // Security Check
            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, uuid);
            SecurityHelper.checkRead(nDoc);

            final NodeLock nLock = nDoc.getLock();

            if (nLock == null) {
                throw new LockException("Node not locked: " + uuid);
            }

            log.debug("getLock: {}", nLock);
            return nLock;
        } catch (final HibernateException e) {
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Get document node size.
     * 
     * @see com.openkm.module.nr.NrStatsModule
     */
    public long getSize(final String context) throws PathNotFoundException,
            DatabaseException {
        log.debug("getSize({})", context);
        final String qs = "select coalesce(sum(ndv.size), 0) from NodeDocumentVersion ndv "
                + "where ndv.current = :current and ndv.parent in "
                + "(select nd.uuid from NodeDocument nd where nd.context = :context)";
        Session session = null;
        Transaction tx = null;
        long total = 0;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            final Query q = session.createQuery(qs);
            q.setBoolean("current", true);
            q.setString("context", PathUtils.fixContext(context));
            total = (Long) q.setMaxResults(1).uniqueResult();

            HibernateUtil.commit(tx);
            log.debug("getSize: {}", total);
            return total;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Get document node size.
     * 
     * @see com.openkm.module.nr.NrStatsModule
     */
    public long getSubtreeSize(final String path) throws PathNotFoundException,
            DatabaseException {
        log.debug("getSubtreeSize({})", path);
        Session session = null;
        Transaction tx = null;
        long total = 0;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            final String uuid = NodeBaseDAO.getInstance().getUuidFromPath(path);
            total = getSubtreeSizeHelper(session, uuid);

            HibernateUtil.commit(tx);
            log.debug("getSubtreeSize: {}", total);
            return total;
        } catch (final DatabaseException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw e;
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Helper method.
     */
    @SuppressWarnings("unchecked")
    private long getSubtreeSizeHelper(final Session session,
            final String parentUuid) throws HibernateException,
            DatabaseException {
        log.debug("getSubtreeSizeHelper({})", new Object[] { parentUuid });
        final String qs = "from NodeBase n where n.parent=:parent";
        final Query q = session.createQuery(qs);
        q.setString("parent", parentUuid);
        final List<NodeBase> nodes = q.list();
        long total = 0;

        for (final NodeBase nBase : nodes) {
            if (nBase instanceof NodeFolder) {
                total += getSubtreeSizeHelper(session, nBase.getUuid());
            } else if (nBase instanceof NodeDocument) {
                final NodeDocumentVersion nDocVer = NodeDocumentVersionDAO
                        .getInstance().findCurrentVersion(session,
                                nBase.getUuid());
                total += nDocVer.getSize();
            }
        }

        return total;
    }

    /**
     * Clear pending extraction queue
     */
    public int resetAllPendingExtractionFlags() throws DatabaseException {
        log.debug("resetAllPendingExtractionFlags()");
        final String qs = "update NodeDocument nd set nd.textExtracted=:extracted";
        Session session = null;
        Transaction tx = null;
        int rowCount = 0;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            final Query q = session.createQuery(qs);
            q.setBoolean("extracted", false);
            rowCount = q.executeUpdate();

            HibernateUtil.commit(tx);
            log.debug("resetAllPendingExtractionFlags: {}", rowCount);
            return rowCount;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Clear pending extraction queue
     */
    public int resetPendingExtractionFlag(final String docUuid)
            throws DatabaseException {
        log.debug("resetPendingExtractionFlag({})", docUuid);
        final String qs = "update NodeDocument nd set nd.textExtracted=:extracted where nd.uuid=:uuid";
        Session session = null;
        Transaction tx = null;
        int rowCount = 0;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            final Query q = session.createQuery(qs);
            q.setBoolean("extracted", false);
            q.setString("uuid", docUuid);
            rowCount = q.executeUpdate();

            HibernateUtil.commit(tx);
            log.debug("resetPendingExtractionFlag: {}", rowCount);
            return rowCount;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Check for extraction queue
     */
    public boolean hasPendingExtractions() throws DatabaseException {
        log.debug("hasPendingExtractions()");
        final String qs = "from NodeDocument nd where nd.textExtracted=:extracted";
        Session session = null;
        Transaction tx = null;
        boolean ret = false;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            final Query q = session.createQuery(qs);
            q.setBoolean("extracted", false);
            ret = q.iterate().hasNext();

            HibernateUtil.commit(tx);
            log.debug("hasPendingExtractions: {}", ret);
            return ret;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Get pending extraction queue
     */
    @SuppressWarnings("unchecked")
    public List<TextExtractorWork> getPendingExtractions(final int maxResults)
            throws DatabaseException {
        log.debug("getPendingExtractions({})", maxResults);
        final String qsDoc = "select nd.uuid from NodeDocument nd where nd.textExtracted=:extracted";
        final String qsDocVer = "from NodeDocumentVersion ndv where ndv.parent=:parent and ndv.current=:current";
        Session session = null;
        Transaction tx = null;
        final List<TextExtractorWork> ret = new ArrayList<TextExtractorWork>();

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            final Query qDoc = session.createQuery(qsDoc);
            qDoc.setBoolean("extracted", false);
            qDoc.setMaxResults(maxResults);

            for (final String docUuid : (List<String>) qDoc.list()) {
                final Query qDocVer = session.createQuery(qsDocVer);
                qDocVer.setString("parent", docUuid);
                qDocVer.setBoolean("current", true);
                final NodeDocumentVersion nDocVer = (NodeDocumentVersion) qDocVer
                        .uniqueResult();
                final String docPath = NodeBaseDAO.getInstance()
                        .getPathFromUuid(session, docUuid);

                final TextExtractorWork work = new TextExtractorWork();
                work.setDocUuid(docUuid);
                work.setDocPath(docPath);
                work.setDocVerUuid(nDocVer.getUuid());
                work.setDate(nDocVer.getCreated());
                ret.add(work);
            }

            HibernateUtil.commit(tx);
            log.debug("getPendingExtractions: {}", ret);
            return ret;
        } catch (final PathNotFoundException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Get pending extraction size
     */
    public long getPendingExtractionSize() throws DatabaseException {
        log.debug("getPendingExtractionSize()");
        final String qs = "select coalesce(count(*), 0) from NodeDocument nd where nd.textExtracted=:extracted";
        Session session = null;
        Transaction tx = null;
        long total = 0;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            final Query q = session.createQuery(qs);
            q.setBoolean("extracted", false);
            total = (Long) q.setMaxResults(1).uniqueResult();

            HibernateUtil.commit(tx);
            log.debug("getPendingExtractionSize: {}", total);
            return total;
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Helps on extracting text from documents
     */
    public void textExtractorHelper(final TextExtractorWork work)
            throws DatabaseException, FileNotFoundException {
        log.debug("textExtractorHelper({})", work);
        Session session = null;
        Transaction tx = null;
        InputStream isContent = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            if (FsDataStore.DATASTORE_BACKEND_FS
                    .equals(Config.REPOSITORY_DATASTORE_BACKEND)) {
                isContent = FsDataStore.read(work.getDocVerUuid());
            } else {
                final NodeDocumentVersion nDocVer = (NodeDocumentVersion) session
                        .load(NodeDocumentVersion.class, work.getDocVerUuid());
                isContent = new ByteArrayInputStream(nDocVer.getContent());
            }

            final NodeDocument nDoc = (NodeDocument) session.load(
                    NodeDocument.class, work.getDocUuid());

            try {
                // AUTOMATION - PRE
                final Map<String, Object> env = new HashMap<String, Object>();
                env.put(AutomationUtils.DOCUMENT_NODE, nDoc);
                AutomationManager.getInstance().fireEvent(
                        AutomationRule.EVENT_TEXT_EXTRACTOR,
                        AutomationRule.AT_PRE, env);

                String textExtracted = RegisteredExtractors.getText(
                        work.getDocPath(), nDoc.getMimeType(), null, isContent);

                // AUTOMATION - POST
                env.put(AutomationUtils.TEXT_EXTRACTED, textExtracted);
                AutomationManager.getInstance().fireEvent(
                        AutomationRule.EVENT_TEXT_EXTRACTOR,
                        AutomationRule.AT_POST, env);
                textExtracted = (String) env
                        .get(AutomationUtils.TEXT_EXTRACTED);

                // Need to replace 0x00 because PostgreSQL does not accept string containing 0x00
                textExtracted = FormatUtil.fixUTF8(textExtracted);

                // Need to remove Unicode surrogate because of MySQL => SQL Error: 1366, SQLState: HY000
                textExtracted = FormatUtil.trimUnicodeSurrogates(textExtracted);

                nDoc.setText(textExtracted);

                try {
                    final Detector lt = DetectorFactory.create();
                    lt.append(textExtracted);
                    nDoc.setLanguage(lt.detect());
                } catch (final LangDetectException e) {
                    log.warn("Language detection problem: {}", e.getMessage(),
                            e);
                }
            } catch (final Exception e) {
                try {
                    final String docPath = NodeBaseDAO.getInstance()
                            .getPathFromUuid(nDoc.getUuid());
                    log.warn(
                            "There was a problem extracting text from '{}': {}",
                            docPath, e.getMessage());
                    UserActivity.log(Config.SYSTEM_USER,
                            "MISC_TEXT_EXTRACTION_FAILURE", nDoc.getUuid(),
                            docPath, e.getMessage());
                } catch (final PathNotFoundException pnfe) {
                    log.warn("Item not found: {}", nDoc.getUuid());
                }
            }

            nDoc.setTextExtracted(true);
            session.update(nDoc);
            HibernateUtil.commit(tx);
            log.debug("textExtractorHelper: void");
        } catch (final HibernateException e) {
            HibernateUtil.rollback(tx);
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(isContent);
            HibernateUtil.close(session);
        }

        log.debug("textExtractorHelper: void");
    }

    /**
     * Get extracted text.
     */
    public String getExtractedText(final Session session, final String uuid)
            throws PathNotFoundException, DatabaseException {
        // Security Check
        final NodeDocument nDoc = (NodeDocument) session.load(
                NodeDocument.class, uuid);
        SecurityHelper.checkRead(nDoc);

        return nDoc.getText();
    }

    /**
     * Force initialization of a proxy
     */
    public void initialize(final NodeDocument nDocument,
            final boolean initPropGroups) {
        if (nDocument != null) {
            Hibernate.initialize(nDocument);
            Hibernate.initialize(nDocument.getKeywords());
            Hibernate.initialize(nDocument.getCategories());
            Hibernate.initialize(nDocument.getSubscriptors());
            Hibernate.initialize(nDocument.getUserPermissions());
            Hibernate.initialize(nDocument.getRolePermissions());

            if (initPropGroups) {
                Hibernate.initialize(nDocument.getProperties());
            }
        }
    }

    /**
     * Force initialization of a proxy
     */
    private void initialize(final List<NodeDocument> nDocumentList) {
        for (final NodeDocument nDocument : nDocumentList) {
            initialize(nDocument, false);
        }
    }
}