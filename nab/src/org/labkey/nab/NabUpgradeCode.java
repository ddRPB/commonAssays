package org.labkey.nab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.nab.pipeline.NabPopulateFitParametersPipelineJob;

public class NabUpgradeCode implements UpgradeCode
{
    private static final Logger _log = LogManager.getLogger(NabUpgradeCode.class);

    // Invoked by nab-20.000-20.001.sql
    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public static void populateFitParameters(final ModuleContext context)
    {
        if (!context.isNewInstall())
        {
            try
            {
                ViewBackgroundInfo info = new ViewBackgroundInfo(ContainerManager.getRoot(), null, null);
                PipeRoot root = PipelineService.get().findPipelineRoot(ContainerManager.getRoot());

                // Create a new pipeline job and add it to the JobStore. Since this code runs before the module
                // startBackgroundThreads(), the PipelineModule.startBackgroundThreads JobRestarter will pick up this
                // new job and queue/run it.
                NabPopulateFitParametersPipelineJob job = new NabPopulateFitParametersPipelineJob(info, root);
                PipelineJobService.get().getStatusWriter().setStatus(job, PipelineJob.TaskStatus.waiting.toString(), null, true);
                PipelineJobService.get().getJobStore().storeJob(job);
            }
            catch (Exception e)
            {
                _log.error("Unexpected error during NabPopulateFitParametersPipelineJob", e);
            }
        }
    }
}
