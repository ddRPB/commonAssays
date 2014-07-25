<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page import="org.labkey.flow.data.FlowStatus" %>
<%@ page import="org.labkey.api.pipeline.PipelineStatusFile" %>
<%@ page import="org.labkey.flow.script.FlowJob" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="java.io.File" %>
<%@ page import="org.labkey.flow.script.ScriptJob" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.FlowController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromFilePath("Ext4"));
        return resources;
    }
%>
<%
    FlowStatus model = (FlowStatus) getModelBean();
    PipelineStatusFile psf = model.getPipelineStatusFile();
    FlowJob job = model.getJob();

    String log = PageFlowUtil.getFileContentsAsString(new File(psf.getFilePath()));
    ActionURL cancelURL = new ActionURL(FlowController.CancelJobAction.class, getViewContext().getContainer());
%>
<p><%=h(psf.getDescription())%></p>
<p><%=h(model.getStatus())%></p>
<table>
    <tr>
        <td>
            <div id="statusFile" style="height:300px;width:700px;overflow:auto;">
                <code>
                    <%=PageFlowUtil.filter(log, true, false)%>
                    <a id="end">&nbsp;</a>
                </code>
            </div>
        </td>

        <%
            if (job != null && job instanceof ScriptJob)
            {
                ScriptJob scriptJob = (ScriptJob) job;
                Map<FlowProtocolStep, String[]> processedRuns = scriptJob.getProcessedRunLSIDs();

                if (!processedRuns.isEmpty())
                {
        %>
        <td valign="top">
            <br>Completed Runs:<br>
            <%
                for (Map.Entry<FlowProtocolStep, String[]> entry : processedRuns.entrySet())
                {
            %>
            <p>
                <%=h(entry.getKey().getLabel())%> step<br>
                <%
                    for (String lsid : entry.getValue())
                    {
                        FlowRun run = FlowRun.fromLSID(lsid);
                        if (run == null)
                        {
                %>
                Run '<%=h(lsid)%>' not found
                <%
                        }
                        else
                        {
                %>
                <a href="<%=h(run.urlShow())%>"><%=h(run.getLabel())%></a><br>
                <%
                        }
                    }
                %>
            </p>
            <%
                }
            %>
        </td>
        <%
                }
            }
        %>
    </tr>
</table>
<%
    if (psf != null && psf.isActive())
    {
        cancelURL.addParameter("statusFile", psf.getFilePath());
%>
<br><%=PageFlowUtil.button("Cancel Job").href(cancelURL)%>
<%
    }
%>
<script type="text/javascript">
    Ext4.onReady(function() { Ext4.get('end').scrollIntoView(Ext4.get('statusFile')); });
</script>