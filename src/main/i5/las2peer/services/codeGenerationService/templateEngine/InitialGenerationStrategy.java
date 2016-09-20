package i5.las2peer.services.codeGenerationService.templateEngine;

import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

public class InitialGenerationStrategy extends TemplateStrategy {


  /**
   * {@inheritDoc}
   */

  @Override
  public void setSegmentContent(CompositeSegment segment, String content, String id) {
    this.setSegmentContent(segment, content, id, false);
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public Segment getSegment(String segmentId) {
    // we always return null in this strategy
    return null;
  }

}
