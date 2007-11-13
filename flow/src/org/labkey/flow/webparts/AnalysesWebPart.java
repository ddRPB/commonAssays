package org.labkey.flow.webparts;

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.DataRegion;
import org.labkey.api.security.ACL;
import org.labkey.api.view.*;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.query.FlowQuerySettings;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.view.FlowQueryView;

public class AnalysesWebPart extends FlowQueryView
{
    static public final WebPartFactory FACTORY = new WebPartFactory("Flow Analyses")
    {
        public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart wp) throws Exception
        {
            FlowSchema schema = new FlowSchema(portalCtx.getUser(), portalCtx.getContainer());
            FlowQuerySettings settings = schema.getSettings(wp, portalCtx);
            settings.setAllowChooseQuery(false);
            settings.setAllowChooseView(false);
            settings.setQueryName(FlowTableType.Analyses.toString());
            return new AnalysesWebPart(new ViewContext(portalCtx), schema, settings);
        }
    };

    public AnalysesWebPart(ViewContext context, FlowSchema schema, FlowQuerySettings settings)
    {
        super(context, schema, settings);
        setTitle("Flow Analyses");
        setShowExportButtons(false);
        setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        if (!getViewContext().hasPermission(ACL.PERM_INSERT))
            return;
        FlowScript[] scripts = FlowScript.getScripts(getContainer());
        FlowScript analysisScript = null;
        for (FlowScript script : scripts)
        {
            if (script.hasStep(FlowProtocolStep.analysis))
            {
                analysisScript = script;
                break;
            }
        }
        if (analysisScript == null)
        {
            return;
        }
        ActionButton btnAnalyze = new ActionButton("Analyze some flow runs", analysisScript.urlFor(AnalysisScriptController.Action.chooseRunsToAnalyze));
        bar.add(btnAnalyze);
        return;
    }
}
