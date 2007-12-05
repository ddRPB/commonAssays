/*
 * Copyright (c) 2007 LabKey Software Foundation
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
package org.labkey.ms2.pipeline;

import org.apache.beehive.netui.pageflow.FormData;

/**
 * <code>MS2SearchForm</code>
*/
public class MS2SearchForm extends FormData
{
    private String path;
    private String protocol = "";
    private String protocolName = "";
    private String protocolDescription = "";
    private String sequenceDBPath = "";
    private String[] sequenceDBs = new String[0];
    private String configureXml = "";
    private boolean skipDescription;
    private String searchEngine = XTandemCPipelineProvider.name;
    private boolean saveProtocol = false;

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String[] getSequenceDBs()
    {
        return sequenceDBs;
    }

    public void setSequenceDBs(String[] sequenceDBs)
    {
        this.sequenceDBs = (sequenceDBs == null ? new String[0] : sequenceDBs);
    }

    public String getConfigureXml()
    {
        return configureXml;
    }

    public void setConfigureXml(String configureXml)
    {
        this.configureXml = (configureXml == null ? "" : configureXml);
    }

    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = (protocol == null ? "" : protocol);
    }

    public String getProtocolName()
    {
        return protocolName;
    }

    public void setProtocolName(String protocolName)
    {
        this.protocolName = (protocolName == null ? "" : protocolName);
    }

    public String getProtocolDescription()
    {
        return protocolDescription;
    }

    public void setProtocolDescription(String protocolDescription)
    {
        this.protocolDescription = (protocolDescription == null ? "" : protocolDescription);
    }

    public String getSequenceDBPath()
    {
        return sequenceDBPath;
    }

    public void setSequenceDBPath(String sequenceDBPath)
    {
        this.sequenceDBPath = sequenceDBPath;
    }

    public boolean isSkipDescription()
    {
        return skipDescription;
    }

    public void setSkipDescription(boolean skipDescription)
    {
        this.skipDescription = skipDescription;
    }

    public String getSearchEngine()
    {
        return searchEngine;
    }

    public void setSearchEngine(String searchEngine)
    {
        this.searchEngine = searchEngine;
    }

    public boolean isSaveProtocol()
    {
        return saveProtocol;
    }

    public void setSaveProtocol(boolean saveProtocol)
    {
        this.saveProtocol = saveProtocol;
    }
}
