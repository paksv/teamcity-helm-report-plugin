<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<jsp:useBean id="planJsonFileBean" class="jetbrains.buildServer.helmReport.beans.PlanJsonFileBean"/>
<jsp:useBean id="updateBuildStatusBean"
             class="jetbrains.buildServer.helmReport.beans.UpdateBuildStatusBean"/>
<jsp:useBean id="protectedResourcesBean"
             class="jetbrains.buildServer.helmReport.beans.ProtectedResourcesBean"/>

<tr>
    <td colspan="2">
        <em>
            Allows to parse Helm Diff report files to provide a report and/or control build results.<br/>
            Produce JSON file by:<br/>
            - <code>helm diff upgrade &lt;release-name&gt; &lt;folder-with-helm-chart&gt; --output template &gt;
            output.json</code>
            <br/>
            If planned changes look good, you may want to continue to <code>apply</code> the changes.
        </em>
    </td>
</tr>

<tr id="plan_file_path">
    <th><label for="${planJsonFileBean.key}">${planJsonFileBean.label}</label></th>
    <td>
        <props:textProperty name="${planJsonFileBean.key}" className="mediumField"/>
        <span class="smallNote">${planJsonFileBean.description}</span>
    </td>
</tr>

<tr class="advancedSetting" id="update_build_status">
    <th><label>${updateBuildStatusBean.label}</label></th>
    <td>
        <props:checkboxProperty name="${updateBuildStatusBean.key}"/>
        <span class="smallNote">${updateBuildStatusBean.description}</span>
    </td>
</tr>

<%--
<tr class="advancedSetting" id="check_protected_resources">
    <th><label for="${protectedResourcesBean.key}">${protectedResourcesBean.label}</label></th>
    <td>
        <props:textProperty name="${protectedResourcesBean.key}" className="mediumField"/>
        <span class="smallNote">${protectedResourcesBean.description}</span>
    </td>
</tr>--%>
