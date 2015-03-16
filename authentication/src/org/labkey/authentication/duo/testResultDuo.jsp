<%
/*
* Copyright (c) 2015 LabKey Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.authentication.duo.DuoController" %>
<%@ page import="org.labkey.authentication.duo.DuoController.TestDuoForm" %>
<%@ page import="com.duosecurity.duoweb.DuoWeb" %>
<%@ page import="org.labkey.api.util.URLHelper" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.springframework.web.context.support.RequestHandledEvent" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.security.LoginUrls" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TestDuoForm> me = (JspView<TestDuoForm>)HttpView.currentView();
    TestDuoForm form = me.getModelBean();

%>

<labkey:form name="testDuoResult" method="post" action="testDuoResult.post">
    <%--<labkey:csrf />--%>
    <table>
        <tr><td><h2>Duo Authentication Result</h2></td></tr>
        <%
            if(StringUtils.isNotEmpty(form.getUserName()))
            {
        %>
                <tr><td font style="color: forestgreen" size="14"><b>Test Success: Duo authentication for user <%=h(form.getUserName())%> is successful!</b></td></tr>
        <%
            }
            else
            {
        %>
                <tr><td font style="color: red" size="14"><b>Test Failed: Duo authentication test failed.</b></td></tr>
        <%
            }
        %>
        <tr><td>&nbsp;</td></tr>
        <tr><td> <%= button("Back to Authentication").href(urlProvider(LoginUrls.class).getConfigureURL()) %></td></tr>

  </table>
</labkey:form>
