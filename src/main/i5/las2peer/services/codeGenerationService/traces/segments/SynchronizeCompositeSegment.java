package i5.las2peer.services.codeGenerationService.traces.segments;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

public class SynchronizeCompositeSegment extends CompositeSegment {

  final private CompositeSegment compositeSegment;

  public SynchronizeCompositeSegment(String id, CompositeSegment segment) {
    super(id);
    this.compositeSegment = segment;
  }

  @Override
  public Segment getChildRecursive(String id) {
    return this.compositeSegment.getChildRecursive(id);
  }


  private List<String> getReorderedChildrenList() {
    List<String> alreadyAdded = new ArrayList<String>();
    List<String> reordered = new ArrayList<String>();
    System.out.println("old Segments");
    System.out.println(this.compositeSegment.getChildrenList());
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
    System.out.println("new Segments");
    System.out.println(alreadyAdded.toString());
    System.out.println(reordered.toString());
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
