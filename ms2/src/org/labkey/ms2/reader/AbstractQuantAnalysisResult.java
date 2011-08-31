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
package org.labkey.ms2.reader;

import org.labkey.ms2.PepXmlImporter;

import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Aug 16, 2011
 */
public abstract class AbstractQuantAnalysisResult extends PepXmlAnalysisResultHandler.PepXmlAnalysisResult
{
    private long peptideId;
    private int quantId;

    public int getQuantId()
    {
        return quantId;
    }

    public void setQuantId(int quantId)
    {
        this.quantId = quantId;
    }

    public long getPeptideId()
    {
        return peptideId;
    }

    public void setPeptideId(long peptideId)
    {
        this.peptideId = peptideId;
    }

    public abstract void insert(PepXmlImporter pepXmlImporter) throws SQLException;
}
