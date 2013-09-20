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

package com.openkm.extension.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.api.OKMDocument;
import com.openkm.api.OKMFolder;
import com.openkm.api.OKMRepository;
import com.openkm.bean.Document;
import com.openkm.bean.Folder;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.util.EnvironmentDetector;
import com.openkm.util.PathUtils;
import com.openkm.util.UserActivity;
import com.openkm.util.WebUtils;

/**
 * Data browser servlet
 */
public class DataBrowserServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory
            .getLogger(DataBrowserServlet.class);

    private static final String SEL_BOTH = "both";

    private static final String SEL_FOLDER = "fld";

    private static final String SEL_DOCUMENT = "doc";

    @Override
    public void service(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException,
            ServletException {
        final String method = request.getMethod();

        if (method.equals(METHOD_GET)) {
            doGet(request, response);
        } else if (method.equals(METHOD_POST)) {
            doPost(request, response);
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException {
        request.setCharacterEncoding("UTF-8");
        final String action = WebUtils.getString(request, "action");
        updateSessionManager(request);

        try {
            if (action.equals("fs")) {
                fileSystemList(request, response);
            } else if (action.equals("repo")) {
                repositoryList(request, response);
            }
        } catch (final PathNotFoundException e) {
            log.error(e.getMessage(), e);
            sendErrorRedirect(request, response, e);
        } catch (final RepositoryException e) {
            log.error(e.getMessage(), e);
            sendErrorRedirect(request, response, e);
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            sendErrorRedirect(request, response, e);
        }
    }

    /**
     * File system list
     */
    private void fileSystemList(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException,
            ServletException {
        log.debug("fileSystemList({}, {})", request, response);
        final String path = WebUtils.getString(request, "path",
                EnvironmentDetector.getUserHome());
        final String sel = WebUtils.getString(request, "sel", SEL_BOTH);
        final String dst = WebUtils.getString(request, "dst");
        final String root = WebUtils.getString(request, "root");
        final File dir = new File(
                path.isEmpty() ? EnvironmentDetector.getUserHome() : path);
        final List<Map<String, String>> folders = new ArrayList<Map<String, String>>();
        final List<Map<String, String>> documents = new ArrayList<Map<String, String>>();
        boolean browseParent = false;

        if (!root.equals("")) {
            browseParent = !root.equals(dir.getPath());
        } else {
            browseParent = !Arrays.asList(File.listRoots()).contains(dir);
        }

        // Add parent folder link
        if (browseParent) {
            final Map<String, String> item = new HashMap<String, String>();
            final File parent = dir.getParentFile();
            item.put("name", "&lt;PARENT FOLDER&gt;");
            item.put("path", fixPath(parent.getPath()));
            item.put("sel", "false");
            folders.add(item);
        }

        for (final File f : dir.listFiles()) {
            final Map<String, String> item = new HashMap<String, String>();

            if (f.isDirectory() && !f.isHidden()) {
                item.put("name", f.getName());
                item.put("path", fixPath(f.getPath()));

                if (sel.equals(SEL_BOTH) || sel.equals(SEL_FOLDER)) {
                    item.put("sel", "true");
                } else {
                    item.put("sel", "false");
                }

                folders.add(item);
            } else if (f.isFile() && !f.isHidden()
                    && (sel.equals(SEL_BOTH) || sel.equals(SEL_DOCUMENT))) {
                item.put("name", f.getName());
                item.put("path", fixPath(f.getPath()));
                item.put("sel", "true");
                documents.add(item);
            }
        }

        // Sort
        Collections.sort(folders, new MapComparator());
        Collections.sort(documents, new MapComparator());

        final ServletContext sc = getServletContext();
        sc.setAttribute("action", "fs");
        sc.setAttribute("path", path);
        sc.setAttribute("root", root);
        sc.setAttribute("dst", dst);
        sc.setAttribute("sel", sel);
        sc.setAttribute("folders", folders);
        sc.setAttribute("documents", documents);
        sc.getRequestDispatcher("/extension/data_browser.jsp").forward(request,
                response);

        // Activity log
        UserActivity.log(request.getRemoteUser(), "BROWSER_FILESYSTEM_LIST",
                null, path, null);
        log.debug("fileSystemList: void");
    }

    /**
     * Fix path when deployed in Windows
     */
    private String fixPath(final String path) {
        if (EnvironmentDetector.isWindows()) {
            return path.replace("\\", "/");
        } else {
            return path;
        }
    }

    /**
     * File system list
     */
    private void repositoryList(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException,
            ServletException, PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("repositoryList({}, {})", request, response);
        final Folder rootFolder = OKMRepository.getInstance().getRootFolder(
                null);
        final String path = WebUtils.getString(request, "path",
                rootFolder.getPath());
        final String sel = WebUtils.getString(request, "sel", SEL_BOTH);
        final String dst = WebUtils.getString(request, "dst");
        final String root = WebUtils.getString(request, "root", "/");
        final List<Map<String, String>> folders = new ArrayList<Map<String, String>>();
        final List<Map<String, String>> documents = new ArrayList<Map<String, String>>();

        if (!root.equals(path)) {
            // Add parent folder link
            final Map<String, String> item = new HashMap<String, String>();
            item.put("name", "&lt;PARENT FOLDER&gt;");
            item.put("path", PathUtils.getParent(path));
            item.put("sel", "false");
            folders.add(item);
        }

        for (final Folder fld : OKMFolder.getInstance().getChildren(null, path)) {
            final Map<String, String> item = new HashMap<String, String>();
            item.put("name", PathUtils.getName(fld.getPath()));
            item.put("path", fld.getPath());

            if (sel.equals(SEL_BOTH) || sel.equals(SEL_FOLDER)) {
                item.put("sel", "true");
            } else {
                item.put("sel", "false");
            }

            folders.add(item);
        }

        if (sel.equals(SEL_BOTH) || sel.equals(SEL_DOCUMENT)) {
            for (final Document doc : OKMDocument.getInstance().getChildren(
                    null, path)) {
                final Map<String, String> item = new HashMap<String, String>();
                item.put("name", PathUtils.getName(doc.getPath()));
                item.put("path", doc.getPath());
                item.put("sel", "true");
                documents.add(item);
            }
        }

        // Sort
        Collections.sort(folders, new MapComparator());
        Collections.sort(documents, new MapComparator());

        final ServletContext sc = getServletContext();
        sc.setAttribute("action", "repo");
        sc.setAttribute("path", path);
        sc.setAttribute("root", root);
        sc.setAttribute("dst", dst);
        sc.setAttribute("sel", sel);
        sc.setAttribute("folders", folders);
        sc.setAttribute("documents", documents);
        sc.getRequestDispatcher("/extension/data_browser.jsp").forward(request,
                response);

        // Activity log
        UserActivity.log(request.getRemoteUser(), "BROWSER_REPOSITORY_LIST",
                null, path, null);
        log.debug("repositoryList: void");
    }

    /**
     * Specialized comparator.
     */
    private class MapComparator implements Comparator<Map<String, String>> {

        @Override
        public int compare(final Map<String, String> o1,
                final Map<String, String> o2) {
            return o1.get("name").compareToIgnoreCase(o2.get("name"));
        }
    }
}
