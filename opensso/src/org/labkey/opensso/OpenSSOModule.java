package org.labkey.opensso;

import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.AuthenticationManager;
import org.labkey.api.security.AuthenticationProvider;
import org.apache.log4j.Logger;

public class OpenSSOModule extends DefaultModule
{
    public static final String NAME = "OpenSSO";
    private static Logger _log = Logger.getLogger(OpenSSOModule.class);

    public OpenSSOModule()
    {
        super(NAME, 0.01, null, false);
        addController("opensso", OpenSSOController.class);
    }

    public TabDisplayMode getTabDisplayMode()
    {
        return TabDisplayMode.DISPLAY_NEVER;
    }

    public void startup(ModuleContext moduleContext)
    {
        super.startup(moduleContext);

        // TODO: Move to constructor?  Should be safe...
        AuthenticationProvider opensso = new OpenSSOProvider();
        AuthenticationManager.registerProvider(opensso);
    }
}