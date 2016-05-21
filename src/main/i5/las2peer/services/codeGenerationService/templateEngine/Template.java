package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.List;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ContentSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

/**
 * A class representing a single template. Provides methods to set and append variables, that are
 * defined in the template source files
 * 
 * @author Thomas Winkler
 *
 */

public class Template {

  // each template holds a private composition of segments
  private CompositeSegment segment;

  // the reference to its template engine
  private final TemplateEngine templateEngine;

  /**
   * Creates a new Template by a given compositions of segment and a template engine
   * 
   * @param segment - The private composite segment of the template
   * @param templateEngine - The template engine of the template
   */

  public Template(CompositeSegment segment, TemplateEngine templateEngine) {
    this.segment = segment;
    this.templateEngine = templateEngine;
  }

  /**
   * Get the trace model of the template engine
   * 
   * @return The trace model of the template engine
   */

  public FileTraceModel getFileTraceModel() {
    return this.templateEngine.getFileTraceModel();
  }

  /**
   * Get the template engine of the template
   * 
   * @return The template engine of the template
   */

  public TemplateEngine getTemplateEngine() {
    return this.templateEngine;
  }

  /**
   * Sets the composite segment, needed for model synchronization
   * 
   * @param segment - The new composite segment
   */

  public void setSegment(CompositeSegment segment) {
    this.segment = segment;
  }

  public CompositeSegment getSegment() {
    return this.segment;
  }

  public String getId() {
    return this.segment.getId();
  }

  public String getContent() {
    return this.segment.toString();
  }


  /**
   * Set the content of a variable within this template
   * 
   * @param variableName - The name of the variable to set its content
   * @param content - The content that will be set
   */

  public void setVariable(String variableName, String content) {
    this.templateEngine.setSegmentContent(segment, variableName, content);
  }


  /**
   * Adds a template to a variable within this template
   * 
   * @param variableName - The name of the variable
   * @param template - The template that should be added to the variable
   * @param once - If true, you can only append one template to the variable
   */

  public void appendVariable(String variableName, Template template, boolean once) {
    String id = this.getId() + ":" + variableName;
    CompositeSegment containerNew = new CompositeSegment(id);
    CompositeSegment container = containerNew;

    // we can safety cast to a composition as the given "fallback" segment is a composition and
    // getSegmentByStrategy ensures that a segment of
    // the same class is returned
    CompositeSegment segment = (CompositeSegment) templateEngine
        .getSegmentByStrategy(template.getSegment().getId(), template.getSegment());

    Segment recursiveChild = this.segment.getChildRecursive(id);
    if (recursiveChild instanceof CompositeSegment) {
      container = (CompositeSegment) recursiveChild;
    }

    // only add once if ask to do so
    if (once && container.hasChild(segment.getId())) {
      return;
    }

    container.addSegment(segment);

    this.segment.setSegment(variableName, container);
  }


  public void appendVariable(String variableName, Template template) {
    this.appendVariable(variableName, template, false);
  }

  public void appendVariableOnce(String variableName, Template template) {
    this.appendVariable(variableName, template, true);
  }

  public void setVariableIfNotSet(String id, String content) {
    String segmentId = this.getId() + ":" + id;

    // Segment segment = (Segment) templateEngine.getSegmentFromTraceModel(segmentId);
    Segment segment = this.segment.getChildRecursive(segmentId);
    if (segment instanceof ContentSegment && segment.getId().equals(segmentId)) {
      if (segment.toString().equals(id)) {
        ((ContentSegment) segment).setContent(content, false);
      }
    }
  }

  /**
   * Creates a new template of the same template engine as this template
   * 
   * @param id The id of the new template
   * @param sourceCode The template source code
   * @return The new created template
   */

  public Template createTemplate(String id, String sourceCode) {
    return this.templateEngine.createTemplate(id, sourceCode);
  }

  public void insertBreakLine(String idSuffix, String variable) {
    this.appendVariable(variable,
        this.templateEngine.createTemplate(this.getId() + ":breakLine:" + idSuffix, "\n"));
  }

  /**
   * @see #getContent()
   */

  public String toString() {
    return this.getContent();
  }


  /**
   * Factory method to create new templates more easily
   * 
   * @param id Id of the new Template
   * @param content The source code of the template
   * @param traceModel The trace model to which this template should belong to
   * @param fileName The file name of the template
   * @return
   */

  public static Template createInitialTemplate(String id, String content, TraceModel traceModel,
      String fileName) {
    FileTraceModel fileTraceModel = new FileTraceModel(traceModel, fileName);
    TemplateEngine engine =
        new TemplateEngine(new InitialGenerationStrategy(fileTraceModel), fileTraceModel);
    Template template = engine.createTemplate(id, content);
    return template;
  }

  public void addTrace(String modelId, String modelName, Segment segment) {
    this.templateEngine.addTrace(modelId, modelName, segment);
  }

  public void addTrace(String modelId, String modelName, Template template) {
    this.templateEngine.addTrace(modelId, modelName, template);
  }


  /**
   * Remove the last appearance of the given character from the last content segment of the template
   * 
   * @param character The character to remove
   */

  public void removeLastCharacter(char character) {
    List<String> childrenList = this.getSegment().getChildrenList();
    if (childrenList.size() > 0) {
      Segment lastSegment = this.getSegment().getChild(childrenList.get(childrenList.size() - 1));
      if (lastSegment instanceof ContentSegment) {
        ContentSegment contentSegment = (ContentSegment) lastSegment;
        String content = contentSegment.getContent();
        String result = content.substring(0, content.lastIndexOf(character));
        contentSegment.setContent(result);
      }
    }

  }

}
