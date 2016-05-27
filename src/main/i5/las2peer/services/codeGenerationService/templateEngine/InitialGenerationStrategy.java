package i5.las2peer.services.codeGenerationService.templateEngine;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

public class InitialGenerationStrategy extends TemplateStrategy {

  public final FileTraceModel traceModel;

  public InitialGenerationStrategy(FileTraceModel tracemodel) {
    this.traceModel = tracemodel;
  }

  @Override
  public void setSegmentContent(CompositeSegment segment, String content, String id) {
    this.setSegmentContent(segment, content, id, false);
  }


  @Override
  public Segment getSegment(String segmentId) {
    Segment result = this.traceModel.getRecursiveSegment(segmentId);
    return result;
  }

}
