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

package com.openkm.ws.endpoint;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.bean.AppVersion;
import com.openkm.bean.Folder;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.DatabaseException;
import com.openkm.core.LockException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.module.ModuleManager;
import com.openkm.module.RepositoryModule;

@WebService(name = "OKMRepository", serviceName = "OKMRepository", targetNamespace = "http://ws.openkm.com")
public class RepositoryService {
    private static Logger log = LoggerFactory
            .getLogger(RepositoryService.class);

    @WebMethod
    public Folder getRootFolder(@WebParam(name = "token") final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getRootFolder({})", token);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final Folder root = rm.getRootFolder(token);
        log.debug("getRootFolder: {}", root);
        return root;
    }

    @WebMethod
    public Folder getTrashFolder(@WebParam(name = "token") final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getTrashFolder({})", token);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final Folder trash = rm.getTrashFolder(token);
        log.debug("getTrashFolder: {}", trash);
        return trash;
    }

    @WebMethod
    public Folder getTemplatesFolder(
            @WebParam(name = "token") final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getTemplatesFolder({})", token);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final Folder templatesFolder = rm.getTemplatesFolder(token);
        log.debug("getTemplatesFolder: {}", templatesFolder);
        return templatesFolder;
    }

    @WebMethod
    public Folder getPersonalFolder(@WebParam(name = "token") final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getPersonalFolder({})", token);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final Folder personalFolder = rm.getPersonalFolder(token);
        log.debug("getPersonalFolder: {}", personalFolder);
        return personalFolder;
    }

    @WebMethod
    public Folder getMailFolder(@WebParam(name = "token") final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getMailFolder({})", token);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final Folder mailFolder = rm.getMailFolder(token);
        log.debug("getMailFolder: {}", mailFolder);
        return mailFolder;
    }

    @WebMethod
    public Folder getThesaurusFolder(
            @WebParam(name = "token") final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getThesaurusFolder({})", token);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final Folder thesaurusFolder = rm.getThesaurusFolder(token);
        log.debug("getThesaurusFolder: {}", thesaurusFolder);
        return thesaurusFolder;
    }

    @WebMethod
    public Folder getCategoriesFolder(
            @WebParam(name = "token") final String token)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getCategoriesFolder({})", token);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final Folder categoriesFolder = rm.getCategoriesFolder(token);
        log.debug("getCategoriesFolder: {}", categoriesFolder);
        return categoriesFolder;
    }

    @WebMethod
    public void purgeTrash(@WebParam(name = "token") final String token)
            throws PathNotFoundException, AccessDeniedException, LockException,
            RepositoryException, DatabaseException {
        log.debug("purgeTrash({})", token);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        rm.purgeTrash(token);
        log.debug("purgeTrash: void");
    }

    @WebMethod
    public boolean hasNode(@WebParam(name = "token") final String token,
            @WebParam(name = "path") final String path)
            throws RepositoryException, DatabaseException {
        log.debug("hasNode({}, {})", token, path);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final boolean ret = rm.hasNode(token, path);
        log.debug("hasNode: {}", ret);
        return ret;
    }

    @WebMethod
    public String getNodePath(@WebParam(name = "token") final String token,
            @WebParam(name = "uuid") final String uuid)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getNodePath({}, {})", token, uuid);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final String path = rm.getNodePath(token, uuid);
        log.debug("getNodePath: {}", path);
        return path;
    }

    @WebMethod
    public String getNodeUuid(@WebParam(name = "token") final String token,
            @WebParam(name = "path") final String path)
            throws PathNotFoundException, RepositoryException,
            DatabaseException {
        log.debug("getNodeUuid({}, {})", token, path);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final String uuid = rm.getNodeUuid(token, path);
        log.debug("getNodeUuid: {}", uuid);
        return uuid;
    }

    @WebMethod
    public AppVersion getAppVersion(@WebParam(name = "token") final String token)
            throws RepositoryException, DatabaseException {
        log.debug("getAppVersion({})", token);
        final RepositoryModule rm = ModuleManager.getRepositoryModule();
        final AppVersion appVer = rm.getAppVersion(token);
        log.debug("getAppVersion: {}", appVer);
        return appVer;
    }
}
