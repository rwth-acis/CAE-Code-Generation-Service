package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;


/**
 * 
 * InputParameter data class. Represents an input parameter, which is either part of a function or a
 * microservice-call.
 *
 */
public class InputParameter {


  private String modelId;
  private String name;


  /**
   *
   * Event constructor. Takes a {@link SimpleNode} and parses it to an InputParameter.
   * 
   * @param node the node representing the InputParameter
   * 
   * @throws ModelParseException if some error comes up during parsing the node
   * 
   */
  public InputParameter(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "name":
          this.name = attribute.getValue();
          break;
        default:
          throw new ModelParseException("Unknown InputParameter attribute: " + attribute.getName());
      }
    }
  }


  public String getModelId() {
    return modelId;
  }


  public String getName() {
    return name;
  }

}
