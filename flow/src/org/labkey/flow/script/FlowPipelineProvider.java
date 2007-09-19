package org.labkey.flow.script;

import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;
import org.apache.log4j.Logger;

import java.util.*;
import java.io.File;
import java.sql.SQLException;

import org.labkey.flow.controllers.FlowModule;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class FlowPipelineProvider extends PipelineProvider
{
    private static final Logger _log = Logger.getLogger(FlowPipelineProvider.class);
    public static final String NAME = "flow";

    public FlowPipelineProvider()
    {
        super(NAME);
    }

    private boolean hasFlowModule(ViewContext context)
    {
        return FlowModule.isActive(context.getContainer());
    }

    static class WorkspaceRecognizer extends DefaultHandler
    {
        boolean _isWorkspace = false;

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if ("Workspace".equals(qName))
            {
                _isWorkspace = true;
            }
            else
            {
                _isWorkspace = false;
            }
            throw new SAXException("Stop parsing");
        }
        boolean isWorkspace()
        {
            return _isWorkspace;
        }
    }

    private class IsFlowJoWorkspaceFilter extends FileEntryFilter
    {
        public boolean accept(File pathname)
        {
            if (pathname.getName().endsWith(".wsp"))
                return true;
            if (!pathname.getName().endsWith(".xml"))
                return false;
            if (pathname.isDirectory())
                return false;
            WorkspaceRecognizer recognizer = new WorkspaceRecognizer();
            try
            {
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

                parser.parse(pathname, recognizer);
            }
            catch (Exception e)
            {
                // suppress
            }
            return recognizer.isWorkspace();
        }
    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        if (!context.hasPermission(ACL.PERM_INSERT))
            return;
        if (!hasFlowModule(context))
            return;
        if (entries.size() == 0)
            return;
        PipeRoot root;
        try
        {
            root = PipelineService.get().findPipelineRoot(context.getContainer());
        }
        catch (SQLException e)
        {
            return;
        }

        {
            FileEntry entry = entries.get(0);
            File file = new File(entry.getURI());
            ViewURLHelper url = PageFlowUtil.urlFor(AnalysisScriptController.Action.chooseRunsToUpload, context.getContainer());

            url.addParameter("path", root.relativePath(file));
            url.addParameter("srcURL", context.getViewURLHelper().toString());
            FileAction action = new FileAction("Upload FCS files", url, null);
            action.setDescription("<p><b>Flow Instructions:</b><br>Navigate to the directories containing FCS files.  Click the button to upload FCS files in the directories shown.</p>");
            entry.addAction(action);
        }
    }

    public boolean suppressOverlappingRootsWarning(ViewContext context)
    {
        if (!hasFlowModule(context))
            return super.suppressOverlappingRootsWarning(context);
        return true;
    }
}
