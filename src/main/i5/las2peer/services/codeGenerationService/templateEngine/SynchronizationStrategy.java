package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.ArrayList;
import java.util.List;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.AppendableVariableSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import i5.las2peer.services.codeGenerationService.traces.segments.SynchronizeAppendableVariableSegment;

public class SynchronizationStrategy extends TemplateStrategy {
  private final FileTraceModel oldFileTraceModel;
  private final List<FileTraceModel> additionalOldFileTraceModel = new ArrayList<FileTraceModel>();

  public SynchronizationStrategy(FileTraceModel oldFileTraceModel) {
    this.oldFileTraceModel = oldFileTraceModel;
  }

  /**
   * Add an aditional old file trace model to the synchronization process
   * 
   * @param fileTraceModel A file trace model to add
   */

  public void addAditionalOldFileTraceModel(FileTraceModel fileTraceModel) {
    this.additionalOldFileTraceModel.add(fileTraceModel);
  }

  /**
   * Get the segment of the given id within the given
   * {@link i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel}
   * 
   * @param segmentId
   * @param oldFileTraceModel
   * @return The requested segment or null if a segment with the given id was not found
   */

  private Segment getSegment(String segmentId, FileTraceModel oldFileTraceModel) {
    return oldFileTraceModel.getRecursiveSegment(segmentId);
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public void setSegmentContent(CompositeSegment segment, String content, String id) {
    this.setSegmentContent(segment, content, id, true);
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public Segment getSegment(String segmentId) {
    Segment result = this.getSegment(segmentId, this.oldFileTraceModel);
    if (result == null) {
      // if the segment was not found in our file trace model, we try to find it in other registered
      // file trace models, the first found segment will be used
      for (FileTraceModel oldFileTraceModel : this.additionalOldFileTraceModel) {
        result = this.getSegment(segmentId, oldFileTraceModel);
        if (result != null) {
          break;
        }
      }
    }

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

  protected void synchronizeChildren(CompositeSegment cSegment) {
    List<String> childrenList = cSegment.getChildrenList();
    for (String childId : childrenList) {
      Segment child = cSegment.getChild(childId);
      // we need to also synchronize all children that are appendable segments of a variable
      // so we use a special kind of appendable variable segments that still know the segments of
      // the old file trace model but only add them to the new file trace model during the content
      // regeneration if they were actually used
      if (!(child instanceof SynchronizeAppendableVariableSegment)
          && child instanceof AppendableVariableSegment) {
        SynchronizeAppendableVariableSegment pCSegment = new SynchronizeAppendableVariableSegment(
            child.getId(), (AppendableVariableSegment) child);
        cSegment.replaceSegment(child, pCSegment);
      }
    }
  }

}
