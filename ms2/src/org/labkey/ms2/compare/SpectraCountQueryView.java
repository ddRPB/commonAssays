package org.labkey.ms2.compare;

import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.CreateChartButton;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.ActionButton;
import org.labkey.api.view.ActionURL;
import org.labkey.api.reports.report.view.ChartUtil;
import org.labkey.api.reports.report.view.RReportBean;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.SpectraCountConfiguration;
import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
* Date: Jan 22, 2008
*/
public class SpectraCountQueryView extends QueryView
{
    private final MS2Schema _schema;
    private final SpectraCountConfiguration _config;
    private final String _peptideViewName;
    private final int _runListId;

    public SpectraCountQueryView(MS2Schema schema, QuerySettings settings, SpectraCountConfiguration config, String peptideViewName, int runListId)
    {
        super(schema, settings);
        _schema = schema;
        _config = config;
        _peptideViewName = peptideViewName;
        _runListId = runListId;
    }

    protected TableInfo createTable()
    {
        return _schema.createSpectraCountTable(_config, getViewContext().getRequest(), _peptideViewName);
    }

    public ActionButton createReportButton()
    {
        RReportBean bean = new RReportBean();
        bean.setReportType(SpectraCountRReport.TYPE);
        bean.setSchemaName(getSchema().getSchemaName());
        bean.setQueryName(getSettings().getQueryName());
        bean.setViewName(getSettings().getViewName());
        bean.setDataRegionName(getDataRegionName());
	// the redirect after a save
        bean.setRedirectUrl(getViewContext().getActionURL().toString());

	    // your custom params...
        bean.addParam("runIndex", Integer.toString(_runListId));
//        bean.addParam("forExport", Boolean.toString(_forExport));

        ActionURL chartURL = ChartUtil.getRReportDesignerURL(_viewContext, bean);
        ActionURL url = getViewContext().getActionURL();
        chartURL.replaceParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME, url.getParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME));
        chartURL.replaceParameter("spectraConfig", url.getParameter("spectraConfig"));
        chartURL.replaceParameter("runList", url.getParameter("runList"));
        
        return new CreateChartButton(chartURL.toString(), false, true,
                getSchema().getSchemaName(), getSettings().getQueryName(), getSettings().getViewName(), SpectraCountRReport.TYPE);
    }
}
