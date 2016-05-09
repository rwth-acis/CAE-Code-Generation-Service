package i5.las2peer.services.codeGenerationService.traces.segments;

public class ProxyCompositeSegment extends CompositeSegment {

  final private CompositeSegment compositeSegment;

  public ProxyCompositeSegment(String id, CompositeSegment segment) {
    super(id);
    this.compositeSegment = segment;
  }

  public Segment getChildRecursive(String id) {
    return this.compositeSegment.getChildRecursive(id);
  }

}
