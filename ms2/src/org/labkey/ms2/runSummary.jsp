<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.MS2Run" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.RunSummaryBean bean = ((JspView<MS2Controller.RunSummaryBean>)HttpView.currentView()).getModelBean();
    MS2Run run = bean.run;
%>
<table class="dataRegion">
    <tr>
    <td>Search Enzyme:</td><td><%=MS2Controller.defaultIfNull(run.getSearchEnzyme(), "n/a")%></td>
    <td>File Name:</td><td><%=MS2Controller.defaultIfNull(run.getFileName(), "n/a")%></td>
    </tr><tr>
    <td>Search Engine:</td><td><%=MS2Controller.defaultIfNull(run.getSearchEngine(), "n/a")%></td>
    <td>Path:</td><td><%=MS2Controller.defaultIfNull(run.getPath(), "n/a")%></td>
    </tr><tr>
    <td>Mass Spec Type:</td><td><%=MS2Controller.defaultIfNull(run.getMassSpecType(), "n/a")%></td>
    <td>Fasta File:</td><td><%=MS2Controller.defaultIfNull(run.getFastaFileName(), "n/a")%></td>
    </tr><%

if (null != bean.quantAlgorithm)
{ %>
    <tr><td>Quantitation:</td><td><%=h(bean.quantAlgorithm)%></td></tr><%
} %>
    <tr><td colspan=4><%

if (bean.writePermissions)
{ %>
    <a href="renameRun.view?run=<%=run.getRun()%>"><img border=0 src="<%=PageFlowUtil.buttonSrc("Rename")%>"></a><%
} %>
    <%=bean.modHref%><%

if (null != run.getParamsFileName() && null != run.getPath())
{ %>
    <a target="paramFile" href="showParamsFile.view?run=<%=run.getRun()%>"><img border=0 src="<%=PageFlowUtil.buttonSrc("Show " + run.getParamsFileName())%>"></a><%
}

if (run.getHasPeptideProphet())
{ %>
    <a target="peptideProphetSummary" href="showPeptideProphetDetails.view?run=<%=run.getRun()%>"><img border=0 src="<%=PageFlowUtil.buttonSrc("Show Peptide Prophet Details")%>"></a><%
}

if (run.hasProteinProphet())
{ %>
    <a target="proteinProphetSummary" href="showProteinProphetDetails.view?run=<%=run.getRun()%>"><img border=0 src="<%=PageFlowUtil.buttonSrc("Show Protein Prophet Details")%>"></a><%
}

if(run.getNegativeHitCount() > run.getPeptideCount() / 3)
{ %>
    <a href="discriminateScore.view?run=<%=run.getRun()%>"><img border=0 src="<%=PageFlowUtil.buttonSrc("Discriminate")%>"></a><%
} %>
    </tr>
</table>