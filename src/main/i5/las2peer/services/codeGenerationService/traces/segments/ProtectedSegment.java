package i5.las2peer.services.codeGenerationService.traces.segments;

import org.json.simple.JSONObject;

public class ProtectedSegment extends ContentSegment {

  public static final String TYPE = "protected";

  public ProtectedSegment(String id) {
    super(id);
  }

  public ProtectedSegment(JSONObject entry) {
    this((String) entry.get("id"));
  }

  private String content;

  @Override
  public int getLength() {
    return this.getContent().length();
  }

  @Override
  public void setContent(String content, boolean integrityCheck) {
    this.content = content;
  }

  @Override
  public void setContent(String content) {
    this.setContent(content, false);
  }

  @Override
  public String getContent() {
    return this.content;
  }

  @Override
  public String getTypeString() {
    return ProtectedSegment.TYPE;
  }


}
