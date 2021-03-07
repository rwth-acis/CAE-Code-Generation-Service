package i5.las2peer.services.codeGenerationService.traces.segments;

import org.json.simple.JSONObject;

public class SynchronizeAppendableVariableSegment extends AppendableVariableSegment {

  protected final CompositeSegment compositeSegment;

  public SynchronizeAppendableVariableSegment(String id, AppendableVariableSegment segment) {
    super(id);
    this.compositeSegment = segment;
  }

  @Override
  public Segment getChildRecursive(String id) {
    // delegate to the old segments in order to reuse them
    return this.compositeSegment.getChildRecursive(id);
  }



  @Override
  public JSONObject toJSONObject() {
    return super.toJSONObject();
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
