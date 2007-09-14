package org.labkey.flow.gateeditor.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.Window;
import org.labkey.api.gwt.client.util.PropertyUtil;
import org.labkey.api.gwt.client.util.ServiceUtil;
import org.labkey.flow.gateeditor.client.model.*;
import org.labkey.flow.gateeditor.client.ui.GateEditorPanel;
import org.labkey.flow.gateeditor.client.ui.GateEditorListener;


public class GateEditor implements EntryPoint
{
    GateEditorServiceAsync _service;
    private RootPanel _root;
    private GateEditorPanel gateEditorPanel;
    private EditorState editorState;

    public void onModuleLoad()
    {
        editorState = new EditorState();
        GWTEditingMode editingMode = GWTEditingMode.valueOf(PropertyUtil.getServerProperty("editingMode"));
        getState().setEditingMode(editingMode);

        GWTWorkspaceOptions workspaceOptions = new GWTWorkspaceOptions();
        workspaceOptions.editingMode = editingMode;
        String strScriptId = PropertyUtil.getServerProperty("scriptId");
        if (strScriptId != null)
        {
            workspaceOptions.scriptId = Integer.parseInt(strScriptId);
        }
        String strRunId = PropertyUtil.getServerProperty("runId");
        if (strRunId != null)
        {
            workspaceOptions.runId = Integer.parseInt(strRunId);
        }
        getState().setSubsetName(PropertyUtil.getServerProperty("subset"));
        init(workspaceOptions);

        _root = RootPanel.get("org.labkey.flow.gateeditor.GateEditor-Root");
        gateEditorPanel = new GateEditorPanel(this);
        _root.add(gateEditorPanel.getWidget());
    }

    public GateEditorServiceAsync getService()
    {
        if (_service == null)
        {
            _service = (GateEditorServiceAsync) GWT.create(GateEditorService.class);
            ServiceUtil.configureEndpoint(_service, "gateEditorService");
        }
        return _service;
    }

    void init(GWTWorkspaceOptions workspaceOptions)
    {
        getService().getWorkspace(workspaceOptions, new GateCallback() {

            public void onSuccess(Object result)
            {
                getState().setWorkspace((GWTWorkspace) result);
            }
        });
    }

    public void addListener(GateEditorListener listener)
    {
        editorState.addListener(listener);
    }

    public void removeListener(GateEditorListener listener)
    {
        editorState.removeListener(listener);
    }

    public EditorState getState()
    {
        return editorState;
    }

    public void save(GWTScript script)
    {
        getService().save(script, new GateCallback()
        {
            public void onSuccess(Object result)
            {
                getState().setScript((GWTScript) result);
            }
        });
    }

    public void save(GWTWell well, GWTScript script)
    {
        getService().save(well, script, new GateCallback()
        {
            public void onSuccess(Object result)
            {
                GWTWell well = (GWTWell) result;
                GWTWorkspace workspace = getState().getWorkspace();
                GWTWell[] wells = workspace.getWells();
                for (int i = 0; i < wells.length; i ++)
                {
                    GWTWell wellCompare = wells[i];
                    if (well.getWellId() == wellCompare.getWellId())
                    {
                        wells[i] = well;
                        break;
                    }
                }
                if (getState().getWell() != null && getState().getWell().getWellId() == well.getWellId())
                {
                    getState().setWell(well);
                }
            }
        });
    }
    public RootPanel getRootPanel()
    {
        return _root;
    }
}
