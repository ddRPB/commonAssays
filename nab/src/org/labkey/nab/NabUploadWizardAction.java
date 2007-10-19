package org.labkey.nab;

import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.*;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.HttpView;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.Map;

/**
 * User: brittp
 * Date: Sep 27, 2007
 * Time: 3:48:53 PM
 */
@RequiresPermission(ACL.PERM_INSERT)
public class NabUploadWizardAction extends UploadWizardAction<NabRunUploadForm>
{
    public NabUploadWizardAction()
    {
        super(NabRunUploadForm.class);
    }

    public ModelAndView getView(NabRunUploadForm assayRunUploadForm, BindException errors) throws Exception
    {
        return super.getView(assayRunUploadForm, errors);
    }

    protected InsertView createRunInsertView(NabRunUploadForm newRunForm, boolean reshow)
    {
        NabAssayProvider provider = (NabAssayProvider) getProvider(newRunForm);
        InsertView parent = super.createRunInsertView(newRunForm, reshow);
        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(provider, newRunForm);
        PlateSamplePropertyHelper helper = provider.createSamplePropertyHelper(newRunForm.getContainer(), newRunForm.getProtocol(), resolverType);
        helper.addSampleColumns(parent.getDataRegion(), getViewContext().getUser());
        return parent;
    }

    protected StepHandler getRunStepHandler()
    {
        return new NabRunStepHandler();
    }
    
    protected class NabRunStepHandler extends RunStepHandler
    {
        private Map<PropertyDescriptor, String> _postedSampleProperties = null;

        protected boolean validatePost(NabRunUploadForm form)
        {
            boolean runPropsValid = super.validatePost(form);

            NabAssayProvider provider = (NabAssayProvider) getProvider(form);
            PlateSamplePropertyHelper helper = provider.createSamplePropertyHelper(getContainer(), _protocol,
                    getSelectedParticipantVisitResolverType(provider, form));
            _postedSampleProperties = helper.getPostedPropertyValues(form.getRequest());
            boolean samplePropsValid = validatePostedProperties(_postedSampleProperties, getViewContext().getRequest());
            return runPropsValid && samplePropsValid;
        }

        protected ModelAndView handleSuccessfulPost(NabRunUploadForm form) throws SQLException, ServletException
        {
            saveDefaultValues(_postedSampleProperties, RunStepHandler.NAME);
            return super.handleSuccessfulPost(form);
        }
    }

    protected ModelAndView afterRunCreation(NabRunUploadForm form, ExpRun run) throws ServletException, SQLException
    {
        if (form.isMultiRunUpload())
            return super.afterRunCreation(form, run);
        else
        {
            HttpView.throwRedirect(new ViewURLHelper("NabAssay", "details", run.getContainer()).addParameter("rowId", run.getRowId()));
            return null;
        }
    }
}
