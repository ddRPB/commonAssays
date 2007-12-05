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
package org.labkey.ms2.protocol;

import org.apache.log4j.Logger;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.MascotPipelineJob;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * MascotSearchProtocol class
 * <p/>
 * Created: Jun 6, 2006
 *
 * @author bmaclean
 */
public class MascotSearchProtocol extends MS2SearchPipelineProtocol
{
    private static Logger _log = Logger.getLogger(MascotSearchProtocol.class);

    private String mascotServer;
    private String mascotHTTPProxy;

    public MascotSearchProtocol(String name, String description, String[] dbNames, String xml)
    {
        super(name, description, dbNames, xml);
    }

    public String getMascotServer ()
    {
        return mascotServer;
    }

    public void setMascotServer (String mascotServer)
    {
        this.mascotServer = mascotServer;
    }

    public String getMascotHTTPProxy ()
    {
        return mascotHTTPProxy;
    }

    public void setMascotHTTPProxy (String mascotHTTPProxy)
    {
        this.mascotHTTPProxy = mascotHTTPProxy;
    }

    public AbstractMS2SearchProtocolFactory getFactory()
    {
        return MascotSearchProtocolFactory.get();
    }

    protected void save(File file, Map<String, String> addParams) throws IOException
    {
        if (addParams != null)
        {
            addParams.put("pipeline, mascot server", mascotServer);
            addParams.put("pipeline, mascot http proxy", mascotHTTPProxy);
        }

        super.save(file, addParams);
    }

    public AbstractMS2SearchPipelineJob createPipelineJob(ViewBackgroundInfo info,
                                                          File dirSequenceRoot,
                                                          File[] mzXMLFiles,
                                                          File fileParameters,
                                                          boolean fromCluster)
            throws SQLException, IOException
    {
        return new MascotPipelineJob(info, getName(), dirSequenceRoot, mzXMLFiles, fileParameters, fromCluster);
    }
}
