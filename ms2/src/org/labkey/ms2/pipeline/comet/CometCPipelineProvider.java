/*
 * Copyright (c) 2005 LabKey Software, LLC
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
package org.labkey.ms2.pipeline.comet;

import org.labkey.api.pipeline.PipelinePerlClusterSupport;
import org.labkey.api.pipeline.PipelineProtocol;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;

import java.util.Map;
import java.util.List;
import java.net.URI;
import java.io.IOException;

/**
 * CometCPipelineProvider class
 * <p/>
 * Created: Nov 1, 2005
 *
 * @author bmaclean
 */
public class CometCPipelineProvider extends AbstractMS2SearchPipelineProvider
{
    public static String name = "Comet";

    public PipelinePerlClusterSupport _clusterSupport;

    public CometCPipelineProvider()
    {
        super(name);

        _clusterSupport = new PipelinePerlClusterSupport();
    }

    public void preDeleteStatusFile(PipelineStatusFile sf) throws StatusUpdateException
    {
        super.preDeleteStatusFile(sf);
        _clusterSupport.preDeleteStatusFile(sf);
    }

    public void preCompleteStatusFile(PipelineStatusFile sf) throws StatusUpdateException
    {
        super.preCompleteStatusFile(sf);
        _clusterSupport.preCompleteStatusFile(sf);
    }

    public boolean isStatusViewableFile(String name, String basename)
    {
        if ("comet.def".equals(name))
            return true;

        if (_clusterSupport.isStatusViewableFile(name, basename))
            return true;

        return super.isStatusViewableFile(name, basename);
    }

    public List<StatusAction> addStatusActions()
    {
        List<StatusAction> actions = super.addStatusActions();
        _clusterSupport.addStatusActions(actions);
        return actions;
    }

    public ActionURL handleStatusAction(ViewContext ctx, String name, PipelineStatusFile sf) throws HandlerException
    {
        ActionURL url = _clusterSupport.handleStatusAction(ctx, name, sf);
        if (url != null)
            return url;

        return super.handleStatusAction(ctx, name, sf);
    }

    public boolean supportsDirectories()
    {
        return true;
    }

    public boolean remembersDirectories()
    {
        return true;
    }

    public boolean hasRemoteDirectories()
    {
        return false;
    }

    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        // Never actually create a protocol based job.
        return null;
    }

    public void ensureEnabled() throws PipelineProtocol.PipelineValidationException
    {
        // Nothing to do.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public String getHelpTopic()
    {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public List<String> getSequenceDbPaths(URI sequenceRoot) throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public List<String> getSequenceDbDirList(URI sequenceRoot) throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public List<String> getTaxonomyList() throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support Mascot style taxonomy.");
    }

    public Map<String, String> getEnzymes() throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public Map<String, String> getResidue0Mods() throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }

    public Map<String, String> getResidue1Mods() throws IOException {
        // No user interface for this search type.
        throw new UnsupportedOperationException("Comet does not support search job creation.");
    }
}
