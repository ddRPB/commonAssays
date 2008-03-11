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

import org.apache.commons.lang.StringUtils;
import org.labkey.api.pipeline.*;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <code>TPPTask</code> PipelineJob task to run the TPP (xinteract) for further
 * analysis on a pepXML file generated by running a pepXML converter on a search
 * engine's raw output.  This task may run PeptideProphet, ProteinProphet,
 * Quantitation, and batch fractions into a single pepXML.
 */
public class TPPTask extends PipelineJob.Task
{
    public static final FileType FT_PEP_XML = new FileType(".pep.xml");
    public static final FileType FT_PROT_XML = new FileType(".prot.xml");
    public static final FileType FT_INTERMEDIATE_PROT_XML = new FileType(".pep-prot.xml");
    public static final FileType FT_TPP_PROT_XML = new FileType("-prot.xml");

    private static final FileType FT_PEP_XSL = new FileType(".pep.xsl");
    private static final FileType FT_PEP_SHTML = new FileType(".pep.shtml");
    private static final FileType FT_INTERMEDIATE_PROT_XSL = new FileType(".pep-prot.xsl");
    private static final FileType FT_INTERMEDIATE_PROT_SHTML = new FileType(".pep-prot.shtml");

    public static File getPepXMLFile(File dirAnalysis, String baseName)
    {
        return FT_PEP_XML.newFile(dirAnalysis, baseName);
    }

    public static boolean isPepXMLFile(File file)
    {
        return FT_PEP_XML.isType(file);
    }

    public static File getProtXMLFile(File dirAnalysis, String baseName)
    {
        return FT_PROT_XML.newFile(dirAnalysis, baseName);
    }

    public static boolean isProtXMLFile(File file)
    {
        return (FT_PROT_XML.isType(file) ||
                FT_INTERMEDIATE_PROT_XML.isType(file) ||
                FT_TPP_PROT_XML.isType(file));
    }

    public static FileType getProtXMLFileType(File file)
    {
        if (FT_PROT_XML.isType(file))
        {
            return FT_PROT_XML;
        }
        if (FT_INTERMEDIATE_PROT_XML.isType(file))
        {
            return FT_INTERMEDIATE_PROT_XML;
        }
        if (FT_TPP_PROT_XML.isType(file))
        {
            return FT_TPP_PROT_XML;
        }
        return null;
    }

    public static File getProtXMLIntermediatFile(File dirAnalysis, String baseName)
    {
        return FT_INTERMEDIATE_PROT_XML.newFile(dirAnalysis, baseName);
    }

    /**
     * Interface for support required from the PipelineJob to run this task,
     * beyond the base PipelineJob methods.
     */
    public interface JobSupport extends MS2PipelineJobSupport
    {
        /**
         * List of pepXML files to use as inputs to "xinteract".
         */
        File[] getInteractInputFiles();

        /**
         * True if PeptideProphet and ProteinProphet can be run on the input files.
         */
        boolean isProphetEnabled();

        /**
         * True if RefreshParser should run.
         */
        boolean isRefreshRequired();
    }

    public static class Factory extends AbstractTaskFactory
    {
        public Factory()
        {
            super(TPPTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new TPPTask(job);
        }

        public FileType getInputType()
        {
            return FT_PEP_XML;
        }

        public String getStatusName()
        {
            return "ANALYSIS";
        }

        public boolean isJobComplete(PipelineJob job) throws IOException, SQLException
        {
            JobSupport support = (JobSupport) job;
            String baseName = support.getBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            if (!NetworkDrive.exists(getPepXMLFile(dirAnalysis, baseName)))
                return false;

            if (support.isProphetEnabled() &&
                    !NetworkDrive.exists(getProtXMLFile(dirAnalysis, baseName)))
                return false;

            return true;
        }
    }

    protected TPPTask(PipelineJob job)
    {
        super(job);
    }

    public JobSupport getJobSupport()
    {
        return getJob().getJobSupport(JobSupport.class);
    }

    public void run()
    {
        try
        {
            Map<String, String> params = getJob().getParameters();

            WorkDirFactory factory = PipelineJobService.get().getWorkDirFactory();
            WorkDirectory wd = factory.createWorkDirectory(getJob().getJobGUID(), getJobSupport());

            // TODO: mzXML files may also be required, and input disk space requirements
            //          may be too great to copy to a temporary directory.
            File[] inputFiles = getJobSupport().getInteractInputFiles();
            for (int i = 0; i < inputFiles.length; i++)
                inputFiles[i] = wd.inputFile(inputFiles[i]);

            File dirMzXml = getJobSupport().getDataDirectory();

            File fileWorkPepXML = wd.newFile(FT_PEP_XML);
            File fileWorkProtXML = null;
            if (getJobSupport().isProphetEnabled())
                fileWorkProtXML = wd.newFile(FT_INTERMEDIATE_PROT_XML);

            List<String> interactCmd = new ArrayList<String>();
            interactCmd.add("xinteract");

            if (!getJobSupport().isProphetEnabled())
            {
                interactCmd.add("-nP"); // no Prophet analysis
            }
            else
            {
                if ("yes".equalsIgnoreCase(params.get("pipeline prophet, accurate mass")))
                    interactCmd.add("-OptA");
                else
                    interactCmd.add("-Opt");
                interactCmd.add("-x20");    // 20 iterations extra for good measure.

                if (!getJobSupport().isRefreshRequired())
                    interactCmd.add("-nR");

                String paramMinProb = params.get("pipeline prophet, min probability");
                if (paramMinProb != null && paramMinProb.length() > 0)
                    interactCmd.add("-pp" + paramMinProb);
            }

            String quantParam = getQuantitationCmd(params, wd.getRelativePath(dirMzXml));
            if (quantParam != null)
                interactCmd.add(quantParam);

            interactCmd.add("-N" + fileWorkPepXML.getName());

            for (File fileInput : inputFiles)
                interactCmd.add(wd.getRelativePath(fileInput));

            getJob().runSubProcess(new ProcessBuilder(interactCmd),
                    wd.getDir());

            wd.outputFile(fileWorkPepXML);
            if (fileWorkProtXML != null)
            {
                wd.outputFile(fileWorkProtXML,
                        FT_PROT_XML.getName(getJobSupport().getBaseName()));
            }

            // Deal with possible TPP outputs, if TPP was not XML_ONLY
            wd.discardFile(wd.newFile(FT_PEP_XSL));
            wd.discardFile(wd.newFile(FT_PEP_SHTML));
            wd.discardFile(wd.newFile(FT_INTERMEDIATE_PROT_XSL));
            wd.discardFile(wd.newFile(FT_INTERMEDIATE_PROT_SHTML));
            wd.remove();

            // If no combined analysis is coming or this is the combined analysis, remove
            // the raw pepXML file(s).
            if (!getJobSupport().isFractions() || inputFiles.length > 1)
            {
                for (File fileInput : inputFiles)
                {
                    if (!fileInput.delete())
                        getJob().warn("Failed to delete intermediat file " + fileInput);
                }
            }
        }
        catch (PipelineJob.RunProcessException erp)
        {
            // Handled in runSubProcess
        }
        catch (InterruptedException ei)
        {
            // Handled in runSubProcess
        }
        catch (IOException e)
        {
            getJob().error(e.getMessage(), e);
        }
    }

    public String getQuantitationCmd(Map<String, String> params, String pathMzXml) throws FileNotFoundException
    {
        String paramAlgorithm = params.get("pipeline quantitation, algorithm");
        if (paramAlgorithm == null)
            return null;
        if (!"q3".equalsIgnoreCase(paramAlgorithm) && !"xpress".equalsIgnoreCase(paramAlgorithm))
            return null;    // CONSIDER: error message.

        List<String> quantOpts = new ArrayList<String>();

        String paramQuant = params.get("pipeline quantitation, residue label mass");
        if (paramQuant != null)
            getLabelOptions(paramQuant, quantOpts);

        paramQuant = params.get("pipeline quantitation, mass tolerance");
        if (paramQuant != null)
            quantOpts.add("-m" + paramQuant);

        paramQuant = params.get("pipeline quantitation, heavy elutes before light");
        if (paramQuant != null)
            if("yes".equalsIgnoreCase(paramQuant))
                quantOpts.add("-b");

        paramQuant = params.get("pipeline quantitation, fix");
        if (paramQuant != null)
        {
            if ("heavy".equalsIgnoreCase(paramQuant))
                quantOpts.add("-H");
            else if ("light".equalsIgnoreCase(paramQuant))
                quantOpts.add("-L");
        }

        paramQuant = params.get("pipeline quantitation, fix elution reference");
        if (paramQuant != null)
        {
            String refFlag = "-f";
            if ("peak".equalsIgnoreCase(paramQuant))
                refFlag = "-F";
            paramQuant = params.get("pipeline quantitation, fix elution difference");
            if (paramQuant != null)
                quantOpts.add(refFlag + paramQuant);
        }

        paramQuant = params.get("pipeline quantitation, metabolic search type");
        if (paramQuant != null)
        {
            if ("normal".equalsIgnoreCase(paramQuant))
                quantOpts.add("-M");
            else if ("heavy".equalsIgnoreCase(paramQuant))
                quantOpts.add("-N");
        }

        quantOpts.add("\"-d" + pathMzXml + "\"");

        if ("xpress".equals(paramAlgorithm))
            return ("-X" + StringUtils.join(quantOpts.iterator(), ' '));

        String paramMinPP = params.get("pipeline quantitation, min peptide prophet");
        if (paramMinPP != null)
            quantOpts.add("--minPeptideProphet=" + paramMinPP);
        String paramMaxDelta = params.get("pipeline quantitation, max fractional delta mass");
        if (paramMaxDelta != null)
            quantOpts.add("--maxFracDeltaMass=" + paramMaxDelta);
        String paramCompatQ3 = params.get("pipeline quantitation, q3 compat");
        if ("yes".equalsIgnoreCase(paramCompatQ3))
            quantOpts.add("--compat");

        return ("-C1\"" + PipelineJobService.get().getJavaPath() + "\" -client -Xmx256M -jar "
                + "\"" + PipelineJobService.get().getJarPath("viewerApp.jar") + "\""
                + " --q3 " + StringUtils.join(quantOpts.iterator(), ' ')
                + " -C2Q3ProteinRatioParser");
    }

    protected void getLabelOptions(String paramQuant, List<String> quantOpts)
    {
        String[] quantSpecs = paramQuant.split(",");
        for (String spec : quantSpecs)
        {
            String[] specVals = spec.split("@");
            if (specVals.length != 2)
                continue;
            String mass = specVals[0].trim();
            String aa = specVals[1].trim();
            quantOpts.add("-n" + aa + "," + mass);
        }
    }
}
