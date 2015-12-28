package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.AssayDomainEditor;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.QCAssayScriptHelper;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Assays.class, DailyA.class})
public final class AssayTransformWarningTest extends BaseWebDriverTest
{
    public static final File JAVA_TRANSFORM_SCRIPT = TestFileUtils.getSampleData("qc/transformWarning.jar");
    public static final File R_TRANSFORM_SCRIPT = TestFileUtils.getSampleData("qc/assayTransformWarning.R");

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @BeforeClass
    public static void initTest()
    {
        AssayTransformWarningTest init = (AssayTransformWarningTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        new RReportHelper(this).ensureRConfig();
        new QCAssayScriptHelper(this).ensureEngineConfig();

        _containerHelper.createProject(getProjectName(), "Assay");
    }

    @Test
    public void testJavaTransformWarning()
    {
        String assayName = "transformWarningJar";
        String importData = "ParticipantId\nJavaWarned";
        String runName = "java transform run";

        AssayDomainEditor assayDesigner = _assayHelper.createAssayAndEdit("General", assayName);
        assayDesigner.addTransformScript(JAVA_TRANSFORM_SCRIPT);
        assayDesigner.save();
        assayDesigner.saveAndClose();

        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), runName);
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), importData);

        clickButton("Save and Finish");
        assertElementPresent(Locators.labkeyError.containing("Inline warning from Java transform."));
        assertElementPresent(Locator.linkWithText("Warning link").withAttribute("href", "http://www.labkey.test"));

        File extraFile1 = clickAndWaitForDownload(Locator.linkContainingText("test1.txt"));
        File extraFile2 = clickAndWaitForDownload(Locator.linkContainingText("test2.tsv"));

        assertEquals("Wrong text in file generated by transform", "This is test file 1 (Java).", TestFileUtils.getFileContents(extraFile1).trim());
        assertEquals("Wrong text in file generated by transform", "This is test file 2 (Java).", TestFileUtils.getFileContents(extraFile2).trim());

        clickButton("Proceed");

        clickAndWait(Locator.linkWithText(runName), longWaitForPage);

        DataRegionTable table = new DataRegionTable("Data", this);
        assertEquals(1, table.getDataRowCount());
        assertTextPresent("JavaWarned");
    }

    @Test
    public void testRTransformWarning()
    {
        String assayName = "transformWarningR";
        String importData = "ParticipantId\nRWarned";
        String runName = "R transform run";

        AssayDomainEditor assayDesigner = _assayHelper.createAssayAndEdit("General", assayName);
        assayDesigner.addTransformScript(R_TRANSFORM_SCRIPT);
        assayDesigner.save();
        assayDesigner.saveAndClose();

        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), runName);
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), importData);

        clickButton("Save and Finish");
        assertElementPresent(Locators.labkeyError.containing("Inline warning from R transform."));
        assertElementPresent(Locator.linkWithText("Warning link").withAttribute("href", "http://www.labkey.test"));

        File rOutFile = clickAndWaitForDownload(Locator.linkContainingText(R_TRANSFORM_SCRIPT.getName() + "out"));
        File extraFile1 = clickAndWaitForDownload(Locator.linkContainingText("test1.txt"));
        File extraFile2 = clickAndWaitForDownload(Locator.linkContainingText("test2.tsv"));

        String rOut = TestFileUtils.getFileContents(rOutFile);
        assertTrue("Didn't capture R output", rOut.contains("proc.time()"));
        assertEquals("Wrong text in file generated by transform", "This is test file 1 (R).", TestFileUtils.getFileContents(extraFile1).trim());
        assertEquals("Wrong text in file generated by transform", "This is test file 2 (R).", TestFileUtils.getFileContents(extraFile2).trim());

        assertRadioButtonSelected(Locator.id("Previouslyuploadedfiles"));
        clickButton("Proceed");

        clickAndWait(Locator.linkWithText(runName), longWaitForPage);

        DataRegionTable table = new DataRegionTable("Data", this);
        assertEquals(1, table.getDataRowCount());
        assertTextPresent("RWarned");
    }
}
