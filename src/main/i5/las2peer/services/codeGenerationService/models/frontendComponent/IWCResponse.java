package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;

/**
 * 
 * Inter-Widget-Communication Response data class. Represents an ICWResponse, which is part of a
 * frontend component model.
 *
 */
public class IWCResponse {

  private String modelId;
  private String intentAction;
  private String content;

  /**
   *
   * Event constructor. Takes a {@link SimpleNode} and parses it to a IWCResponse.
   * 
   * @param node the node representing the IWCResponse
   * 
   * @throws ModelParseException if some error comes up during parsing the node
   * 
   */
  public IWCResponse(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "intentAction":
          this.intentAction = attribute.getValue();
          break;
        case "content":
          this.content = attribute.getValue();
          break;
        default:
          throw new ModelParseException("Unknown IWCResponse attribute: " + attribute.getName());
      }
    }
  }


  public String getModelId() {
    return this.modelId;
  }


  public String getIntentAction() {
    return this.intentAction;
  }


  public String getContent() {
    return this.content;
  }

}
