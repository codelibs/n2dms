/**
 *  OpenKM, Open Document Management System (http://www.openkm.com)
 *  Copyright (c) 2006-2013  Paco Avila & Josep Llort
 *
 *  No bytes were intentionally harmed during the development of this application.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.openkm.module.jcr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.bean.AppVersion;
import com.openkm.bean.Document;
import com.openkm.bean.Folder;
import com.openkm.bean.Permission;
import com.openkm.bean.Property;
import com.openkm.bean.PropertyGroup;
import com.openkm.bean.Repository;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.module.RepositoryModule;
import com.openkm.module.jcr.base.BaseDocumentModule;
import com.openkm.module.jcr.base.BaseFolderModule;
import com.openkm.module.jcr.stuff.JCRUtils;
import com.openkm.module.jcr.stuff.JcrSessionManager;
import com.openkm.module.jcr.stuff.SystemSession;
import com.openkm.util.MailUtils;
import com.openkm.util.UserActivity;
import com.openkm.util.WarUtils;

public class JcrRepositoryModule implements RepositoryModule {
    private static Logger log = LoggerFactory
            .getLogger(JcrRepositoryModule.class);

    private static javax.jcr.Repository repository = null;

    private static Session systemSession = null;

    /**
     * Cache the repository information
     * 
     * @return The actual repository.
     * @throws javax.jcr.RepositoryException
     */
    public synchronized static javax.jcr.Repository getRepository()
            throws javax.jcr.RepositoryException {
        log.debug("getRepository()");
        WorkspaceConfig wc = null;

        if (repository == null) {
            // Repository configuration
            try {
                final RepositoryConfig config = getRepositoryConfig();
                wc = config
                        .getWorkspaceConfig(config.getDefaultWorkspaceName());
                repository = RepositoryImpl.create(config);
            } catch (final ConfigurationException e) {
                log.error(e.getMessage(), e);
                throw e;
            } catch (final javax.jcr.RepositoryException e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        }

        // Creation of a top access level SYSTEM. Needed by the AccessManager.
        if (systemSession == null) {
            // System User Session
            try {
                systemSession = SystemSession.create(
                        (RepositoryImpl) repository, wc);
            } catch (final LoginException e) {
                log.error(e.getMessage(), e);
                throw e;
            } catch (final NoSuchWorkspaceException e) {
                log.error(e.getMessage(), e);
                throw e;
            } catch (final javax.jcr.RepositoryException e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        }

        log.debug("getRepository: " + repository);
        return repository;
    }

    /**
     * Obtain repository configuration
     */
    public static RepositoryConfig getRepositoryConfig()
            throws ConfigurationException {
        final String repConfig = Config.REPOSITORY_CONFIG;
        String repHome = null;

        // Allow absolute repository path
        if (new File(Config.REPOSITORY_HOME).isAbsolute()) {
            repHome = Config.REPOSITORY_HOME;
        } else {
            repHome = Config.HOME_DIR + File.separator + Config.REPOSITORY_HOME;
        }

        return RepositoryConfig.create(repConfig, repHome);
    }

    /**
     * Close repository and free the lock 
     */
    public synchronized static void shutdown() {
        log.debug("shutdownRepository()");

        if (systemSession != null && systemSession.isLive()) {
            systemSession.logout();
        }

        systemSession = null;
        ((RepositoryImpl) repository).shutdown();
        repository = null;
        log.debug("shutdownRepository: void");
    }

    /**
     * Get the System User Session to perform unsecured operations.
     * 
     * @return The System User Session.
     */
    public static Session getSystemSession() {
        log.debug("getSystemSession()");

        if (systemSession != null) {
            log.debug("systemSession.isLive() = " + systemSession.isLive());
            log.debug("systemSession.getUserID() = "
                    + systemSession.getUserID());

            try {
                log.debug("systemSession.hasPendingChanges() = "
                        + systemSession.hasPendingChanges());
            } catch (final javax.jcr.RepositoryException e) {
                log.error("# MKK-1 # MKK-1 # MKK-1 # MKK-1 # MKK-1 # MKK-1 # MKK-1 #");
                log.error(e.getMessage(), e);
                log.error("# MKK-1 # MKK-1 # MKK-1 # MKK-1 # MKK-1 # MKK-1 # MKK-1 #");
            }
        } else {
            log.error("# MKK-2 # MKK-2 # MKK-2 # MKK-2 # MKK-2 # MKK-2 # MKK-2 #");
            log.error("systemSession is NULL");
            log.error("# MKK-2 # MKK-2 # MKK-2 # MKK-2 # MKK-2 # MKK-2 # MKK-2 #");
        }

        log.debug("getSystemSession: {}", systemSession);
        return systemSession;
    }

    /**
     * Initialize the repository.
     * 
     * @return The root path of the initialized repository.
     * @throws AccessDeniedException If there is any security problem: you can't access the parent
     * document folder because of lack of permissions.
     * @throws RepositoryException If there is any general repository problem.
     */
    public synchronized static String initialize()
            throws javax.jcr.RepositoryException, FileNotFoundException,
            InvalidNodeTypeDefException, ParseException, DatabaseException {
        log.debug("initialize()");

        // Initializes Repository and SystemSession
        getRepository();
        final Session systemSession = getSystemSession();
        final String okmRootPath = create(systemSession);

        // Store system session token 
        JcrAuthModule.loadUserData(systemSession);
        JcrSessionManager.getInstance().putSystemSession(systemSession);
        log.debug("*** System user created {}", systemSession.getUserID());

        log.debug("initialize: {}", okmRootPath);
        return okmRootPath;
    }

    /**
     * Create OpenKM repository structure
     */
    public synchronized static String create(final Session session)
            throws javax.jcr.RepositoryException, FileNotFoundException,
            InvalidNodeTypeDefException, ParseException {
        String okmRootPath = null;
        Node rootNode = null;

        try {
            rootNode = session.getRootNode().getNode(Repository.ROOT);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.info("No {} node found", Repository.ROOT);
        } catch (final javax.jcr.RepositoryException e) {
            log.info("No {} node found", Repository.ROOT);
        }

        try {
            if (rootNode == null) {
                log.info("Repository creation");

                // Register namespaces
                log.info("Register namespace");
                final Workspace ws = session.getWorkspace();
                final NamespaceRegistry nsr = ws.getNamespaceRegistry();
                nsr.registerNamespace(Repository.OKM, Repository.OKM_URI);
                nsr.registerNamespace(PropertyGroup.GROUP,
                        PropertyGroup.GROUP_URI);
                nsr.registerNamespace(PropertyGroup.GROUP_PROPERTY,
                        PropertyGroup.GROUP_PROPERTY_URI);

                // Register custom node types from resources
                log.info("Register custom node types");
                final InputStream is = JcrRepositoryModule.class
                        .getResourceAsStream(Config.NODE_DEFINITIONS);

                if (is != null) {
                    registerCustomNodeTypes(session, is);
                } else {
                    final String msg = "Configuration error: "
                            + Config.NODE_DEFINITIONS + " not found";
                    log.debug(msg);
                    throw new javax.jcr.RepositoryException(msg);
                }

                final Node root = session.getRootNode();

                // Create okm:root
                log.info("Create {}", Repository.ROOT);
                final Node okmRoot = createBase(session, root, Repository.ROOT);
                okmRootPath = okmRoot.getPath();

                // Create okm:thesaurus
                log.info("Create {}", Repository.THESAURUS);
                createBase(session, root, Repository.THESAURUS);

                // Create okm:categories
                log.info("Create {}", Repository.CATEGORIES);
                createBase(session, root, Repository.CATEGORIES);

                // Create okm:templates
                log.info("Create {}", Repository.TEMPLATES);
                createBase(session, root, Repository.TEMPLATES);

                // Create okm:personal
                log.info("Create {}", Repository.PERSONAL);
                createBase(session, root, Repository.PERSONAL);

                // Create okm:mail
                log.info("Create {}", Repository.MAIL);
                createBase(session, root, Repository.MAIL);

                // Create okm:trash
                log.info("Create {}", Repository.TRASH);
                createBase(session, root, Repository.TRASH);

                // Create okm:config
                log.info("Create okm:config");
                final Node okmConfig = root.addNode(Repository.SYS_CONFIG,
                        Repository.SYS_CONFIG_TYPE);

                // Generate installation UUID
                final String uuid = UUID.randomUUID().toString();
                okmConfig.setProperty(Repository.SYS_CONFIG_UUID, uuid);
                Repository.setUuid(uuid);

                // Set repository version
                okmConfig.setProperty(Repository.SYS_CONFIG_VERSION, WarUtils
                        .getAppVersion().getMajor());

                root.save();
            } else {
                log.info("Repository already created");
                final Node root = session.getRootNode();
                final Node okmConfig = root.getNode(Repository.SYS_CONFIG);

                // Get installation UUID
                final String uuid = okmConfig.getProperty(
                        Repository.SYS_CONFIG_UUID).getString();
                Repository.setUuid(uuid);

                // Test repository version
                final String repoVer = okmConfig.getProperty(
                        Repository.SYS_CONFIG_VERSION).getString();

                if (!WarUtils.getAppVersion().getMajor().equals(repoVer)) {
                    log.warn("### Actual repository version (" + repoVer
                            + ") differs from application repository version ("
                            + WarUtils.getAppVersion().getMajor() + ") ###");
                    log.warn("### You should upgrade the repository ###");
                }
            }
        } catch (final NamespaceException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (final FileNotFoundException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (final InvalidNodeTypeDefException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (final ParseException e) {
            log.error(e.getMessage(), e);
            throw e;
        }

        return okmRootPath;
    }

    /**
     * Create base node
     */
    private static Node createBase(final Session session, final Node root,
            final String name) throws ItemExistsException,
            javax.jcr.PathNotFoundException, NoSuchNodeTypeException,
            LockException, VersionException, ConstraintViolationException,
            javax.jcr.RepositoryException {
        final Node base = root.addNode(name, Folder.TYPE);

        // Add basic properties
        base.setProperty(Folder.AUTHOR, session.getUserID());
        base.setProperty(Folder.NAME, name);
        base.setProperty(Property.KEYWORDS, new String[] {});
        base.setProperty(Property.CATEGORIES, new String[] {},
                PropertyType.REFERENCE);

        // Auth info
        base.setProperty(Permission.USERS_READ,
                new String[] { session.getUserID() });
        base.setProperty(Permission.USERS_WRITE,
                new String[] { session.getUserID() });
        base.setProperty(Permission.USERS_DELETE,
                new String[] { session.getUserID() });
        base.setProperty(Permission.USERS_SECURITY,
                new String[] { session.getUserID() });
        base.setProperty(Permission.ROLES_READ,
                new String[] { Config.DEFAULT_USER_ROLE });
        base.setProperty(Permission.ROLES_WRITE,
                new String[] { Config.DEFAULT_USER_ROLE });
        base.setProperty(Permission.ROLES_DELETE,
                new String[] { Config.DEFAULT_USER_ROLE });
        base.setProperty(Permission.ROLES_SECURITY,
                new String[] { Config.DEFAULT_USER_ROLE });

        return base;
    }

    /**
     * Remove a repository from disk.
     * 
     * @throws AccessDeniedException If there is any security problem: you can't access the parent
     * document folder because of lack of permissions. 
     * @throws RepositoryException If there is any general repository problem.
     */
    public synchronized void remove() throws RepositoryException {
        log.debug("remove()");
        String repHome = null;

        // Allow absolute repository path
        if (new File(Config.REPOSITORY_HOME).isAbsolute()) {
            repHome = Config.REPOSITORY_HOME;
        } else {
            repHome = Config.HOME_DIR + File.separator + Config.REPOSITORY_HOME;
        }

        try {
            FileUtils.deleteDirectory(new File(repHome));
        } catch (final IOException e) {
            System.err.println("No previous repository found");
        }

        log.debug("create: void");
    }

    @Override
    public Folder getRootFolder(final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getRootFolder({})", token);
        Folder rootFolder = new Folder();
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            final Node rootNode = session.getRootNode()
                    .getNode(Repository.ROOT);
            rootFolder = BaseFolderModule.getProperties(session, rootNode);

            // Activity log
            UserActivity.log(session.getUserID(), "GET_ROOT_FOLDER",
                    rootNode.getUUID(), rootFolder.getPath(), null);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getRootFolder: {}", rootFolder);
        return rootFolder;
    }

    @Override
    public Folder getTrashFolder(final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getTrashFolder({})", token);
        Folder trashFolder = new Folder();
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            final Node trashNode = session.getRootNode().getNode(
                    Repository.TRASH + "/" + session.getUserID());
            trashFolder = BaseFolderModule.getProperties(session, trashNode);

            // Activity log
            UserActivity.log(session.getUserID(), "GET_TRASH_FOLDER",
                    trashNode.getUUID(), trashFolder.getPath(), null);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getTrashFolder: {}", trashFolder);
        return trashFolder;
    }

    @Override
    public Folder getTrashFolderBase(final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getTrashFolderBase({})", token);
        Folder trashFolder = new Folder();
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            final Node trashNode = session.getRootNode().getNode(
                    Repository.TRASH);
            trashFolder = BaseFolderModule.getProperties(session, trashNode);

            // Activity log
            UserActivity.log(session.getUserID(), "GET_TRASH_FOLDER_BASE",
                    trashNode.getUUID(), trashFolder.getPath(), null);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getTrashFolderBase: {}", trashFolder);
        return trashFolder;
    }

    @Override
    public Folder getTemplatesFolder(final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getTemplatesFolder({})", token);
        Folder templatesFolder = new Folder();
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            final Node templatesNode = session.getRootNode().getNode(
                    Repository.TEMPLATES);
            templatesFolder = BaseFolderModule.getProperties(session,
                    templatesNode);

            // Activity log
            UserActivity.log(session.getUserID(), "GET_TEMPLATES_FOLDER",
                    templatesNode.getUUID(), templatesFolder.getPath(), null);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getTemplatesFolder: {}", templatesFolder);
        return templatesFolder;
    }

    @Override
    public Folder getPersonalFolder(final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getPersonalFolder({})", token);
        Folder personalFolder = new Folder();
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            final Node personalNode = session.getRootNode().getNode(
                    Repository.PERSONAL + "/" + session.getUserID());
            personalFolder = BaseFolderModule.getProperties(session,
                    personalNode);

            // Activity log
            UserActivity.log(session.getUserID(), "GET_PERSONAL_FOLDER",
                    personalNode.getUUID(), personalFolder.getPath(), null);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getPersonalFolder: {}", personalFolder);
        return personalFolder;
    }

    @Override
    public Folder getPersonalFolderBase(final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getPersonalFolderBase({})", token);
        Folder personalFolder = new Folder();
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            final Node personalNode = session.getRootNode().getNode(
                    Repository.PERSONAL);
            personalFolder = BaseFolderModule.getProperties(session,
                    personalNode);

            // Activity log
            UserActivity.log(session.getUserID(), "GET_PERSONAL_FOLDER_BASE",
                    personalNode.getUUID(), personalFolder.getPath(), null);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getPersonalFolderBase: {}", personalFolder);
        return personalFolder;
    }

    @Override
    public Folder getMailFolder(final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getMailFolder({})", token);
        Folder mailFolder = new Folder();
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            final String mailPath = MailUtils.getUserMailPath(session
                    .getUserID());
            final Node mailNode = session.getRootNode().getNode(
                    mailPath.substring(1));
            mailFolder = BaseFolderModule.getProperties(session, mailNode);

            // Activity log
            UserActivity.log(session.getUserID(), "GET_MAIL_FOLDER",
                    mailNode.getUUID(), mailFolder.getPath(), null);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getMailFolder: {}", mailFolder);
        return mailFolder;
    }

    @Override
    public Folder getMailFolderBase(final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getMailFolderBase({})", token);
        Folder mailFolder = new Folder();
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            final Node mailNode = session.getRootNode()
                    .getNode(Repository.MAIL);
            mailFolder = BaseFolderModule.getProperties(session, mailNode);

            // Activity log
            UserActivity.log(session.getUserID(), "GET_MAIL_FOLDER_BASE",
                    mailNode.getUUID(), mailFolder.getPath(), null);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getMailFolderBase: {}", mailFolder);
        return mailFolder;
    }

    @Override
    public Folder getThesaurusFolder(final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getThesaurusFolder({})", token);
        Folder thesaurusFolder = new Folder();
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            final Node thesaurusNode = session.getRootNode().getNode(
                    Repository.THESAURUS);
            thesaurusFolder = BaseFolderModule.getProperties(session,
                    thesaurusNode);

            // Activity log
            UserActivity.log(session.getUserID(), "GET_THESAURUS_FOLDER",
                    thesaurusNode.getUUID(), thesaurusFolder.getPath(), null);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getThesaurusFolder: {}", thesaurusFolder);
        return thesaurusFolder;
    }

    @Override
    public Folder getCategoriesFolder(final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getCategoriesFolder({})", token);
        Folder categoriesFolder = new Folder();
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            final Node categoriesNode = session.getRootNode().getNode(
                    Repository.CATEGORIES);
            categoriesFolder = BaseFolderModule.getProperties(session,
                    categoriesNode);

            // Activity log
            UserActivity.log(session.getUserID(), "GET_CATEGORIES_FOLDER",
                    categoriesNode.getUUID(), categoriesFolder.getPath(), null);
        } catch (final javax.jcr.PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getCategoriesFolder: {}", categoriesFolder);
        return categoriesFolder;
    }

    /**
     * Register custom node definition from file.
     *
     * TODO For Jackrabbit 2.0 should be done as:
     *   InputStream is = getClass().getClassLoader().getResourceAsStream("test.cnd");
     *   Reader cnd = new InputStreamReader(is);
     *   NodeType[] nodeTypes = CndImporter.registerNodeTypes(cnd, session);
     * 
     * The key method is:
     *   CndImporter.registerNodeTypes("cndfile", session);
     */
    @SuppressWarnings("unchecked")
    public synchronized static void registerCustomNodeTypes(
            final Session session, final InputStream cndFile)
            throws FileNotFoundException, ParseException,
            javax.jcr.RepositoryException, InvalidNodeTypeDefException {
        log.debug("registerCustomNodeTypes({}, {})", session, cndFile);

        // Read in the CND file
        final InputStreamReader fileReader = new InputStreamReader(cndFile);

        // Create a CompactNodeTypeDefReader
        final CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(
                fileReader, Config.NODE_DEFINITIONS);

        // Get the List of NodeTypeDef objects
        final List<NodeTypeDef> ntdList = cndReader.getNodeTypeDefs();

        // Get the NodeTypeManager from the Workspace.
        // Note that it must be cast from the generic JCR NodeTypeManager to the
        // Jackrabbit-specific implementation.
        final Workspace ws = session.getWorkspace();
        final NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) ws
                .getNodeTypeManager();

        // Acquire the NodeTypeRegistry
        final NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

        // Loop through the prepared NodeTypeDefs
        for (final NodeTypeDef ntd : ntdList) {
            // ...and register or reregister it
            if (!ntreg.isRegistered(ntd.getName())) {
                log.info("Register type " + ntd.getName().toString());
                ntreg.registerNodeType(ntd);
            } else {
                log.info("Reregister type " + ntd.getName().toString());
                ntreg.reregisterNodeType(ntd);
            }
        }

        log.debug("registerCustomNodeTypes: void");
    }

    @Override
    public void purgeTrash(final String token) throws AccessDeniedException,
            RepositoryException, DatabaseException {
        log.debug("purgeTrash({})", token);
        Node userTrash = null;
        Session session = null;

        if (Config.SYSTEM_READONLY) {
            throw new AccessDeniedException("System is in read-only mode");
        }

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            userTrash = session.getRootNode().getNode(
                    Repository.TRASH + "/" + session.getUserID());

            for (final NodeIterator it = userTrash.getNodes(); it.hasNext();) {
                final Node child = it.nextNode();

                if (child.isNodeType(Document.TYPE)) {
                    BaseDocumentModule.purge(session, child.getParent(), child);
                } else if (child.isNodeType(Folder.TYPE)) {
                    BaseFolderModule.purge(session, child);
                }
            }

            userTrash.save();

            // Activity log
            UserActivity.log(session.getUserID(), "PURGE_TRASH",
                    userTrash.getUUID(), userTrash.getPath(), null);
        } catch (final javax.jcr.AccessDeniedException e) {
            log.error(e.getMessage(), e);
            JCRUtils.discardsPendingChanges(userTrash);
            throw new AccessDeniedException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            JCRUtils.discardsPendingChanges(userTrash);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("purgeTrash: void");
    }

    @Override
    public String getUpdateMessage(final String token)
            throws RepositoryException {
        return Repository.getUpdateMsg();
    }

    @Override
    public String getRepositoryUuid(final String token)
            throws RepositoryException {
        return Repository.getUuid();
    }

    @Override
    public boolean hasNode(final String token, final String path)
            throws RepositoryException, DatabaseException {
        log.debug("hasNode({}, {})", token, path);
        boolean ret = false;
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            ret = session.getRootNode().hasNode(path.substring(1));
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("hasNode: {}", ret);
        return ret;
    }

    @Override
    public String getNodePath(final String token, final String uuid)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getNodePath({}, {})", token, uuid);
        String ret;
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            ret = session.getNodeByUUID(uuid).getPath();
        } catch (final javax.jcr.ItemNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getNodePath: {}", ret);
        return ret;
    }

    @Override
    public String getNodeUuid(final String token, final String path)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getNodeUuid({}, {})", token, path);
        String ret;
        Session session = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            ret = session.getRootNode().getNode(path.substring(1)).getUUID();
        } catch (final javax.jcr.ItemNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getNodeUuid: {}", ret);
        return ret;
    }

    @Override
    public AppVersion getAppVersion(final String token)
            throws RepositoryException, DatabaseException {
        log.debug("getAppVersion({})", token);
        Session session = null;
        AppVersion ret = null;

        try {
            if (token == null) {
                session = JCRUtils.getSession();
            } else {
                session = JcrSessionManager.getInstance().get(token);
            }

            ret = WarUtils.getAppVersion();
        } catch (final javax.jcr.RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token == null) {
                JCRUtils.logout(session);
            }
        }

        log.debug("getAppVersion: {}", ret);
        return ret;
    }
}
