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

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.AccessDeniedException;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.module.ModuleManager;
import com.openkm.module.NotificationModule;
import com.openkm.principal.PrincipalAdapterException;

@WebService(name = "OKMNotification", serviceName = "OKMNotification", targetNamespace = "http://ws.openkm.com")
public class NotificationService {
    private static Logger log = LoggerFactory
            .getLogger(NotificationService.class);

    @WebMethod
    public void subscribe(@WebParam(name = "token") final String token,
            @WebParam(name = "nodePath") final String nodePath)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException, DatabaseException {
        log.debug("subscribe({}, {})", token, nodePath);
        final NotificationModule nm = ModuleManager.getNotificationModule();
        nm.subscribe(token, nodePath);
        log.debug("subscribe: void");
    }

    @WebMethod
    public void unsubscribe(@WebParam(name = "token") final String token,
            @WebParam(name = "nodePath") final String nodePath)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException, DatabaseException {
        log.debug("unsubscribe({}, {})", token, nodePath);
        final NotificationModule nm = ModuleManager.getNotificationModule();
        nm.unsubscribe(token, nodePath);
        log.debug("unsubscribe: void");
    }

    @WebMethod
    public String[] getSubscriptors(
            @WebParam(name = "token") final String token,
            @WebParam(name = "nodePath") final String nodePath)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException, DatabaseException {
        log.debug("getSubscriptors({}, {})", token, nodePath);
        final NotificationModule nm = ModuleManager.getNotificationModule();
        final Set<String> col = nm.getSubscriptors(token, nodePath);
        final String[] result = col.toArray(new String[col.size()]);
        log.debug("getSubscriptors: {}", result);
        return result;
    }

    @WebMethod
    public void notify(@WebParam(name = "token") final String token,
            @WebParam(name = "nodePath") final String nodePath,
            @WebParam(name = "users") final String[] users,
            @WebParam(name = "message") final String message,
            @WebParam(name = "attachment") final boolean attachment)
            throws PathNotFoundException, AccessDeniedException,
            PrincipalAdapterException, RepositoryException, DatabaseException,
            IOException {
        log.debug("notify({}, {}, {}, {}, {})", new Object[] { token, nodePath,
                users, message, attachment });
        final NotificationModule nm = ModuleManager.getNotificationModule();
        nm.notify(token, nodePath, Arrays.asList(users), message, attachment);
        log.debug("notify: void");
    }
}