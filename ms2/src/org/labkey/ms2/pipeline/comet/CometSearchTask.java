package org.labkey.ms2.pipeline.comet;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.util.FileType;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.TPPTask;
import org.labkey.ms2.pipeline.sequest.AbstractSequestSearchTaskFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: jeckels
 * Date: 9/16/13
 */
public class CometSearchTask extends AbstractMS2SearchTask<CometSearchTask.Factory>
{
    public static final String COMET_PARAMS = "comet.params";

    private static final String COMET_ACTION_NAME = "Comet Search";

    public static class Factory extends AbstractSequestSearchTaskFactory
    {
        public Factory()
        {
            super(CometSearchTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new CometSearchTask(this, job);
        }

        public List<String> getProtocolActionNames()
        {
            return Arrays.asList(COMET_ACTION_NAME);
        }
    }

    protected CometSearchTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public CometPipelineJob getJob()
    {
        return (CometPipelineJob)super.getJob();
    }

    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            // Copy the mzXML file to be local
            File fileMzXML = _factory.findInputFile(getJob());
            File localMzXML = _wd.inputFile(fileMzXML, true);

            // Write out comet.params file
            File fileWorkParams = _wd.newFile(COMET_PARAMS);
            CometParamsBuilder builder = new CometParamsBuilder(getJob().getParameters(), getJob().getSequenceRootDirectory());
            builder.initXmlValues();
            builder.writeFile(fileWorkParams);

            // Perform Comet search
            List<String> args = new ArrayList<>();
            String cometPath = PipelineJobService.get().getExecutablePath("comet", null, "comet", null, getJob().getLogger());
            args.add(cometPath);
            args.add(localMzXML.getName());
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            getJob().runSubProcess(processBuilder, _wd.getDir());


            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(_wd.getDir(),
                                getJob().getBaseName(),
                                FileType.gzSupportLevel.NO_GZ);

            File pepXMLFile = TPPTask.FT_PEP_XML.getFile(_wd.getDir(), getJob().getBaseName());
            if (fileWorkPepXMLRaw.exists())
            {
                fileWorkPepXMLRaw.delete();
            }
            pepXMLFile.renameTo(fileWorkPepXMLRaw);

            try (WorkDirectory.CopyingResource ignored = _wd.ensureCopyingLock())
            {
                RecordedAction cometAction = new RecordedAction(COMET_ACTION_NAME);
                cometAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(args, " "));
                cometAction.addOutput(_wd.outputFile(fileWorkParams), "CometParams", true);
                cometAction.addOutput(_wd.outputFile(fileWorkPepXMLRaw), "RawPepXML", true);
                for (File file : getJob().getSequenceFiles())
                {
                    cometAction.addInput(file, FASTA_INPUT_ROLE);
                }
                cometAction.addInput(fileMzXML, SPECTRA_INPUT_ROLE);

                return new RecordedActionSet(cometAction);
            }
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }
    }
}
