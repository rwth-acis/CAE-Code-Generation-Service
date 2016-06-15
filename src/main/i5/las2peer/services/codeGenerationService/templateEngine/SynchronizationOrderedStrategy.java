package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.List;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.AppendableVariableSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import i5.las2peer.services.codeGenerationService.traces.segments.SynchronizeAppendableVariableSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.SynchronizeOrderedAppendableVariableSegment;

public class SynchronizationOrderedStrategy extends SynchronizationStrategy {

  public SynchronizationOrderedStrategy(FileTraceModel oldFileTraceModel) {
    super(oldFileTraceModel);
  }

  /**
   * Synchronizes the children of a composition of segments and also keeps their original position
   * 
   * @param cSegment The composition whose children should be synchronized
   */
  @Override
  protected void synchronizeChildren(CompositeSegment cSegment) {
    List<String> childrenList = cSegment.getChildrenList();
    for (String childId : childrenList) {
      Segment child = cSegment.getChild(childId);
      // we need to also synchronize all children that are appendable segments of a variable
      // so we use a special kind of appendable variable segment that still know the segments of
      // the old file trace model but only add them to the new file trace model during the content
      // regeneration if they were actually used
      if (!(child instanceof SynchronizeAppendableVariableSegment)
          && child instanceof AppendableVariableSegment) {
        SynchronizeAppendableVariableSegment pCSegment =
            new SynchronizeOrderedAppendableVariableSegment(child.getId(),
                (AppendableVariableSegment) child);
        cSegment.replaceSegment(child, pCSegment);
      }
    }
  }

}
