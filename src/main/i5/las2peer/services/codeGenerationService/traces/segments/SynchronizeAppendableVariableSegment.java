package i5.las2peer.services.codeGenerationService.traces.segments;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

public class SynchronizeAppendableVariableSegment extends AppendableVariableSegment {

  final private CompositeSegment compositeSegment;

  public SynchronizeAppendableVariableSegment(String id, AppendableVariableSegment segment) {
    super(id);
    this.compositeSegment = segment;
  }

  @Override
  public Segment getChildRecursive(String id) {
    // delegate to the old segments
    return this.compositeSegment.getChildRecursive(id);
  }


  private List<String> getReorderedChildrenList() {
    List<String> alreadyAdded = new ArrayList<String>();
    List<String> reordered = new ArrayList<String>();
    for (String id : this.compositeSegment.getChildrenList()) {
      Segment segment = this.getChild(id);

      if (segment != null && !alreadyAdded.contains(id)) {
        reordered.add(id);
        alreadyAdded.add(id);
      }
    }

    for (String id : this.getChildrenList()) {
      if (!alreadyAdded.contains(id)) {
        reordered.add(id);
        alreadyAdded.add(id);
      }
    }
    return reordered;
  }

  @Override
  public JSONObject toJSONObject() {
    return this.toJSONObject(this.getReorderedChildrenList());
  }

  @Override
  public String toString() {
    return this.toString(this.getReorderedChildrenList());
  }

}
