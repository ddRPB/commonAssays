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
package org.labkey.ms2.pipeline;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.upload.FormFile;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.labkey.api.action.*;
import org.labkey.api.data.*;
import org.labkey.api.exp.ExperimentPipelineJob;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.labkey.api.view.template.PageConfig;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protocol.*;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

/**
 * <code>PipelineController</code>
 */
public class PipelineController extends SpringActionController
{
    private static Logger _log = Logger.getLogger(PipelineController.class);
    private static DefaultActionResolver _resolver = new DefaultActionResolver(PipelineController.class);

    public static final String DEFAULT_EXPERIMENT_OBJECTID = "DefaultExperiment";

    private static HelpTopic getHelpTopic(String topic)
    {
        return new HelpTopic(topic, HelpTopic.Area.CPAS);
    }

    public PipelineController()
    {
        super();
        setActionResolver(_resolver);
    }

    public PageConfig defaultPageConfig()
    {
        PageConfig p = super.defaultPageConfig();    //To change body of overridden methods use File | Settings | File Templates.
        p.setHelpTopic(getHelpTopic("ms2"));
        return p;
    }

    public ViewURLHelper urlProjectStart()
    {
        // TODO: Better way?
        return new ViewURLHelper("Project", "start", getContainer());
    }

    public String getErrorMessage(BindException errors)
    {
        StringBuffer s = new StringBuffer();
        if (errors != null && errors.getGlobalErrors() != null)
        {
            for (ObjectError error : (List<ObjectError>) errors.getGlobalErrors())
            {
                if (s.length() > 0)
                    s.append("<br>");
                s.append(error.getDefaultMessage());
            }
        }

        return StringUtils.trimToNull(s.toString());
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleRedirectAction
    {
        public ViewURLHelper getRedirectURL(Object o)
        {
            return MS2Controller.getBeginUrl(getContainer());
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class UploadAction extends RedirectAction<PipelinePathForm>
    {
        public ViewURLHelper getSuccessURL(PipelinePathForm form)
        {
            return MS2Controller.getShowListUrl(getContainer());
        }

        public void validateCommand(PipelinePathForm form, Errors errors)
        {
        }

        public boolean doAction(PipelinePathForm form, BindException errors) throws Exception
        {
            Container c = getContainer();
            PipeRoot pr = PipelineService.get().findPipelineRoot(c);
            if (pr == null || !URIUtil.exists(pr.getUri()))
            {
                HttpView.throwNotFound();
                return false;
            }

            URI uriUpload = URIUtil.resolve(pr.getUri(c), form.getPath());
            if (uriUpload == null)
            {
                HttpView.throwNotFound();
                return false;
            }

            File fileUpload = new File(uriUpload);
            File[] files = new File[] { fileUpload };
            if (fileUpload.isDirectory())
                files = fileUpload.listFiles(MS2PipelineManager.getUploadFilter());

            for (File file : files)
            {
                int extParts = 1;
                if (file.getName().endsWith(".xml"))
                    extParts = 2;
                String baseName = FileUtil.getBaseName(file, extParts);
                File dir = file.getParentFile();
                // If the data was created by our pipeline, try to get the name
                // to look like the normal generated name.

                String protocolName;
                File dirDataOriginal;
                String description;
                if (MascotSearchTask.isNativeOutputFile(file))
                {
                    //TODO: wch: use an appropriate protocol
                    //      after all, this is what the Mascot search processing is doing
                    // mascot .dat result file does not follow that of pipeline
                    protocolName = "none";
                    dirDataOriginal = file;
                    description = MS2PipelineManager.
                            getDataDescription(null, file.getName(), protocolName);
                }
                else
                {
                    // If the data was created by our pipeline, try to get the name
                    // to look like the normal generated name.
                    protocolName = dir.getName();
                    dirDataOriginal = dir.getParentFile();
                    if (dirDataOriginal != null &&
                            dirDataOriginal.getName().equals(XTandemSearchProtocolFactory.get().getName()))
                    {
                        dirDataOriginal = dirDataOriginal.getParentFile();
                    }
                    description = MS2PipelineManager.
                            getDataDescription(dirDataOriginal, baseName, protocolName);
                }

                PipelineService service = PipelineService.get();
                ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), file);
                try
                {
                    if (MS2PipelineManager.isSearchExperimentFile(file))
                    {
                        ExperimentPipelineJob job = new ExperimentPipelineJob(info, file, description, false);
                        PipelineService.get().queueJob(job);
                    }
                    else if (TPPTask.isPepXMLFile(file))
                    {
                        MS2Manager.addRunToQueue(info, file, description, false);
                    }
                    else if (MascotSearchTask.isNativeOutputFile(file))
                    {
                        MS2Manager.addMascotRunToQueue(info, file, description, false);
                    }
                }
                catch (IOException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
                catch (SQLException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
            }

            return true;
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class SearchXTandemAction extends SearchAction
    {
        public String getProviderName()
        {
            return XTandemCPipelineProvider.name;
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class SearchMascotAction extends SearchAction
    {
        public String getProviderName()
        {
            return MascotCPipelineProvider.name;
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class SearchSequestAction extends SearchAction
    {
        public String getProviderName()
        {
            return SequestLocalPipelineProvider.name;
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class SearchAction extends FormViewAction<MS2SearchForm>
    {
        private File _dirRoot;
        private File _dirSeqRoot;
        private File _dirData;
        private File _dirAnalysis;
        private MS2SearchPipelineProvider provider;
        private MS2SearchPipelineProtocol protocol;

        public String getProviderName()
        {
            return null;
        }
        
        public ViewURLHelper getSuccessURL(MS2SearchForm form)
        {
            return urlProjectStart();
        }

        public ModelAndView handleRequest(MS2SearchForm form, BindException errors) throws Exception
        {
            PipeRoot pr = PipelineService.get().findPipelineRoot(getContainer());
            if (pr == null || !URIUtil.exists(pr.getUri()))
                return HttpView.throwNotFoundMV();

            URI uriRoot = pr.getUri();
            URI uriData = URIUtil.resolve(uriRoot, form.getPath());
            if (uriData == null)
                return HttpView.throwNotFoundMV();

            _dirRoot = new File(uriRoot);
            _dirSeqRoot = new File(MS2PipelineManager.getSequenceDatabaseRoot(pr.getContainer()));
            _dirData = new File(uriData);
            if (!NetworkDrive.exists(_dirData))
                return HttpView.throwNotFoundMV();

            if (getProviderName() != null)
                form.setSearchEngine(getProviderName());

            provider = (MS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(form.getSearchEngine());
            if (provider == null)
                return HttpView.throwNotFoundMV();

            setHelpTopic(getHelpTopic(provider.getHelpTopic()));

            AbstractMS2SearchProtocolFactory protocolFactory = provider.getProtocolFactory();

            String protocolName = form.getProtocol();

            _dirAnalysis = protocolFactory.getAnalysisDir(_dirData, protocolName);

            if (protocolName.length() != 0)
            {
                try
                {
                    File protocolFile = protocolFactory.getParametersFile(_dirData, protocolName);
                    if (NetworkDrive.exists(protocolFile))
                    {
                        protocol = protocolFactory.loadInstance(protocolFile);

                        // Don't allow the instance file to override the protocol name.
                        protocol.setName(protocolName);
                    }
                    else
                    {
                        protocol = protocolFactory.load(uriRoot, protocolName);
                    }

                    form.setProtocolName(protocol.getName());
                    form.setProtocolDescription(protocol.getDescription());
                    String[] seqDbNames = protocol.getDbNames();
                    form.setConfigureXml(protocol.getXml());
                    if (seqDbNames == null || seqDbNames.length == 0)
                        errors.reject(ERROR_MSG, "Protocol must have specify a FASTA file.");
                    else if (seqDbNames.length > 1)
                        errors.reject(ERROR_MSG, "Protocol specifies multiple FASTA files.");
                    else
                        form.setSequenceDB(seqDbNames[0]);
                }
                catch (IOException eio)
                {
                    errors.reject(ERROR_MSG, "Failed to load requested protocol.");
                }
            }

            return super.handleRequest(form, errors);
        }

        public void validateCommand(MS2SearchForm form, Errors errors)
        {
        }

        public boolean handlePost(MS2SearchForm form, BindException errors) throws Exception
        {
            try
            {
                provider.ensureEnabled();   // throws exception if not enabled

                // If not a saved protocol, create one from the information in the form.
                if ("".equals(form.getProtocol()))
                {
                    protocol = provider.getProtocolFactory().createProtocolInstance(
                            form.getProtocolName(),
                            form.getProtocolDescription(),
                            _dirSeqRoot,
                            form.getSequenceDBPath(),
                            new String[] {form.getSequenceDB()},
                            form.getConfigureXml());

                    protocol.setEmail(getUser().getEmail());
                    protocol.validate(_dirRoot.toURI());
                    if (form.isSaveProtocol())
                    {
                        protocol.saveDefinition(_dirRoot.toURI());
                    }
                }

                Container c = getContainer();
                File[] annotatedFiles = MS2PipelineManager.getAnalysisFiles(_dirData, _dirAnalysis, FileStatus.ANNOTATED, c);
                File[] unprocessedFile = MS2PipelineManager.getAnalysisFiles(_dirData, _dirAnalysis, FileStatus.UNKNOWN, c);
                List<File> mzXMLFileList = new ArrayList<File>();
                mzXMLFileList.addAll(Arrays.asList(annotatedFiles));
                mzXMLFileList.addAll(Arrays.asList(unprocessedFile));
                File[] mzXMLFiles = mzXMLFileList.toArray(new File[mzXMLFileList.size()]);
                if (mzXMLFiles.length == 0)
                    throw new IllegalArgumentException("Analysis for this protocol is already complete.");

                protocol.getFactory().ensureDefaultParameters(_dirRoot);

                File fileParameters = protocol.getParametersFile(_dirData);
                // Make sure configure.xml file exists for the job when it runs.
                if (!fileParameters.exists())
                {
                    protocol.setEmail(getUser().getEmail());
                    protocol.saveInstance(fileParameters, getContainer());
                }

                AbstractMS2SearchPipelineJob job =
                        protocol.createPipelineJob(getViewBackgroundInfo(), _dirSeqRoot, mzXMLFiles, fileParameters, false);

                boolean hasStatusFile = (job.getStatusFile() != job.getLogFile());

                // If there are multiple files, and this is not a fractions job, or a Perl driven cluster
                // will do the work, then create the single-file child jobs.
                if (mzXMLFiles.length > 1)
                {
                    for (AbstractMS2SearchPipelineJob jobSingle : job.getSingleFileJobs())
                    {
                        // If fractions or for a Perl cluster, just create a placeholder for the status.
                        if (job.isFractions() || hasStatusFile)
                            jobSingle.setStatus(PipelineJob.WAITING_STATUS);
                        else
                            PipelineService.get().queueJob(jobSingle);
                    }
                }

                // If this is a single file job, or a factions job, then it requires its own processing.
                if (mzXMLFiles.length == 1 || job.isFractions())
                {
                    // If a Perl driven cluster will do the work, then just create a placeholder for the status.
                    if (hasStatusFile)
                        job.setStatus(PipelineJob.WAITING_STATUS);
                    else
                        PipelineService.get().queueJob(job);
                }
            }
            catch (IllegalArgumentException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }
            catch (PipelineValidationException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }
            catch (IOException e)
            {
                errors.reject(ERROR_MSG, "Failure attempting to write input parameters." + e.getMessage());
                return false;
            }

            return true;
        }

        public ModelAndView getView(MS2SearchForm form, boolean reshow, BindException errors) throws Exception
        {
            if (!reshow)
                form.setSaveProtocol(true);

            Map<File, FileStatus> mzXmlFileStatus =
                    MS2PipelineManager.getAnalysisFileStatus(_dirData, _dirAnalysis, getContainer());
            for (FileStatus status : mzXmlFileStatus.values())
            {
                // Look for unannotated data.
                if (status == FileStatus.UNKNOWN && !form.isSkipDescription())
                {
                    ViewURLHelper redirectUrl = getContainer().urlFor(ShowDescribeMS2RunAction.class);
                    redirectUrl.addParameter ("searchEngine", form.getSearchEngine());
                    redirectUrl.addParameter("path", form.getPath());
                    return HttpView.redirect(redirectUrl.getLocalURIString());
                }
            }

            Set<ExpRun> creatingRuns = new HashSet<ExpRun>();
            Set<File> annotationFiles = new HashSet<File>();
            try
            {
                for (File mzXMLFile : mzXmlFileStatus.keySet())
                {
                    if (mzXmlFileStatus.get(mzXMLFile) == FileStatus.UNKNOWN)
                        continue;

                    ExpRun run = ExperimentService.get().getCreatingRun(mzXMLFile, getContainer());
                    if (run != null)
                    {
                        creatingRuns.add(run);
                    }
                    File annotationFile = MS2PipelineManager.findAnnotationFile(mzXMLFile);
                    if (annotationFile != null)
                    {
                        annotationFiles.add(annotationFile);
                    }
                }
            }
            catch (IOException e)
            {
                String errorMessage = "While attempting to initiate the search on the mzXML file there was an " +
                    "error interacting with the file system. " + ((e.getMessage() != null) ? "<br>\nDetails: " + e.getMessage() : "");
                return HttpView.throwNotFoundMV(errorMessage);
            }

            if (form.getConfigureXml().length() == 0)
            {
                form.setConfigureXml("<?xml version=\"1.0\"?>\n" +
                        "<bioml>\n" +
                        "<!-- Override default parameters here. -->\n" +
                        "</bioml>");
            }

            Map<String, String[]> sequenceDBs = new HashMap<String, String[]>();
            // If the protocol is being loaded, then the user doesn't need to pick a FASTA file,
            // it will be part of the protocol.
            // CONSIDER: Should we check for the existence of the protocol's FASTA file, since
            //           it may have been deleted?
            if ("".equals(form.getProtocol()))
            {
                try
                {
                    sequenceDBs = provider.getSequenceFiles(_dirSeqRoot.toURI());
                    if (0 == sequenceDBs.size())
                        errors.reject(ERROR_MSG, "No databases available for searching.");
                }
                catch (IOException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                }
            }

            String[] protocolNames = provider.getProtocolFactory().getProtocolNames(_dirRoot.toURI());

            SearchPage page = (SearchPage) FormPage.get(PipelineController.class, form, "search.jsp");

            page.setMzXmlFileStatus(mzXmlFileStatus);
            page.setSequenceDBs(sequenceDBs);
            page.setProtocolNames(protocolNames);
            page.setAnnotationFiles(annotationFiles);
            page.setCreatingRuns(creatingRuns);

            return page.createView(errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Search MS2 Data");
        }
    }

    @RequiresPermission(ACL.PERM_ADMIN)
    public class SetupClusterSequenceDBAction extends FormViewAction<SequenceDBRootForm>
    {
        public void validateCommand(SequenceDBRootForm form, Errors errors)
        {
        }

        public boolean handlePost(SequenceDBRootForm form, BindException errors) throws Exception
        {
            boolean ret = true;

            String newSequenceRoot = form.getLocalPathRoot();
            URI root = null;
            if (newSequenceRoot != null && newSequenceRoot.length() > 0)
            {
                File file = new File(newSequenceRoot);
                if (!NetworkDrive.exists(file))
                    ret = false;    // Reshow the form, if non-existent.
                root = file.toURI();
            }

            MS2PipelineManager.setSequenceDatabaseRoot(getUser(), form.getContainer(),
                    root, form.isAllowUpload());

            return ret;
        }

        public ViewURLHelper getSuccessURL(SequenceDBRootForm form)
        {
            return PipelineService.get().urlSetup(getContainer());
        }

        public ModelAndView getView(SequenceDBRootForm form, boolean reshow, BindException errors) throws Exception
        {
            ConfigureSequenceDB page = (ConfigureSequenceDB) FormPage.get(
                    PipelineController.class, form, "ConfigureSequenceDB.jsp");

            URI localSequenceRoot = MS2PipelineManager.getSequenceDatabaseRoot(getContainer());
            if (localSequenceRoot == null)
                page.setLocalPathRoot("");
            else
            {
                File fileRoot = new File(localSequenceRoot);
                if (!NetworkDrive.exists(fileRoot))
                    errors.reject(ERROR_MSG, "Sequence database root does not exist.");
                boolean allowUpload = MS2PipelineManager.allowSequenceDatabaseUploads(getUser(), getContainer());
                page.setAllowUpload(allowUpload);
                page.setLocalPathRoot(fileRoot.toString());
            }

            return page.createView(errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Configure Sequence Databases");
        }
    }

    @RequiresPermission(ACL.PERM_ADMIN)
    public class SetTandemDefaultsAction extends SetDefaultsActionBase
    {
        public String getProviderName()
        {
            return XTandemCPipelineProvider.name;
        }

        public String getJspName()
        {
            return "setTandemDefaults.jsp";
        }

        public HelpTopic getHelpTopic()
        {
            return PipelineController.getHelpTopic("MS2-Pipeline/setTandemDefaults");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Set X! Tandem Defaults");
        }
    }

    @RequiresPermission(ACL.PERM_ADMIN)
    public class SetMascotDefaultsAction extends SetDefaultsActionBase
    {
        public String getProviderName()
        {
            return MascotCPipelineProvider.name;
        }

        public String getJspName()
        {
            return "setMascotDefaults.jsp";
        }

        public HelpTopic getHelpTopic()
        {
            return PipelineController.getHelpTopic("MS2-Pipeline/setMascotDefaults");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Set Mascot Defaults");
        }
    }

    @RequiresPermission(ACL.PERM_ADMIN)
    public class SetSequestDefaultsAction extends SetDefaultsActionBase
    {
        public String getProviderName()
        {
            return SequestLocalPipelineProvider.name;
        }

        public String getJspName()
        {
            return "setSequestDefaults.jsp";
        }

        public HelpTopic getHelpTopic()
        {
            return PipelineController.getHelpTopic("MS2-Pipeline/setSequestDefaults");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Set Sequest Defaults");
        }
    }

    public static class SetDefaultsForm extends ViewForm
    {
        private String configureXml;

        public String getConfigureXml()
        {
            return configureXml;
        }

        public void setConfigureXml(String configureXml)
        {
            this.configureXml = configureXml;
        }
    }

    protected abstract class SetDefaultsActionBase extends FormViewAction<SetDefaultsForm>
    {
        private File _dirRoot;
        private MS2SearchPipelineProvider _provider;

        public abstract String getProviderName();
        public abstract HelpTopic getHelpTopic();
        public abstract String getJspName();

        public ModelAndView handleRequest(SetDefaultsForm setDefaultsForm, BindException errors) throws Exception
        {
            URI uriRoot = PipelineService.get().getPipelineRootSetting(getContainer());
            if (uriRoot == null)
                return HttpView.throwNotFoundMV("A pipeline root is not set on this folder.");

            _dirRoot = new File(uriRoot);
            _provider = (MS2SearchPipelineProvider)
                    PipelineService.get().getPipelineProvider(getProviderName());

            return super.handleRequest(setDefaultsForm, errors);
        }

        public void validateCommand(SetDefaultsForm form, Errors errors)
        {
        }

        public boolean handlePost(SetDefaultsForm form, BindException errors) throws Exception
        {
            try
            {
                _provider.getProtocolFactory().setDefaultParametersXML(_dirRoot, form.getConfigureXml());
            }
            catch (IllegalArgumentException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }
            catch (FileNotFoundException e)
            {
                if (e.getMessage().indexOf("Access") != -1)
                    errors.reject("Access denied attempting to write defaults. Contact the server administrator.");
                else
                    errors.reject("Failure attempting to write defaults.  Please try again.");
                return false;
            }
            catch (IOException eio)
            {
                errors.reject("Failure attempting to write defaults.  Please try again.");
                return false;
            }

            return true;
        }

        public ModelAndView getView(SetDefaultsForm form, boolean reshow, BindException errors) throws Exception
        {
            setHelpTopic(getHelpTopic());
            if (!reshow)
                form.setConfigureXml(_provider.getProtocolFactory().getDefaultParametersXML(_dirRoot));
            return FormPage.getView(PipelineController.class, form, errors, getJspName());
        }

        public ViewURLHelper getSuccessURL(SetDefaultsForm form)
        {
            return urlProjectStart();
        }
    }

    public static class SequenceDBForm extends ViewForm
    {
        private FormFile sequenceDBFile;

        public FormFile getSequenceDBFile()
        {
            return sequenceDBFile;
        }

        public void setSequenceDBFile(FormFile sequenceDBFile)
        {
            this.sequenceDBFile = sequenceDBFile;
        }
    }

    @RequiresPermission(ACL.PERM_ADMIN)
    public class AddSequenceDBAction extends FormViewAction<SequenceDBForm>
    {
        public void validateCommand(SequenceDBForm form, Errors errors)
        {
        }

        public ModelAndView getView(SequenceDBForm form, boolean reshow, BindException errors) throws Exception
        {
            setHelpTopic(getHelpTopic("MS2-Pipeline/addSequenceDB"));
            return FormPage.getView(PipelineController.class, form, errors, "addSequenceDB.jsp");
        }

        public boolean handlePost(SequenceDBForm form, BindException errors) throws Exception
        {
            FormFile ff = form.getSequenceDBFile();
            String name = (ff == null ? "" : ff.getFileName());
            if (ff == null || ff.getFileSize() == 0)
            {
                errors.reject(ERROR_MSG, "Please specify a FASTA file.");
                return false;
            }
            else if (name.indexOf(File.separatorChar) != -1 || name.indexOf('/') != -1)
            {
                errors.reject(ERROR_MSG, "Invalid sequence database name '" + name + "'.");
                return false;
            }
            else
            {
                BufferedReader reader = null;
                try
                {
                    reader = new BufferedReader(new InputStreamReader(ff.getInputStream()));

                    MS2PipelineManager.addSequenceDB(getContainer(), name, reader);
                }
                catch (IllegalArgumentException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
                catch (IOException e)
                {
                    errors.reject(e.getMessage());
                    return false;
                }
                finally
                {
                    if (reader != null)
                    {
                        try
                        {
                            reader.close();
                        }
                        catch (IOException eio)
                        {
                        }
                    }
                }
            }

            return true;
        }

        public ViewURLHelper getSuccessURL(SequenceDBForm form)
        {
            return urlProjectStart();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Add Sequence Database");
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class ShowCreateMS2ProtocolAction extends FormViewAction<MS2ProtocolForm>
    {
        public void validateCommand(MS2ProtocolForm form, Errors errors)
        {
        }

        public boolean handlePost(MS2ProtocolForm form, BindException errors) throws Exception
        {
            PipeRoot pr = PipelineService.get().findPipelineRoot(getContainer());
            if (pr == null || !URIUtil.exists(pr.getUri()))
            {
                HttpView.throwNotFound();
                return false;
            }

            URI uriRoot = pr.getUri(getContainer());

            MassSpecProtocol protocol = new MassSpecProtocol(form.getName(), form.getTemplateName(), form.getTokenReplacements());
            try
            {
                protocol.validate(uriRoot);
            }
            catch (PipelineValidationException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }

            try
            {
                protocol.saveDefinition(uriRoot);
            }
            catch (IOException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }

            return true;
        }

        public ViewURLHelper getSuccessURL(MS2ProtocolForm form)
        {
            ViewURLHelper url = getContainer().urlFor(ShowDescribeMS2RunAction.class);
            url.addParameter("searchEngine", form.getSearchEngine());
            url.addParameter("path", form.getPath());
            return url;
        }

        public ModelAndView getView(MS2ProtocolForm form, boolean reshow, BindException errors) throws Exception
        {
            PipeRoot pr = PipelineService.get().findPipelineRoot(getContainer());
            if (pr == null || !URIUtil.exists(pr.getUri()))
                return HttpView.throwNotFoundMV();

//            setHelpTopic(getHelpTopic("MS2-Pipeline/showCreateMS2Protocol"));

            form.setError(getErrorMessage(errors));

            URI uriRoot = pr.getUri();
            String templateName = form.getTemplateName();

            if (null == templateName)
            {
                List<String> templateNames = MassSpecProtocolFactory.get().getTemplateNames(uriRoot);
                if (templateNames.size() == 1)
                    templateName = templateNames.get(0);
                else
                {
                    return new PickTemplateView(form, templateNames);
                }
            }

            XarTemplate template = MassSpecProtocolFactory.get().getXarTemplate(uriRoot, templateName);

            return new PopulateTemplateView(form, template);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Create MS2 Protocol");
        }
    }

    private static class PickTemplateView extends WebPartView
    {
        private final List<String> templates;
        private final MS2ProtocolForm form;

        public PickTemplateView(MS2ProtocolForm form, List<String> templates)
        {
            this.templates = templates;
            this.form = form;
        }

        @Override
        protected void renderView(Object model, PrintWriter out) throws Exception
        {
            out.write("<span class=\"heading-1\">");
            out.write("To create a protocol, start with a protocol template and fill in values specified by the template.<br><br>");
            out.write("Which protocol template would you like to start with?<br>");
            out.write("<ul>");
            for (String t : templates)
            {
                out.write("<a href=\"");
                out.write("showCreateMS2Protocol.view?templateName=");
                out.write(PageFlowUtil.filter(t));
                out.write("&path=");
                out.write(PageFlowUtil.encode(form.getPath()));
                out.write("&searchEngine=");
                out.write(PageFlowUtil.encode(form.getSearchEngine()));
                out.write("\">");
                out.write(PageFlowUtil.filter(t));
                out.write("</a><br>\n");
            }
            out.write("</ul>");
        }
    }

    private static class PopulateTemplateView extends WebPartView
    {
        private XarTemplate template;
        private MS2ProtocolForm form;

        public PopulateTemplateView(MS2ProtocolForm form, XarTemplate template)
        {
            this.template = template;
            this.form = form;
        }


        @Override
        protected void renderView(Object model, PrintWriter out) throws Exception
        {
            DisplayColumn nameCol = SimpleInputColumn.create("name", "");
            nameCol.setCaption("Protocol Name");
            DataRegion dr = new DataRegion();
            dr.setName("protocolRegion");
            dr.addColumn(nameCol);
            dr.addHiddenFormField("templateName", template.getName());
            dr.addHiddenFormField("path", form.getPath());
            dr.addHiddenFormField("searchEngine", form.getSearchEngine());
            dr.addColumns(template.getSubstitutionFields());

            ButtonBar bb = new ButtonBar();
            ActionButton ab = new ActionButton("showCreateMS2Protocol.post", "Submit");
            bb.add(ab);
            dr.setButtonBar(bb);

            if (null != form.getError())
                out.write("<span class=\"labkey-error\">" + form.getError() + "</span>");

            RenderContext rc = new RenderContext(getViewContext());
            rc.setMode(DataRegion.MODE_INSERT);

            dr.render(rc, out);
        }
    }

    @RequiresPermission(ACL.PERM_DELETE)
    public class RedescribeFilesAction extends RedirectAction<MS2ExperimentForm>
    {
        public void validateCommand(MS2ExperimentForm form, Errors errors)
        {
        }

        public boolean doAction(MS2ExperimentForm form, BindException errors) throws Exception
        {
            Container c = getContainer();
            PipeRoot pr = PipelineService.get().findPipelineRoot(c);
            if (pr == null || !URIUtil.exists(pr.getUri()))
            {
                HttpView.throwNotFound();
                return false;
            }

            URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
            if (uriData == null)
            {
                HttpView.throwNotFound();
                return false;
            }

            File[] mzXMLFiles = new File(uriData).listFiles(MS2PipelineManager.getAnalyzeFilter());
            for (File mzXMLFile : mzXMLFiles)
            {
                ExpRun run = ExperimentService.get().getCreatingRun(mzXMLFile, c);
                if (run != null)
                {
                    ExperimentService.get().deleteExperimentRunsByRowIds(c, getUser(), run.getRowId());
                }
                File annotationFile = MS2PipelineManager.findAnnotationFile(mzXMLFile, new HashSet<File>(), new HashSet<File>());
                if (annotationFile != null)
                {
                    annotationFile.delete();
                }
            }

            return true;
        }

        public ViewURLHelper getSuccessURL(MS2ExperimentForm form)
        {
            ViewURLHelper url = getContainer().urlFor(ShowDescribeMS2RunAction.class);
            url.addParameter("searchEngine", form.getSearchEngine());
            url.addParameter("path", form.getPath());
            return url;
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class ShowDescribeMS2RunAction extends FormViewAction<MS2ExperimentForm>
    {
        public ModelAndView handleRequest(MS2ExperimentForm form, BindException errors) throws Exception
        {
            form.calcFields();
            return super.handleRequest(form, errors);
        }

        public void validateCommand(MS2ExperimentForm target, Errors errors)
        {
        }

        public boolean handlePost(MS2ExperimentForm form, BindException errors) throws Exception
        {
            if (form.isProtocolIndividual())
            {
                for (String formProtocolName : form.getProtocolNames())
                {
                    if (null == StringUtils.trimToNull(formProtocolName))
                    {
                        form.setError(0, "Choose a protocol for each run");
                        return false;
                    }
                }
            }
            else if ((form.isProtocolShare() &&
                            null == StringUtils.trimToNull(form.getSharedProtocol()))
                || (form.isProtocolFractions() &&
                            null == StringUtils.trimToNull(form.getFractionProtocol())))
            {
                form.setError(0, "Choose a protocol");
                return false;
            }

            if (form.getStep() == MS2ExperimentForm.Step.pickProtocol)
            {
                form.setStep(MS2ExperimentForm.Step.describeSamples);
                return false;
            }

            int numRuns;
            if (form.isProtocolFractions())
                numRuns=1;
            else
                numRuns= form.getProtocolNames().length;

            for (int i = 0; i < numRuns; i++)
            {
                final String runName = form.getRunNames()[i];

                if (runName == null || runName.length() == 0)
                    form.setError(i, "Please specify a run name.");
            }

            MassSpecProtocol[] protocols = form.getProtocols();
            for (int i = 0; i < numRuns; i++)
            {
                final MassSpecProtocol protocol = protocols[i];
                if (protocol != null)
                {
                    MassSpecProtocol.RunInfo runInfo = form.getRunInfos()[i];
                    String error = protocol.validateSubstitution(form.getDirRoot().toURI(), runInfo);
                    if (null != error)
                        form.setError(i, error);
                }
            }

            if (form.hasErrors())
                return false;

            // Kick off jobs, if no form entry errors.
            for (int i = 0; i < numRuns; i++)
            {
                final String runName = form.getRunNames()[i];
                final MassSpecProtocol protocol = protocols[i];

                MassSpecProtocol.RunInfo runInfo = form.getRunInfos()[i];
                runInfo.setRunName(runName);

                //todo- how to handle multi mzxml files per run
                //  need to point MS2Fraction row to a data object representing the search results

                runInfo.setRunFileName(form.getFileNames()[i]);

                File mzXMLFile = new File(form.getDirData(), form.getFileNames()[i]);
                String baseName = FileUtil.getBaseName(mzXMLFile);

                // quick hack to get the search to go forward.
                if (form.isProtocolFractions())
                    baseName = MS2PipelineManager._allFractionsMzXmlFileBase;

                File fileInstance = MS2PipelineManager.getAnnotationFile(form.getDirData(), baseName);
                try
                {
                    protocol.saveInstance(form.getDirRoot().toURI(), fileInstance, runInfo);
                }
                catch (IOException e)
                {
                    String message = e.getMessage();
                    if (message == null || message.length() == 0)
                    {
                        message = "Unable to save protocol";
                    }
                    form.setError(0, message);
                    return false;
                }

                String dataDescription = MS2PipelineManager.getDataDescription(form.getDirData(), baseName, protocol.getName());
                // The experiment needs to be loaded into the right container for where the mzXML file
                // lives on disk, not where the generated XAR file sits. This is important for when
                // the container's pipeline is configured to mirror the file system hierarchy.
                ViewBackgroundInfo info = PipelineService.get().getJobBackgroundInfo(getViewBackgroundInfo(), mzXMLFile);
                ExperimentPipelineJob job = new ExperimentPipelineJob(info, fileInstance, dataDescription, false);
                PipelineService.get().queueJob(job);
            }

            return true;
        }

        public ViewURLHelper getSuccessURL(MS2ExperimentForm form)
        {
            ViewURLHelper url = getContainer().urlFor(SearchAction.class);
            url.addParameter("skipDescription", "true");
            url.addParameter("path", form.getPath());
            url.addParameter("searchEngine", form.getSearchEngine());
            return url;
        }

        public ModelAndView getView(MS2ExperimentForm form, boolean reshow, BindException errors) throws Exception
        {
            setHelpTopic(getHelpTopic("pipelineSearch"));

            ExperimentService.get().ensureDefaultSampleSet();
            ExpSampleSet activeMaterialSource = ExperimentService.get().ensureActiveSampleSet(getContainer());

            URI uriRoot = form.getDirRoot().toURI();
            if (form.getStep() == MS2ExperimentForm.Step.pickProtocol)
            {
                return FormPage.getView(PipelineController.class, form, errors, "pickProtocol.jsp");
            }
            else
            {
                DescribeRunPage drv = (DescribeRunPage) JspLoader.createPage(getViewContext().getRequest(),
                        PipelineController.class, "describe.jsp");
                Map<File, FileStatus> mzXmlFileStatus = form.getMzXmlFileStatus();
                drv.setMzXmlFileStatus(mzXmlFileStatus);
                drv.setForm(form);
                ExpSampleSet[] materialSources;
                if (activeMaterialSource.getLSID().equals(ExperimentService.get().getDefaultSampleSetLsid()))
                {
                    materialSources = new ExpSampleSet[] { activeMaterialSource };
                }
                else
                {
                    materialSources = new ExpSampleSet[] { activeMaterialSource, ExperimentService.get().ensureDefaultSampleSet() };
                }

                Map<Integer, ExpMaterial[]> materialSourceMaterials = new HashMap<Integer, ExpMaterial[]>();
                for (ExpSampleSet source : materialSources)
                {
                    ExpMaterial[] materials = ExperimentService.get().getMaterialsForSampleSet(source.getMaterialLSIDPrefix(), source.getContainer());
                    materialSourceMaterials.put(source.getRowId(), materials);
                }
                drv.setSampleSets(materialSources);
                drv.setMaterialSourceMaterials(materialSourceMaterials);
                if (form.isProtocolShare())
                {
                    MassSpecProtocol prot = MassSpecProtocolFactory.get().load(uriRoot, form.getSharedProtocol());
                    ExperimentArchiveDocument doc = prot.getInstanceXar(uriRoot);
                    ExperimentArchiveDocument[] xarDocs = new ExperimentArchiveDocument[mzXmlFileStatus.size()];

                    Arrays.fill(xarDocs, doc);
                    drv.setXarDocs(xarDocs);
                    return new JspView(drv);
                }
                else if (form.isProtocolFractions())
                {
                    //throw new UnsupportedOperationException("Fractions not currently supported");
                    MassSpecProtocol prot = MassSpecProtocolFactory.get().load(uriRoot, form.getFractionProtocol());
                    ExperimentArchiveDocument doc = prot.getInstanceXar(uriRoot);
                    ExperimentArchiveDocument[] xarDocs = new ExperimentArchiveDocument[1];

                    Arrays.fill(xarDocs, doc);
                    drv.setXarDocs(xarDocs);
                    return new JspView(drv);
                }
                else
                {
                    MassSpecProtocol[] protocols = form.getProtocols();
                    ExperimentArchiveDocument xarDocs[] = new ExperimentArchiveDocument[protocols.length];

                    for (int i = 0; i < protocols.length; i++)
                    {
                        if (protocols[i] == null)
                        {
                            return FormPage.getView(PipelineController.class, form, errors, "pickProtocol.jsp");
                        }
                        xarDocs[i] = protocols[i].getInstanceXar(uriRoot);
                    }

                    drv.setXarDocs(xarDocs);
                    return new JspView(drv);
                }

            }
        }

        public NavTree appendNavTrail(NavTree root)
        {
            // TODO: "Specify Experimental Protocol"
            return root.addChild("Describe MS2 Runs");
        }

    }

    public static class MS2ProtocolForm extends ViewForm
    {
        private String searchEngine;
        private String path;
        private String name;
        private String templateName;
        private String error;

        public String getSearchEngine()
        {
            return searchEngine;
        }

        public void setSearchEngine(String searchEngine)
        {
            this.searchEngine = searchEngine;
        }

        public String getPath()
        {
            return path;
        }

        public void setPath(String path)
        {
            this.path = path;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Map<String, String> getTokenReplacements() throws SQLException
        {
            PipeRoot pRoot = PipelineService.get().findPipelineRoot(getContainer());
            XarTemplate template = MassSpecProtocolFactory.get().getXarTemplate(pRoot.getUri(), templateName);
            Set<String> tokenNames = template.getTokens();
            Map<String, String> replacements = new HashMap<String, String>();

            for (String token : tokenNames)
            {
                String val = getRequest().getParameter(token);
                if (null != val)
                    replacements.put(token, val);
            }

            return replacements;
        }

        public String getTemplateName()
        {
            return templateName;
        }

        public void setTemplateName(String templateName)
        {
            this.templateName = templateName;
        }

        public String getError()
        {
            return error;
        }

        public void setError(String error)
        {
            this.error = error;
        }
    }

    public static class SequenceDBRootForm extends ViewForm
    {
        private String _localPathRoot;
        private boolean _allowUpload;

        public boolean isAllowUpload()
        {
            return _allowUpload;
        }

        public void setAllowUpload(boolean allowUpload)
        {
            _allowUpload = allowUpload;
        }

        public void setLocalPathRoot(String localPathRoot)
        {
            _localPathRoot = localPathRoot;
        }

        public String getLocalPathRoot()
        {
            return _localPathRoot;
        }
    }
}
