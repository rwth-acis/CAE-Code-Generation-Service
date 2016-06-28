package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.generators.ApplicationGenerator;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import i5.las2peer.services.codeGenerationService.traces.segments.SegmentFactory;
import i5.las2peer.services.codeGenerationService.traces.segments.UnprotectedSegment;

/**
 * The model violation detection class is responsible to find not allowed elements in unprotected
 * segments of the traced model elements.
 * 
 * @author Thomas Winkler
 *
 */

public class ModelViolationDetection {

  private static final L2pLogger logger =
      L2pLogger.getInstance(ApplicationGenerator.class.getName());


  /**
   * The actual method that performs the checking.
   * 
   * @param files The files of the repository of the component that should be tested
   * @param violationsRules The rules for the violation detection
   * @return A json array containing corresponding guidances of the found violations
   */
  @SuppressWarnings("unchecked")
  public static JSONArray performViolationCheck(HashMap<String, JSONObject> files,
      JSONObject violationsRules) {
    JSONArray feedback = new JSONArray();
    GuidanceModel guidanceModel = new GuidanceModel();
    guidanceModel.addRules(violationsRules);

    Iterator<String> it = files.keySet().iterator();
    while (it.hasNext()) {
      String fileName = it.next();
      JSONObject fileObject = files.get(fileName);
      String content = (String) fileObject.get("content");
      byte[] base64decodedBytes = Base64.getDecoder().decode(content);

      try {
        content = new String(base64decodedBytes, "utf-8");
        JSONObject fileTraces = (JSONObject) fileObject.get("fileTraces");
        JSONArray traceSegments = (JSONArray) fileTraces.get("traceSegments");
        JSONObject traces = (JSONObject) fileTraces.get("traces");

        List<Segment> segments = SegmentFactory.createSegments(traceSegments, content, 0L);
        for (Segment segment : segments) {
          feedback.addAll(checkSegment(segment, traces, guidanceModel));
        }

      } catch (Exception e) {
        logger.printStackTrace(e);
      }
    }
    return feedback;
  }

  private static List<UnprotectedSegment> getUnprotectedSegments(CompositeSegment cSegment) {
    List<UnprotectedSegment> list = new ArrayList<UnprotectedSegment>();

    List<String> children = cSegment.getChildrenList();
    for (String child : children) {
      Segment childSegment = cSegment.getChild(child);
      if (childSegment instanceof UnprotectedSegment) {
        list.add((UnprotectedSegment) childSegment);
      } else if (childSegment instanceof CompositeSegment) {
        list.addAll(getUnprotectedSegments((CompositeSegment) childSegment));
      }
    }

    return list;
  }

  /**
   * Method to check a single segment if it contains not allowed content
   * 
   * @param segment The segment to check
   * @param traces The traces of the file the segment is contained. Used for additional information
   *        needed during the check
   * @param guidanceModel The guidance model to use
   * @return A list of all found violations
   */

  private static List<JSONObject> checkSegment(Segment segment, JSONObject traces,
      GuidanceModel guidanceModel) {

    List<JSONObject> feedback = new ArrayList<JSONObject>();

    if (segment instanceof CompositeSegment) {
      CompositeSegment cSegment = (CompositeSegment) segment;

      // bottom up check
      List<String> children = cSegment.getChildrenList();
      for (String child : children) {
        Segment childSegment = cSegment.getChild(child);
        feedback.addAll(checkSegment(childSegment, traces, guidanceModel));
      }



      JSONObject modelMeta = getModelFromSegmentId(segment.getId(), traces);
      if (modelMeta != null) {
        String type = (String) modelMeta.get("type");
        feedback.addAll(guidanceModel.createFeedback(type, getUnprotectedSegments(cSegment)));
      }

    }
    return feedback;
  }

  @SuppressWarnings("unchecked")
  private static JSONObject getModelFromSegmentId(String id, JSONObject traces) {
    JSONObject modelMeta = null;
    Iterator<String> itr = traces.keySet().iterator();
    while (itr.hasNext()) {
      String modelId = itr.next();
      JSONObject model = (JSONObject) traces.get(modelId);
      JSONArray segments = (JSONArray) model.get("segments");
      if (segments != null && segments.contains(id)) {
        modelMeta = model;
        break;
      }
    }

    if (modelMeta != null) {
      return modelMeta;
    }

    return modelMeta;
  }
}
