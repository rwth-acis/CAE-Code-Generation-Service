package i5.las2peer.services.codeGenerationService.traces.segments;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A factory class providing utilities to create new content segments and compositions of segments.
 * 
 * @author Thomas Winkler
 *
 */

public class SegmentFactory {

  /**
   * A helper method that takes care about the fact, that depending on the used json implementation
   * sometimes numbers are saved as strings or as real numbers, i.e. decorated with or without
   * double quotes (e.g. {"number":"1"} or {"number" : 1} )
   * 
   * @param obj The object containing the property that should be cast to long
   * @param key The key of the property
   * @return The cast long value
   */

  public static Long getLong(JSONObject obj, String key) {
    Object length = obj.get(key);
    // if the entry is already an instance of the Long class, we can directly cast it
    if (length instanceof Long) {
      return ((Long) length);
    }
    // otherwise we need to parse it as a long value
    else {
      return Long.parseLong(length.toString() + "");
    }
  }

  /**
   * Create a list of segments from a given json array and extract the content of each segment from
   * a given source code
   * 
   * @param jSegments An array of json objects representing the segments
   * @param source The source code of the segments
   * @param start The relative start position of the segements within the source code
   * @return A list of extracted and created Segments
   */

  public static List<Segment> createSegments(JSONArray jSegments, String source, Long start) {
    List<Segment> list = new ArrayList<Segment>();

    for (int i = 0; i < jSegments.size(); i++) {
      JSONObject entry = (JSONObject) jSegments.get(i);
      Segment segment = createSegment(entry, source, start);
      list.add(segment);
      start += segment.getLength();
    }

    return list;
  }

  /**
   * Create a single segment from a json object and extract its content from a given source code.
   * 
   * @param entry A json object of the segment
   * @param source The source code from which the content should be extracted
   * @param start The relative start position of the segment within the source code
   * @return The created segment containing the extracted content
   */


  private static Segment createSegment(JSONObject entry, String source, Long start) {
    Segment segment = null;

    String type = (String) entry.get("type");
    switch (type) {
      case CompositeSegment.TYPE:
      case AppendableVariableSegment.TYPE:
        segment = createCompositeSegment(entry, source, start);
        break;
      case UnprotectedSegment.TYPE:
      case ProtectedSegment.TYPE:
        Long length = getLong(entry, "length");
        String segmentContent =
            source.substring(Math.toIntExact(start), Math.toIntExact(start + length));
        segment = createContentSegment(entry, segmentContent);
    }

    return segment;
  }

  /**
   * Create a composition of segments from a json object. It extracts the content of its children
   * from the given source code and creates the corresponding content segments
   * 
   * @param entry The json object of the composition
   * @param source The source code of the composition
   * @param start The relative start position of the composition within the source code
   * @return The created composition containing its children
   */

  private static CompositeSegment createCompositeSegment(JSONObject entry, String source,
      Long start) {

    String segmentId = (String) entry.get("id");
    String type = (String) entry.get("type");

    CompositeSegment segment;
    if (type.equals(AppendableVariableSegment.TYPE)) {
      segment = new AppendableVariableSegment(segmentId);
    } else {
      segment = new CompositeSegment(segmentId);
    }

    JSONArray subSegments = (JSONArray) entry.get("traceSegments");
    if (subSegments != null) {
      segment.addAllSegments(createSegments(subSegments, source, start));
    }
    return segment;
  }

  /**
   * Create a new instance of a content segment from a json object. This can be either a protected
   * or an unprotected segment. An integrity check for unprotected segment will be enabled if the
   * segment should be protected.
   * 
   * @param entry A json object of the segement
   * @param content The content of the new segment
   * @return A new created content segment
   */

  public static Segment createContentSegment(JSONObject entry, String content) {
    ContentSegment segment = null;
    String type = (String) entry.get("type");

    if (type.equals(ProtectedSegment.TYPE)) {
      segment = new ProtectedSegment(entry);
      segment.setContent(content);
    } else if (type.equals(UnprotectedSegment.TYPE)) {
      segment = new UnprotectedSegment(entry);
      segment.setContent(content);
    }
    return segment;
  }

}
