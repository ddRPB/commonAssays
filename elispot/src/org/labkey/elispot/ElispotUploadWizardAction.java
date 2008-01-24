package org.labkey.elispot;

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.*;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.JspView;
import org.labkey.api.util.PageFlowUtil;
import org.springframework.web.servlet.ModelAndView;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;

import javax.servlet.ServletException;
import java.io.File;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 9, 2008
 */

@RequiresPermission(ACL.PERM_INSERT)
public class ElispotUploadWizardAction extends UploadWizardAction<ElispotRunUploadForm>
{
    public ElispotUploadWizardAction()
    {
        super(ElispotRunUploadForm.class);
        addStepHandler(new AntigenStepHandler());
    }

    protected InsertView createRunInsertView(ElispotRunUploadForm newRunForm, boolean reshow)
    {
        InsertView parent = super.createRunInsertView(newRunForm, reshow);

        ElispotAssayProvider provider = (ElispotAssayProvider) getProvider(newRunForm);
        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(provider, newRunForm);

        PlateSamplePropertyHelper helper = provider.createSamplePropertyHelper(newRunForm.getContainer(), newRunForm.getProtocol(), resolverType);
        helper.addSampleColumns(parent.getDataRegion(), getViewContext().getUser());

        return parent;
    }

    public PlateAntigenPropertyHelper createAntigenPropertyHelper(Container container, ExpProtocol protocol, ElispotAssayProvider provider)
    {
        PlateTemplate template = provider.getPlateTemplate(container, protocol);
        return new PlateAntigenPropertyHelper(provider.getAntigenWellGroupColumns(protocol), template);
    }

    protected void addRunActionButtons(ElispotRunUploadForm newRunForm, InsertView insertView, ButtonBar bbar)
    {
        PropertyDescriptor[] antigenColumns = AbstractAssayProvider.getPropertiesForDomainPrefix(_protocol, ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
        if (antigenColumns.length == 0)
        {
            super.addRunActionButtons(newRunForm, insertView, bbar);
        }
        else
        {
            addNextButton(bbar);
            addResetButton(newRunForm, insertView, bbar);
        }
    }

    protected ModelAndView afterRunCreation(ElispotRunUploadForm form, ExpRun run) throws ServletException, SQLException
    {
        PropertyDescriptor[] antigenColumns = AbstractAssayProvider.getPropertiesForDomainPrefix(_protocol, ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
        if (antigenColumns.length == 0)
        {
            return super.afterRunCreation(form, run);
        }
        else
        {
            return getAntigenView(form, false);
        }
    }

    private ModelAndView getAntigenView(ElispotRunUploadForm form, boolean reshow) throws ServletException
    {
        Map<PropertyDescriptor, String> map = new LinkedHashMap<PropertyDescriptor, String>();
        InsertView view = createInsertView(ExperimentService.get().getTinfoExperimentRun(),
                "lsid", map, reshow, form.isResetDefaultValues(), AntigenStepHandler.NAME, form);

        try {
            PlateAntigenPropertyHelper antigenHelper = createAntigenPropertyHelper(form.getContainer(), form.getProtocol(), (ElispotAssayProvider)form.getProvider());
            antigenHelper.addSampleColumns(view.getDataRegion(), getViewContext().getUser());

            // add existing page properties
            addHiddenUploadSetProperties(form, view);
            addHiddenRunProperties(form, view);

            ElispotAssayProvider provider = (ElispotAssayProvider) getProvider(form);
            PlateSamplePropertyHelper helper = provider.createSamplePropertyHelper(getContainer(), _protocol,
                    getSelectedParticipantVisitResolverType(provider, form));
            addHiddenProperties(helper.getPostedPropertyValues(form.getRequest()), view);

            PreviouslyUploadedDataCollector collector = new PreviouslyUploadedDataCollector(form.getUploadedData());
            collector.addHiddenFormFields(view, form);

            ButtonBar bbar = new ButtonBar();
            addFinishButtons(form, view, bbar);
            addResetButton(form, view, bbar);
            //addNextButton(bbar);

            ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
            bbar.add(cancelButton);

            _stepDescription = "Antigen Properties";

            view.getDataRegion().setHorizontalGroups(false);
            view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);
        }
        catch (ExperimentException e)
        {
            throw new ServletException(e);
        }
        return view;        
    }

    private ModelAndView getPlateSummary(ElispotRunUploadForm form, boolean reshow)
    {
/*
        try {
            AssayProvider provider = form.getProvider();
            PlateTemplate template = provider.getPlateTemplate(form.getContainer(), form.getProtocol());

            File dataFile = form.getUploadedData().get("uploadedFile");
            if (dataFile != null)
            {
                Plate plate = ElispotDataHandler.loadDataFile(dataFile, template);
                ModelAndView view = new JspView<ElispotRunUploadForm>("/org/labkey/elispot/view/plateSummary.jsp", form);
                view.addObject("plate", plate);

                return view;
            }

        }
        catch (ExperimentException e)
        {
            throw new RuntimeException(e);
        }
*/
        return null;

    }

    protected StepHandler getRunStepHandler()
    {
        return new ElispotRunStepHandler();
    }

    protected class ElispotRunStepHandler extends RunStepHandler
    {
        private Map<PropertyDescriptor, String> _postedSampleProperties = null;

        protected boolean validatePost(ElispotRunUploadForm form)
        {
            boolean runPropsValid = super.validatePost(form);
            boolean samplePropsValid = true;

            if (runPropsValid)
            {
                try {
                    form.getUploadedData();
                    ElispotAssayProvider provider = (ElispotAssayProvider) getProvider(form);
                    PlateSamplePropertyHelper helper = provider.createSamplePropertyHelper(getContainer(), _protocol,
                            getSelectedParticipantVisitResolverType(provider, form));
                    _postedSampleProperties = helper.getPostedPropertyValues(form.getRequest());
                    samplePropsValid = validatePostedProperties(_postedSampleProperties, getViewContext().getRequest());
                }
                catch (ExperimentException e)
                {
                    ActionErrors strutsErrors = PageFlowUtil.getActionErrors(getViewContext().getRequest(), true);
                    strutsErrors.add("main", new ActionMessage("Error", e.getMessage()));

                    return false;
                }
            }
            return runPropsValid && samplePropsValid;
        }

        protected ModelAndView handleSuccessfulPost(ElispotRunUploadForm form) throws SQLException, ServletException
        {
            saveDefaultValues(_postedSampleProperties, form.getRequest(), form.getProvider(), RunStepHandler.NAME);
            return super.handleSuccessfulPost(form);
        }

        protected ExpRun saveExperimentRun(ElispotRunUploadForm form) throws ExperimentException
        {
            // dont save until after the antigen step
            return null;
        }
    }

    public class AntigenStepHandler extends StepHandler<ElispotRunUploadForm>
    {
        public static final String NAME = "ANTIGEN";
        private Map<PropertyDescriptor, String> _postedAntigenProperties = null;

        public ModelAndView handleStep(ElispotRunUploadForm form) throws ServletException, SQLException
        {
            if (!form.isResetDefaultValues() && validatePost(form))
                return handleSuccessfulPost(form);
            else
                return getAntigenView(form, true);

            //return getPlateSummary(form, false);
        }

        protected boolean validatePost(ElispotRunUploadForm form)
        {
            PlateAntigenPropertyHelper helper = createAntigenPropertyHelper(form.getContainer(),
                    form.getProtocol(), (ElispotAssayProvider)form.getProvider());
            _postedAntigenProperties = helper.getPostedPropertyValues(form.getRequest());

            return true;
        }

        protected ModelAndView handleSuccessfulPost(ElispotRunUploadForm form) throws SQLException, ServletException
        {
            try {
                AssayProvider provider = form.getProvider();
                provider.saveExperimentRun(form);

                saveDefaultValues(_postedAntigenProperties, form.getRequest(), provider, getName());
            }
            catch (ExperimentException e)
            {
                ActionErrors strutsErrors = PageFlowUtil.getActionErrors(getViewContext().getRequest(), true);
                strutsErrors.add("main", new ActionMessage("Error", e.getMessage()));
                return getAntigenView(form, true);
            }
            return runUploadComplete(form);
        }

        public String getName()
        {
            return NAME;
        }
    }
}
