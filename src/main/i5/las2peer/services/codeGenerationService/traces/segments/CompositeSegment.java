package i5.las2peer.services.codeGenerationService.traces.segments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class CompositeSegment extends Segment {
  public CompositeSegment(String id) {
    super(id);
  }

  final private List<String> children = new ArrayList<String>();
  final private Map<String, Segment> map = new HashMap<String, Segment>();

  public void add(final Segment comp) {
    String id = comp.getId();
    if (!map.containsKey(id)) {
      map.put(id, comp);
    }
    children.add(id);
  }

  public Segment getChild(String id) {
    return this.map.get(id);
  }

  public boolean hasChild(String id) {
    return this.map.containsKey(id);
  }

  public void remove(final Segment comp) {
    children.remove(comp.getId());
  }

  public void setSegment(String id, CompositeSegment segment) {
    map.put(id, segment);
  }


  public void setSegmentContent(String id, String content) {

    // if this composite segment holds a segment with the given id, set its content
    if (map.containsKey(id)) {
      Segment segment = map.get(id);
      if (segment instanceof ContentSegment) {
        ((ContentSegment) segment).setContent(content);
      }
    }

    // now propagate the content recursively to the segments that are also compositions
    for (String cid : this.children) {
      Segment segment = this.map.get(cid);
      if (segment instanceof CompositeSegment) {
        ((CompositeSegment) segment).setSegmentContent(id, content);
      }
    }
  }

  public void deleteSegment(String id) {
    this.map.remove(this.getId() + ":" + id);
  }

  @SuppressWarnings("unchecked")
  public static JSONObject createJSONSegment(int length, String id, String type) {
    JSONObject obj = new JSONObject();
    obj.put("id", id);
    obj.put("length", length);
    obj.put("type", type);
    return obj;
  }

  public static Segment createSegment(String type, String id, String content) {
    ContentSegment segment = null;
    if (type.equals("protected")) {
      segment = new ProtectedSegment(id);
      segment.setContent(content);
    } else if (type.equals("unprotected")) {
      segment = new UnprotectedSegment(id);
      segment.setContent(content);
    }
    return segment;
  }


  public static CompositeSegment createByTraces(String id, String traces, String source) {
    JSONParser parser = new JSONParser();
    CompositeSegment cS = new CompositeSegment(id);

    try {
      Object obj = parser.parse(traces);
      JSONObject jobj = (JSONObject) obj;
      JSONArray jarray = (JSONArray) jobj.get("traceSegments");
      Long start = 0L;
      Pattern pattern = Pattern.compile("(\\$([a-zA-Z_]*)\\$)");

      for (int i = 0; i < jarray.size(); i++) {
        JSONObject entry = (JSONObject) jarray.get(i);
        Long length = (Long) entry.get("length");
        String type = (String) entry.get("type");
        String idSuffix = (String) entry.get("id");
        String sourcePart =
            source.substring(Math.toIntExact(start), Math.toIntExact(start + length));
        Matcher matcher = pattern.matcher(sourcePart);

        // check for variable name
        if (matcher.find()) {
          // if a variable name was found, we need to check if a segment for that specific variable
          // has already been created
          if (!cS.hasChild(matcher.group(1))) {
            cS.add(createSegment(type, matcher.group(1), sourcePart));
          } else {
            cS.add(cS.getChild(matcher.group(1)));
          }
        } else {
          // a variable name was not found, so we use the idSuffix
          cS.add(createSegment(type, id + ":" + idSuffix, sourcePart));
        }

        start += length;
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return cS;

  }

  public static int incrementElementCount(Map<String, Integer> map, String id) {
    if (!map.containsKey(id)) {
      map.put(id, 0);
    } else {
      map.put(id, map.get(id) + 1);
    }
    return map.get(id);
  }


  @SuppressWarnings("unchecked")
  public static String generateTraces(String content) {
    JSONObject outerObject = new JSONObject();
    JSONArray segments = new JSONArray();
    Map<String, Integer> elementCountMap = new HashMap<String, Integer>();

    Pattern pattern = Pattern.compile("(\\$[^\\$]*\\$)");
    Matcher matcher = pattern.matcher(content);

    Pattern r2 = Pattern.compile("^\\$\\{([^\\$\\}]*)\\}\\$$");

    int s = 0;
    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();
      String segmentName = matcher.group();
      Matcher m = r2.matcher(segmentName);

      String id = matcher.group();
      boolean unprotected = false;

      if (m.find()) {
        unprotected = true;
        id = "unprotected";
        id += "[" + incrementElementCount(elementCountMap, id) + "]";
      }

      if (start - s > 0) {
        String key = id + "before";
        key += "[" + incrementElementCount(elementCountMap, key) + "]";
        segments.add(createJSONSegment(start - s, key, "protected"));
      }

      if (unprotected) {
        segments.add(createJSONSegment(m.group(1).length(), id, "unprotected"));
      } else {
        segments.add(createJSONSegment(end - start, id, "protected"));
      }

      s = end;
    }

    String id = "End";
    segments.add(createJSONSegment(content.length() - s, id, "protected"));
    outerObject.put("traceableSegments", segments);
    return outerObject.toString();
  }



  public String toString() {
    String content = "";
    for (String id : this.children) {
      if (this.map.containsKey(id)) {
        content += this.map.get(id).toString();// this.map.get(id).getContent();
      }
    }
    return content;
  }

  @Override
  public void replace(String pattern, String replacement) {
    // TODO Auto-generated method stub

  }

  @SuppressWarnings("unchecked")
  @Override
  public JSONObject toJSONObject() {
    JSONObject jObject = new JSONObject();
    jObject.put("type", this.getTypeString());
    jObject.put("id", this.getId());
    JSONArray jArray = new JSONArray();

    for (String id : this.children) {
      Segment segment = this.getChild(id);
      jArray.add(segment.toJSONObject());
    }

    jObject.put("traceSegments", jArray);

    return jObject;
  }

  public int getLength() {
    return this.toString().length();
  }


  public void removeLastBreakLine() {
    String id = this.children.get(this.children.size() - 1);
    Segment segm = this.map.get(id);
    if (segm instanceof CompositeSegment) {
      ((CompositeSegment) segm).removeLastBreakLine();
    } else if (segm instanceof ContentSegment) {
      ((ContentSegment) segm).setContent(segm.toString().trim());
    }

  }

  @Override
  public String getTypeString() {
    return "composite";
  }

  public void addAllSegments(List<Segment> parseTraces) {
    for (Segment segment : parseTraces) {
      this.add(segment);
    }

  }

  public Segment getChildRecursive(String id) {
    for (String segmentId : this.children) {
      if (segmentId.equals(id)) {
        return this.getChild(segmentId);
      } else {
        Segment segment = this.getChild(segmentId);
        if (segment instanceof CompositeSegment) {
          CompositeSegment cS = (CompositeSegment) segment;
          Segment recursiveChild = cS.getChildRecursive(id);
          if (recursiveChild != null) {
            return recursiveChild;
          }
        }
      }
    }
    return null;
  }

  public void remove(String segmentId) {
    this.map.remove(segmentId);
  }

}
