/*
 * Copyright (c) 2011-2012 LabKey Corporation
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
package org.labkey.luminex;

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.assay.AssayRunUploadContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * General Luminex-specific run context info
 * User: jeckels
 * Date: Oct 7, 2011
 */
public interface LuminexRunContext extends AssayRunUploadContext<LuminexAssayProvider>
{
    public String[] getAnalyteNames();

    public Map<DomainProperty, String> getAnalyteProperties(String analyteName);

    public Map<String, String> getAnalyteColumnProperties(String analyteName);

    public Set<String> getTitrationsForAnalyte(String analyteName) throws ExperimentException;

    public List<Titration> getTitrations() throws ExperimentException;

    public LuminexExcelParser getParser() throws ExperimentException;
}
