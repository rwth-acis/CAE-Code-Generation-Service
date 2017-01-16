package i5.las2peer.services.codeGenerationService;

import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;

import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.templateEngine.Template;

public class TemplateBasicSetup {
  protected static TraceModel traceModel = null;
  protected static String testFileTemplateContent;
  protected static String testContentTemplateContent1;
  protected static String testContentTemplateContent2;
  protected static String testContentTemplateContent3;



  protected static void setContentTemplateVariables(Template template) {
    setContentTemplateVariables(template, "id", "ElementContetn");
  }

  protected static void setContentTemplateVariables(Template template, String id, String content) {
    template.setVariable("$Id$", id);
    template.setVariable("$ElementContent$", content);
  }

  @BeforeClass
  public static void setUpTests() throws Exception {
    traceModel = new TraceModel();
    testFileTemplateContent = getContent("testFiles/sharedFiles/templates/main.txt");
    testContentTemplateContent1 =
        getContent("testFiles/sharedFiles/templates/ContentTemplate1.txt");
    testContentTemplateContent2 =
        getContent("testFiles/sharedFiles/templates/ContentTemplate2.txt");
    testContentTemplateContent3 =
        getContent("testFiles/sharedFiles/templates/ContentTemplate3.txt");
  }

  @Before
  public void setUpTest() throws Exception {
    traceModel = new TraceModel();
  }


  protected static String getContent(String path) {
    String content = "";
    try {
      FileInputStream fis = new FileInputStream(path);
      content = IOUtils.toString(fis, "UTF-8");
    } catch (Exception e) {
      e.printStackTrace();
    }

    return content;
  }
}
