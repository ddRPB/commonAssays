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

import org.labkey.api.pipeline.PipelineJob;
import org.labkey.di.pipeline.TransformJobContext;
import org.labkey.di.pipeline.TransformTask;
import org.labkey.di.pipeline.TransformTaskFactory;

import java.util.Collections;
import java.util.List;

/**
 * User: tgaluhn
 * Date: 10/9/13
 */
public class StoredProcedureStepProvider implements StepProvider
{
    @Override
    public String getName()
    {
        return "StoredProcedure";
    }

    @Override
    public List<String> getLegacyNames()
    {
        return Collections.emptyList();
    }

    @Override
    public Class getStepClass()
    {
        return StoredProcedureStep.class;
    }

    @Override
    public StepMeta createMetaInstance()
    {
        return new StoredProcedureStepMeta();
    }

    @Override
    public TransformTask createStepInstance(TransformTaskFactory f, PipelineJob job, StepMeta meta, TransformJobContext context)
    {
       return new StoredProcedureStep(f, job, (StoredProcedureStepMeta)meta, context);
    }
}