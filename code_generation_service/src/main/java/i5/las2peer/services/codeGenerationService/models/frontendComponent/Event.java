package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;


/**
 * 
 * Event data class. Represents an event, which is part of a frontend component model.
 *
 */
public class Event {


  /**
   * 
   * Represents the different event causes of an {@link Event}.
   * 
   */
  public enum EventCause {
    click
  }

  private String modelId;
  private EventCause eventCause;
  private String calledFunctionId;

  /**
   *
   * Event constructor. Takes a {@link SimpleNode} and parses it to an Event.
   * 
   * @param node the node representing the Event
   * 
   * @throws ModelParseException if some error comes up during parsing the node
   * 
   */
  public Event(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "eventCause":
          switch (attribute.getValue()) {
            case "click":
              this.eventCause = EventCause.click;
              break;
            default:
              throw new ModelParseException("Unknown Event cause: " + attribute.getValue());
          }
          break;
          case "name":
            //do nothing right now, just for display reasons
      	  break;
        default:
          throw new ModelParseException("Unknown Event attribute: " + attribute.getName());
      }
    }
  }


  public String getModelId() {
    return modelId;
  }


  public EventCause getEventCause() {
    return eventCause;
  }


  public String getCalledFunctionId() {
    return calledFunctionId;
  }


  /**
   * 
   * Sets the called function id.
   * 
   * @param functionId the SyncMeta id
   * 
   * @throws ModelParseException if an id was already set previously.
   * 
   */
  public void setCalledFunctionId(String functionId) throws ModelParseException {
    if (calledFunctionId != null) {
      throw new ModelParseException("Event " + this.modelId + " already has a called function!");
    }
    this.calledFunctionId = functionId;
  }

}
