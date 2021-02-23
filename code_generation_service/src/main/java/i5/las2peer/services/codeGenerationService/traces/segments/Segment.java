package i5.las2peer.services.codeGenerationService.traces.segments;

import org.json.simple.JSONObject;

/**
 * An abstract class describing the needed methods of all segments
 * 
 * @author Thomas Winkler
 *
 */

public abstract class Segment {
  private String id;

  /**
   * Create a new segment with the given id
   * 
   * @param id The id of the segment
   */

  public Segment(String id) {
    this.id = id;
  }

  /**
   * Return the id of the segment
   * 
   * @return The id of the segment
   */

  public String getId() {
    return this.id;
  }

  /**
   * Set the id of the segment
   * 
   * @param id The new id of the segment
   */

  protected void setId(String id) {
    this.id = id;
  }

  /**
   * Return the type of the segment as a string
   * 
   * @return The type of the segment as a string
   */

  public abstract String getTypeString();

  /**
   * Create the json object of the segment
   * 
   * @return The json object of the segment
   */

  public abstract Object toJSONObject();

  /**
   * Return the length of the content of the segment
   * 
   * @return The length of the segment
   */

  public abstract int getLength();

  /**
   * Create a new json object of a segment with the given length, id and type.
   * 
   * @param length The length of the segment
   * @param id The id of the segment
   * @param type The type of the segment
   * @return A json object of a segment with the given properties.
   */

  @SuppressWarnings("unchecked")
  public static JSONObject createJSONSegment(int length, String id, String type) {
    JSONObject obj = new JSONObject();
    obj.put("id", id);
    obj.put("length", Long.valueOf(length));
    obj.put("type", type);
    if (type.equals("unprotectedIntegrity")) {
      obj.put("integrityCheck", true);
      obj.put("type", "unprotected");
    } else if (type.equals("unprotected")) {
      obj.put("integrityCheck", false);
    }
    return obj;
  }

}
