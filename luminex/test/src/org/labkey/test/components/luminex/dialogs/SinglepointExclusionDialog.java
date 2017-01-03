package org.labkey.test.components.luminex.dialogs;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ExtHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by iansigmon on 12/29/16.
 */
public class SinglepointExclusionDialog extends BaseExclusionDialog
{
    protected static final String MENU_BUTTON_ITEM = "Exclude Singlepoint Unknowns";
    private static final String TITLE = "Exclude Singlepoint Unknowns from Analysis";

    Elements _elements;
    protected SinglepointExclusionDialog(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected void openDialog()
    {
        clickButton(MENU_BUTTON, 0);
        elements().exclusionMenuItem.click();
    }

    public static SinglepointExclusionDialog beginAt(WebDriver driver)
    {
        SinglepointExclusionDialog dialog = new SinglepointExclusionDialog(driver);
        dialog.openDialog();
        dialog.waitForText("Exclusions", "Analyte Name");
        return dialog;
    }

    public void selectDilution(String description, String dilution)
    {
        click(Locator.xpath("//div[contains(@class, 'x-grid3-row')]").withDescendant(Locator.tagWithText("td", description).followingSibling("td").withText(dilution)));
    }

    public void checkAnalyte(String analyte)
    {
        //TODO: do something more robust
        click(ExtHelper.locateGridRowCheckbox(analyte));
    }


    public void uncheckAnalyte(String analyte)
    {
        //TODO: do something more robust
        click(ExtHelper.locateGridRowCheckbox(analyte));
    }

    protected Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    public class Elements extends LabKeyPage.ElementCache
    {
        protected WebElement exclusionMenuItem = Ext4Helper.Locators.menuItem(MENU_BUTTON_ITEM).findWhenNeeded(this);
//        protected WebElement

    }

    public static class Locators extends org.labkey.test.Locators
    {

    }
}