package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.List;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.AppendableVariableSegment;
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
   * Creates a new Template by a given composition of segments and a template engine
   * 
   * @param segment - The private composite segment of the template
   * @param templateEngine - The template engine of the template
   */

  public Template(CompositeSegment segment, TemplateEngine templateEngine) {
    this.segment = segment;
    this.templateEngine = templateEngine;
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
   * Get the file name the template belongs to
   * 
   * @return The file name the template belongs to
   */

  public String getTemplateFileName() {
    return this.getTemplateEngine().getFileTraceModel().getFileName();
  }

  /**
   * Sets the composite segment, needed for model synchronization
   * 
   * @param segment - The new composite segment
   */

  public void setSegment(CompositeSegment segment) {
    this.segment = segment;
  }

  /**
   * Get the segment that is hold by the template
   * 
   * @return The segment of the template
   */

  public CompositeSegment getSegment() {
    return this.segment;
  }

  /**
   * Get the id of the template, i.e. the id of the segment hold by the template
   * 
   * @return The id of the template
   */

  public String getId() {
    return this.segment.getId();
  }

  /**
   * Get the content of the template
   * 
   * @return The content of the template
   */

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
    this.templateEngine.setSegmentContent(this.segment, variableName, content);
  }

  /**
   * Force to set the content of a variable within this template
   * 
   * @param variableName - The name of the variable to set its content
   * @param content - The content that will be set
   */

  public void setVariableForce(String variableName, String content) {
    this.segment.setSegmentContent(variableName, content, false);
  }

  /**
   * Adds a template to a variable within this template
   * 
   * @param variableName - The name of the variable
   * @param template - The template that should be added to the variable
   * @param once - If true, you can only append one template to the variable
   */

  private void appendVariable(String variableName, Template template, boolean once) {
    AppendableVariableSegment container = this.getAppendableVariableSegment(variableName);
    CompositeSegment segment = template.getSegment();

    // only add once if ask to do so
    if (once && container.hasChild(segment.getId())) {
      return;
    }

    // add the segment
    container.addSegment(segment);

  }


  public void appendVariable(String variableName, Template template) {
    this.appendVariable(variableName, template, false);
  }

  public void appendVariableOnce(String variableName, Template template) {
    this.appendVariable(variableName, template, true);
  }

  public void setVariableIfNotSet(String variableName, String content) {
    String segmentId = this.getId() + ":" + variableName;

    Segment segment = this.segment.getChildRecursive(segmentId);
    if (segment instanceof ContentSegment && segment.getId().equals(segmentId)) {
      if (segment.toString().equals(variableName)) {
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


  /**
   * Adds a new line to a composition of segments
   * 
   * @param idSuffix An id suffix
   * @param variable The name of the variable
   */

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
   * Factory method to create a template of a template engine using the initial generation template
   * strategy.
   * 
   * @param traceModel The trace model to which the template should belong to
   * @param fileName The file name of the template
   * @return A new template
   */

  public static TemplateEngine createInitialTemplateEngine(TraceModel traceModel, String fileName) {
    FileTraceModel fileTraceModel = new FileTraceModel(traceModel, fileName);
    TemplateEngine engine = new TemplateEngine(new InitialGenerationStrategy(), fileTraceModel);

    traceModel.addFileTraceModel(fileTraceModel);

    return engine;
  }


  /**
   * Get a appendable segment for a variable. That is a container for a variable name that can hold
   * the appended templates. If it does not exist yet, it will be created.
   * 
   * @param variableName The variable name
   * @return The appendable segment for the given variable name
   */

  private AppendableVariableSegment getAppendableVariableSegment(String variableName) {
    String id = this.getId() + ":" + variableName;
    AppendableVariableSegment container = new AppendableVariableSegment(id);

    Segment recursiveChild = this.segment.getChildRecursive(id);
    if (recursiveChild instanceof AppendableVariableSegment) {
      container = (AppendableVariableSegment) recursiveChild;
    }

    // set the container
    this.segment.setVariableSegment(variableName, container);

    return container;
  }

  /**
   * Get the last segment of the template if this is a content segment, otherwise null is returned.
   * 
   * @return The last segment if this is a content segment, otherwise null
   */

  private ContentSegment getLastContentSegment() {
    List<String> childrenList = this.getSegment().getChildrenList();
    if (childrenList.size() > 0) {
      Segment lastSegment = this.getSegment().getChild(childrenList.get(childrenList.size() - 1));
      if (lastSegment instanceof ContentSegment) {
        return (ContentSegment) lastSegment;
      }
    }

    return null;
  }

  /**
   * Remove the last appearance of the given character from the last segment of the template if it
   * is not a composition
   * 
   * @param character The character to remove
   */

  public void removeLastCharacter(char character) {
    ContentSegment contentSegment = this.getLastContentSegment();
    if (contentSegment != null) {
      String content = contentSegment.getContent();
      int characterIndex = content.lastIndexOf(character);
      if (characterIndex > -1) {
        String result = content.substring(0, content.lastIndexOf(character));
        contentSegment.setContent(result);
      }
    }
  }

  /**
   * Append a character to the last segment of the template if it is not a composition
   * 
   * @param character The character to add
   */

  public void appendContent(char character) {
    ContentSegment contentSegment = this.getLastContentSegment();
    if (contentSegment != null) {
      String content = contentSegment.getContent() + character;
      contentSegment.setContent(content);
    }
  }

}
