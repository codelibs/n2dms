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

package com.openkm.servlet.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.api.OKMAuth;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.dao.AuthDAO;
import com.openkm.dao.ProfileDAO;
import com.openkm.dao.bean.Profile;
import com.openkm.dao.bean.Role;
import com.openkm.dao.bean.User;
import com.openkm.frontend.client.OKMException;
import com.openkm.principal.DatabasePrincipalAdapter;
import com.openkm.principal.PrincipalAdapterException;
import com.openkm.servlet.frontend.ChatServlet;
import com.openkm.util.SecureStore;
import com.openkm.util.UserActivity;
import com.openkm.util.WebUtils;

/**
 * User servlet
 */
public class AuthServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(AuthServlet.class);

    private static boolean db = Config.PRINCIPAL_ADAPTER
            .equals(DatabasePrincipalAdapter.class.getCanonicalName());

    @Override
    public void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException,
            ServletException {
        log.debug("doGet({}, {})", request, response);
        request.setCharacterEncoding("UTF-8");
        final String action = WebUtils.getString(request, "action");
        final String userId = request.getRemoteUser();
        updateSessionManager(request);

        if (request.isUserInRole(Config.DEFAULT_ADMIN_ROLE)) {
            try {
                if (action.equals("userCreate")) {
                    userCreate(userId, request, response);
                } else if (action.equals("roleCreate")) {
                    roleCreate(userId, request, response);
                } else if (action.equals("userEdit")) {
                    userEdit(userId, request, response);
                } else if (action.equals("roleEdit")) {
                    roleEdit(userId, request, response);
                } else if (action.equals("userDelete")) {
                    userDelete(userId, request, response);
                } else if (action.equals("roleDelete")) {
                    roleDelete(userId, request, response);
                } else if (action.equals("userActive")) {
                    userActive(userId, request, response);
                } else if (action.equals("roleActive")) {
                    roleActive(userId, request, response);
                } else if (action.equals("userChatDisconnect")) {
                    userChatDisconnect(request, response);
                } else if (action.equals("validateUser")) {
                    validateUser(request, response);
                } else if (action.equals("validateRole")) {
                    validateRole(request, response);
                }

                if (action.equals("") || action.equals("userActive")
                        || action.equals("userChatDisconnect")
                        || action.startsWith("user")
                        && WebUtils.getBoolean(request, "persist")) {
                    userList(userId, request, response);
                } else if (action.equals("roleList")
                        || action.equals("roleActive")
                        || action.startsWith("role")
                        && WebUtils.getBoolean(request, "persist")) {
                    roleList(userId, request, response);
                }
            } catch (final DatabaseException e) {
                log.error(e.getMessage(), e);
                sendErrorRedirect(request, response, e);
            } catch (final NoSuchAlgorithmException e) {
                log.error(e.getMessage(), e);
                sendErrorRedirect(request, response, e);
            } catch (final PrincipalAdapterException e) {
                log.error(e.getMessage(), e);
                sendErrorRedirect(request, response, e);
            } catch (final AccessDeniedException e) {
                log.error(e.getMessage(), e);
                sendErrorRedirect(request, response, e);
            }
        } else {
            // Activity log
            UserActivity.log(userId, "ADMIN_ACCESS_DENIED",
                    request.getRequestURI(), null, request.getQueryString());

            final AccessDeniedException ade = new AccessDeniedException(
                    "You should not access this resource");
            sendErrorRedirect(request, response, ade);
        }
    }

    /**
     * Validate user name
     */
    private void validateUser(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException, DatabaseException {
        final String value = WebUtils.getString(request, "value");
        final PrintWriter out = response.getWriter();
        response.setContentType("text/json");

        if (AuthDAO.findUserByPk(value) == null) {
            out.print("{ \"success\": true }");
        } else {
            out.print("{ \"success\": false, \"message\": \"Name is already taken.\" }");
        }

        out.flush();
        out.close();
    }

    /**
     * Validate role name
     */
    private void validateRole(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException, DatabaseException {
        final String value = WebUtils.getString(request, "value");
        final PrintWriter out = response.getWriter();
        response.setContentType("text/json");

        if (AuthDAO.findRoleByPk(value) == null) {
            out.print("{ \"success\": true }");
        } else {
            out.print("{ \"success\": false, \"message\": \"Name is already taken.\" }");
        }

        out.flush();
        out.close();
    }

    /**
     * New user
     */
    private void userCreate(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            AccessDeniedException, NoSuchAlgorithmException {
        log.debug("userCreate({}, {}, {})", new Object[] { userId, request,
                response });

        if (WebUtils.getBoolean(request, "persist")) {
            final String reqCsrft = WebUtils.getString(request, "csrft");
            final String sesCsrft = (String) request.getSession().getAttribute(
                    "csrft");

            if (reqCsrft.equals(sesCsrft)) {
                final String usrId = WebUtils.getString(request, "usr_id");

                if (AuthDAO.findUserByPk(usrId) == null) {
                    if (!usrId.matches("^[a-zA-Z0-9._]+$")) {
                        throw new DatabaseException("User name is not correct");
                    } else if (Config.VALIDATOR_ID_MIN_LENGTH > 0
                            && usrId.length() < Config.VALIDATOR_ID_MIN_LENGTH) {
                        throw new DatabaseException(
                                Config.VALIDATOR_ID_ERROR_MIN_LENGTH);
                    } else if (Config.VALIDATOR_ID_MAX_LENGTH > 0
                            && usrId.length() > Config.VALIDATOR_ID_MAX_LENGTH) {
                        throw new DatabaseException(
                                Config.VALIDATOR_ID_ERROR_MAX_LENGTH);
                    }
                    final User usr = new User();
                    usr.setId(usrId);
                    usr.setName(WebUtils.getString(request, "usr_name"));
                    usr.setPassword(WebUtils.getString(request, "usr_password"));
                    usr.setEmail(WebUtils.getString(request, "usr_email"));
                    usr.setActive(WebUtils.getBoolean(request, "usr_active"));
                    final List<String> usrRoles = WebUtils.getStringList(
                            request, "usr_roles");

                    for (final String rolId : usrRoles) {
                        usr.getRoles().add(AuthDAO.findRoleByPk(rolId));
                    }

                    AuthDAO.createUser(usr);

                    // Activity log
                    UserActivity.log(userId, "ADMIN_USER_CREATE", usr.getId(),
                            null, usr.toString());
                } else {
                    throw new DatabaseException("User name already taken");
                }
            } else {
                // Activity log
                UserActivity.log(request.getRemoteUser(),
                        "ADMIN_SECURITY_RISK", request.getRemoteHost(), null,
                        null);

                throw new AccessDeniedException("Security risk detected");
            }
        } else {
            final String genCsrft = SecureStore.md5Encode(UUID.randomUUID()
                    .toString().getBytes());
            request.getSession().setAttribute("csrft", genCsrft);
            final ServletContext sc = getServletContext();
            sc.setAttribute("action", WebUtils.getString(request, "action"));
            sc.setAttribute("persist", true);
            sc.setAttribute("csrft", genCsrft);
            sc.setAttribute("roles", AuthDAO.findAllRoles());
            sc.setAttribute("usr", null);
            sc.getRequestDispatcher("/admin/user_edit.jsp").forward(request,
                    response);
        }

        log.debug("userCreate: void");
    }

    /**
     * Edit user
     */
    private void userEdit(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            NoSuchAlgorithmException, AccessDeniedException {
        log.debug("userEdit({}, {}, {})", new Object[] { userId, request,
                response });
        final String usrId = WebUtils.getString(request, "usr_id");

        if (WebUtils.getBoolean(request, "persist")) {
            final String reqCsrft = WebUtils.getString(request, "csrft");
            final String sesCsrft = (String) request.getSession().getAttribute(
                    "csrft");

            if (reqCsrft.equals(sesCsrft)) {
                final String password = WebUtils.getString(request,
                        "usr_password");
                final User usr = new User();
                usr.setId(usrId);
                usr.setName(WebUtils.getString(request, "usr_name"));
                usr.setEmail(WebUtils.getString(request, "usr_email"));
                usr.setActive(WebUtils.getBoolean(request, "usr_active"));
                final List<String> usrRoles = WebUtils.getStringList(request,
                        "usr_roles");

                for (final String rolId : usrRoles) {
                    usr.getRoles().add(AuthDAO.findRoleByPk(rolId));
                }

                AuthDAO.updateUser(usr);

                if (!password.equals("")) {
                    AuthDAO.updateUserPassword(usr.getId(), password);
                }

                // Activity log
                UserActivity.log(userId, "ADMIN_USER_EDIT", usr.getId(), null,
                        usr.toString());
            } else {
                // Activity log
                UserActivity.log(request.getRemoteUser(),
                        "ADMIN_SECURITY_RISK", request.getRemoteHost(), null,
                        null);

                throw new AccessDeniedException("Security risk detected");
            }
        } else {
            final String genCsrft = SecureStore.md5Encode(UUID.randomUUID()
                    .toString().getBytes());
            request.getSession().setAttribute("csrft", genCsrft);
            final ServletContext sc = getServletContext();
            sc.setAttribute("action", WebUtils.getString(request, "action"));
            sc.setAttribute("persist", true);
            sc.setAttribute("csrft", genCsrft);
            sc.setAttribute("roles", AuthDAO.findAllRoles());
            sc.setAttribute("usr", AuthDAO.findUserByPk(usrId));
            sc.getRequestDispatcher("/admin/user_edit.jsp").forward(request,
                    response);
        }

        log.debug("userEdit: void");
    }

    /**
     * Update user
     */
    private void userDelete(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            NoSuchAlgorithmException, AccessDeniedException {
        log.debug("userDelete({}, {}, {})", new Object[] { userId, request,
                response });
        final String usrId = WebUtils.getString(request, "usr_id");

        if (WebUtils.getBoolean(request, "persist")) {
            final String reqCsrft = WebUtils.getString(request, "csrft");
            final String sesCsrft = (String) request.getSession().getAttribute(
                    "csrft");

            if (reqCsrft.equals(sesCsrft)) {
                AuthDAO.deleteUser(usrId);

                // Activity log
                UserActivity
                        .log(userId, "ADMIN_USER_DELETE", usrId, null, null);
            } else {
                // Activity log
                UserActivity.log(request.getRemoteUser(),
                        "ADMIN_SECURITY_RISK", request.getRemoteHost(), null,
                        null);

                throw new AccessDeniedException("Security risk detected");
            }
        } else {
            final String genCsrft = SecureStore.md5Encode(UUID.randomUUID()
                    .toString().getBytes());
            request.getSession().setAttribute("csrft", genCsrft);
            final ServletContext sc = getServletContext();
            sc.setAttribute("action", WebUtils.getString(request, "action"));
            sc.setAttribute("persist", true);
            sc.setAttribute("csrft", genCsrft);
            sc.setAttribute("roles", AuthDAO.findAllRoles());
            sc.setAttribute("usr", AuthDAO.findUserByPk(usrId));
            sc.getRequestDispatcher("/admin/user_edit.jsp").forward(request,
                    response);
        }

        log.debug("userDelete: void");
    }

    /**
     * Active user
     */
    private void userActive(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            NoSuchAlgorithmException {
        log.debug("userActive({}, {}, {})", new Object[] { userId, request,
                response });
        final boolean active = WebUtils.getBoolean(request, "usr_active");
        final String usrId = WebUtils.getString(request, "usr_id");
        AuthDAO.activeUser(usrId, active);

        // Activity log
        UserActivity.log(userId, "ADMIN_USER_ACTIVE", usrId, null,
                Boolean.toString(active));

        log.debug("userActive: void");
    }

    /**
     * List users
     */
    private void userList(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            PrincipalAdapterException {
        log.debug("userList({}, {}, {})", new Object[] { userId, request,
                response });
        final String roleFilter = WebUtils.getString(request, "roleFilter");
        final ServletContext sc = getServletContext();
        sc.setAttribute("roleFilter", roleFilter);
        sc.setAttribute("chatUsers", ChatServlet.getChatManager()
                .getLoggedUsers());

        if (roleFilter.equals("")) {
            if (db) {
                final List<User> users = sortUserRoles(AuthDAO
                        .findAllUsers(false));
                sc.setAttribute("users", toMapSetProfile(users));
                sc.setAttribute("roles", AuthDAO.findAllRoles());
            } else {
                final List<User> users = str2user(OKMAuth.getInstance()
                        .getUsers(null));
                sc.setAttribute("users", toMapSetProfile(users));
                sc.setAttribute("roles", str2role(OKMAuth.getInstance()
                        .getRoles(null)));
            }
        } else {
            if (db) {
                final List<User> users = sortUserRoles(AuthDAO.findUsersByRole(
                        roleFilter, false));
                sc.setAttribute("users", toMapSetProfile(users));
                sc.setAttribute("roles", AuthDAO.findAllRoles());
            } else {
                final List<User> users = str2user(OKMAuth.getInstance()
                        .getUsersByRole(null, roleFilter));
                sc.setAttribute("users", toMapSetProfile(users));
                sc.setAttribute("roles", str2role(OKMAuth.getInstance()
                        .getRoles(null)));
            }
        }

        sc.setAttribute("db", db);
        sc.getRequestDispatcher("/admin/user_list.jsp").forward(request,
                response);
        log.debug("userList: void");
    }

    /**
     * New role
     */
    private void roleCreate(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            AccessDeniedException, NoSuchAlgorithmException {
        log.debug("roleCreate({}, {}, {})", new Object[] { userId, request,
                response });

        if (WebUtils.getBoolean(request, "persist")) {
            final String reqCsrft = WebUtils.getString(request, "csrft");
            final String sesCsrft = (String) request.getSession().getAttribute(
                    "csrft");

            if (reqCsrft.equals(sesCsrft)) {
                final String rolId = WebUtils.getString(request, "rol_id");

                if (AuthDAO.findRoleByPk(rolId) == null) {
                    final Role rol = new Role();
                    rol.setId(rolId);
                    rol.setActive(WebUtils.getBoolean(request, "rol_active"));
                    AuthDAO.createRole(rol);

                    // Activity log
                    UserActivity.log(userId, "ADMIN_ROLE_CREATE", rol.getId(),
                            null, rol.toString());
                } else {
                    throw new DatabaseException("Role name already taken");
                }
            } else {
                // Activity log
                UserActivity.log(request.getRemoteUser(),
                        "ADMIN_SECURITY_RISK", request.getRemoteHost(), null,
                        null);

                throw new AccessDeniedException("Security risk detected");
            }
        } else {
            final String genCsrft = SecureStore.md5Encode(UUID.randomUUID()
                    .toString().getBytes());
            request.getSession().setAttribute("csrft", genCsrft);
            final ServletContext sc = getServletContext();
            sc.setAttribute("action", WebUtils.getString(request, "action"));
            sc.setAttribute("persist", true);
            sc.setAttribute("csrft", genCsrft);
            sc.setAttribute("rol", null);
            sc.getRequestDispatcher("/admin/role_edit.jsp").forward(request,
                    response);
        }

        log.debug("roleCreate: void");
    }

    /**
     * Edit role
     */
    private void roleEdit(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            NoSuchAlgorithmException, AccessDeniedException {
        log.debug("roleEdit({}, {}, {})", new Object[] { userId, request,
                response });

        if (WebUtils.getBoolean(request, "persist")) {
            final String reqCsrft = WebUtils.getString(request, "csrft");
            final String sesCsrft = (String) request.getSession().getAttribute(
                    "csrft");

            if (reqCsrft.equals(sesCsrft)) {
                final Role rol = new Role();
                rol.setId(WebUtils.getString(request, "rol_id"));
                rol.setActive(WebUtils.getBoolean(request, "rol_active"));
                AuthDAO.updateRole(rol);

                // Activity log
                UserActivity.log(userId, "ADMIN_ROLE_EDIT", rol.getId(), null,
                        rol.toString());
            } else {
                // Activity log
                UserActivity.log(request.getRemoteUser(),
                        "ADMIN_SECURITY_RISK", request.getRemoteHost(), null,
                        null);

                throw new AccessDeniedException("Security risk detected");
            }
        } else {
            final String genCsrft = SecureStore.md5Encode(UUID.randomUUID()
                    .toString().getBytes());
            request.getSession().setAttribute("csrft", genCsrft);
            final ServletContext sc = getServletContext();
            final String rolId = WebUtils.getString(request, "rol_id");
            sc.setAttribute("action", WebUtils.getString(request, "action"));
            sc.setAttribute("persist", true);
            sc.setAttribute("csrft", genCsrft);
            sc.setAttribute("rol", AuthDAO.findRoleByPk(rolId));
            sc.getRequestDispatcher("/admin/role_edit.jsp").forward(request,
                    response);
        }

        log.debug("roleEdit: void");
    }

    /**
     * Delete role
     */
    private void roleDelete(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            NoSuchAlgorithmException, AccessDeniedException {
        log.debug("roleDelete({}, {}, {})", new Object[] { userId, request,
                response });

        if (WebUtils.getBoolean(request, "persist")) {
            final String reqCsrft = WebUtils.getString(request, "csrft");
            final String sesCsrft = (String) request.getSession().getAttribute(
                    "csrft");

            if (reqCsrft.equals(sesCsrft)) {
                final String rolId = WebUtils.getString(request, "rol_id");
                AuthDAO.deleteRole(rolId);

                // Activity log
                UserActivity
                        .log(userId, "ADMIN_ROLE_DELETE", rolId, null, null);
            } else {
                // Activity log
                UserActivity.log(request.getRemoteUser(),
                        "ADMIN_SECURITY_RISK", request.getRemoteHost(), null,
                        null);

                throw new AccessDeniedException("Security risk detected");
            }
        } else {
            final String genCsrft = SecureStore.md5Encode(UUID.randomUUID()
                    .toString().getBytes());
            request.getSession().setAttribute("csrft", genCsrft);
            final ServletContext sc = getServletContext();
            final String rolId = WebUtils.getString(request, "rol_id");
            sc.setAttribute("action", WebUtils.getString(request, "action"));
            sc.setAttribute("persist", true);
            sc.setAttribute("csrft", genCsrft);
            sc.setAttribute("rol", AuthDAO.findRoleByPk(rolId));
            sc.getRequestDispatcher("/admin/role_edit.jsp").forward(request,
                    response);
        }

        log.debug("roleDelete: void");
    }

    /**
     * Active role
     */
    private void roleActive(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            NoSuchAlgorithmException {
        log.debug("roleActive({}, {}, {})", new Object[] { userId, request,
                response });
        final String rolId = WebUtils.getString(request, "rol_id");
        final boolean active = WebUtils.getBoolean(request, "rol_active");
        AuthDAO.activeRole(rolId, active);

        // Activity log
        UserActivity.log(userId, "ADMIN_ROLE_ACTIVE", rolId, null,
                Boolean.toString(active));
        log.debug("roleActive: void");
    }

    /**
     * Diconnect user chat
     */
    private void userChatDisconnect(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException, DatabaseException, NoSuchAlgorithmException {
        log.debug("userChatDisconnect({}, {})", new Object[] { request,
                response });
        try {
            final String userId = WebUtils.getString(request, "usr_id");
            ChatServlet.getChatManager().logout(userId);
        } catch (final OKMException e) {
            throw new ServletException(e.getMessage());
        }
        log.debug("userChatDisconnect: void");
    }

    /**
     * List roles
     */
    private void roleList(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            PrincipalAdapterException {
        log.debug("roleList({}, {}, {})", new Object[] { userId, request,
                response });
        final ServletContext sc = getServletContext();

        if (db) {
            sc.setAttribute("roles", AuthDAO.findAllRoles());
        } else {
            sc.setAttribute("roles",
                    str2role(OKMAuth.getInstance().getRoles(null)));
        }

        sc.setAttribute("db", db);
        sc.getRequestDispatcher("/admin/role_list.jsp").forward(request,
                response);
        log.debug("roleList: void");
    }

    /**
     * Convenient conversion method
     */
    private List<User> str2user(final List<String> strList)
            throws PrincipalAdapterException {
        final List<User> usrList = new ArrayList<User>();

        for (final String usrId : strList) {
            final List<String> roleList = OKMAuth.getInstance().getRolesByUser(
                    null, usrId);
            final User usr = new User();
            usr.setId(usrId);
            usr.setActive(true);
            usr.setName(OKMAuth.getInstance().getName(null, usrId));
            usr.setEmail(OKMAuth.getInstance().getMail(null, usrId));

            if (!roleList.isEmpty()) {
                final Set<Role> roles = new TreeSet<Role>(new RoleComparator());

                for (final String rolId : roleList) {
                    final Role rol = new Role();
                    rol.setId(rolId);
                    rol.setActive(true);
                    roles.add(rol);
                }

                usr.setRoles(roles);
            }

            usrList.add(usr);
        }

        Collections.sort(usrList, new UserComparator());
        return usrList;
    }

    /**
     * Convenient conversion method
     */
    private List<Role> str2role(final List<String> strList) {
        final List<Role> roleList = new ArrayList<Role>();

        for (final String id : strList) {
            final Role rol = new Role();
            rol.setId(id);
            rol.setActive(true);
            roleList.add(rol);
        }

        Collections.sort(roleList, new RoleComparator());
        return roleList;
    }

    /**
     * User comparator
     */
    private class UserComparator implements Comparator<User> {
        @Override
        public int compare(final User arg0, final User arg1) {
            if (arg0 != null && arg1 != null) {
                return arg0.getId().compareTo(arg1.getId());
            } else {
                return 0;
            }
        }
    }

    /**
     * Role comparator
     */
    private class RoleComparator implements Comparator<Role> {
        @Override
        public int compare(final Role arg0, final Role arg1) {
            if (arg0 != null && arg1 != null) {
                return arg0.getId().compareTo(arg1.getId());
            } else {
                return 0;
            }
        }
    }

    /**
     * Sort roles from user
     */
    private List<User> sortUserRoles(final List<User> users) {
        final List<User> ret = new ArrayList<User>();

        for (final User user : users) {
            final Set<Role> sortedRoles = new TreeSet<Role>(
                    new RoleComparator());
            sortedRoles.addAll(user.getRoles());
            user.setRoles(sortedRoles);
            ret.add(user);
        }

        return ret;
    }

    /**
     * Convert to Map and set Profile
     */
    private List<Map<String, Object>> toMapSetProfile(final List<User> users)
            throws DatabaseException {
        final List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();

        for (final User user : users) {
            final Map<String, Object> usrMap = new HashMap<String, Object>();
            final Profile prf = ProfileDAO.findByUser(user.getId());

            if (prf != null) {
                usrMap.put("profile", prf.getName());
            } else {
                usrMap.put("profile", "");
            }

            usrMap.put("id", user.getId());
            usrMap.put("name", user.getName());
            usrMap.put("email", user.getEmail());
            usrMap.put("active", user.isActive());
            usrMap.put("roles", user.getRoles());
            ret.add(usrMap);
        }

        return ret;
    }
}
