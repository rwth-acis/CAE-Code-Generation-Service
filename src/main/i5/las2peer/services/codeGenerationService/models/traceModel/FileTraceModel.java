package i5.las2peer.services.codeGenerationService.models.traceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ContentSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ProtectedSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import i5.las2peer.services.codeGenerationService.traces.segments.UnprotectedSegment;

public class FileTraceModel {
  private List<Segment> segmentList = new ArrayList<Segment>();
  private Map<String, List<Segment>> model2Segment = new HashMap<String, List<Segment>>();
  private Map<String, Segment> segmentMap = new HashMap<String, Segment>();
  private Map<String, JSONObject> modelMetaInformation = new HashMap<String, JSONObject>();
  private String fileName;

  private final TraceModel traceModel;

  public FileTraceModel(TraceModel traceModel, String fileName) {
    this.fileName = fileName;
    this.traceModel = traceModel;
  }

  public static CompositeSegment createCompositeSegmentByTraces(String id, String traces,
      String template) {
    return CompositeSegment.createByTraces(id, traces, template);
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
    this.segmentList.add(segment);
    if (!this.hasSegment(segment.getId())) {
      this.segmentMap.put(segment.getId(), segment);
    }
  }

  public void addSegments(List<Segment> segments) {
    for (Segment segment : segments) {
      this.addSegment(segment);
    }
  }

  public void addTrace(String modelId, JSONObject metaInformation, Segment segment) {

    if (segment == null) {
      return;
    }

    this.traceModel.addTrace(modelId, this.fileName);

    if (!this.model2Segment.containsKey(modelId)) {
      List<Segment> segmentList = new ArrayList<Segment>();
      this.model2Segment.put(modelId, segmentList);
    }

    this.modelMetaInformation.put(modelId, metaInformation);

    List<Segment> segmentList = this.model2Segment.get(modelId);
    segmentList.add(segment);

  }

  public void setSegmentContent(String content, String context) {
    Segment segment = this.segmentMap.get(context);
    if (segment instanceof ContentSegment) {
      ((ContentSegment) segment).setContent(content);
    }
  }

  public static FileTraceModel parseFileTraceModel(String source, String traceSource,
      TraceModel traceModel, String fileName) {
    FileTraceModel fileTraceModel = new FileTraceModel(traceModel, fileName);
    JSONParser parser = new JSONParser();
    JSONObject jobj;

    try {
      jobj = (JSONObject) parser.parse(traceSource);
      JSONArray segments = (JSONArray) jobj.get("traceSegments");
      JSONObject traces = (JSONObject) jobj.get("traces");

      fileTraceModel.addSegments(parseSegments(segments, source, 0L));
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return fileTraceModel;
  }

  public static Long getLength(JSONObject entry) {
    Object length = entry.get("length");
    // if the entry is already an instance of the Long class, we can directly cast it
    if (length instanceof Long) {
      return ((Long) length);
    }
    // otherwise we need to parse it as a long value
    else {
      return Long.parseLong(length.toString() + "");
    }
  }

  public static List<Segment> parseSegments(JSONArray jSegments, String template, Long start) {

    List<Segment> list = new ArrayList<Segment>();

    for (int i = 0; i < jSegments.size(); i++) {
      JSONObject entry = (JSONObject) jSegments.get(i);

      String type = (String) entry.get("type");
      String segmentId = (String) entry.get("id");
      switch (type) {
        case "composite":
          JSONArray subSegments = (JSONArray) entry.get("traceSegments");
          CompositeSegment c = new CompositeSegment(segmentId);
          if (subSegments != null) {
            c.addAllSegments(parseSegments(subSegments, template, start));
          }
          list.add(c);
          start += c.getLength();
          break;
        case "unprotected": {
          Long length = getLength(entry);
          ContentSegment segment = new UnprotectedSegment(segmentId);
          segment.setContent(
              template.substring(Math.toIntExact(start), Math.toIntExact(start + length)));
          list.add(segment);
          start += segment.getLength();
          break;
        }
        case "protected": {
          Long length = getLength(entry);
          ContentSegment segment = new ProtectedSegment(segmentId);
          segment.setContent(
              template.substring(Math.toIntExact(start), Math.toIntExact(start + length)));
          list.add(segment);
          start += segment.getLength();
          break;
        }
      }

    }


    return list;
  }

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

  public String toJSONString() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this.toJSONObject());
  }

  public boolean containsSegment(String segmentId) {
    return this.segmentMap.containsKey(segmentId);
  }

  public Segment getSegment(String segmentId) {
    return this.segmentMap.get(segmentId);
  }
}
