package i5.las2peer.services.codeGenerationService.templateEngine;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ContentSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

public class InitialGenerationStrategy extends TemplateStrategy {

  public final FileTraceModel traceModel;

  public InitialGenerationStrategy(FileTraceModel tracemodel) {
    this.traceModel = tracemodel;
  }

  @Override
  public void setSegmentContent(Segment segment, String content, String id) {
    // if the segment is a composition, we need to set the content recursively by the
    // setSegmentContent function
    if (segment instanceof CompositeSegment) {
      ((CompositeSegment) segment).setSegmentContent(id, content, false);
    }
    // otherwise we can directly set the new content
    else if (segment instanceof ContentSegment) {
      ((ContentSegment) segment).setContent(content, false);
    }
  }

  @Override
  public Segment addSegment(String id, Segment segment) {
    // check if a segment with the given id already exists
    Segment result = this.traceModel.getRecursiveSegment(id);
    // if so, return it
    if (result != null) {

      if (result.getClass() == segment.getClass()) {
        return result;
      } else {

        return segment;
      }

    }
    // otherwise add it to the trace model
    else {
      this.traceModel.addSegment(segment);
    }
    return segment;
  }

  @Override
  public Segment getSegment(String segmentId) {
    return this.traceModel.getRecursiveSegment(segmentId);
  }

}
