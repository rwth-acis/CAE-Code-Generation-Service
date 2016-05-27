package i5.las2peer.services.codeGenerationService.traces.segments;

public class AppendableVariableSegment extends CompositeSegment {

  public static final String TYPE = "appendableVariable";

  public AppendableVariableSegment(String segmentId) {
    super(segmentId);
  }

  @Override
  public String getTypeString() {
    return AppendableVariableSegment.TYPE;
  }

}
