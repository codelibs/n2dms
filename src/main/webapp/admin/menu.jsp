<%@ page import="com.openkm.core.Config" %>
<%@ page import="com.openkm.servlet.admin.BaseServlet" %>
<%@ page import="com.openkm.extension.dao.ExtensionDAO" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="isMultipleInstancesAdmin"><%=BaseServlet.isMultipleInstancesAdmin(request)%></c:set>
<c:set var="isRepositoryNative"><%=Config.REPOSITORY_NATIVE%></c:set>
<!-- http://stackoverflow.com/questions/1708054/center-ul-li-into-div -->
<c:set var="isSiteAdmin"><%=BaseServlet.isSiteAdmin(request)%></c:set>
<div style="text-align: center">
  <ul style="display: inline-block;">
    <li>
      <a target="frame" href="home.jsp" title="Home">
        <img src="img/toolbar/home.png">
      </a>
    </li>
<c:if test="${!isSiteAdmin}">
    <c:if test="${isMultipleInstancesAdmin}">
      <li>
        <a target="frame" href="Config" title="Configuration">
          <img src="img/toolbar/config.png">
        </a>
      </li>
    </c:if>
    <li>
      <a target="frame" href="MimeType" title="Mime types">
        <img src="img/toolbar/mime.png">
      </a>
    </li>
</c:if>
    <li>
      <a target="frame" href="stats.jsp" title="Statistics">
        <img src="img/toolbar/stats.png">
      </a>
    </li>
<c:if test="${!isSiteAdmin}">
    <c:if test="${isMultipleInstancesAdmin}">
      <li>
        <a target="frame" href="scripting.jsp" title="Scripting">
          <img src="img/toolbar/scripting.png">
        </a>
      </li>
      <c:if test="${!isRepositoryNative}">
        <li>
          <a target="frame" href="RepositorySearch" title="Repository search">
            <img src="img/toolbar/search.png" >
          </a>
        </li>
      </c:if>
    </c:if>
    <li>
      <a target="frame" href="PropertyGroups" title="Metadata">
        <img src="img/toolbar/properties.png">
      </a>
    </li>
</c:if>
    <li>
      <a target="frame" href="Auth" title="Users">
        <img src="img/toolbar/users.png">
      </a>
    </li>
<c:if test="${!isSiteAdmin}">
    <li>
      <a target="frame" href="Profile"  title="Profiles">
        <img src="img/toolbar/profile.png">
      </a>
    </li>
    <c:if test="${isMultipleInstancesAdmin}">
      <li>
        <a target="frame" href="DatabaseQuery" title="Database query">
          <img src="img/toolbar/database.png">
        </a>
      </li>
    </c:if>
    <li>
      <a target="frame" href="Report" title="Reports">
        <img src="img/toolbar/report.png" title="Reports">
      </a>
    </li>
</c:if>
    <li>
      <a target="frame" href="ActivityLog" title="Log">
        <img src="img/toolbar/activity.png">
      </a>
    </li>
<c:if test="${!isSiteAdmin}">
    <li>
      <a target="frame" href="Workflow" title="Workflow">
        <img src="img/toolbar/workflow.png">
      </a>
    </li>
    <li>
      <a target="frame" href="Automation" title="Automation">
        <img src="img/toolbar/automation.png">
      </a>
    </li>
    <c:if test="${isMultipleInstancesAdmin}">
      <li>
        <a target="frame" href="CronTab" title="Crontab">
          <img src="img/toolbar/crontab.png">
        </a>
      </li>
      <li>
        <a target="frame" href="Omr" title="OMR">
          <img src="img/toolbar/omr.png">
        </a>
      </li>
      <li>
        <a target="frame" href="generate_thesaurus.jsp" title="Thesaurus">
          <img src="img/toolbar/thesaurus.png">
        </a>
      </li>
      <li>
        <a target="frame" href="Language" title="Language">
          <img src="img/toolbar/language.png">
        </a>
      </li>
</c:if>
      <li>
        <a target="frame" href="repository_import.jsp" title="Import">
          <img src="img/toolbar/import.png">
        </a>
      </li>
      <li>
        <a target="frame" href="repository_export.jsp" title="Export">
          <img src="img/toolbar/export.png">
         </a>
      </li>
<c:if test="${!isSiteAdmin}">
      <li>
        <a target="frame" href="utilities.jsp" title="Utilities">
          <img src="img/toolbar/utilities.png">
         </a>
      </li>
    </c:if>
</c:if>
    <script type="text/javascript">
      // Identify if being loaded inside an iframe
      if (self == top) {
         document.write('<li>\n');
         document.write('<a href="logout.jsp" title="Exit"><img src="img/toolbar/exit.png"></a>\n');
         document.write('</li>\n');
      }
    </script>
  </ul>
</div>
