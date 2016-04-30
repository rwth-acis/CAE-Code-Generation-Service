package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

/**
 * A template engine that provides an advanced mechanism to generate and regenerate source code of a
 * model with respect to a trace model. By using the trace model it allows to locate the variables
 * even after the first variable assignment, i.e. the first code generation.
 *
 */

public class TemplateEngine {

  private FileTraceModel traceModel;
  private TemplateStrategy strategy;

  /**
   * Creates a new template engine by a given template strategy and a trace model
   * 
   * @param strategy The strategy to use in this template engine
   * @param traceModel The trace model of the source code. If the source code was not yet generated,
   *        an initial trace model can be used.
   */

  public TemplateEngine(TemplateStrategy strategy, FileTraceModel traceModel) {
    this.traceModel = traceModel;
    this.strategy = strategy;
  }

  public FileTraceModel getFileTraceModel() {
    return this.traceModel;
  }

  public Segment getSegment(String segmentId) {
    return this.traceModel.getRecursiveSegment(segmentId);
  }

  /**
   * Returns the corresponding segment of a given id when it already exists otherwise it returns the
   * given one
   * 
   * TODO: Move to TemplateStrategy
   * 
   * @param segmentId The id of the needed segment
   * @param segment The segment that is returned if the needed segment is not found
   * @return
   */

  public Segment getSegment(String segmentId, Segment segment) {
    Segment result = this.traceModel.getRecursiveSegment(segmentId);
    if (result != null) {
      if (result.getClass() == segment.getClass()) {
        return result;
      } else {
        return segment;
      }
    } else {
      return segment;
    }
  }



  public Segment getSegment(String segmentId, Segment segment, Template context) {
    Segment result = this.traceModel.getRecursiveSegment(segmentId);
    if (result != null) {
      if (result.getClass() == segment.getClass()) {
        return result;
      } else {
        return segment;
      }
    } else {
      return segment;
    }
  }

  public Segment addSegmentById(String segmentId, Segment segment) {
    return this.strategy.addSegment(segmentId, segment);
  }

  public Segment addSegment(Segment segment) {
    return this.addSegmentById(segment.getId(), segment);
  }

  /**
   * Returns the generated source code as a string
   * 
   * @return The generated source code
   */

  public String getContent() {
    return this.traceModel.getContent();
  }

  /**
   * Returns the trace model as a json object
   * 
   * @return The trace model represented in a json object
   */

  public JSONObject toJSONObject() {
    return this.traceModel.toJSONObject();
  }

  /**
   * Returns the generated source code as a string. It is an alias for the getContent() method
   * 
   * @return The generated source code
   */

  public String toString() {
    return this.getContent();
  }

  public Template createTemplate(String id, String sourceCode) {
    JSONObject traces = TemplateEngine.generateTraces(sourceCode);
    String code = TemplateEngine.removeUnprotectedBlocks(sourceCode);
    CompositeSegment segment =
        FileTraceModel.createCompositeSegmentByTraces(id, traces.toJSONString(), code);
    return new Template(segment, this);
  }

  public void addTemplate(Template templateFile) {
    CompositeSegment segment = templateFile.getSegment();
    segment = (CompositeSegment) this.addSegmentById(segment.getId(), segment);
    if (segment != templateFile.getSegment()) {
      templateFile.setSegment(segment);
    }
  }

  public void addTrace(String modelId, String modelName, Template template) {
    this.addTrace(modelId, modelName, template.getSegment());
  }

  @SuppressWarnings("unchecked")
  public void addTrace(String modelId, String modelName, Segment segment) {
    JSONObject metaInformation = new JSONObject();
    metaInformation.put("name", modelName);
    this.traceModel.addTrace(modelId, metaInformation, segment);
  }

  private static String removeUnprotectedBlocks(String code) {
    Pattern unprotectedBlocks = Pattern.compile("(-\\{(.*?)\\}-)", Pattern.DOTALL);
    Matcher matcher = unprotectedBlocks.matcher(code);

    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();
      int startGroup = matcher.start(2);
      int endGroup = matcher.end(2);

      String group = code.substring(startGroup, endGroup);
      StringBuilder strBuilder = new StringBuilder(code);
      strBuilder.replace(start, end, group);
      code = strBuilder.toString();
      // reset matcher as code may have changed
      matcher = unprotectedBlocks.matcher(code);
    }
    return code;
  }

  /**
   * 
   * Generates initial traces from a given source code
   * 
   * @param content The source code that should be used to generate the traces
   * 
   * @return Initial traces represented as json object
   * 
   */

  @SuppressWarnings("unchecked")
  public static JSONObject generateTraces(String content) {
    JSONObject outerObject = new JSONObject();
    JSONArray segments = new JSONArray();
    Map<String, Integer> elementCountMap = new HashMap<String, Integer>();

    Pattern segmentPattern = Pattern.compile("(\\$[a-zA-Z_]*?\\$)|(-\\{.*?\\}-)", Pattern.DOTALL);
    Matcher matcher = segmentPattern.matcher(content);

    Pattern unprotectedBlockPattern = Pattern.compile("^-\\{(.*?)\\}-$", Pattern.DOTALL);

    int s = 0;
    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();
      String segmentName = matcher.group();
      Matcher m = unprotectedBlockPattern.matcher(segmentName);

      String id = matcher.group();
      boolean unprotected = false;

      if (m.find()) {
        unprotected = true;
        id = "unprotected";
        id += "[" + CompositeSegment.incrementElementCount(elementCountMap, id) + "]";
      }

      if (start - s > 0) {
        String key = id + "before";
        key += "[" + CompositeSegment.incrementElementCount(elementCountMap, key) + "]";
        segments.add(CompositeSegment.createJSONSegment(start - s, key, "protected"));
      }

      if (unprotected) {
        segments.add(CompositeSegment.createJSONSegment(m.group(1).length(), id, "unprotected"));
      } else {
        segments.add(CompositeSegment.createJSONSegment(end - start, id, "protected"));
      }

      s = end;
    }

    String id = "End";
    segments.add(CompositeSegment.createJSONSegment(content.length() - s, id, "protected"));
    outerObject.put("traceSegments", segments);
    return outerObject;
  }

}
