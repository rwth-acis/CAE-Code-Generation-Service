package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.List;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ContentSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import i5.las2peer.services.codeGenerationService.traces.segments.SynchronizeCompositeSegment;

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
      ((CompositeSegment) segment).setSegmentContent(id, content, true);
    }
    // otherwise we can directly set the new content
    else if (segment instanceof ContentSegment) {
      ((ContentSegment) segment).setContent(content, true);
    }

  }

  @Override
  public Segment addSegment(String id, Segment segment) {
    // check if a segment with the given id already exists in the old file trace model
    Segment result = this.oldFileTraceModel.getRecursiveSegment(id);

    // if the segment exists and is a composition, we need to synchronize children that are also
    // compositions
    if (result instanceof CompositeSegment) {
      this.synchronizeChildren((CompositeSegment) result);
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

    // if the segment exists and is a composition, we need to synchronize children that are also
    // compositions
    if (segment instanceof CompositeSegment) {
      this.synchronizeChildren((CompositeSegment) segment);
    }

    return segment;
  }

  /**
   * Synchronizes the children of a compositions of segments
   * 
   * @param cSegment The composition whose children should be synchronized
   */

  private void synchronizeChildren(CompositeSegment cSegment) {
    List<String> childrenList = cSegment.getChildrenList();
    for (String childId : childrenList) {
      Segment child = cSegment.getChild(childId);
      // inner compositions of segments are used as placeholder to add templates
      // so we use a special kind of compositions that still know the segments of the old
      // file trace model but only use them during the content generation if they were actually
      // used
      if (!(child instanceof SynchronizeCompositeSegment) && child instanceof CompositeSegment) {
        SynchronizeCompositeSegment pCSegment =
            new SynchronizeCompositeSegment(child.getId(), (CompositeSegment) child);
        cSegment.replaceSegment(child, pCSegment);
      }
    }
  }

}
