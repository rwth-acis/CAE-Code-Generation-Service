package i5.las2peer.services.codeGenerationService.traces.segments;

public class ProtectedSegment extends ContentSegment {

  public ProtectedSegment(String id) {
    super(id);
  }

  private String content;

  @Override
  public int getLength() {
    return this.content.length();
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
  public void replace(String pattern, String replacement) {
    this.content = this.content.replace(pattern, replacement);
  }

  @Override
  public String getTypeString() {
    return "protected";
  }


}
