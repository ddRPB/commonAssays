package org.labkey.ms2;

import org.labkey.ms2.client.CompareService;
import org.labkey.ms2.client.CompareResult;
import org.labkey.ms2.query.CompareProteinsView;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.query.QueryParam;
import org.apache.log4j.Logger;

/**
 * User: jeckels
 * Date: Jun 8, 2007
 */
public class CompareServiceImpl extends BaseRemoteService implements CompareService
{
    private static Logger _log = Logger.getLogger(CompareServiceImpl.class);
    private final MS2Controller _controller;

    public CompareServiceImpl(ViewContext context, MS2Controller controller)
    {
        super(context);
        _controller = controller;
    }

    public CompareResult getProteinProphetComparison(String originalURL) throws Exception
    {
        try
        {
            ActionURL url = new ActionURL(originalURL);
            int runList = Integer.parseInt(url.getParameter("runList"));
            String viewName = url.getParameter(MS2Controller.CompareProteinProphetQuerySetupAction.COMPARE_PROTEIN_PROPHET_PEPTIDES_FILTER + "." + QueryParam.viewName.toString());
            ViewContext queryContext = new ViewContext(_context);
            queryContext.setActionURL(url);

            CompareProteinsView view = new CompareProteinsView(queryContext, _controller, runList, false, viewName);
            return view.createCompareResult();
        }
        catch (Exception e)
        {
            _log.error("Problem generating comparison", e);
            throw e;
        }
    }

    public CompareResult getPeptideComparison(String originalURL) throws Exception
    {
/*        ActionURL url = new ActionURL(originalURL);
        int runList = Integer.parseInt(url.getParameter("runList"));
        String viewName = url.getParameter(MS2Controller..COMPARE_PEPTIDES_PEPTIDES_FILTER + "." + QueryParam.viewName.toString());
        ViewContext queryContext = new ViewContext(_context);
        queryContext.setActionURL(url);

        ComparePeptidesView view = new ComparePeptidesView(queryContext, _controller, runList, false, viewName);
        return view.createCompareResult();
        */
        throw new UnsupportedOperationException();
    }
}