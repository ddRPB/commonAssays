/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.flow.webparts;

import org.labkey.api.view.*;

public class OverviewWebPart extends HtmlView
{
    static public final WebPartFactory FACTORY = new Factory();

    static class Factory extends WebPartFactory
    {
        Factory()
        {
            super("Flow Experiment Management");
            addLegacyNames("Flow Overview");
        }

        @Override
        public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart) throws Exception
        {
            return new OverviewWebPart(portalCtx);
        }
    }

    public OverviewWebPart(ViewContext portalCtx) throws Exception
    {
        super(new FlowOverview(portalCtx.getUser(), portalCtx.getContainer()).toString());
        setTitle("Flow Experiment Management");
    }
}
