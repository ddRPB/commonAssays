package org.labkey.portal;

import org.apache.log4j.Logger;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.DotRunner;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.template.PageConfig;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: 2011-11-18
 * Time: 10:26 AM
 */
public class UtilController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(UtilController.class);
    private static final Logger _log = Logger.getLogger(ProjectController.class);

    public UtilController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }


    public static class DotForm
    {
        public String getDot()
        {
            return _text;
        }
        public void setDot(String text)
        {
            _text = text;
        }
        String _text;
    }


    @RequiresPermissionClass(ReadPermission.class)
    public static class DotSvgAction extends SimpleViewAction<DotForm>
    {
        @Override
        public ModelAndView getView(DotForm form, BindException errors) throws Exception
        {
            // Don't allow GET to avoid security holes as this may inject script
            if (!"POST".equals(getViewContext().getRequest().getMethod()))
                throw new UnauthorizedException("use POST");

            getPageConfig().setTemplate(PageConfig.Template.None);

            String dot = form.getDot();
            File dir = FileUtil.getTempDirectory();
            File svgFile = null;
            try
            {
                svgFile = File.createTempFile("groups", ".svg", dir);
                svgFile.deleteOnExit();
                DotRunner runner = new DotRunner(dir, dot);
                runner.addSvgOutput(svgFile);
                runner.execute();
                String svg = PageFlowUtil.getFileContentsAsString(svgFile);
                String html = svg.substring(svg.indexOf("<svg"));
                HtmlView v = new HtmlView(html);
                v.setFrame(WebPartView.FrameType.NONE);
                return v;
            }
            finally
            {
                if (null != svgFile)
                    svgFile.delete();
            }
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public static class TestDotSvgAction extends SimpleViewAction<DotForm>
    {
        @Override
        public ModelAndView getView(DotForm form, BindException errors) throws Exception
        {
            return new JspView(ProjectController.class, "testsvg.jsp", form);
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }
}
