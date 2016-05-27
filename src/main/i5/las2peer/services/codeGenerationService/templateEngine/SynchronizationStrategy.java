package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.List;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.AppendableVariableSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import i5.las2peer.services.codeGenerationService.traces.segments.SynchronizeAppendableVariableSegment;

public class SynchronizationStrategy extends TemplateStrategy {
  private final FileTraceModel oldFileTraceModel;

  public SynchronizationStrategy(FileTraceModel oldFileTraceModel) {
    this.oldFileTraceModel = oldFileTraceModel;
  }

  @Override
  public void setSegmentContent(CompositeSegment segment, String content, String id) {
    this.setSegmentContent(segment, content, id, true);
  }

  @Override
  public Segment getSegment(String segmentId) {
    Segment result = this.oldFileTraceModel.getRecursiveSegment(segmentId);

    // if the segment exists and is a composition, we need to synchronize its children that are
    // appendable variable segments
    if (result instanceof CompositeSegment) {
      this.synchronizeChildren((CompositeSegment) result);
    }

    return result;

  }

  /**
   * Synchronizes the children of a composition of segments
   * 
   * @param cSegment The composition whose children should be synchronized
   */

  private void synchronizeChildren(CompositeSegment cSegment) {
    List<String> childrenList = cSegment.getChildrenList();
    for (String childId : childrenList) {
      Segment child = cSegment.getChild(childId);
      // we need to also synchronize all children segments that are appendable segments for a
      // variable
      // so we use a special kind of appendable variable segments that still know the segments of
      // the old
      // file trace model but only use them during the content generation if they were actually
      // used
      if (!(child instanceof SynchronizeAppendableVariableSegment)
          && child instanceof AppendableVariableSegment) {
        SynchronizeAppendableVariableSegment pCSegment = new SynchronizeAppendableVariableSegment(
            child.getId(), (AppendableVariableSegment) child);
        cSegment.replaceSegment(child, pCSegment);
      }
    }
  }

}
