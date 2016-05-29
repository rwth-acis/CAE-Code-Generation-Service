package i5.las2peer.services.codeGenerationService.templateEngine;

import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

public abstract class TemplateStrategy {

  /**
   * Get a segment by its id
   * 
   * @param id The segment id
   * @return The segment
   */

  public abstract Segment getSegment(String id);

  /**
   * Set the content of a content segment
   * 
   * @param segment The composition in which the content segment is located
   * @param content The content string to set
   * @param id The id of the content segment
   */

  public abstract void setSegmentContent(CompositeSegment segment, String content, String id);

  /**
   * Set the content of a content segment
   * 
   * @param segment The composition in which the content segment is located
   * @param content The content string to set
   * @param id The id of the content segment
   * @param integrityCheck If true, an integrity check for unprotected blocks is used
   */

  protected void setSegmentContent(CompositeSegment segment, String content, String id,
      boolean integrityCheck) {
    segment.setSegmentContent(id, content, integrityCheck);
  }

}
