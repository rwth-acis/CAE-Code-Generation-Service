package i5.las2peer.services.codeGenerationService.traces.segments;

import org.json.simple.JSONObject;

public abstract class ContentSegment extends Segment {

  public ContentSegment(String id) {
    super(id);
  }

  public abstract int getLength();

  public abstract void setContent(String content);

  public abstract void setContent(String content, boolean integrityCheck);

  public abstract String getContent();

  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    JSONObject jObject = new JSONObject();
    jObject.put("id", this.getId());
    jObject.put("type", this.getTypeString());
    jObject.put("length", this.getLength());
    return jObject;
  }

  public String toString() {
    return this.getContent();
  }

}
