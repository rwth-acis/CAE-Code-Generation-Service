package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;


/**
 * 
 * Function data class. Represents a function, which is part of a frontend component model.
 *
 */
public class Function {


  private String modelId;
  private String name;
  private String returnParameter;


  /**
   *
   * Event constructor. Takes a {@link SimpleNode} and parses it to a Function.
   * 
   * @param node the node representing the Function
   * 
   * @throws ModelParseException if some error comes up during parsing the node
   * 
   */
  public Function(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "name":
          this.name = attribute.getValue();
          break;
        case "returnParameter":
          this.returnParameter = attribute.getValue();
          break;
        default:
          throw new ModelParseException("Unknown Function attribute: " + attribute.getName());
      }
    }
  }


  public String getName() {
    return name;
  }


  public String getReturnParameter() {
    return returnParameter;
  }


  public String getModelId() {
    return modelId;
  }

}
