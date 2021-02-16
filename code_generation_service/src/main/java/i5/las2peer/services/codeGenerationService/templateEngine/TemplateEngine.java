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
import i5.las2peer.services.codeGenerationService.traces.segments.SegmentFactory;

/**
 * A template engine that provides an advanced mechanism to generate and regenerate source code of a
 * CAE model with the help of a file trace model. Using the file trace model allows to locate the
 * positions of placeholder of the variables even after the first variable assignment, i.e. the
 * first code generation.
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
   * Get the trace model of the tepmlate engine
   * 
   * @return The trace model of the source code / template engine
   */

  public FileTraceModel getFileTraceModel() {
    return this.traceModel;
  }

  /**
   * Get the full file name the template engine belongs to
   * 
   * @return The file name the template engine belongs to
   */

  public String getFileName() {
    return this.getFileTraceModel().getFileName();
  }

  /**
   * Add a segment to the template engine.
   * 
   * @param segment The segment that should be added
   * @return The added segment. Based on the template strategy this needs not to be the same object
   *         as the given segment.
   */

  private Segment addSegment(Segment segment) {
    // return this.strategy.addSegment(segment.getId(), segment);

    // check if a segment with the given id already exists
    Segment result = this.strategy.getSegment(segment.getId());
    // if so, return it
    if (result != null) {
      if (result.getClass() == segment.getClass()) {
        return result;
      } else {
        return segment;
      }
    }
    // otherwise add it to the trace model
    else {
      this.getFileTraceModel().addSegment(segment);
    }
    return segment;

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
   * Returns the generated source code as a string. It is an alias for the {@link #getContent()}
   * method
   * 
   * @return The generated source code
   */

  public String toString() {
    return this.getContent();
  }

  /**
   * Creates or return a template of the template engine for a given source code. Depending on the
   * used template strategy of the template engine, this can also be an already existing template
   * 
   * @param id The id of the template
   * @param sourceCode The source code of the template
   * @return A template of the template engine for the source code
   */

  public Template createTemplate(String id, String sourceCode) {
    // the strategy determines whether we should reuse a segment for a template or not
    Segment segment = this.strategy.getSegment(id);
    CompositeSegment cSegment = new CompositeSegment(id);

    if (segment instanceof CompositeSegment) {

      // "reuse" an existing template, i.e. the segment held by the template is set to an already
      // existing one
      cSegment = (CompositeSegment) segment;
    } else {
      // handle window EOL bug.
      sourceCode = sourceCode.replaceAll("\\r\\n", "\n");
      sourceCode = sourceCode.replaceAll("\\r", "\n");

      // create a new composition of segments for the template
      JSONObject traces = TemplateEngine.generateTraces(id, sourceCode);
      sourceCode = TemplateEngine.removeUnprotectedSurroundings(sourceCode);
      JSONArray segments = (JSONArray) traces.get("traceSegments");

      cSegment.addAllSegments(SegmentFactory.createSegments(segments, sourceCode, 0L));
    }

    return new Template(cSegment, this);
  }

  /**
   * Add a template to the template engine. Typically used to add a "root" template to the engine.
   * 
   * @param template The template to add
   */

  public void addTemplate(Template template) {
    CompositeSegment segment = template.getSegment();
    segment = (CompositeSegment) this.addSegment(segment);
    // if the segment was already added, use that one
    if (segment != template.getSegment()) {
      template.setSegment(segment);
    }
  }

  public void addTrace(String modelId, String modelName, Template template) {
    this.addTrace(modelId, modelName, template.getSegment());
  }

  public void addTrace(String modelId, String modelType, String modelName,
      Template elementTemplate) {
    this.addTrace(modelId, modelType, modelName, elementTemplate.getSegment());
  }

  @SuppressWarnings("unchecked")
  public void addTrace(String modelId, String modelName, Segment segment) {
    JSONObject metaInformation = new JSONObject();
    metaInformation.put("name", modelName);
    this.traceModel.addTrace(modelId, metaInformation, segment);
  }

  @SuppressWarnings("unchecked")
  public void addTrace(String modelId, String modelType, String modelName, Segment segment) {
    JSONObject metaInformation = new JSONObject();
    metaInformation.put("name", modelName);
    metaInformation.put("type", modelType);
    this.traceModel.addTrace(modelId, metaInformation, segment);
  }

  /**
   * A helper method that removes the surrounding syntax of unprotected blocks
   * 
   * @param code The code from which the surroundings should be removed
   * @return The updated code
   */

  private static String removeUnprotectedSurroundings(String code) {
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
   * Generates initial traces for a given source code
   * 
   * @param content The source code that should be used to generate the traces
   * @return Initial traces for the given source code as a json object
   * 
   */

  @SuppressWarnings("unchecked")
  private static JSONObject generateTraces(String idPrefix, String content) {
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
        segments.add(Segment.createJSONSegment(start - s, idPrefix + ":" + key, "protected"));
      }

      if (unprotected) {
        String type = "unprotected";
        if (unprotectedIntegrityCheck) {
          type += "Integrity";
        }
        segments.add(Segment.createJSONSegment(unprotectedBlockMatcher.group(1).length(),
            idPrefix + ":" + id, type));
      } else {
        segments.add(Segment.createJSONSegment(end - start, idPrefix + ":" + id, "protected"));
      }

      s = end;
    }

    // add the last trailing segment
    String id = "End";

    segments.add(Segment.createJSONSegment(content.length() - s, idPrefix + ":" + id, "protected"));
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
