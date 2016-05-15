package i5.las2peer.services.codeGenerationService.traces.segments;

import org.json.simple.JSONObject;

public abstract class Segment {
  private String id;

  public Segment(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  public abstract void replace(String pattern, String replacement);

  public abstract String getTypeString();

  public abstract Object toJSONObject();

  @SuppressWarnings("unchecked")
  public static JSONObject createJSONSegment(int length, String id, String type) {
    JSONObject obj = new JSONObject();
    obj.put("id", id);
    obj.put("length", length);
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
