/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.di.steps;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.di.ScheduledPipelineJobDescriptor;
import org.labkey.api.etl.AsyncDataIterator;
import org.labkey.api.etl.CopyConfig;
import org.labkey.api.etl.DataIteratorBuilder;
import org.labkey.api.etl.DataIteratorContext;
import org.labkey.api.etl.QueryDataIteratorBuilder;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QueryParseException;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.di.TransformDataIteratorBuilder;
import org.labkey.di.data.TransformProperty;
import org.labkey.di.filters.FilterStrategy;
import org.labkey.di.filters.ModifiedSinceFilterStrategy;
import org.labkey.di.pipeline.BaseQueryTransformDescriptor;
import org.labkey.di.pipeline.TransformJobContext;
import org.labkey.di.pipeline.TransformTask;
import org.labkey.di.pipeline.TransformTaskFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewb
 * Date: 2013-04-16
 * Time: 12:27 PM
 */
public class SimpleQueryTransformStep extends TransformTask
{
    final SimpleQueryTransformStepMeta _meta;
    final TransformJobContext _context;
    // todo: make these long again but then update the AbstractParameter code
    // or else you'll get a cast exception.
    int _recordsInserted = -1;
    int _recordsDeleted = -1;

    boolean _useAsynchrousQuery = false;

    public SimpleQueryTransformStep(TransformTaskFactory f, PipelineJob job, SimpleQueryTransformStepMeta meta, TransformJobContext context)
    {
        super(f, job);
        _meta = meta;
        _context = context;
    }


    @Override
    public boolean hasWork()
    {
        QuerySchema sourceSchema = DefaultSchema.get(_context.getUser(), _context.getContainer(), _meta.getSourceSchema());
        if (null == sourceSchema || null == sourceSchema.getDbSchema())
            throw new IllegalArgumentException("ERROR: Source schema not found: " + _meta.getSourceSchema());

        TableInfo t = sourceSchema.getTable(_meta.getSourceQuery());
        if (null == t)
            throw new IllegalArgumentException("Could not find table: " +  _meta.getSourceSchema() + "." + _meta.getSourceQuery());

        FilterStrategy filterStrategy = getFilterStrategy();
        return filterStrategy.hasWork();
    }


    public void doWork(RecordedAction action) throws PipelineJobException
    {
        try
        {
            getJob().getLogger().info("SimpleQueryTransformStep.doWork called");
            if (!executeCopy(_meta, _context.getContainer(), _context.getUser(), getJob().getLogger()))
                getJob().setStatus("ERROR");
            recordWork(action);
        }
        catch (Exception x)
        {
            getJob().getLogger().error(x);
        }
   }

    private void recordWork(RecordedAction action)
    {
        if (-1 != _recordsInserted)
            action.addProperty(TransformProperty.RecordsInserted.getPropertyDescriptor(), _recordsInserted);
        if (-1 != _recordsDeleted)
            action.addProperty(TransformProperty.RecordsDeleted.getPropertyDescriptor(), _recordsDeleted);

        try
        {
            // input is source table
            // output is dest table
            // todo: this is a fake URI, figure out the real story for the Data Input/Ouput for a transform step
            action.addInput(new URI(_meta.getSourceSchema() + "." + _meta.getSourceQuery()), TransformTask.INPUT_ROLE);
            action.addOutput(new URI(_meta.getTargetSchema() + "." + _meta.getTargetQuery()), TransformTask.OUTPUT_ROLE, false);
        }
        catch (URISyntaxException ignore)
        {
        }
    }


    public boolean executeCopy(CopyConfig meta, Container c, User u, Logger log) throws IOException, SQLException
    {
        QuerySchema sourceSchema = DefaultSchema.get(u, c, meta.getSourceSchema());
        if (null == sourceSchema || null == sourceSchema.getDbSchema())
        {
            log.error("ERROR: Source schema not found: " + meta.getSourceSchema());
            return false;
        }

        QuerySchema targetSchema = DefaultSchema.get(u, c, meta.getTargetSchema());
        if (null == targetSchema || null == targetSchema.getDbSchema())
        {
            log.error("ERROR: Target schema not found: " + meta.getTargetSchema());
            return false;
        }

        DbScope targetScope = targetSchema.getDbSchema().getScope();
        DbScope sourceScope = sourceSchema.getDbSchema().getScope();
        if (sourceScope.equals(targetScope))
            sourceScope = null;

        ResultSet rs = null;
        DataIteratorContext context = new DataIteratorContext();
        context.setInsertOption(QueryUpdateService.InsertOption.MERGE);
        context.setFailFast(true);
        try
        {
            targetScope.ensureTransaction(Connection.TRANSACTION_SERIALIZABLE);
            if (null != sourceScope)
                sourceScope.ensureTransaction(Connection.TRANSACTION_REPEATABLE_READ);

            long start = System.currentTimeMillis();
            log.info(DateUtil.toISO(start) + " Copying data from " + meta.getSourceSchema() + "." + meta.getSourceQuery() + " to " +
                    meta.getTargetSchema() + "." + meta.getTargetQuery());

            DataIteratorBuilder source = selectFromSource(meta, c, u, context, log);
            if (null == source)
                return false;
            int transformRunId = getTransformJob().getTransformRunId();
            DataIteratorBuilder transformSource = new TransformDataIteratorBuilder(transformRunId, source);

            _recordsInserted = appendToTarget(meta, c, u, context, transformSource);

            targetScope.commitTransaction();
            if (null != sourceScope)
                sourceScope.commitTransaction();

            long finish = System.currentTimeMillis();
            log.info(DateUtil.toISO(finish) + " Copied " + _recordsInserted + " row" + (_recordsInserted != 1 ? "s" : "") + " in " + DateUtil.formatDuration(finish - start) + ".");
        }
        catch (Exception x)
        {
            log.error(x);
            return false;
        }
        finally
        {
            ResultSetUtil.close(rs);
            targetScope.closeConnection();
            if (null != sourceScope)
                sourceScope.closeConnection();
        }
        if (context.getErrors().hasErrors())
        {
            for (ValidationException v : context.getErrors().getRowErrors())
            {
                log.error(v.getMessage());
            }
            return false;
        }
        return true;
    }


    DataIteratorBuilder selectFromSource(CopyConfig meta, Container c, User u, DataIteratorContext context, Logger log) throws SQLException
    {
        try
        {
            QuerySchema sourceSchema = DefaultSchema.get(u, c, meta.getSourceSchema());
            TableInfo t = sourceSchema.getTable(meta.getSourceQuery());
            FilterStrategy filterStrategy = getFilterStrategy();
            SimpleFilter f = filterStrategy.getFilter(getVariableMap());

            DataIteratorBuilder source = new QueryDataIteratorBuilder(sourceSchema, meta.getSourceQuery(), null, f);
            if (_useAsynchrousQuery)
                source = new AsyncDataIterator.Builder(source);
            return source;
        }
        catch (QueryParseException x)
        {
            log.error(x.getMessage());
            return null;
        }
    }


    static int appendToTarget(CopyConfig meta, Container c, User u, DataIteratorContext context, DataIteratorBuilder source)
    {
        QuerySchema querySchema =  DefaultSchema.get(u, c, meta.getTargetSchema());
        if (null == querySchema || null == querySchema.getDbSchema())
        {
            context.getErrors().addRowError(new ValidationException("Could not find schema: " + meta.getTargetSchema()));
            return -1;
        }
        TableInfo targetTableInfo = querySchema.getTable(meta.getTargetQuery());
        if (null == targetTableInfo)
        {
            context.getErrors().addRowError(new ValidationException("Could not find table: " +  meta.getTargetSchema() + "." + meta.getTargetQuery()));
            return -1;
        }
        try
        {
            QueryUpdateService qus = targetTableInfo.getUpdateService();
            if (null == qus)
            {
                context.getErrors().addRowError(new ValidationException("Can't import into table: " + meta.getTargetSchema() + "." + meta.getTargetQuery()));
                return -1;
            }
            if (CopyConfig.TargetOptions.merge == meta.getTargetOptions())
                return qus.mergeRows(u, c, source.getDataIterator(context), context.getErrors(), null);
            else
                return qus.importRows(u, c, source, context.getErrors(), null);
        }
        catch (SQLException sqlx)
        {
            throw new RuntimeException(sqlx);
        }
    }


    FilterStrategy _filterStrategy = null;

    FilterStrategy getFilterStrategy()
    {
        if (null == _filterStrategy)
        {
            FilterStrategy.Factory factory = null;
            ScheduledPipelineJobDescriptor jd = _context.getJobDescriptor();
            if (jd instanceof BaseQueryTransformDescriptor)
                factory = ((BaseQueryTransformDescriptor)jd).getDefaultFilterFactory();
            if (null == factory)
                factory = new ModifiedSinceFilterStrategy.Factory(jd);

            _filterStrategy = factory.getFilterStrategy(_context, _meta);
        }

        return _filterStrategy;
    }
}