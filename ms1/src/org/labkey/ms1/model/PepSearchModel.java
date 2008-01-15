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
package org.labkey.ms1.model;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Module;
import org.labkey.ms1.MS1Controller;

/**
 * Model for the PepSearchView.jsp
 * 
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 9, 2008
 * Time: 9:25:10 AM
 */
public class PepSearchModel
{
    private String _resultsUri;
    private String _pepSeq = null;
    private boolean _exact = false;
    private boolean _subfolders = false;
    private String _errorMsg = null;

    public PepSearchModel(Container container)
    {
        _resultsUri = new ActionURL(MS1Controller.SearchFeaturesAction.class, container).getLocalURIString();
    }

    public PepSearchModel(Container container, String pepSeq, boolean exact, boolean includeSubfolders)
    {
        this(container);
        _pepSeq = pepSeq;
        _exact = exact;
        _subfolders = includeSubfolders;
    }

    public String getResultsUri()
    {
        return _resultsUri;
    }

    public ExpRun[] getRuns(Container container)
    {
        ExperimentService.Interface expSvc = ExperimentService.get();
        return expSvc.getExpRuns(container, expSvc.getExpProtocol(container, MS1Module.PROTOCOL_MS1), null);
    }

    public String getPepSeq()
    {
        return null != _pepSeq ? _pepSeq : "";
    }

    public void setPepSeq(String pepSeq)
    {
        _pepSeq = pepSeq;
    }

    public boolean isExact()
    {
        return _exact;
    }

    public void setExact(boolean exact)
    {
        _exact = exact;
    }

    public void setIncludeSubfolders(boolean subfolders)
    {
        _subfolders = subfolders;
    }

    public boolean includeSubfolders()
    {
        if(_pepSeq != null && _pepSeq.length() > 0)
            return _subfolders;
        else
            return true; //forces it on by default for a new search
    }

    public boolean noSearchTerms()
    {
        return (null == _pepSeq || _pepSeq.length() == 0);
    }

    public String getErrorMsg()
    {
        return _errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        _errorMsg = errorMsg;
    }

    public boolean hasErrorMsg()
    {
        return (null != _errorMsg && _errorMsg.length() > 0);
    }
}
