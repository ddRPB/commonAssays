/*
 * Copyright (c) 2011 LabKey Corporation
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
package org.labkey.flow.reports;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.CachedResultSet;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.FilterInfo;
import org.labkey.api.data.Results;
import org.labkey.api.data.ResultsImpl;
import org.labkey.api.query.AliasManager;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryService;
import org.labkey.api.reports.report.RReport;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.reports.report.ScriptReportDescriptor;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.ICSMetadata;
import org.labkey.flow.query.FlowSchema;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public abstract class FilterFlowReport extends FlowReport
{
    public RReport _inner = null;
    public String _query = null;

    abstract String getScriptResource() throws IOException;

    public RReport getInnerReport() throws IOException
    {
        if (null == _inner)
        {
            _inner = new RReport()
            {
                @Override
                public Results generateResults(ViewContext context) throws Exception
                {
                    ResultSet rs = generateResultSet(context);
                    return rs == null ? null : new ResultsImpl(rs);
                }

                @Override
                protected String getScriptProlog(ViewContext context, File inputFile)
                {
                    String labkeyProlog = super.getScriptProlog(context, inputFile);

                    StringBuffer reportProlog = new StringBuffer(labkeyProlog);
                    reportProlog.append("report.parameters <- list(");
                    ReportDescriptor d = FilterFlowReport.this.getDescriptor();
                    Map<String, Object> props = d.getProperties();
                    String comma = "";

                    for (Map.Entry<String, Object> e : props.entrySet())
                    {
                        String key = e.getKey();
                        if (ScriptReportDescriptor.Prop.script.name().equals(key))
                            continue;
                        String value = null == e.getValue() ? null : String.valueOf(e.getValue());
                        reportProlog.append(comma);
                        reportProlog.append(toR(e.getKey())).append("=").append(toR(value));
                        comma = ",";
                    }

                    reportProlog.append(")\n");
                    addScriptProlog(context, reportProlog);
                    return reportProlog.toString();
                }
            };

            String script = getScriptResource();
            _inner.setScriptSource(script);
        }

        return _inner;
    }

    protected ICSMetadata getMetadata(Container c)
    {
        FlowProtocol protocol = FlowProtocol.getForContainer(c);
        if (protocol == null)
            throw new NotFoundException("flow protocol not found");

        ICSMetadata metadata = protocol.getICSMetadata();
        if (metadata == null || metadata.isEmpty())
            return null;

        return metadata;
    }

    protected Collection<FieldKey> getMatchColumns(ICSMetadata metadata)
    {
        Collection<FieldKey> fieldKeys = new ArrayList<FieldKey>(metadata.getMatchColumns().size());

        for (FieldKey fieldKey : metadata.getMatchColumns())
        {
            // Use the 'Run' RowId instead of Run.  The 'Run' display name is already added to the select list.
            if (fieldKey.getName().equals("Run"))
                fieldKey = new FieldKey(fieldKey, "RowId");
            fieldKeys.add(fieldKey);
        }

        return fieldKeys;
    }

    void addScriptProlog(ViewContext context, StringBuffer sb)
    {
        ICSMetadata metadata = getMetadata(context.getContainer());
        if (metadata == null)
            return;

        String comma = "";
        sb.append("flow.metadata.matchColumns <- c(");
        for (FieldKey fieldKey : getMatchColumns(metadata))
        {
            if (fieldKey != null)
            {
                String name = oldLegalName(fieldKey);
                sb.append(comma).append("\"").append(name).append("\"");
                comma = ", ";
            }
        }
        sb.append(")\n");

        comma = "";
        sb.append("flow.metadata.background <- list(");
        for (FilterInfo filter : metadata.getBackgroundFilter())
        {
            if (filter != null && filter.getField() != null && filter.getOp() != null)
            {
                sb.append(comma);
                sb.append("list(");

                String name = oldLegalName(filter.getField());
                sb.append("\"filter\"=\"").append(name).append("\"");
                sb.append(", \"op\"=\"").append(filter.getOp()).append("\"");
                if (filter.getValue() != null)
                    sb.append(", \"value\"=\"").append(filter.getValue()).append("\"");

                sb.append(")");
                comma = ", ";
            }
        }
        sb.append(")\n");
    }

    // UNDONE: Get the name of the column from the Results metadata to match ScriptEngineReport.outputColumnNames().
    // Copied from ScriptEngineReport.
    private String oldLegalName(FieldKey fkey)
    {
        String r = AliasManager.makeLegalName(StringUtils.join(fkey.getParts(), "_"), null, false);
//        if (r.length() > 40)
//            r = r.substring(0,40);
        return ColumnInfo.propNameFromName(r).toLowerCase();
    }

    protected void convertDateColumn(CachedResultSet rs, String fromCol, String toCol) throws SQLException
    {
        int from = rs.findColumn(fromCol);
        int to = rs.findColumn(toCol);

        while (rs.next())
        {
            Object o = rs.getObject(from);

            if (o != null)
            {
                Date d = null;

                if (o instanceof Date)
                {
                    d = (Date) o;
                }
                else
                {
                    String s = String.valueOf(o);

                    try
                    {
                        d = new Date(DateUtil.parseDateTime(s));
                    }
                    catch (ConversionException x)
                    {
                        try
                        {
                            d = new Date(DateUtil.parseDateTime(s.replace('-', ' ')));
                        }
                        catch (ConversionException y)
                        {
                        }
                    }
                }

                rs._setObject(to, d);
            }
        }

        rs.beforeFirst();
    }

    protected CachedResultSet filterDateRange(CachedResultSet rs, String dateColumn, Date start, Date end) throws SQLException
    {
        int col = rs.findColumn(dateColumn);
        if (null == start && null == end)
            return rs;
        int size = rs.getSize();
        ArrayList<Map<String, Object>> rows = new ArrayList<Map<String, Object>>(size);
        rs.beforeFirst();

        while (rs.next())
        {
            Date d = rs.getTimestamp(col);
            if (null == d || null != start && start.compareTo(d) > 0 || null != end && end.compareTo(d) <= 0)
                continue;
            rows.add(rs.getRowMap());
        }

        CachedResultSet ret;

        if (rs.getSize() == rows.size())
            ret = rs;
        else
        {
            ret = new CachedResultSet(rs.getMetaData(), false, rows, true);
            rs.close();
        }

        ret.beforeFirst();

        return ret;
    }

    protected ResultSet generateResultSet(ViewContext context) throws Exception
    {
        ReportDescriptor d = getDescriptor();
        ArrayList<ControlsQCReport.Filter> filters = new ArrayList<ControlsQCReport.Filter>();
        for (int i = 0; i < 20; i++)
        {
            ControlsQCReport.Filter f = new ControlsQCReport.Filter(d, i);
            if (f.isValid())
                filters.add(f);
        }

        String wellURL = new ActionURL(WellController.ShowWellAction.class, context.getContainer()).addParameter("wellId", "").getLocalURIString();
        String runURL = new ActionURL(RunController.ShowRunAction.class, context.getContainer()).addParameter("runId", "").getLocalURIString();
        Date startDate = null;
        Date endDate = null;

        // UNDONE SQL ENCODING
        StringBuilder query = new StringBuilder();
        query.append("SELECT\n");
        query.append("  A.Run.Name AS run,\n");
        query.append("  ").append(toSQL(runURL)).append(" || CONVERT(A.Run, SQL_VARCHAR) AS \"run.href\",\n");
        query.append("  A.Name AS well,\n");
        query.append("  ").append(toSQL(wellURL)).append(" || CONVERT(A.RowID, SQL_VARCHAR) AS \"well.href\",\n");
        query.append("  A.FCSFile.Keyword.\"EXPORT TIME\" AS Xdatetime,\n");
        query.append("  NULL AS datetime,\n");
        addSelectList(context, "A", query);
        query.append("FROM FCSAnalyses A");
        String and = "\nWHERE ";
        for (ControlsQCReport.Filter f : filters)
        {
            if ("keyword".equals(f.type))
            {
                if ("EXPORT TIME".equals(f.property))
                {
                    if ("gte".equals(f.op) && !StringUtils.isEmpty(f.value))
                        try
                        {
                            startDate = new Date(DateUtil.parseDateTime(f.value));
                        }
                        catch (ConversionException x)
                        {
                        }
                    if ("lt".equals(f.op) && !StringUtils.isEmpty(f.value))
                        try
                        {
                            endDate = new Date(DateUtil.parseDateTime(f.value));
                        }
                        catch (ConversionException x)
                        {
                        }
                    continue;
                }
                query.append(and);
                query.append("A.FCSFile.Keyword.\"" + f.property + "\" = " + toSQL(f.value));
                and = " AND ";
            }
            else if ("sample".equals(f.type))
            {
                query.append(and);
                query.append("A.FCSFile.Sample.Property.\"" + f.property + "\" = " + toSQL(f.value));
                and = " AND\n";
            }
        }
        _query = query.toString();
        QuerySchema flow = new FlowSchema(context);
        ResultSet rs = QueryService.get().select(flow, _query);
        convertDateColumn((CachedResultSet) rs, "Xdatetime", "datetime");
        rs = filterDateRange((CachedResultSet) rs, "datetime", startDate, endDate);
        return rs;
    }

    /** Add any additional columns to the query select list. */
    abstract void addSelectList(ViewContext context, String tableName, StringBuilder query);

    public HttpView renderReport(ViewContext context) throws Exception
    {
        RReport r = getInnerReport();
        HttpView plot = r.renderReport(context);
        return new VBox(
                plot,
                new HtmlView(PageFlowUtil.filter(_query, true))
        );
    }

    public void updateFilterProperties(PropertyValues pvs)
    {
        ReportDescriptor d = getDescriptor();

        // delete all previous
        for (String key : getDescriptor().getProperties().keySet())
        {
            if (key.startsWith("filter["))
                d.setProperty(key, null);
        }

        int count = 0;
        for (int i = 0; i < 20; i++)
        {
            Filter f = new Filter(pvs, i);
            if (f.isValid())
            {
                d.setProperty("filter[" + count + "].property", f.property);
                d.setProperty("filter[" + count + "].type", f.type);
                d.setProperty("filter[" + count + "].value", f.value);
                d.setProperty("filter[" + count + "].op", null == f.op ? "eq" : f.op);
                count++;
            }
        }
    }

    public String toSQL(String s)
    {
        return null == s ? "''" : "'" + StringUtils.replace(s, "'", "\'\'") + "'";
    }

    public String toSQL(FieldKey fieldKey)
    {
        String sep = "";
        StringBuilder sb = new StringBuilder();
        for (String part : fieldKey.getParts())
        {
            sb.append(sep).append("\"").append(part).append("\"");
            sep = ".";
        }
        return sb.toString();
    }

    public String toR(String s)
    {
        String r = PageFlowUtil.jsString(s);
        return "\"" + StringUtils.strip(r, "'") + "\"";
    }

    public static class Filter
    {
        public String property;
        public String type;
        public String value;
        public String op = "eq";

        private String _get(PropertyValues pvs, String key)
        {
            PropertyValue pv = pvs.getPropertyValue(key);
            return null == pv ? null : pv.getValue() == null ? null : String.valueOf(pv.getValue());
        }

        Filter(PropertyValues pvs, int i)
        {
            property = _get(pvs, "filter[" + i + "].property");
            type = _get(pvs, "filter[" + i + "].type");
            value = _get(pvs, "filter[" + i + "].value");
            op = _get(pvs, "filter[" + i + "].op");
        }

        public Filter(ReportDescriptor d, int i)
        {
            property = d.getProperty("filter[" + i + "].property");
            type = d.getProperty("filter[" + i + "].type");
            value = d.getProperty("filter[" + i + "].value");
            op = d.getProperty("filter[" + i + "].op");
        }

        boolean isValid()
        {
            return !StringUtils.isEmpty(property) &&
                    !StringUtils.isEmpty(value) &&
                    ("keyword".equals(type) || "sample".equals(type));
        }
    }
}
