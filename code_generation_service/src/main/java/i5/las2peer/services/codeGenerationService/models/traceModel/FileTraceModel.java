package i5.las2peer.services.codeGenerationService.models.traceModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

/**
 * The trace model for a single file that contains the sequence of all segments of that file. In
 * addition, it manages which segment belongs to which model element, needed for the model
 * synchronization
 * 
 * @author Thomas Winkler
 *
 */

public class FileTraceModel {
  // some private data structures
  private List<Segment> segmentList = new ArrayList<Segment>();
  private Map<String, List<Segment>> model2Segment = new HashMap<String, List<Segment>>();
  private Map<String, Segment> segmentMap = new HashMap<String, Segment>();
  private Map<String, JSONObject> modelMetaInformation = new HashMap<String, JSONObject>();
  private String fileName;

  // a reference to a global trace model
  private final TraceModel traceModel;

  public FileTraceModel(TraceModel traceModel, String fileName) {
    this.fileName = fileName;
    this.traceModel = traceModel;
  }

  public Segment getRecursiveSegment(String segmentId) {
    for (Segment segment : this.segmentList) {
      if (segment.getId().equals(segmentId)) {
        return segment;
      } else if (segment instanceof CompositeSegment) {
        CompositeSegment cS = ((CompositeSegment) segment);
        Segment recursiveChild = cS.getChildRecursive(segmentId);
        if (recursiveChild != null) {
          return recursiveChild;
        }
      }
    }
    return null;
  }

  public boolean hasSegment(String segmentId) {
    return this.getRecursiveSegment(segmentId) != null;
  }

  public void addSegment(Segment segment) {
    if (!this.hasSegment(segment.getId())) {
      this.segmentMap.put(segment.getId(), segment);
    }
    this.segmentList.add(segment);
  }

  public void addSegments(Collection<Segment> segments) {
    for (Segment segment : segments) {
      this.addSegment(segment);
    }
  }

  /**
   * Add trace information about which model element belongs the given segment. Furthermore, meta
   * information about the model element are saved to the file trace model
   * 
   * @param modelId The id of the model element the segment belongs to
   * @param metaInformation Additional information about the model element that should be saved to
   *        the file trace model, e.g. model type or name
   * @param segment The segment that should be linked to the model element
   */

  public void addTrace(String modelId, JSONObject metaInformation, Segment segment) {

    // we only add trace of segments that does exist
    if (segment == null) {
      return;
    }

    // propagate the trace information to the global trace model
    this.traceModel.addTrace(modelId, this.fileName);

    if (!this.model2Segment.containsKey(modelId)) {
      List<Segment> segmentList = new ArrayList<Segment>();
      this.model2Segment.put(modelId, segmentList);
    }

    this.modelMetaInformation.put(modelId, metaInformation);

    List<Segment> segmentList = this.model2Segment.get(modelId);
    segmentList.add(segment);

  }

  /**
   * Get the file name to which the file trace model belongs
   * 
   * @return The file name
   */

  public String getFileName() {
    return this.fileName;
  }

  /**
   * Get the source code / content of the segments contained in the file trace model, i.e. the
   * current source code the file trace model belongs to
   * 
   * @return The source code of the file
   */

  public String getContent() {
    String res = "";
    for (Segment segment : this.segmentList) {
      res += segment.toString();
    }
    return res;
  }

  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    JSONObject outerObject = new JSONObject();
    JSONArray segments = new JSONArray();
    JSONObject traces = new JSONObject();

    for (Segment segment : this.segmentList) {
      segments.add(segment.toJSONObject());
    }

    for (String modelId : this.model2Segment.keySet()) {
      List<Segment> segmentList = this.model2Segment.get(modelId);
      JSONObject jModelObject = new JSONObject();

      if (this.modelMetaInformation.containsKey(modelId)) {
        jModelObject = this.modelMetaInformation.get(modelId);
      }

      JSONArray jSegmentArray = new JSONArray();
      jModelObject.put("segments", jSegmentArray);

      for (Segment segment : segmentList) {
        if (segment != null) {
          jSegmentArray.add(segment.getId());
        }
      }

      traces.put(modelId, jModelObject);
    }

    outerObject.put("traces", traces);
    outerObject.put("traceSegments", segments);

    return outerObject;
  }

  /**
   * Set the name of the file
   * 
   * @param fileName The new name of the file
   */

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

}
