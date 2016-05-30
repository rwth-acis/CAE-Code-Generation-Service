package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.generators.ApplicationGenerator;
import i5.las2peer.services.codeGenerationService.generators.Generator;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import i5.las2peer.services.codeGenerationService.traces.segments.SegmentFactory;
import i5.las2peer.services.codeGenerationService.traces.segments.UnprotectedSegment;

/**
 * The model checker class is responsible to find not allowed elements in unprotected segments of
 * the traced model elements.
 * 
 * @author Thomas Winkler
 *
 */

public class ModelChecker {

  private static final L2pLogger logger =
      L2pLogger.getInstance(ApplicationGenerator.class.getName());

  /**
   * Retrieves the guidance model from the template repository model
   * 
   * @param templateRepositoryName The name of the template repository
   * @param gitHubOrganization The organization that is used in the CAE
   * @return A guidance model
   */

  public static GuidanceModel getGuidances(String folder, String templateRepositoryName,
      String gitHubOrganization) {
    GuidanceModel guidanceModel = new GuidanceModel();
    try (TreeWalk treeWalk =
        Generator.getTemplateRepositoryContent(templateRepositoryName, gitHubOrganization)) {

      treeWalk.setFilter(PathFilter.create(folder));
      ObjectReader reader = treeWalk.getObjectReader();

      // walk through the tree and retrieve the guidances
      while (treeWalk.next()) {
        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = reader.open(objectId);

        switch (treeWalk.getNameString()) {
          case "guidances.json":
            String guidances = new String(loader.getBytes(), "UTF-8");
            guidanceModel.addGuidances(guidances);
            return guidanceModel;
        }
      }
    } catch (Exception e) {
      logger.printStackTrace(e);
    }

    return guidanceModel;
  }

  /**
   * The actual method that performs the checking.
   * 
   * @param files The files of the repository of the component that should be tested
   * @param templateRepositoryName The name of the template repository
   * @param gitHubOrganization The organization that is used in the CAE
   * @return A json array containing corresponding guidances of the found violations
   */
  @SuppressWarnings("unchecked")
  public static JSONArray performViolationCheck(HashMap<String, JSONObject> files,
      String templateRepositoryName, String folder, String gitHubOrganization) {

    JSONArray guidances = new JSONArray();
    GuidanceModel guidanceModel = getGuidances(folder, templateRepositoryName, gitHubOrganization);

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
          guidances.addAll(checkSegment(segment, traces, guidanceModel));
        }

      } catch (Exception e) {
        logger.printStackTrace(e);
      }
    }

    return guidances;
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
   * @param parent The parent of the segment, or null if the segment does not have a parent
   * @return True if the check passes, otherwise false
   */

  private static List<JSONObject> checkSegment(Segment segment, JSONObject traces,
      GuidanceModel guidanceModel) {

    List<JSONObject> guidances = new ArrayList<JSONObject>();

    if (segment instanceof CompositeSegment) {
      CompositeSegment cSegment = (CompositeSegment) segment;

      // bottom up check
      List<String> children = cSegment.getChildrenList();
      for (String child : children) {
        Segment childSegment = cSegment.getChild(child);
        guidances.addAll(checkSegment(childSegment, traces, guidanceModel));
      }

      JSONObject modelMeta = getModelFromSegmentId(segment.getId(), traces);
      if (modelMeta != null) {
        String name = (String) modelMeta.get("type");
        guidances.addAll(guidanceModel.createGuidances(name, getUnprotectedSegments(cSegment)));
      }

    }
    return guidances;
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
