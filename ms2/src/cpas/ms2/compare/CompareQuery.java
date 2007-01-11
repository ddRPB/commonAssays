package cpas.ms2.compare;

import org.labkey.api.view.ViewURLHelper;
import org.fhcrc.cpas.util.Pair;
import org.labkey.api.ms2.MS2Run;
import org.labkey.api.ms2.MS2Manager;
import org.labkey.api.data.*;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;

/**
 * User: adam
 * Date: Oct 3, 2006
 * Time: 10:07:32 AM
 */
public abstract class CompareQuery extends SQLFragment
{
    protected ViewURLHelper _currentUrl;
    protected String _compareColumn;
    protected List<RunColumn> _gridColumns = new ArrayList<RunColumn>();
    protected List<MS2Run> _runs;
    protected int _runCount;
    private int _indent = 0;
    private String _header;
    protected static final String HEADER_PREFIX = "Numbers below represent ";

    public static CompareQuery getCompareQuery(String compareColumn /* TODO: Get this from url? */, ViewURLHelper currentUrl, List<MS2Run> runs)
    {
        if ("Peptide".equals(compareColumn))
            return new PeptideCompareQuery(currentUrl, runs);
        else if ("Protein".equals(compareColumn))
            return new ProteinCompareQuery(currentUrl, runs);
        else if ("ProteinProphet".equals(compareColumn))
            return new ProteinProphetCompareQuery(currentUrl, runs);
        else
            return null;
    }

    protected CompareQuery(ViewURLHelper currentUrl, String compareColumn, List<MS2Run> runs)
    {
        _currentUrl = currentUrl;
        _compareColumn = compareColumn;
        _runs = runs;
        _runCount = _runs.size();
    }

    public abstract String getComparisonDescription();

    protected void addGridColumn(String label, String name)
    {
        addGridColumn(label, name, "COUNT");
    }

    protected void addGridColumn(String label, String name, String aggregate)
    {
        _gridColumns.add(new RunColumn(label, name, aggregate));
    }


    public ResultSet createResultSet() throws SQLException
    {
        generateSql();
        return Table.executeQuery(MS2Manager.getSchema(), getSQL(), getParams().toArray());
    }

    protected String getLabelColumn()
    {
        return _compareColumn;
    }

    protected void generateSql()
    {
        selectColumns();
        selectRows();
        groupByCompareColumn();
        sort();
    }

    protected void selectColumns()
    {
        // SELECT SeqId, Max(Run0Total) AS Run0Total, MAX(Run0Unique) AS Run0Unique..., COUNT(Run) As RunCount,
        append("SELECT ");
        append(_compareColumn);
        append(",");
        indent();
        appendNewLine();

        for (int i = 0; i < _runCount; i++)
        {
            for (RunColumn column : _gridColumns)
            {
                append("MAX(Run");
                append(i);
                append(column.getLabel());
                append(") AS Run");
                append(i);
                append(column.getLabel());
                append(", ");
            }
            appendNewLine();
        }
        append("COUNT(Run) AS RunCount,");
        appendNewLine();

        String firstColumnName = _gridColumns.get(0).getLabel();

        // (CASE WHEN MAX(Run0Total) IS NULL THEN 0 ELSE 8) + (CASE WHEN MAX(Run1Total) IS NULL THEN 4 ELSE 0) + ... AS Pattern
        for (int i = 0; i < _runCount; i++)
        {
            if (i > 0)
                append("+");
            append("(CASE WHEN MAX(Run");
            append(i);
            append(firstColumnName);
            append(") IS NULL THEN 0 ELSE ");
            append(Math.round(Math.pow(2, _runCount - i - 1)));
            append(" END)");
        }
        append(" AS Pattern");
    }

    protected void selectRows()
    {
        // FROM (SELECT Run, SeqId, CASE WHEN Run=1 THEN COUNT(DISTINCT Peptide) ELSE NULL END AS Run0, ... FROM MS2Peptides WHERE (peptideFilter)
        //       AND Run IN (?, ?, ...) GROUP BY Run, SeqId
        outdent();
        appendNewLine();
        append("FROM");
        appendNewLine();
        append("(");
        indent();
        appendNewLine();
        append("SELECT Run, ");
        append(_compareColumn);
        indent();

        for (int i = 0; i < _runCount; i++)
        {
            String separator = "," + getNewLine();

            for (RunColumn column : _gridColumns)
            {
                append(separator);
                append("CASE WHEN Run=?");
                add(_runs.get(i).getRun());
                append(" THEN ");
                append(column.getAggregate());
                append("(");
                append(column.getName());
                append(") ELSE NULL END AS Run");
                append(i);
                append(column.getLabel());
                separator = ", ";
            }
        }
        outdent();
        appendNewLine();
        append("FROM ");
        append(getFromClause());
        appendNewLine();
        SimpleFilter filter = new SimpleFilter();
        filter.addInClause("Run", MS2Manager.getRunIds(_runs));

        addWhereClauses(filter);

        append(filter.getWhereSQL(MS2Manager.getSqlDialect()));
        addAll(filter.getWhereParams(MS2Manager.getTableInfoRuns()));
        
        appendNewLine();
        append("GROUP BY Run, ");
        append(_compareColumn);
    }

    protected String getFromClause()
    {
        return MS2Manager.getTableInfoPeptides().toString();        
    }

    protected abstract void addWhereClauses(SimpleFilter filter);

    protected void groupByCompareColumn()
    {
        outdent();
        appendNewLine();
        append(") X");
        appendNewLine();

        // GROUP BY Peptide/SeqId
        append("GROUP BY ");
        append(_compareColumn);
    }

    protected void sort()
    {
        appendNewLine();
        // ORDER BY RunCount DESC, Pattern DESC, Protein ASC (plus apply any URL sort)
        Sort sort = new Sort("-RunCount,-Pattern," + getLabelColumn());
        sort.applyURLSort(_currentUrl, MS2Manager.getDataRegionNameCompare());
        append(sort.getOrderByClause(MS2Manager.getSqlDialect()));
    }

    protected void appendNewLine()
    {
        append(getNewLine());
    }

    protected String getNewLine()
    {
        assert _indent >= 0;
        return "\n" + StringUtils.repeat("\t", _indent);
    }

    protected void indent()
    {
        _indent++;
    }

    protected void outdent()
    {
        _indent--;
    }

    protected void setHeader(String header)
    {
        _header = header;
    }

    public String getHeader()
    {
        return _header;
    }

    public List<RunColumn> getGridColumns()
    {
        return _gridColumns;
    }

    /** @return link filter */
    protected abstract String setupComparisonColumnLink(ViewURLHelper linkURL, String columnName, String runPrefix);

    // CONSIDER: Split into getCompareGrid (for Excel export) and getCompareGridForDisplay?
    public CompareDataRegion getCompareGrid() throws SQLException
    {
        CompareDataRegion rgn = new CompareDataRegion(createResultSet());
        TableInfo ti = MS2Manager.getTableInfoCompare();
        rgn.addColumn(getComparisonCommonColumn(ti));

        ViewURLHelper originalLinkURL = this._currentUrl.clone();
        originalLinkURL.deleteParameters(".select");
        originalLinkURL.deleteParameter("column");
        originalLinkURL.deleteParameter("total");
        originalLinkURL.deleteParameter("unique");

        Pair<String, String>[] params = originalLinkURL.getParameters();

        for (Pair<String, String> param : params)
            if (param.getKey().startsWith(MS2Manager.getDataRegionNameCompare()))
                originalLinkURL.deleteParameter(param.getKey());

        ResultSetMetaData md = rgn.getResultSet().getMetaData();

        for (int i = 0; i < _runs.size(); i++)
        {
            ViewURLHelper linkURL = originalLinkURL.clone();
            linkURL.setExtraPath(ContainerManager.getForId(_runs.get(i).getContainer()).getPath());
            linkURL.replaceParameter("run", String.valueOf(_runs.get(i).getRun()));

            for (RunColumn column : _gridColumns)
            {
                String runPrefix = "Run" + i;
                String columnName = runPrefix + column.getLabel();

                DisplayColumn displayColumn = createColumn(linkURL, column, runPrefix, columnName, ti, md, rgn);
                if (displayColumn != null)
                {
                    rgn.addColumn(displayColumn);
                }
            }
        }

        rgn.addColumns(ti.getColumns("RunCount, Pattern"));
        rgn.setFixedWidthColumns(false);
        rgn.setShowFilters(false);

        ButtonBar bb = new ButtonBar();

        ViewURLHelper excelUrl = _currentUrl.clone();
        ActionButton exportAll = new ActionButton("ExportAll", "Export to Excel");
        excelUrl.setAction("exportCompareToExcel");
        exportAll.setURL(excelUrl.getEncodedLocalURIString());
        exportAll.setActionType(ActionButton.Action.LINK);
        bb.add(exportAll);

        rgn.setButtonBar(bb, DataRegion.MODE_GRID);

        return rgn;
    }

    protected DisplayColumn createColumn(ViewURLHelper linkURL, RunColumn column, String runPrefix, String columnName, TableInfo ti, ResultSetMetaData md, CompareDataRegion rgn)
        throws SQLException
    {
        String columnFilter = setupComparisonColumnLink(linkURL, column.getLabel(), runPrefix);
        ColumnInfo ci = new ColumnInfo(columnName);
        ci.setParentTable(ti);
        ci.setSqlTypeName(md.getColumnTypeName(rgn.getResultSet().findColumn(columnName)));
        ci.setCaption(column.getLabel());
        DataColumn dc = new DataColumn(ci);
        dc.setURL(linkURL.getLocalURIString() + "&" + columnFilter);
        return dc;
    }

    protected abstract ColumnInfo getComparisonCommonColumn(TableInfo ti);

    public abstract List<Pair<String, String>> getSQLSummaries();
}
