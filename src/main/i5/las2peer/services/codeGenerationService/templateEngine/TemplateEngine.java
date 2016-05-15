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
import i5.las2peer.services.codeGenerationService.traces.segments.SynchronizeCompositeSegment;

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

  /**
   * Gets the trace model of the tepmlate engine
   * 
   * @return The trace model of the source code / template engine
   */

  public FileTraceModel getFileTraceModel() {
    return this.traceModel;
  }

  /**
   * Get the segment with the given id within the template engine. Therefore, a recursive lookup in
   * the trace model of the engine is performed. Returns null, if non segment with the given id is
   * found
   * 
   * @param segmentId The id of the segment that should be returned
   * @return If the segment is found the requested segment within the template engine, or null if
   *         not found
   */

  public Segment getSegmentFromTraceModel(String segmentId) {
    return this.traceModel.getRecursiveSegment(segmentId);
  }

  /**
   * Get the segment with the given id by using the current template strategy. If the template
   * strategy has found a segment with the given id, it will return that segment. Otherwise it
   * returns the given segment to signal, that non segment with such an id was found
   * 
   * @param segmentId The id of the needed segment
   * @param segment The segment that is returned if the needed segment is not found
   * @return
   */

  public Segment getSegmentByStrategy(String segmentId, Segment segment) {
    Segment result = this.strategy.getSegment(segmentId);
    if (result != null) {
      if (result instanceof SynchronizeCompositeSegment
          || result.getClass() == segment.getClass()) {
        return result;
      } else {
        return segment;
      }
    } else {
      return segment;
    }
  }

  /**
   * Add a segment to the template engine, i.e. the trace model of the engine.
   * 
   * @param segmentId The id of the segment that should be added
   * @param segment The segment that should be added
   * @return The added segment. Based on the template strategy this needs not to be the same object
   *         as the given segment.
   */

  protected Segment addSegmentById(String segmentId, Segment segment) {
    return this.strategy.addSegment(segmentId, segment);
  }

  /**
   * Add a segment to the template engine.
   * 
   * @param segment The segment that should be added
   * @return The added segment. Based on the template strategy this needs not to be the same object
   *         as the given segment.
   */

  protected Segment addSegment(Segment segment) {
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

  public void setSegmentContent(CompositeSegment segment, String variableName, String content) {
    this.strategy.setSegmentContent(segment, content, variableName);
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
    // the strategy determines whether we should reuse a segment or not
    Segment segment = this.strategy.getSegment(id);

    CompositeSegment cSegment = null;
    if (segment instanceof CompositeSegment) {
      cSegment = (CompositeSegment) segment;
    } else {
      cSegment = FileTraceModel.createCompositeSegmentByTraces(id, traces.toJSONString(), code);
    }


    return new Template(cSegment, this);
  }

  public void addTemplate(Template templateFile) {
    CompositeSegment segment = templateFile.getSegment();
    segment = (CompositeSegment) this.addSegmentById(segment.getId(), segment);
    // if the segment was already added, use that one
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
   * Generates initial traces of a given source code
   * 
   * @param content The source code that should be used to generate the traces
   * @return Initial traces of the given source code as a json object
   * 
   */

  @SuppressWarnings("unchecked")
  private static JSONObject generateTraces(String content) {
    JSONObject outerObject = new JSONObject();
    JSONArray segments = new JSONArray();
    Map<String, Integer> elementCountMap = new HashMap<String, Integer>();

    Pattern segmentPattern = Pattern.compile("(\\$[a-zA-Z_]*?\\$)|(-\\{(.*?)\\}-)", Pattern.DOTALL);
    Matcher matcher = segmentPattern.matcher(content);

    Pattern unprotectedBlockPattern = Pattern.compile("^-\\{(.*?)\\}-$", Pattern.DOTALL);
    Pattern unprotectedBlockIdTokenPattern =
        Pattern.compile("^(\\$[a-zA-Z_]*?\\$)", Pattern.DOTALL);
    int s = 0;
    // loop through all found variables or unprotected blocks
    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();
      String segmentName = matcher.group();
      Matcher unprotectedBlockMatcher = unprotectedBlockPattern.matcher(segmentName);

      String id = matcher.group();
      boolean unprotected = false;
      boolean unprotectedIntegrityCheck = false;
      // unprotected block?
      if (unprotectedBlockMatcher.find()) {
        String inner = unprotectedBlockMatcher.group(1);

        Matcher unprotectedBlockIdTokenMatcher = unprotectedBlockIdTokenPattern.matcher(inner);
        // is a id for the unprotected block defined
        if (unprotectedBlockIdTokenMatcher.find()) {
          id = unprotectedBlockIdTokenMatcher.group();
          unprotectedIntegrityCheck = true;
        } else {
          id = "unprotected";
          id += "[" + incrementElementCount(elementCountMap, id) + "]";
        }

        unprotected = true;
      }

      // add the segment before this variable or unprotected block
      if (start - s > 0) {

        String key = id + "before";
        key += "[" + incrementElementCount(elementCountMap, key) + "]";
        segments.add(Segment.createJSONSegment(start - s, key, "protected"));
      }

      if (unprotected) {
        String type = "unprotected";
        if (unprotectedIntegrityCheck) {
          type += "Integrity";
        }
        segments
            .add(Segment.createJSONSegment(unprotectedBlockMatcher.group(1).length(), id, type));
      } else {
        segments.add(Segment.createJSONSegment(end - start, id, "protected"));
      }

      s = end;
    }

    // add the last trailing segment
    String id = "End";

    System.out.println("end '" + content.substring(s, content.length()) + "'");
    segments.add(Segment.createJSONSegment(content.length() - s, id, "protected"));
    outerObject.put("traceSegments", segments);
    return outerObject;
  }

  /**
   * A helper method to count up ids of segments
   * 
   * @param map The map containing ids
   * @param id The id to count
   * @return The count of the given id incremented by one
   */

  private static int incrementElementCount(Map<String, Integer> map, String id) {
    if (!map.containsKey(id)) {
      map.put(id, 0);
    } else {
      map.put(id, map.get(id) + 1);
    }
    return map.get(id);
  }

}
