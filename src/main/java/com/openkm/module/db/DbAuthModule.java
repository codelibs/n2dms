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

package com.openkm.module.db;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.openkm.bean.Permission;
import com.openkm.bean.Repository;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.dao.NodeFolderDAO;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.module.AuthModule;
import com.openkm.module.common.CommonAuthModule;
import com.openkm.module.db.stuff.DbSessionManager;
import com.openkm.principal.PrincipalAdapterException;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.GenericHolder;
import com.openkm.util.PathUtils;
import com.openkm.util.StackTraceUtils;
import com.openkm.util.UserActivity;

@Component
public class DbAuthModule implements AuthModule, ApplicationContextAware {
    private static Logger log = LoggerFactory.getLogger(DbAuthModule.class);

    private static ApplicationContext appCtx;

    @Override
    public void setApplicationContext(final ApplicationContext appCtx)
            throws BeansException {
        DbAuthModule.appCtx = appCtx;
    }

    @Override
    public void login() throws RepositoryException, DatabaseException {
        log.debug("login()");

        try {
            final Authentication auth = PrincipalUtils.getAuthentication();

            if (auth != null) {
                final String user = auth.getName();
                loadUserData(user);

                // Activity log
                // @see com.openkm.spring.LoggerListener
            } else {
                throw new RepositoryException("User not authenticated");
            }
        } catch (final DatabaseException e) {
            throw e;
        } catch (final PathNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getClass().getSimpleName() + ": "
                    + e.getMessage(), e);
        } catch (final AccessDeniedException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getClass().getSimpleName() + ": "
                    + e.getMessage(), e);
        } catch (final ItemExistsException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryException(e.getClass().getSimpleName() + ": "
                    + e.getMessage(), e);
        }

        log.debug("grantRole: void");
    }

    @Override
    public String login(final String user, final String password)
            throws AccessDeniedException, RepositoryException,
            DatabaseException {
        log.debug("login({}, {})", user, password);
        final String token = UUID.randomUUID().toString();

        try {
            if (Config.SYSTEM_MAINTENANCE) {
                throw new AccessDeniedException("System under maintenance");
            } else {
                GenericHolder.set(token);

                final AuthenticationManager authMgr = (AuthenticationManager) appCtx
                        .getBean("authenticationManager");
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        user, password);
                auth = authMgr.authenticate(auth);
                log.debug("Authentication: {}", auth);

                DbSessionManager.getInstance().add(token, auth);
                loadUserData(user);

                // Activity log
                // @see com.openkm.spring.LoggerListener
            }
        } catch (final AuthenticationException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        } catch (final Exception e) {
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            GenericHolder.unset();
        }

        log.debug("login: {}", token);
        return token;
    }

    @Override
    public void logout(final String token) throws RepositoryException,
            DatabaseException {
        log.debug("logout({})", token);
        Authentication auth = null, oldAuth = null;

        try {
            if (token == null) {
                auth = PrincipalUtils.getAuthentication();
            } else {
                oldAuth = PrincipalUtils.getAuthentication();
                auth = PrincipalUtils.getAuthenticationByToken(token);
            }

            if (auth != null) {
                if (!Config.SYSTEM_USER.equals(auth.getName())) {
                    DbSessionManager.getInstance().remove(token);

                    // Activity log
                    UserActivity.log(auth.getName(), "LOGOUT", token, null,
                            null);
                } else {
                    log.warn("'" + Config.SYSTEM_USER
                            + "' user should not logout");
                    StackTraceUtils.logTrace(log);
                }
            }
        } catch (final Exception e) {
            throw new RepositoryException(e.getMessage(), e);
        } finally {
            if (token != null) {
                PrincipalUtils.setAuthentication(oldAuth);
            }
        }

        log.debug("logout: void");
    }

    @Override
    public void grantUser(final String token, final String nodePath,
            final String guser, final int permissions, final boolean recursive)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException, DatabaseException {
        log.debug("grantUser({}, {}, {}, {}, {})", new Object[] { token,
                nodePath, guser, permissions, recursive });
        Authentication auth = null, oldAuth = null;

        try {
            if (token == null) {
                auth = PrincipalUtils.getAuthentication();
            } else {
                oldAuth = PrincipalUtils.getAuthentication();
                auth = PrincipalUtils.getAuthenticationByToken(token);
            }

            final String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(
                    nodePath);
            NodeBaseDAO.getInstance().grantUserPermissions(nodeUuid, guser,
                    permissions, recursive);

            // Activity log
            UserActivity.log(auth.getName(), "GRANT_USER", nodeUuid, nodePath,
                    guser + ", " + permissions);
        } catch (final DatabaseException e) {
            throw e;
        } finally {
            if (token != null) {
                PrincipalUtils.setAuthentication(oldAuth);
            }
        }

        log.debug("grantUser: void");
    }

    @Override
    public void revokeUser(final String token, final String nodePath,
            final String guser, final int permissions, final boolean recursive)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException, DatabaseException {
        log.debug("revokeUser({}, {}, {}, {}, {})", new Object[] { token,
                nodePath, guser, permissions, recursive });
        Authentication auth = null, oldAuth = null;

        try {
            if (token == null) {
                auth = PrincipalUtils.getAuthentication();
            } else {
                oldAuth = PrincipalUtils.getAuthentication();
                auth = PrincipalUtils.getAuthenticationByToken(token);
            }

            final String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(
                    nodePath);
            NodeBaseDAO.getInstance().revokeUserPermissions(nodeUuid, guser,
                    permissions, recursive);

            // Activity log
            UserActivity.log(auth.getName(), "REVOKE_USER", nodeUuid, nodePath,
                    guser + ", " + permissions);
        } catch (final DatabaseException e) {
            throw e;
        } finally {
            if (token != null) {
                PrincipalUtils.setAuthentication(oldAuth);
            }
        }

        log.debug("revokeUser: void");
    }

    @Override
    public Map<String, Integer> getGrantedUsers(final String token,
            final String nodePath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException, DatabaseException {
        log.debug("getGrantedUsers({}, {})", token, nodePath);
        Map<String, Integer> users = new HashMap<String, Integer>();
        @SuppressWarnings("unused")
        Authentication oldAuth = null;

        try {
            if (token == null) {
                PrincipalUtils.getAuthentication();
            } else {
                oldAuth = PrincipalUtils.getAuthentication();
                PrincipalUtils.getAuthenticationByToken(token);
            }

            final String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(
                    nodePath);
            users = NodeBaseDAO.getInstance().getUserPermissions(nodeUuid);
        } catch (final DatabaseException e) {
            throw e;
        } finally {
            if (token != null) {
                PrincipalUtils.setAuthentication(oldAuth);
            }
        }

        log.debug("getGrantedUsers: {}", users);
        return users;
    }

    @Override
    public void grantRole(final String token, final String nodePath,
            final String role, final int permissions, final boolean recursive)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException, DatabaseException {
        log.debug("grantRole({}, {}, {}, {}, {})", new Object[] { token,
                nodePath, role, permissions, recursive });
        Authentication auth = null, oldAuth = null;

        try {
            if (token == null) {
                auth = PrincipalUtils.getAuthentication();
            } else {
                oldAuth = PrincipalUtils.getAuthentication();
                auth = PrincipalUtils.getAuthenticationByToken(token);
            }

            final String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(
                    nodePath);
            NodeBaseDAO.getInstance().grantRolePermissions(nodeUuid, role,
                    permissions, recursive);

            // Activity log
            UserActivity.log(auth.getName(), "GRANT_ROLE", nodeUuid, nodePath,
                    role + ", " + permissions);
        } catch (final DatabaseException e) {
            throw e;
        } finally {
            if (token != null) {
                PrincipalUtils.setAuthentication(oldAuth);
            }
        }

        log.debug("grantRole: void");
    }

    @Override
    public void revokeRole(final String token, final String nodePath,
            final String role, final int permissions, final boolean recursive)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException, DatabaseException {
        log.debug("revokeRole({}, {}, {}, {}, {})", new Object[] { token,
                nodePath, role, permissions, recursive });
        Authentication auth = null, oldAuth = null;

        try {
            if (token == null) {
                auth = PrincipalUtils.getAuthentication();
            } else {
                oldAuth = PrincipalUtils.getAuthentication();
                auth = PrincipalUtils.getAuthenticationByToken(token);
            }

            final String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(
                    nodePath);
            NodeBaseDAO.getInstance().revokeRolePermissions(nodeUuid, role,
                    permissions, recursive);

            // Activity log
            UserActivity.log(auth.getName(), "REVOKE_ROLE", nodeUuid, nodePath,
                    role + ", " + permissions);
        } catch (final DatabaseException e) {
            throw e;
        } finally {
            if (token != null) {
                PrincipalUtils.setAuthentication(oldAuth);
            }
        }

        log.debug("revokeRole: void");
    }

    @Override
    public void changeSecurity(final String token, final String nodePath,
            final Map<String, Integer> grantUsers,
            final Map<String, Integer> revokeUsers,
            final Map<String, Integer> grantRoles,
            final Map<String, Integer> revokeRoles, final boolean recursive)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException, DatabaseException {
        log.debug("changeSecurity({}, {}, {}, {}, {}, {}, {})", new Object[] {
                token, nodePath, grantUsers, revokeUsers, grantRoles,
                revokeRoles, recursive });
        Authentication auth = null, oldAuth = null;

        try {
            if (token == null) {
                auth = PrincipalUtils.getAuthentication();
            } else {
                oldAuth = PrincipalUtils.getAuthentication();
                auth = PrincipalUtils.getAuthenticationByToken(token);
            }

            final String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(
                    nodePath);
            NodeBaseDAO.getInstance().changeSecurity(nodeUuid, grantUsers,
                    revokeUsers, grantRoles, revokeRoles, recursive);

            // Activity log
            UserActivity.log(auth.getName(), "CHANGE_SECURITY", nodeUuid,
                    nodePath, grantUsers + ", " + revokeUsers + ", "
                            + grantRoles + ", " + revokeRoles + ", "
                            + recursive);
        } catch (final DatabaseException e) {
            throw e;
        } finally {
            if (token != null) {
                PrincipalUtils.setAuthentication(oldAuth);
            }
        }

        log.debug("changeSecurity: void");
    }

    @Override
    public Map<String, Integer> getGrantedRoles(final String token,
            final String nodePath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException, DatabaseException {
        log.debug("getGrantedRoles({}, {})", token, nodePath);
        Map<String, Integer> roles = new HashMap<String, Integer>();
        @SuppressWarnings("unused")
        Authentication oldAuth = null;

        try {
            if (token == null) {
                PrincipalUtils.getAuthentication();
            } else {
                oldAuth = PrincipalUtils.getAuthentication();
                PrincipalUtils.getAuthenticationByToken(token);
            }

            final String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(
                    nodePath);
            roles = NodeBaseDAO.getInstance().getRolePermissions(nodeUuid);
        } catch (final DatabaseException e) {
            throw e;
        } finally {
            if (token != null) {
                PrincipalUtils.setAuthentication(oldAuth);
            }
        }

        log.debug("getGrantedRoles: {}", roles);
        return roles;
    }

    @Override
    public List<String> getUsers(final String token)
            throws PrincipalAdapterException {
        return CommonAuthModule.getUsers(token);
    }

    @Override
    public List<String> getRoles(final String token)
            throws PrincipalAdapterException {
        return CommonAuthModule.getRoles(token);
    }

    @Override
    public List<String> getUsersByRole(final String token, final String role)
            throws PrincipalAdapterException {
        return CommonAuthModule.getUsersByRole(token, role);
    }

    @Override
    public List<String> getRolesByUser(final String token, final String user)
            throws PrincipalAdapterException {
        return CommonAuthModule.getRolesByUser(token, user);
    }

    @Override
    public String getMail(final String token, final String user)
            throws PrincipalAdapterException {
        return CommonAuthModule.getMail(token, user);
    }

    @Override
    public String getName(final String token, final String user)
            throws PrincipalAdapterException {
        return CommonAuthModule.getName(token, user);
    }

    /**
     * Load user data
     */
    public static void loadUserData(final String user)
            throws PathNotFoundException, AccessDeniedException,
            ItemExistsException, DatabaseException {
        log.debug("loadUserData({})", user);
        final String baseTrashPath = "/" + Repository.TRASH;
        final String basePersonalPath = "/" + Repository.PERSONAL;
        final String baseMailPath = "/" + Repository.MAIL;
        final String userTrashPath = baseTrashPath + "/" + user;
        final String userPersonalPath = basePersonalPath + "/" + user;
        final String userMailPath = baseMailPath + "/" + user;

        synchronized (user) {
            if (!NodeBaseDAO.getInstance().itemPathExists(userTrashPath)) {
                log.info("Create {}/{}", Repository.TRASH, user);
                createBase(user, baseTrashPath);
            }

            if (!NodeBaseDAO.getInstance().itemPathExists(userPersonalPath)) {
                log.info("Create {}/{}", Repository.PERSONAL, user);
                createBase(user, basePersonalPath);
            }

            if (!NodeBaseDAO.getInstance().itemPathExists(userMailPath)) {
                log.info("Create {}/{}", Repository.MAIL, user);
                createBase(user, baseMailPath);
            }
        }

        log.debug("loadUserData: void");
    }

    /**
     * Create base node
     */
    private static void createBase(final String user, final String basePath)
            throws PathNotFoundException, AccessDeniedException,
            ItemExistsException, DatabaseException {
        log.debug("createBase({}, {})", user, basePath);
        final String baseUuid = NodeBaseDAO.getInstance().getUuidFromPath(
                basePath);
        final NodeFolder nFolder = new NodeFolder();

        // Add basic properties
        nFolder.setParent(baseUuid);
        nFolder.setAuthor(user);
        nFolder.setName(user);
        nFolder.setContext(PathUtils.fixContext(basePath));
        nFolder.setUuid(UUID.randomUUID().toString());
        nFolder.setCreated(Calendar.getInstance());

        // Auth info
        final int perms = Permission.READ | Permission.WRITE
                | Permission.DELETE | Permission.SECURITY;
        nFolder.getUserPermissions().put(user, perms);
        NodeFolderDAO.getInstance().create(nFolder);
    }
}
