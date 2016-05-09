package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.List;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ContentSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ProxyCompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

public class SynchronizationStrategy extends TemplateStrategy {
  private final FileTraceModel fileTraceModel;
  private final FileTraceModel oldFileTraceModel;

  public SynchronizationStrategy(FileTraceModel oldFileTraceModel, FileTraceModel fileTraceModel) {
    this.fileTraceModel = fileTraceModel;
    this.oldFileTraceModel = oldFileTraceModel;
  }

  @Override
  public void setSegmentContent(Segment segment, String content, String id) {
    // if the segment is a composition, we need to set the content recursively by the
    // setSegmentContent function
    if (segment instanceof CompositeSegment) {
      ((CompositeSegment) segment).setSegmentContent(id, content);
    }
    // otherwise we can directly set the new content
    else if (segment instanceof ContentSegment) {
      ((ContentSegment) segment).setContent(content);
    }

  }

  @Override
  public Segment addSegment(String id, Segment segment) {
    // check if a segment with the given id already exists
    Segment result = this.oldFileTraceModel.getRecursiveSegment(id);

    if (result instanceof CompositeSegment) {

      CompositeSegment cSegment = (CompositeSegment) result;

      List<String> childrenList = cSegment.getChildrenList();
      for (String childId : childrenList) {
        Segment child = cSegment.getChild(childId);
        // inner composite segments are used to add templates that corresponds to mode element

        if (!(child instanceof ProxyCompositeSegment) && child instanceof CompositeSegment) {
          ProxyCompositeSegment pCSegment =
              new ProxyCompositeSegment(child.getId(), (CompositeSegment) child);
          cSegment.replaceSegment(child, pCSegment);
        }
      }
    }

    // if so, return it
    if (result != null) {
      return result;
    }
    // otherwise add it to the trace model
    else {
      this.fileTraceModel.addSegment(segment);
    }
    return segment;
  }

  @Override
  public Segment getSegment(String segmentId) {
    Segment segment = this.oldFileTraceModel.getRecursiveSegment(segmentId);

    if (segment instanceof CompositeSegment) {

      CompositeSegment cSegment = (CompositeSegment) segment;

      List<String> childrenList = cSegment.getChildrenList();
      for (String childId : childrenList) {
        Segment child = cSegment.getChild(childId);
        // inner composite segments are used to add templates that corresponds to mode element

        if (!(child instanceof ProxyCompositeSegment) && child instanceof CompositeSegment) {
          ProxyCompositeSegment pCSegment =
              new ProxyCompositeSegment(child.getId(), (CompositeSegment) child);
          cSegment.replaceSegment(child, pCSegment);
        }
      }
    }

    return segment;
  }

}
