package i5.las2peer.services.codeGenerationService.templateEngine;

import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

public abstract class TemplateStrategy {
  public abstract void setSegmentContent(CompositeSegment segment, String content, String id);

  public abstract Segment getSegment(String id);

  protected void setSegmentContent(CompositeSegment segment, String content, String id,
      boolean integrityCheck) {
    segment.setSegmentContent(id, content, integrityCheck);
  }

}
