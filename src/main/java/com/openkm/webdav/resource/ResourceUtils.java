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

package com.openkm.webdav.resource;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Resource;
import com.openkm.api.OKMDocument;
import com.openkm.api.OKMFolder;
import com.openkm.api.OKMMail;
import com.openkm.bean.Document;
import com.openkm.bean.Folder;
import com.openkm.bean.Mail;
import com.openkm.bean.Repository;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;

public class ResourceUtils {
    private static final Logger log = LoggerFactory
            .getLogger(ResourceUtils.class);

    /**
     * Resolve node resource (may be folder or document)
     */
    public static Resource getNode(final Path srcPath, final String path)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException, DatabaseException {
        log.debug("getNode({}, {})", srcPath, path);
        final String fixedPath = ResourceUtils.fixRepositoryPath(path);
        Resource res = null;

        try {
            if (OKMFolder.getInstance().isValid(null, fixedPath)) {
                if (path.startsWith(fixRepositoryPath("/"
                        + Repository.CATEGORIES))) {
                    // Is from categories
                    log.info("Path: {}", path);
                    res = getCategory(srcPath, path);
                } else {
                    res = getFolder(srcPath, path);
                }
            } else if (OKMDocument.getInstance().isValid(null, fixedPath)) {
                res = getDocument(path);
            } else if (OKMMail.getInstance().isValid(null, fixedPath)) {
                res = getMail(path);
            }
        } catch (final PathNotFoundException e) {
            log.warn("PathNotFoundException: {}", e.getMessage());
        }

        log.debug("getNode: {}", res);
        return res;
    }

    /**
     * Resolve folder resource.
     */
    private static Resource getFolder(final Path path, final String fldPath)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        final String fixedFldPath = fixRepositoryPath(fldPath);
        final Folder fld = OKMFolder.getInstance().getProperties(null,
                fixedFldPath);
        final List<Folder> fldChilds = OKMFolder.getInstance().getChildren(
                null, fixedFldPath);
        final List<Document> docChilds = OKMDocument.getInstance().getChildren(
                null, fixedFldPath);
        final List<Mail> mailChilds = OKMMail.getInstance().getChildren(null,
                fixedFldPath);
        final Resource fldResource = new FolderResource(path, fld, fldChilds,
                docChilds, mailChilds);

        return fldResource;
    }

    /**
     * Resolve document resource.
     */
    private static Resource getDocument(final String docPath)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        final String fixedDocPath = fixRepositoryPath(docPath);
        final Document doc = OKMDocument.getInstance().getProperties(null,
                fixedDocPath);
        final Resource docResource = new DocumentResource(doc);

        return docResource;
    }

    /**
     * Resolve mail resource.
     */
    private static Resource getMail(final String mailPath)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        final String fixedMailPath = fixRepositoryPath(mailPath);
        final Mail mail = OKMMail.getInstance().getProperties(null,
                fixedMailPath);
        final Resource docResource = new MailResource(mail);

        return docResource;
    }

    /**
     * Resolve category resource.
     */
    private static Resource getCategory(final Path path, final String catPath)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        final String fixedFldPath = fixRepositoryPath(catPath);
        final Folder cat = OKMFolder.getInstance().getProperties(null,
                fixedFldPath);
        final List<Folder> catChilds = OKMFolder.getInstance().getChildren(
                null, fixedFldPath);
        //String uuid = OKMFolder.getInstance().getProperties(null, fixedFldPath).getUuid();
        //List<Folder> fldChilds = OKMSearch.getInstance().getCategorizedFolders(null, uuid);
        //List<Document> docChilds = OKMSearch.getInstance().getCategorizedDocuments(null, uuid);
        //List<Mail> mailChilds = OKMSearch.getInstance().getCategorizedMails(null, uuid);

        // Fix node name
        //for (Folder fld : fldChilds) {
        //fld.setPath(fld.getPath() + "#" + fld.getUuid());
        //}

        //catChilds.addAll(fldChilds);
        final List<Document> docChilds = new ArrayList<Document>();
        final List<Mail> mailChilds = new ArrayList<Mail>();
        final Resource catResource = new CategoryResource(path, cat, catChilds,
                docChilds, mailChilds);

        return catResource;
    }

    /**
     * Create HTML content.
     */
    public static void createContent(final OutputStream out, final Path path,
            final List<Folder> fldChilds, final List<Document> docChilds,
            final List<Mail> mailChilds) {
        log.debug("createContent({}, {}, {}, {}, {})", new Object[] { out,
                path, fldChilds, docChilds, mailChilds });
        final PrintWriter pw = new PrintWriter(out);
        pw.println("<html>");
        pw.println("<header>");
        pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
        pw.println("<link rel=\"Shortcut icon\" href=\"/" + path.getFirst()
                + "/favicon.ico\" />");
        pw.println("<link rel=\"stylesheet\" href=\"/" + path.getFirst()
                + "/css/style.css\" type=\"text/css\" />");
        pw.println("<title>OpenKM WebDAV</title>");
        pw.println("</header>");
        pw.println("<body>");
        pw.println("<h1>OpenKM WebDAV</h1>");
        pw.println("<table>");

        if (!path.getStripFirst().getStripFirst().isRoot()) {
            final String url = path.getParent().toPath();
            pw.print("<tr>");
            pw.print("<td><img src='/" + path.getFirst()
                    + "/img/webdav/folder.png'/></td>");
            pw.print("<td><a href='" + url + "'>..</a></td>");
            pw.println("<tr>");
        }

        if (fldChilds != null) {
            for (final Folder fld : fldChilds) {
                final Path fldPath = Path.path(fld.getPath());
                final String url = path.toPath().concat("/")
                        .concat(fldPath.getName());
                pw.print("<tr>");
                pw.print("<td><img src='/" + path.getFirst()
                        + "/img/webdav/folder.png'/></td>");
                pw.print("<td><a href='" + url + "'>" + fldPath.getName()
                        + "</a></td>");
                pw.println("<tr>");
            }
        }

        if (docChilds != null) {
            for (final Document doc : docChilds) {
                final Path docPath = Path.path(doc.getPath());
                final String url = path.toPath().concat("/")
                        .concat(docPath.getName());
                pw.print("<tr>");
                pw.print("<td><img src='/" + path.getFirst() + "/mime/"
                        + doc.getMimeType() + "'/></td>");
                pw.print("<td><a href='" + url + "'>" + docPath.getName()
                        + "</a></td>");
                pw.println("<tr>");
            }
        }

        if (mailChilds != null) {
            for (final Mail mail : mailChilds) {
                final Path mailPath = Path.path(mail.getPath());
                final String url = path.toPath().concat("/")
                        .concat(mailPath.getName());
                pw.print("<tr>");

                if (mail.getAttachments().isEmpty()) {
                    pw.print("<td><img src='/" + path.getFirst()
                            + "/img/webdav/email.png'/></td>");
                } else {
                    pw.print("<td><img src='/" + path.getFirst()
                            + "/img/webdav/email_attach.png'/></td>");
                }

                pw.print("<td><a href='" + url + "'>" + mailPath.getName()
                        + "</a></td>");
                pw.println("<tr>");
            }
        }

        pw.println("</table>");
        pw.println("</body>");
        pw.println("</html>");
        pw.flush();
        pw.close();
    }

    /**
     * Correct webdav folder path
     */
    public static Folder fixResourcePath(final Folder fld) {
        if (Config.SYSTEM_WEBDAV_FIX) {
            fld.setPath(fixResourcePath(fld.getPath()));
        }

        return fld;
    }

    /**
     * Correct webdav document path
     */
    public static Document fixResourcePath(final Document doc) {
        if (Config.SYSTEM_WEBDAV_FIX) {
            doc.setPath(fixResourcePath(doc.getPath()));
        }

        return doc;
    }

    /**
     * Correct webdav mail path
     */
    public static Mail fixResourcePath(final Mail mail) {
        if (Config.SYSTEM_WEBDAV_FIX) {
            mail.setPath(fixResourcePath(mail.getPath()));
        }

        return mail;
    }

    /**
     * 
     */
    private static String fixResourcePath(final String path) {
        return path.replace("okm:", "okm_");
    }

    /**
     * 
     */
    public static String fixRepositoryPath(final String path) {
        if (Config.SYSTEM_WEBDAV_FIX) {
            return path.replace("okm_", "okm:");
        } else {
            return path;
        }
    }
}