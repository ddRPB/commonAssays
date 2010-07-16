/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.flow.controllers;

import org.labkey.api.action.BaseViewAction;
import org.labkey.api.action.HasPageConfig;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.jsp.JspBase;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.security.User;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.template.PageConfig;
import org.labkey.flow.data.FlowObject;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.webparts.FlowFolderType;
import org.labkey.flow.FlowModule;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;


public abstract class BaseFlowController<A extends Enum> extends SpringActionController
{
    public static HelpTopic DEFAULT_HELP_TOPIC = new HelpTopic("flowDefault");
    
    protected BaseFlowController.FlowPage getFlowPage(String name) throws Exception
    {
        return getFlowPage(name, getClass().getPackage());
    }

    protected BaseFlowController.FlowPage getFlowPage(String name, Package thePackage) throws Exception
    {
        BaseFlowController.FlowPage ret = (BaseFlowController.FlowPage) JspLoader.createPage(getRequest(), thePackage.getName(), name);
        ret._controller = this;
        return ret;
    }

    protected FlowScript getScript() throws Exception
    {
        return FlowScript.fromURL(getActionURL(), getRequest());
    }

    public FlowRun getRun() throws Exception
    {
        FlowRun ret;
        ret = FlowRun.fromURL(getActionURL());
        return ret;
    }

    protected ActionURL executeScript(PipelineJob job) throws Exception
    {
        FlowProtocol.ensureForContainer(getUser(), job.getContainer());
        PipelineService service = PipelineService.get();
        service.getPipelineQueue().addJob(job);

        ActionURL forward = job.getStatusHref().clone();
        putParam(forward, FlowParam.redirect, 1);
        forward.setFragment("end");
        return forward;
    }

    public HelpTopic getHelpTopic()
    {
        return DEFAULT_HELP_TOPIC;
    }

    // override to append root nav to all paths
    @Override
    protected void appendNavTrail(Controller action, NavTree root)
    {
        PageConfig page = null;
        if (action instanceof HasPageConfig)
            page = ((HasPageConfig)action).getPageConfig();
        root.addChild(getFlowNavStart(page, getViewContext()));
        super.appendNavTrail(action, root);
    }

    public NavTree appendFlowNavTrail(PageConfig page, NavTree root, FlowObject object, String title, A action)
    {
        ArrayList<NavTree> children = new ArrayList<NavTree>();
        while (object != null)
        {
            children.add(0, new NavTree(object.getLabel(), object.urlShow()));
            object = object.getParent();
        }

        root.addChildren(children);
        if (title != null)
            root.addChild(title);

        if (page.getHelpTopic() == HelpTopic.DEFAULT_HELP_TOPIC)
            page.setHelpTopic(getHelpTopic());

        return root;
    }


    public NavTree getFlowNavStart(PageConfig page, ViewContext context)
    {
        NavTree project;
        if (context.getContainer().getFolderType() instanceof FlowFolderType)
        {
            project = new NavTree("Dashboard", PageFlowUtil.urlProvider(ProjectUrls.class).getStartURL(context.getContainer()));
            if (page.getHelpTopic() == HelpTopic.DEFAULT_HELP_TOPIC)
                page.setHelpTopic(getHelpTopic());
        }
        else
        {
            ActionURL url = PageFlowUtil.urlFor(FlowController.Action.begin, context.getContainer());
            project = new NavTree(FlowModule.getShortProductName(), url.clone());
        }
        return project;
    }


    abstract static public class FlowPage<C extends BaseFlowController> extends JspBase
    {
        C _controller;

        public void setPageFlow(C controller)
        {
            _controller = controller;
        }

        public C getPageFlow()
        {
            return _controller;
        }

        public User getUser()
        {
            return _controller.getUser();
        }

        public Container getContainer() throws ServletException
        {
            return _controller.getContainer();
        }

        public String getContainerPath() throws ServletException
        {
            return getContainer().getPath();
        }
    }


    public ActionURL urlFor(Enum action)
    {
        return PageFlowUtil.urlFor(action, getContainer());
    }


    protected int getIntParam(FlowParam param)
    {
        String value = getParam(param);
        if (value == null)
            return 0;
        return Integer.valueOf(value).intValue();
    }

    protected String getParam(FlowParam param)
    {
        return getRequest().getParameter(param.toString());
    }

    protected void putParam(ActionURL url, Enum param, String value)
    {
        url.replaceParameter(param.toString(), value);
    }

    protected void putParam(ActionURL url, Enum param, int value)
    {
        putParam(url, param, Integer.toString(value));
    }

    protected boolean hasParameter(String name)
    {
        if (getRequest().getParameter(name) != null)
            return true;
        if (getRequest().getParameter(name + ".x") != null)
            return true;
        return false;
    }

    public String getContainerPath()
    {
        return getActionURL().getExtraPath();
    }

    public HttpServletRequest getRequest()
    {
        return getViewContext().getRequest();
    }

    public ActionURL getActionURL()
    {
        return getViewContext().getActionURL();
    }
}
