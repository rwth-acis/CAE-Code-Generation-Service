package i5.las2peer.services.codeGenerationService.traces.segments;

/**
 * A special {@link i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment}
 * used for variable names holding multiple templates
 * 
 * @author Thomas Winkler
 *
 */

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
