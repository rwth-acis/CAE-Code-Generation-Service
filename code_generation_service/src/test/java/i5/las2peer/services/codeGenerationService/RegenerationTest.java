package i5.las2peer.services.codeGenerationService;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModelFactory;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.templateEngine.SynchronizationOrderedStrategy;
import i5.las2peer.services.codeGenerationService.templateEngine.SynchronizationStrategy;
import i5.las2peer.services.codeGenerationService.templateEngine.Template;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateEngine;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateStrategy;
import i5.las2peer.services.codeGenerationService.traces.segments.AppendableVariableSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import i5.las2peer.services.codeGenerationService.traces.segments.SynchronizeOrderedAppendableVariableSegment;

public class RegenerationTest extends TemplateBasicSetup {

  private static TraceModel traceModel = null;
  private static String testCase1FileContent;
  private static JSONObject testCase1FileTraces;
  private static String testCase2FileContent;
  private static JSONObject testCase2FileTraces;

  @BeforeClass
  public static void setUpRegenerationTests() throws Exception {
    traceModel = new TraceModel();
    testCase1FileContent = getContent("testFiles/ModelSynchronization/TestCase1/testFile.txt");
    JSONParser parser = new JSONParser();
    testCase1FileTraces = (JSONObject) parser
        .parse(getContent("testFiles/ModelSynchronization/TestCase1/testFile.txt.traces"));

    testCase2FileContent = getContent("testFiles/ModelSynchronization/TestCase2/testFile.txt");
    testCase2FileTraces = (JSONObject) parser
        .parse(getContent("testFiles/ModelSynchronization/TestCase2/testFile.txt.traces"));
  }

  /**
   * Test to add an additional template to a variable segment The segment already contains two
   * templates, so we need to add three
   */
  @Test
  public void addTemplateTest() {
    FileTraceModel oldFileTraceModel = FileTraceModelFactory.createFileTraceModelFromJSON(
        testCase1FileContent, testCase1FileTraces, traceModel, "testFile.txt");
    TemplateStrategy strategy = new SynchronizationStrategy(oldFileTraceModel);
    TemplateEngine templateEngine = new TemplateEngine(strategy, oldFileTraceModel);


    Template mainTemplate = templateEngine.createTemplate("testFileId", testFileTemplateContent);
    Template contentTemplate1 =
        templateEngine.createTemplate("content1", testContentTemplateContent1);
    setContentTemplateVariables(contentTemplate1);

    mainTemplate.appendVariable("$Content$", contentTemplate1);

    Template contentTemplate2 =
        templateEngine.createTemplate("content2", testContentTemplateContent1);
    setContentTemplateVariables(contentTemplate2);

    mainTemplate.appendVariable("$Content$", contentTemplate2);

    Template contentTemplate3 =
        templateEngine.createTemplate("content3", testContentTemplateContent1);
    setContentTemplateVariables(contentTemplate2);

    mainTemplate.appendVariable("$Content$", contentTemplate3);

    // add root/main template
    templateEngine.addTemplate(mainTemplate);
    FileTraceModel fileTraceModel = templateEngine.getFileTraceModel();
    CompositeSegment mainSegment = (CompositeSegment) fileTraceModel.getRecursiveSegment("testFileId");
    Segment contentSegment = mainSegment.getChildRecursive("testFileId:$Content$");

    // content segment must not be null and must be an appendable variable segment
    assertEquals(true, contentSegment instanceof AppendableVariableSegment);
    AppendableVariableSegment aVSeg = (AppendableVariableSegment) contentSegment;
    List<String> children = aVSeg.getChildrenList();
    // number of children must be 3
    assertEquals(3, children.size());

    // check children
    assertEquals("content1", children.get(0));
    assertEquals("content2", children.get(1));
    assertEquals("content3", children.get(2));

  }

  @Test
  public void removeTemplateTest() {
    FileTraceModel oldFileTraceModel = FileTraceModelFactory.createFileTraceModelFromJSON(
        testCase1FileContent, testCase1FileTraces, traceModel, "testFile.txt");
    TemplateStrategy strategy = new SynchronizationStrategy(oldFileTraceModel);
    TemplateEngine templateEngine = new TemplateEngine(strategy, oldFileTraceModel);
    Template mainTemplate = templateEngine.createTemplate("testFileId", testFileTemplateContent);
    // add root/main template
    templateEngine.addTemplate(mainTemplate);

    Template contentTemplate1 =
        templateEngine.createTemplate("content1", testContentTemplateContent1);
    setContentTemplateVariables(contentTemplate1);

    mainTemplate.appendVariable("$Content$", contentTemplate1);

    CompositeSegment contentSegment =
        (CompositeSegment) mainTemplate.getSegment().getChild("testFileId:$Content$");

    // content segment must not be null and must be an appendable variable segment
    assertEquals(true, contentSegment instanceof AppendableVariableSegment);
    AppendableVariableSegment aVSeg = (AppendableVariableSegment) contentSegment;

    List<String> children = aVSeg.getChildrenList();
    // number of children must be 1
    assertEquals(1, children.size());
    assertEquals("content1", children.get(0));
  }

  @Test
  public void removeAllTemplateTest() {
    FileTraceModel oldFileTraceModel = FileTraceModelFactory.createFileTraceModelFromJSON(
        testCase1FileContent, testCase1FileTraces, traceModel, "testFile.txt");
    TemplateStrategy strategy = new SynchronizationStrategy(oldFileTraceModel);
    TemplateEngine templateEngine = new TemplateEngine(strategy, oldFileTraceModel);
    Template mainTemplate = templateEngine.createTemplate("testFileId", testFileTemplateContent);

    // add root/main template
    templateEngine.addTemplate(mainTemplate);
    FileTraceModel fileTraceModel = templateEngine.getFileTraceModel();
    CompositeSegment mainSegment = (CompositeSegment) fileTraceModel.getRecursiveSegment("testFileId");
    Segment contentSegment = mainSegment.getChildRecursive("testFileId:$Content$");

    // content segment must not be null and must be an appendable variable segment
    assertEquals(true, contentSegment instanceof AppendableVariableSegment);
    AppendableVariableSegment aVSeg = (AppendableVariableSegment) contentSegment;
    // the content segment must not contain any templates anymore
    assertEquals(0, aVSeg.getChildrenList().size());
  }

  @Test
  public void reuseTemplateTest() {
    FileTraceModel oldFileTraceModel = FileTraceModelFactory.createFileTraceModelFromJSON(
        testCase1FileContent, testCase1FileTraces, traceModel, "testFile.txt");
    TemplateStrategy strategy = new SynchronizationStrategy(oldFileTraceModel);
    TemplateEngine templateEngine = new TemplateEngine(strategy, oldFileTraceModel);

    CompositeSegment oldSegment =
        (CompositeSegment) oldFileTraceModel.getRecursiveSegment("content1");

    Template contentTemplate1 =
        templateEngine.createTemplate("content1", testContentTemplateContent1);
    setContentTemplateVariables(contentTemplate1);

    // the old segment must be reused in the content template
    assertEquals(oldSegment, contentTemplate1.getSegment());
    // the content must not have changed
    assertEquals(oldSegment.toString(), contentTemplate1.getContent());
  }

  @Test
  public void orderedTemplateTest() {
    FileTraceModel oldFileTraceModel = FileTraceModelFactory.createFileTraceModelFromJSON(
        testCase2FileContent, testCase2FileTraces, traceModel, "testFile.txt");
    CompositeSegment mainSegment = (CompositeSegment) oldFileTraceModel.getRecursiveSegment("testFileId");
    CompositeSegment contentSegment =
        (CompositeSegment) mainSegment.getChildRecursive("testFileId:$Content$");
    List<String> oldOrder = contentSegment.getChildrenList();

    TemplateStrategy strategy2 = new SynchronizationOrderedStrategy(oldFileTraceModel);
    TemplateEngine templateEngine2 = new TemplateEngine(strategy2, oldFileTraceModel);
    Template mainTemplate = templateEngine2.createTemplate("testFileId", testFileTemplateContent);
    // add root/main template
    templateEngine2.addTemplate(mainTemplate);

    Template contentTemplate1 =
        templateEngine2.createTemplate("content1", testContentTemplateContent1);
    Template contentTemplate2 =
        templateEngine2.createTemplate("content2", testContentTemplateContent1);
    Template contentTemplate3 =
        templateEngine2.createTemplate("content3", testContentTemplateContent1);

    mainTemplate.appendVariable("$Content$", contentTemplate3);
    mainTemplate.appendVariable("$Content$", contentTemplate1);
    mainTemplate.appendVariable("$Content$", contentTemplate2);

    SynchronizeOrderedAppendableVariableSegment reorderSegments =
        (SynchronizeOrderedAppendableVariableSegment) mainTemplate.getSegment()
            .getChild("testFileId:$Content$");

    List<String> newOrder = reorderSegments.getReorderedChildrenList();

    // check right order
    int i = 0;
    for (i = 0; i < oldOrder.size(); i++) {
      String oldId = oldOrder.get(i);
      String newId = newOrder.get(i);
      assertEquals(oldId, newId);
    }
  }


  @Test
  public void orderedRemoveTemplateTest() {

    FileTraceModel oldFileTraceModel = FileTraceModelFactory.createFileTraceModelFromJSON(
        testCase2FileContent, testCase2FileTraces, traceModel, "testFile.txt");

    TemplateStrategy strategy2 = new SynchronizationOrderedStrategy(oldFileTraceModel);
    TemplateEngine templateEngine2 = new TemplateEngine(strategy2, oldFileTraceModel);
    Template mainTemplate = templateEngine2.createTemplate("testFileId", testFileTemplateContent);
    // add root/main template
    templateEngine2.addTemplate(mainTemplate);

    Template contentTemplate2 =
        templateEngine2.createTemplate("content2", testContentTemplateContent1);
    Template contentTemplate3 =
        templateEngine2.createTemplate("content3", testContentTemplateContent1);

    mainTemplate.appendVariable("$Content$", contentTemplate3);
    mainTemplate.appendVariable("$Content$", contentTemplate2);

    SynchronizeOrderedAppendableVariableSegment reorderSegments =
        (SynchronizeOrderedAppendableVariableSegment) mainTemplate.getSegment()
            .getChild("testFileId:$Content$");

    List<String> newOrder = reorderSegments.getReorderedChildrenList();

    // check right order
    assertEquals("content2", newOrder.get(0));
    assertEquals("content3", newOrder.get(1));
  }


  /**
   * A test to check the ordering of templates when additional templates are also added, e.g. 3
   * templates must keep their ordering and a 4th template must be added to the end of the list
   */

  @Test
  public void orderedAddTemplateTest() {

    FileTraceModel oldFileTraceModel = FileTraceModelFactory.createFileTraceModelFromJSON(
        testCase2FileContent, testCase2FileTraces, traceModel, "testFile.txt");
    CompositeSegment mainSegment = (CompositeSegment) oldFileTraceModel.getRecursiveSegment("testFileId");
    CompositeSegment contentSegment =
        (CompositeSegment) mainSegment.getChildRecursive("testFileId:$Content$");
    List<String> oldOrder = contentSegment.getChildrenList();

    TemplateStrategy strategy2 = new SynchronizationOrderedStrategy(oldFileTraceModel);
    TemplateEngine templateEngine2 = new TemplateEngine(strategy2, oldFileTraceModel);
    Template mainTemplate = templateEngine2.createTemplate("testFileId", testFileTemplateContent);
    // add root/main template
    templateEngine2.addTemplate(mainTemplate);

    Template contentTemplate1 =
        templateEngine2.createTemplate("content1", testContentTemplateContent1);
    Template contentTemplate2 =
        templateEngine2.createTemplate("content2", testContentTemplateContent1);
    Template contentTemplate3 =
        templateEngine2.createTemplate("content3", testContentTemplateContent1);
    Template contentTemplate4 =
        templateEngine2.createTemplate("content4", testContentTemplateContent1);

    mainTemplate.appendVariable("$Content$", contentTemplate3);
    mainTemplate.appendVariable("$Content$", contentTemplate1);
    mainTemplate.appendVariable("$Content$", contentTemplate2);
    mainTemplate.appendVariable("$Content$", contentTemplate4);

    SynchronizeOrderedAppendableVariableSegment reorderSegments =
        (SynchronizeOrderedAppendableVariableSegment) mainTemplate.getSegment()
            .getChild("testFileId:$Content$");

    List<String> newOrder = reorderSegments.getReorderedChildrenList();

    // check right order
    int i = 0;
    for (i = 0; i < oldOrder.size(); i++) {
      String oldId = oldOrder.get(i);
      String newId = newOrder.get(i);
      assertEquals(oldId, newId);
    }
    assertEquals("content4", newOrder.get(i));

  }


}
