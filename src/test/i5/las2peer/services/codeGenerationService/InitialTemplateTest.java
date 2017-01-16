package i5.las2peer.services.codeGenerationService;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.templateEngine.Template;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateEngine;
import i5.las2peer.services.codeGenerationService.traces.segments.AppendableVariableSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import i5.las2peer.services.codeGenerationService.traces.segments.UnprotectedSegment;

public class InitialTemplateTest extends TemplateBasicSetup {


  @Test
  public void rootTemplateTest() {
    TemplateEngine templateEngine =
        Template.createInitialTemplateEngine(traceModel, "testFile.txt");
    Template mainTemplate = templateEngine.createTemplate("testFileId", testFileTemplateContent);

    // add root/main template
    templateEngine.addTemplate(mainTemplate);
    FileTraceModel fileTraceModel = templateEngine.getFileTraceModel();

    // file trace model must contain the root/main template, and it must be an composite segment
    assertEquals(true, fileTraceModel.hasSegment("testFileId"));
    assertEquals(true,
        fileTraceModel.getRecursiveSegment("testFileId") instanceof CompositeSegment);
  }

  @Test
  public void unprotectedSegmentIntegrityCheckTest() {
    TemplateEngine templateEngine =
        Template.createInitialTemplateEngine(traceModel, "testFile.txt");
    Template mainTemplate = templateEngine.createTemplate("testFileId", testFileTemplateContent);

    // add root/main template
    templateEngine.addTemplate(mainTemplate);

    Template contentTemplate3 =
        templateEngine.createTemplate("content1", testContentTemplateContent3);

    String content = "content";
    contentTemplate3.setVariable("$UnprotectedBlock$", content);
    mainTemplate.appendVariable("$Content$", contentTemplate3);

    // get the unprotected segment
    CompositeSegment mainSegment = mainTemplate.getSegment();
    CompositeSegment contentSegment =
        (CompositeSegment) mainSegment.getChildRecursive("testFileId:$Content$");

    CompositeSegment content1Segment = (CompositeSegment) contentSegment.getChild("content1");

    Segment segment = content1Segment.getChild("content1:$UnprotectedBlock$");

    // the segment must be an unprotected segment
    assertEquals(true, segment instanceof UnprotectedSegment);
    UnprotectedSegment unprotectedSegment = (UnprotectedSegment) segment;

    // the content of the segment must not contain the surrounding syntax of unprotected segments,
    // i.e. "-{ }-" must be removed from the content
    assertEquals(true, unprotectedSegment.getContent().indexOf("-{") == -1
        && unprotectedSegment.getContent().indexOf("}-") == -1);

    // the unprotected segment must have hash
    assertEquals(true, unprotectedSegment.getHash() != null);
    // the hash value must be equal to the md5 hash of the content
    assertEquals(true, unprotectedSegment.getHash().equals(UnprotectedSegment.getHash(content)));

    String newContent = "newContent";
    unprotectedSegment.setContent(newContent, true);

    // if the integrity check passes, the content must be set
    assertEquals(true, unprotectedSegment.getContent().equals(newContent));
    assertEquals(true, unprotectedSegment.getHash().equals(UnprotectedSegment.getHash(newContent)));
    // set content without updating hash to simulate manually change of the segment
    unprotectedSegment.setContent(content);
    // try to set new content
    unprotectedSegment.setContent(newContent, true);

    // the content must not be changed, i.e. it must be equal to the old content
    assertEquals(true, unprotectedSegment.getContent().equals(content));
    // the hash must be equal to the new content
    assertEquals(true, unprotectedSegment.getHash().equals(UnprotectedSegment.getHash(newContent)));


  }

  @Test
  public void unprotectedSegmentTest() {
    TemplateEngine templateEngine =
        Template.createInitialTemplateEngine(traceModel, "testFile.txt");
    Template mainTemplate = templateEngine.createTemplate("testFileId", testFileTemplateContent);
    Template contentTemplate2 =
        templateEngine.createTemplate("content1", testContentTemplateContent2);

    mainTemplate.appendVariable("$Content$", contentTemplate2);

    // add root/main template
    templateEngine.addTemplate(mainTemplate);
    FileTraceModel fileTraceModel = templateEngine.getFileTraceModel();

    // get the unprotected segment
    CompositeSegment mainSegment =
        (CompositeSegment) fileTraceModel.getRecursiveSegment("testFileId");
    CompositeSegment contentSegment =
        (CompositeSegment) mainSegment.getChildRecursive("testFileId:$Content$");
    CompositeSegment content1Segment = (CompositeSegment) contentSegment.getChild("content1");

    Segment segment = content1Segment.getChild("content1:unprotected[0]");

    // the segment must be an unprotected segment
    assertEquals(true, segment instanceof UnprotectedSegment);
    UnprotectedSegment unprotectedSegment = (UnprotectedSegment) segment;

    // the content of the segment must not contain the surrounding syntax of unprotected segments,
    // i.e. "-{ }-" must be removed from the content
    assertEquals(true, unprotectedSegment.getContent().indexOf("-{") == -1
        && unprotectedSegment.getContent().indexOf("}-") == -1);

    // the unprotected segment must not have any hash
    assertEquals(true, unprotectedSegment.getHash() == null);
  }

  @Test
  public void appendTest() {
    TemplateEngine templateEngine =
        Template.createInitialTemplateEngine(traceModel, "testFile.txt");
    Template mainTemplate = templateEngine.createTemplate("testFileId", testFileTemplateContent);
    Template contentTemplate1 =
        templateEngine.createTemplate("content1", testContentTemplateContent1);
    setContentTemplateVariables(contentTemplate1);

    mainTemplate.appendVariable("$Content$", contentTemplate1);

    // add root/main template
    templateEngine.addTemplate(mainTemplate);
    FileTraceModel fileTraceModel = templateEngine.getFileTraceModel();
    CompositeSegment mainSegment =
        (CompositeSegment) fileTraceModel.getRecursiveSegment("testFileId");
    Segment contentSegment = mainSegment.getChildRecursive("testFileId:$Content$");

    // content segment must not be null and must be an appendable variable segment
    assertEquals(true, contentSegment instanceof AppendableVariableSegment);
    AppendableVariableSegment aVSeg = (AppendableVariableSegment) contentSegment;
    // number of children must be 1
    assertEquals(1, aVSeg.getChildrenList().size());
  }

  @Test
  public void multipleAppendTest() {
    TemplateEngine templateEngine =
        Template.createInitialTemplateEngine(traceModel, "testFile.txt");
    Template mainTemplate = templateEngine.createTemplate("testFileId", testFileTemplateContent);
    Template contentTemplate1 =
        templateEngine.createTemplate("content1", testContentTemplateContent1);
    setContentTemplateVariables(contentTemplate1);

    mainTemplate.appendVariable("$Content$", contentTemplate1);

    Template contentTemplate2 =
        templateEngine.createTemplate("content2", testContentTemplateContent1);
    setContentTemplateVariables(contentTemplate2);

    mainTemplate.appendVariable("$Content$", contentTemplate2);

    // add root/main template
    templateEngine.addTemplate(mainTemplate);
    FileTraceModel fileTraceModel = templateEngine.getFileTraceModel();
    CompositeSegment mainSegment =
        (CompositeSegment) fileTraceModel.getRecursiveSegment("testFileId");
    Segment contentSegment = mainSegment.getChildRecursive("testFileId:$Content$");

    // content segment must not be null and must be an appendable variable segment
    assertEquals(true, contentSegment instanceof AppendableVariableSegment);
    AppendableVariableSegment aVSeg = (AppendableVariableSegment) contentSegment;
    // number of children must be 2
    assertEquals(2, aVSeg.getChildrenList().size());
  }

  @Test
  public void variableSetTest() {
    TemplateEngine templateEngine =
        Template.createInitialTemplateEngine(traceModel, "testContentFile.txt");
    Template contentTemplate1 =
        templateEngine.createTemplate("content1", testContentTemplateContent1);
    // add root/main template
    templateEngine.addTemplate(contentTemplate1);

    String id = "testId1234";
    String content = "content\n ";
    setContentTemplateVariables(contentTemplate1, id, content);

    String compare =
        testContentTemplateContent1.replace("$Id$", id).replace("$ElementContent$", content);
    assertEquals(compare, contentTemplate1.getContent());

  }

  @Test
  public void variableIfNotSetTest() {
    TemplateEngine templateEngine =
        Template.createInitialTemplateEngine(traceModel, "testContentFile.txt");
    Template contentTemplate1 =
        templateEngine.createTemplate("content1", testContentTemplateContent1);

    String id = "testId1234";
    String content = "content\n ";

    contentTemplate1.setVariableIfNotSet("$Id$", id);
    contentTemplate1.setVariableIfNotSet("$ElementContent$", content);

    // add root/main template
    templateEngine.addTemplate(contentTemplate1);

    // the variables should have been set correctly
    String compare =
        testContentTemplateContent1.replace("$Id$", id).replace("$ElementContent$", content);
    assertEquals(compare, contentTemplate1.getContent());

    id = "neueId1234";
    content = "Batman";

    contentTemplate1.setVariableIfNotSet("$Id$", id);
    contentTemplate1.setVariableIfNotSet("$ElementContent$", content);
    // the content of the template may not have changed
    assertEquals(compare, contentTemplate1.getContent());
  }

}
