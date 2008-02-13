/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2.pipeline.sequest;

import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.util.AppProps;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * SequestPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class SequestPipelineJob extends AbstractMS2SearchPipelineJob implements SequestSearchTask.JobSupport
{
    enum Pipelines
    {
        sample, fraction, fractionGroup;

        public TaskId getTaskId()
        {
            return new TaskId(getClass().getEnclosingClass(), toString());
        }
    }

    private String _sequestServer;

    public SequestPipelineJob(SequestSearchProtocol protocol,
                              ViewBackgroundInfo info,
                              String name,
                              File dirSequenceRoot,
                              File filesMzXML[],
                              File fileInputXML,
                              boolean fromCluster) throws SQLException, IOException
    {
        super(protocol, SequestLocalPipelineProvider.name, info, name, dirSequenceRoot, fileInputXML, filesMzXML, fromCluster);

        AppProps appProps = AppProps.getInstance();
        _sequestServer = appProps.getSequestServer();

        header("Sequest search for " + getBaseName());
    }

    public SequestPipelineJob(SequestPipelineJob job, File fileFraction)
    {
        super(job, fileFraction);

        _sequestServer = job._sequestServer;
    }

    public String getSequestServer()
    {
        return _sequestServer;
    }

    public String getSearchEngine()
    {
        return "sequest";
    }

    public AbstractFileAnalysisJob createSingleFileJob(File file)
    {
        return new SequestPipelineJob(this, file);
    }

    public TaskId getTaskPipelineId()
    {
        TaskId tid = super.getTaskPipelineId();
        if (tid != null)
            return tid;

        if (getInputFiles().length > 1)
            return Pipelines.fractionGroup.getTaskId();
        if (!isSamples())
            return Pipelines.fraction.getTaskId();

        return Pipelines.sample.getTaskId();
    }

    public boolean isRefreshRequired()
    {
        return true;
    }

    public File getSearchNativeOutputFile()
    {
        return SequestSearchTask.getNativeOutputFile(getAnalysisDirectory(), getBaseName());
    }

    public String getXarTemplateResource()
    {
        StringBuilder templateResource = new StringBuilder("org/labkey/ms2/pipeline/sequest/templates/MS2SearchSequest");
         if (getInputFiles().length > 1)
         {
             templateResource.append("Fractions");
         }
         if (isXPressQuantitation())
         {
             templateResource.append("Xpress");
         }
         templateResource.append(".xml");

         return templateResource.toString();
    }
}
