package i5.las2peer.services.codeGenerationService.traces.segments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A class representing a composition of segments. Such a composition is used for model elements
 * that are made up of a sequence of segments. In addition, compositions are also used as containers
 * for {@link i5.las2peer.services.codeGenerationService.templateEngine.Template} and variables that
 * should hold multiple templates, e.g. the $Main_Content$ variable holding the templates of the
 * html elements
 * 
 * 
 * @author Thomas Winkler
 *
 */

public class CompositeSegment extends Segment {

  public static final String TYPE = "composite";
  final private List<String> children;
  final private Map<String, Segment> map;

  /**
   * Creates a new and therefore empty composition of segments with the given id
   * 
   * @param segmentId The id of the composition
   */

  public CompositeSegment(String segmentId) {
    super(segmentId);
    children = new ArrayList<String>();
    map = new HashMap<String, Segment>();
  }

  /**
   * Creates a composition of segments with the id and the children of the given segment
   * 
   * @param segment The segment whose id and children should be used
   */

  protected CompositeSegment(CompositeSegment segment) {
    super(segment.getId());
    children = segment.getChildrenList();
    map = segment.getMap();
  }

  /**
   * Adds a segment to the composition
   * 
   * @param segment The segment that should be added
   */

  public void addSegment(final Segment segment) {
    String id = segment.getId();
    if (!map.containsKey(id)) {
      map.put(id, segment);
    }
    children.add(id);
  }

  /**
   * Adds a collection to segments to this composition
   * 
   * @param segments A collection of segments to add
   */

  public void addAllSegments(Collection<Segment> segments) {
    for (Segment segment : segments) {
      this.addSegment(segment);
    }

  }

  /**
   * Get the map containing all children segments
   * 
   * @return The map containing all children segments
   */

  protected Map<String, Segment> getMap() {
    return this.map;
  }

  /**
   * Get the list of ids of the children
   * 
   * @return The list of ids of the children
   */

  public List<String> getChildrenList() {
    return this.children;
  }

  /**
   * Get the segment with the give id from the children of this composition. A recursive lookup is
   * not performed.
   * 
   * @param segmentId The id of the needed segment
   * @return The segment or null if not found in the composition
   */

  public Segment getChild(String segmentId) {
    return this.map.get(segmentId);
  }

  /**
   * Get the segment with the given id from the children of the composition. In addition to
   * getChild, it performs a recursive lookup in its children.
   * 
   * @param segmentId The id of the needed segment
   * @return The segment or null if not found in the composition or in its children
   */

  public Segment getChildRecursive(String segmentId) {
    for (String childSegmentId : this.children) {
      if (childSegmentId.equals(segmentId)) {
        return this.getChild(childSegmentId);
      } else {
        Segment segment = this.getChild(childSegmentId);
        // recursively look up for the segment if the child is also a composition
        if (segment instanceof CompositeSegment) {
          CompositeSegment cS = (CompositeSegment) segment;
          Segment recursiveChild = cS.getChildRecursive(segmentId);
          if (recursiveChild != null) {
            return recursiveChild;
          }
        }
      }
    }
    // if the segment was not found we will return null
    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public int getLength() {
    return this.toString().length();
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public String getTypeString() {
    return CompositeSegment.TYPE;
  }

  /**
   * Check if the composition contains a segment of a specific id
   * 
   * @param id The id of the segment
   * @return True, if the composition already contains a segment with the given id
   */

  public boolean hasChild(String id) {
    return this.map.containsKey(id);
  }

  /**
   * Replaces a segment with an other segment
   * 
   * @param oldSegment The old segment to be replaced
   * @param segment The new segment that replaces the old segment
   */

  public void replaceSegment(Segment oldSegment, Segment segment) {
    map.put(oldSegment.getId(), segment);
  }

  public void setVariableSegment(String variableName, CompositeSegment segment) {
    String id = this.getId() + ":" + variableName;
    // only update a segment of a variable name that exists
    if (this.hasChild(id)) {
      map.put(id, segment);
    }
  }

  public void setSegmentContent(String id, String content, boolean integrityCheck) {
    // if this composite segment holds a segment with the given id, set its content
    if (map.containsKey(this.getId() + ":" + id)) {
      Segment segment = map.get(this.getId() + ":" + id);
      if (segment instanceof ContentSegment) {
        ((ContentSegment) segment).setContent(content, integrityCheck);
      }
    }

    // now propagate the content recursively to the segments that are also compositions
    for (String cid : this.children) {
      Segment segment = this.map.get(cid);
      if (segment instanceof CompositeSegment) {
        ((CompositeSegment) segment).setSegmentContent(id, content, integrityCheck);
      }
    }
  }

  /**
   * Get the string content for a (sub) list of the composition's children
   * 
   * @param childrenList A (sub) list of the composition's children
   * @return The composed content of the (sub) list of children
   */

  protected String toString(List<String> childrenList) {
    String content = "";
    for (String id : childrenList) {
      if (this.map.containsKey(id)) {
        content += this.map.get(id).toString();
      }
    }


    return content;
  }

  /**
   * Get the string content of the composition
   * 
   * @return The string content of the composition, i.e. the composed content of its children
   */

  public String toString() {
    return this.toString(this.getChildrenList());
  }

  @SuppressWarnings("unchecked")
  protected JSONObject toJSONObject(List<String> childrenList) {
    JSONObject jObject = new JSONObject();
    jObject.put("type", this.getTypeString());
    jObject.put("id", this.getId());
    JSONArray jArray = new JSONArray();

    for (String id : childrenList) {
      Segment segment = this.getChild(id);
      jArray.add(segment.toJSONObject());
    }

    jObject.put("traceSegments", jArray);

    return jObject;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public JSONObject toJSONObject() {
    return this.toJSONObject(this.getChildrenList());
  }
}
