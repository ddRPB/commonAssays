/*
 * Copyright (c) 2008-2013 LabKey Corporation
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

package org.labkey.elispot;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.PlateBasedAssayProvider;
import org.labkey.api.study.assay.plate.PlateReaderService;
import org.labkey.api.view.WebPartFactory;
import org.labkey.elispot.pipeline.ElispotPipelineProvider;
import org.labkey.api.study.assay.plate.ExcelPlateReader;
import org.labkey.api.study.assay.plate.TextPlateReader;

import java.util.Collection;
import java.util.Collections;

public class ElispotModule extends DefaultModule
{
    private static final Logger _log = Logger.getLogger(ElispotModule.class);

    public String getName()
    {
        return "ELISpotAssay";
    }

    public double getVersion()
    {
        return 13.20;
    }

    protected void init()
    {
        addController("elispot-assay", ElispotController.class);
    }

    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    public boolean hasScripts()
    {
        return true;
    }

    @NotNull
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    public void doStartup(ModuleContext moduleContext)
    {
        PlateService.get().registerPlateTypeHandler(new ElispotPlateTypeHandler());
        ExperimentService.get().registerExperimentDataHandler(new ElispotDataHandler());

        PlateBasedAssayProvider provider = new ElispotAssayProvider();
        AssayService.get().registerAssayProvider(provider);

        PlateReaderService.registerPlateReader(provider, new ExcelPlateReader());
        PlateReaderService.registerPlateReader(provider, new TextPlateReader());

        PipelineService.get().registerPipelineProvider(new ElispotPipelineProvider(this));
    }

    @Override
    public UpgradeCode getUpgradeCode()
    {
        return new ElispotUpgradeCode();
    }
}