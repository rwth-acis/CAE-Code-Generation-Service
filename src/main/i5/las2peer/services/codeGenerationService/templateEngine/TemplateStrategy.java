package i5.las2peer.services.codeGenerationService.templateEngine;

import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

public abstract class TemplateStrategy {
  public abstract void setSegmentContent(Segment segment, String content, String id);

  public abstract Segment addSegment(String id, Segment segment);

  public abstract Segment getSegment(String id);

}
