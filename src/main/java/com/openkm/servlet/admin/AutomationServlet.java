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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.openkm.api.OKMRepository;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.AutomationDAO;
import com.openkm.dao.bean.AutomationAction;
import com.openkm.dao.bean.AutomationMetadata;
import com.openkm.dao.bean.AutomationRule;
import com.openkm.dao.bean.AutomationValidation;
import com.openkm.util.UserActivity;
import com.openkm.util.WebUtils;

/**
 * Automation servlet
 */
public class AutomationServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory
            .getLogger(AutomationServlet.class);

    private static String ats[] = { AutomationRule.AT_PRE,
            AutomationRule.AT_POST };

    private static String events[] = { AutomationRule.EVENT_DOCUMENT_CREATE,
            AutomationRule.EVENT_DOCUMENT_MOVE,
            AutomationRule.EVENT_FOLDER_CREATE,
            AutomationRule.EVENT_PROPERTY_GROUP_ADD,
            AutomationRule.EVENT_PROPERTY_GROUP_SET,
            AutomationRule.EVENT_TEXT_EXTRACTOR,
            AutomationRule.EVENT_CONVERSION_PDF,
            AutomationRule.EVENT_CONVERSION_SWF };

    @Override
    public void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException,
            ServletException {
        log.debug("doGet({}, {})", request, response);
        request.setCharacterEncoding("UTF-8");
        final String action = WebUtils.getString(request, "action");
        final String userId = request.getRemoteUser();
        updateSessionManager(request);

        try {
            if (action.equals("ruleList")) {
                ruleList(userId, request, response);
            } else if (action.equals("definitionList")) {
                definitionList(userId, request, response);
            } else if (action.equals("getMetadata")) {
                getMetadata(userId, request, response);
            } else if (action.equals("create")) {
                create(userId, request, response);
            } else if (action.equals("edit")) {
                edit(userId, request, response);
            } else if (action.equals("delete")) {
                delete(userId, request, response);
            } else if (action.equals("loadMetadataForm")) {
                loadMetadataForm(userId, request, response);
            } else if (action.equals("createAction")) {
                createAction(userId, request, response);
            } else if (action.equals("deleteAction")) {
                deleteAction(userId, request, response);
            } else if (action.equals("editAction")) {
                editAction(userId, request, response);
            } else if (action.equals("createValidation")) {
                createValidation(userId, request, response);
            } else if (action.equals("deleteValidation")) {
                deleteValidation(userId, request, response);
            } else if (action.equals("editValidation")) {
                editValidation(userId, request, response);
            }

            if (action.equals("") || WebUtils.getBoolean(request, "persist")) {
                ruleList(userId, request, response);
            } else if (action.equals("createAction")
                    || action.equals("createValidation")
                    || action.equals("deleteAction")
                    || action.equals("editAction")
                    || action.equals("deleteValidation")
                    || action.equals("editValidation")) {
                definitionList(userId, request, response);
            }
        } catch (final DatabaseException e) {
            log.error(e.getMessage(), e);
            sendErrorRedirect(request, response, e);
        } catch (final PathNotFoundException e) {
            log.error(e.getMessage(), e);
            sendErrorRedirect(request, response, e);
        } catch (final RepositoryException e) {
            log.error(e.getMessage(), e);
            sendErrorRedirect(request, response, e);
        }
    }

    /**
     * List rules
     */
    private void ruleList(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException {
        log.debug("ruleList({}, {}, {})", new Object[] { userId, request,
                response });
        final ServletContext sc = getServletContext();
        sc.setAttribute("automationRules", AutomationDAO.getInstance()
                .findAll());
        sc.getRequestDispatcher("/admin/automation_rule_list.jsp").forward(
                request, response);

        // Activity log
        UserActivity.log(userId, "ADMIN_AUTOMATION_LIST", null, null, null);
        log.debug("ruleList: void");
    }

    /**
     * List rules
     */
    private void definitionList(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            PathNotFoundException, RepositoryException {
        log.debug("definitionList({}, {}, {})", new Object[] { userId, request,
                response });
        final ServletContext sc = getServletContext();
        final long arId = WebUtils.getLong(request, "ar_id");
        final AutomationRule aRule = AutomationDAO.getInstance().findByPk(arId);

        for (final AutomationValidation av : aRule.getValidations()) {
            for (int i = 0; i < av.getParams().size(); i++) {
                av.getParams().set(
                        i,
                        convertToHumanValue(av.getParams().get(i),
                                av.getType(), i));
            }
        }

        sc.setAttribute("ar", aRule);
        sc.setAttribute("metadaActions", AutomationDAO.getInstance()
                .findMetadataActionsByAt(aRule.getAt()));
        sc.setAttribute("metadaValidations", AutomationDAO.getInstance()
                .findMetadataValidationsByAt(aRule.getAt()));
        sc.getRequestDispatcher("/admin/automation_definition_list.jsp")
                .forward(request, response);

        // Activity log
        UserActivity.log(userId, "ADMIN_AUTOMATION_GET_DEFINITION_LIST", null,
                null, null);
        log.debug("definitionList: void");
    }

    /**
     * getMetadataAction
     */
    private void getMetadata(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException {
        final long amId = WebUtils.getLong(request, "amId");
        final Gson son = new Gson();
        final AutomationMetadata am = AutomationDAO.getInstance()
                .findMetadataByPk(amId);
        final String json = son.toJson(am);
        final PrintWriter writer = response.getWriter();
        writer.print(json);
        writer.flush();

        // Activity log
        UserActivity.log(userId, "ADMIN_AUTOMATION_GET_METADATA",
                Long.toString(amId), null, am.getName());
    }

    /**
     * New automation
     */
    private void create(final String userId, final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException, DatabaseException {
        log.debug("create({}, {}, {})", new Object[] { userId, request,
                response });

        if (WebUtils.getBoolean(request, "persist")) {
            final AutomationRule ar = new AutomationRule();
            ar.setName(WebUtils.getString(request, "ar_name"));
            ar.setOrder(WebUtils.getInt(request, "ar_order"));
            ar.setExclusive(WebUtils.getBoolean(request, "ar_exclusive"));
            ar.setActive(WebUtils.getBoolean(request, "ar_active"));
            ar.setAt(WebUtils.getString(request, "ar_at"));
            ar.setEvent(WebUtils.getString(request, "ar_event"));
            AutomationDAO.getInstance().create(ar);

            // Activity log
            UserActivity.log(userId, "ADMIN_AUTOMATION_CREATE",
                    Long.toString(ar.getId()), null, ar.toString());
        } else {
            final ServletContext sc = getServletContext();
            final AutomationRule ar = new AutomationRule();
            sc.setAttribute("action", WebUtils.getString(request, "action"));
            sc.setAttribute("persist", true);
            sc.setAttribute("ar", ar);
            sc.setAttribute("ats", ats);
            sc.setAttribute("events", events);
            sc.getRequestDispatcher("/admin/automation_rule_edit.jsp").forward(
                    request, response);
        }

        log.debug("create: void");
    }

    /**
     * New metadata action
     */
    private void createAction(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException {
        final long arId = WebUtils.getLong(request, "ar_id");
        final AutomationAction aa = new AutomationAction();
        aa.setType(WebUtils.getLong(request, "am_id"));
        aa.setOrder(WebUtils.getInt(request, "am_order"));
        aa.setActive(WebUtils.getBoolean(request, "am_active"));
        final List<String> params = new ArrayList<String>();
        final String am_param00 = WebUtils.getString(request, "am_param00");
        final String am_param01 = WebUtils.getString(request, "am_param01");

        if (!am_param00.equals("")) {
            params.add(am_param00);
        }

        if (!am_param01.equals("")) {
            params.add(am_param01);
        }

        aa.setParams(params);
        AutomationDAO.getInstance().createAction(aa);
        final AutomationRule ar = AutomationDAO.getInstance().findByPk(arId);
        ar.getActions().add(aa);
        AutomationDAO.getInstance().update(ar);

        // Activity log
        UserActivity.log(userId, "ADMIN_AUTOMATION_ADD_ACTION",
                Long.toString(ar.getId()), null, ar.toString());
    }

    /**
     * Delete action
     */
    private void deleteAction(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException {
        final long aaId = WebUtils.getLong(request, "aa_id");
        final long arId = WebUtils.getLong(request, "ar_id");
        final AutomationRule ar = AutomationDAO.getInstance().findByPk(arId);

        for (final AutomationAction action : ar.getActions()) {
            if (action.getId() == aaId) {
                ar.getActions().remove(action);
                break;
            }
        }

        AutomationDAO.getInstance().update(ar);
        AutomationDAO.getInstance().deleteAction(aaId);

        // Activity log
        UserActivity.log(userId, "ADMIN_AUTOMATION_DELETE_ACTION",
                Long.toString(ar.getId()), null, ar.toString());
    }

    /**
     * Edit action
     */
    private void editAction(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException {
        final long aaId = WebUtils.getLong(request, "aa_id");
        final AutomationAction aa = AutomationDAO.getInstance().findActionByPk(
                aaId);
        aa.setOrder(WebUtils.getInt(request, "am_order"));
        aa.setActive(WebUtils.getBoolean(request, "am_active"));
        final List<String> params = new ArrayList<String>();
        final String am_param00 = WebUtils.getString(request, "am_param00");
        final String am_param01 = WebUtils.getString(request, "am_param01");

        if (!am_param00.equals("")) {
            params.add(am_param00);
        }

        if (!am_param01.equals("")) {
            params.add(am_param01);
        }

        aa.setParams(params);
        AutomationDAO.getInstance().updateAction(aa);

        // Activity log
        UserActivity.log(userId, "ADMIN_AUTOMATION_	EDIT_ACTION",
                Long.toString(aa.getId()), null, aa.toString());
    }

    /**
     * Edit validation
     */
    private void editValidation(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            PathNotFoundException, RepositoryException {
        final long avId = WebUtils.getLong(request, "av_id");
        final AutomationValidation av = AutomationDAO.getInstance()
                .findValidationByPk(avId);
        av.setOrder(WebUtils.getInt(request, "am_order"));
        av.setActive(WebUtils.getBoolean(request, "am_active"));
        final List<String> params = new ArrayList<String>();
        final String am_param00 = WebUtils.getString(request, "am_param00");
        final String am_param01 = WebUtils.getString(request, "am_param01");

        if (!am_param00.equals("")) {
            params.add(convertToInternalValue(am_param00, av.getType(), 0));
        }

        if (!am_param01.equals("")) {
            params.add(convertToInternalValue(am_param01, av.getType(), 1));
        }

        av.setParams(params);
        AutomationDAO.getInstance().updateValidation(av);

        // Activity log
        UserActivity.log(userId, "ADMIN_AUTOMATION_	EDIT_VALIDATION",
                Long.toString(av.getId()), null, av.toString());
    }

    /**
     * New metadata validation
     */
    private void createValidation(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            PathNotFoundException, RepositoryException {
        final long arId = WebUtils.getLong(request, "ar_id");
        final AutomationValidation av = new AutomationValidation();
        av.setType(WebUtils.getLong(request, "am_id"));
        av.setOrder(WebUtils.getInt(request, "am_order"));
        av.setActive(WebUtils.getBoolean(request, "am_active"));
        final List<String> params = new ArrayList<String>();
        final String am_param00 = WebUtils.getString(request, "am_param00");
        final String am_param01 = WebUtils.getString(request, "am_param01");

        if (!am_param00.equals("")) {
            params.add(convertToInternalValue(am_param00, av.getType(), 0));
        }

        if (!am_param01.equals("")) {
            params.add(convertToInternalValue(am_param01, av.getType(), 1));
        }

        av.setParams(params);
        AutomationDAO.getInstance().createValidation(av);
        final AutomationRule ar = AutomationDAO.getInstance().findByPk(arId);
        ar.getValidations().add(av);
        AutomationDAO.getInstance().update(ar);

        // Activity log
        UserActivity.log(userId, "ADMIN_AUTOMATION_ADD_VALIDATION",
                Long.toString(ar.getId()), null, ar.toString());
    }

    /**
     * Delete validation
     */
    private void deleteValidation(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException {
        final long avId = WebUtils.getLong(request, "av_id");
        final long arId = WebUtils.getLong(request, "ar_id");
        final AutomationRule ar = AutomationDAO.getInstance().findByPk(arId);

        for (final AutomationValidation validation : ar.getValidations()) {
            if (validation.getId() == avId) {
                ar.getValidations().remove(validation);
                break;
            }
        }

        AutomationDAO.getInstance().update(ar);
        AutomationDAO.getInstance().deleteValidation(avId);

        // Activity log
        UserActivity.log(userId, "ADMIN_AUTOMATION_DELETE_VALIDATION",
                Long.toString(ar.getId()), null, ar.toString());
    }

    /**
     * Edit automation
     */
    private void edit(final String userId, final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException, DatabaseException {
        log.debug("edit({}, {}, {})",
                new Object[] { userId, request, response });

        if (WebUtils.getBoolean(request, "persist")) {
            final long arId = WebUtils.getLong(request, "ar_id");
            final AutomationRule ar = AutomationDAO.getInstance()
                    .findByPk(arId);
            ar.setName(WebUtils.getString(request, "ar_name"));
            ar.setOrder(WebUtils.getInt(request, "ar_order"));
            ar.setExclusive(WebUtils.getBoolean(request, "ar_exclusive"));
            ar.setActive(WebUtils.getBoolean(request, "ar_active"));
            ar.setEvent(WebUtils.getString(request, "ar_event"));
            AutomationDAO.getInstance().update(ar);

            // Activity log
            UserActivity.log(userId, "ADMIN_AUTOMATION_EDIT",
                    Long.toString(ar.getId()), null, ar.toString());
        } else {
            final ServletContext sc = getServletContext();
            final long arId = WebUtils.getLong(request, "ar_id");
            sc.setAttribute("action", WebUtils.getString(request, "action"));
            sc.setAttribute("persist", true);
            sc.setAttribute("ar", AutomationDAO.getInstance().findByPk(arId));
            sc.setAttribute("ats", ats);
            sc.setAttribute("events", events);
            sc.getRequestDispatcher("/admin/automation_rule_edit.jsp").forward(
                    request, response);
        }

        log.debug("edit: void");
    }

    /**
     * Load Metadata form
     */
    private void loadMetadataForm(final String userId,
            final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, DatabaseException,
            PathNotFoundException, RepositoryException {
        final ServletContext sc = getServletContext();
        final String action = WebUtils.getString(request, "newAction");
        sc.setAttribute("action", action);
        sc.setAttribute("ar_id", WebUtils.getString(request, "ar_id"));
        final long amId = WebUtils.getLong(request, "am_id");
        sc.setAttribute("am_id", amId);

        if (action.equals("createAction") || action.equals("createValidation")) {
            sc.setAttribute("am",
                    AutomationDAO.getInstance().findMetadataByPk(amId));
            sc.setAttribute("aa_id", "");
            sc.setAttribute("av_id", "");
            sc.setAttribute("am_order", "0");
            sc.setAttribute("am_param00", "");
            sc.setAttribute("am_param01", "");
        } else if (action.equals("deleteAction") || action.equals("editAction")) {
            final long aaId = WebUtils.getLong(request, "aa_id");
            sc.setAttribute("aa_id", aaId);
            sc.setAttribute("av_id", "");
            final AutomationMetadata am = AutomationDAO.getInstance()
                    .findMetadataByPk(amId);
            final AutomationAction aa = AutomationDAO.getInstance()
                    .findActionByPk(aaId);

            for (int i = 0; i < aa.getParams().size(); i++) {
                switch (i) {
                case 0:
                    sc.setAttribute("am_param00", aa.getParams().get(0));
                    break;
                case 1:
                    sc.setAttribute("am_param01", aa.getParams().get(1));
                    break;
                }
            }

            sc.setAttribute("am_order", String.valueOf(aa.getOrder()));
            am.setActive(aa.isActive());
            sc.setAttribute("am", am);
        } else if (action.equals("deleteValidation")
                || action.equals("editValidation")) {
            final long avId = WebUtils.getLong(request, "av_id");
            sc.setAttribute("aa_id", "");
            sc.setAttribute("av_id", avId);
            final AutomationMetadata am = AutomationDAO.getInstance()
                    .findMetadataByPk(amId);
            final AutomationValidation av = AutomationDAO.getInstance()
                    .findValidationByPk(avId);

            for (int i = 0; i < av.getParams().size(); i++) {
                switch (i) {
                case 0:
                    sc.setAttribute(
                            "am_param00",
                            convertToHumanValue(av.getParams().get(0),
                                    av.getType(), 0));
                    break;
                case 1:
                    sc.setAttribute(
                            "am_param01",
                            convertToHumanValue(av.getParams().get(1),
                                    av.getType(), 1));
                    break;
                }
            }

            sc.setAttribute("am_order", String.valueOf(av.getOrder()));
            am.setActive(av.isActive());
            sc.setAttribute("am", am);
        }

        sc.getRequestDispatcher("/admin/automation_definition_form.jsp")
                .forward(request, response);
    }

    /**
     * Delete automation
     */
    private void delete(final String userId, final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException, DatabaseException {
        log.debug("delete({}, {}, {})", new Object[] { userId, request,
                response });

        if (WebUtils.getBoolean(request, "persist")) {
            final long arId = WebUtils.getLong(request, "ar_id");
            AutomationDAO.getInstance().delete(arId);

            // Activity log
            UserActivity.log(userId, "ADMIN_AUTOMATION_DELETE",
                    Long.toString(arId), null, null);
        } else {
            final ServletContext sc = getServletContext();
            final long arId = WebUtils.getLong(request, "ar_id");
            sc.setAttribute("action", WebUtils.getString(request, "action"));
            sc.setAttribute("persist", true);
            sc.setAttribute("ar", AutomationDAO.getInstance().findByPk(arId));
            sc.setAttribute("ats", ats);
            sc.setAttribute("events", events);
            sc.getRequestDispatcher("/admin/automation_rule_edit.jsp").forward(
                    request, response);
        }

        log.debug("edit: void");
    }

    /**
     * convertToInternalValue
     */
    private String convertToInternalValue(String value, final long amId,
            final int param) throws DatabaseException, PathNotFoundException,
            RepositoryException {
        final AutomationMetadata am = AutomationDAO.getInstance()
                .findMetadataByPk(amId);

        // Convert folder path to UUID
        switch (param) {
        case 0:
            if (AutomationMetadata.SOURCE_FOLDER.equals(am.getSource00())) {
                value = OKMRepository.getInstance().getNodeUuid(null, value);
            }
        case 1:
            if (AutomationMetadata.SOURCE_FOLDER.equals(am.getSource01())) {
                return value = OKMRepository.getInstance().getNodeUuid(null,
                        value);
            }
        }

        return value;
    }

    /**
     * convertToHumanValue
     */
    private String convertToHumanValue(String value, final long amId,
            final int param) throws DatabaseException, PathNotFoundException,
            RepositoryException {
        final AutomationMetadata am = AutomationDAO.getInstance()
                .findMetadataByPk(amId);

        // Convert folder path to UUID
        switch (param) {
        case 0:
            if (AutomationMetadata.SOURCE_FOLDER.equals(am.getSource00())) {
                value = OKMRepository.getInstance().getNodePath(null, value);
            }
        case 1:
            if (AutomationMetadata.SOURCE_FOLDER.equals(am.getSource01())) {
                return value = OKMRepository.getInstance().getNodePath(null,
                        value);
            }
        }

        return value;
    }
}
