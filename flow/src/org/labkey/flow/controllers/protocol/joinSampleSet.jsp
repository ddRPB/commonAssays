<%@ page import="org.labkey.flow.controllers.protocol.JoinSampleSetForm"%>
<%@ page import="java.util.Map"%>
<%@ page import="org.labkey.api.query.FieldKey"%>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController.Action"%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% JoinSampleSetForm form = (JoinSampleSetForm) __form;
    Map<String, String> sampleKeyFields = form.getAvailableSampleKeyFields();
    Map<FieldKey, String> dataKeyFields = form.getAvailableDataKeyFields();

%>
<p>Use this page to set which properties of the sample need to match keywords of the FCS files.</p>

<form class="normal" action="<%=form.getProtocol().urlFor(Action.joinSampleSet)%>" method="POST">
    <table><tr><th>Sample Property</th><th>FCS Property</th></tr>
        <% for (int i = 0; i < form.ff_samplePropertyURI.length; i ++)
        { %>
        <tr><td>
            <%=PageFlowUtil.strSelect("ff_samplePropertyURI", sampleKeyFields, form.ff_samplePropertyURI[i]) %>
        </td>
            <td>
                <%=org.labkey.api.util.PageFlowUtil.strSelect("ff_dataField", dataKeyFields, form.ff_dataField[i])%>
            </td>
        </tr>
        <% } %>
    </table>
    <labkey:button text="update" /> <labkey:button text="cancel" href="<%=form.getProtocol().urlShow()%>" />
</form>
