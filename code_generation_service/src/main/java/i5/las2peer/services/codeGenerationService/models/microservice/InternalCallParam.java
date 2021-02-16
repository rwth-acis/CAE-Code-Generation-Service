package i5.las2peer.services.codeGenerationService.models.microservice;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;

/**
 * 
 * InternalCallParam data class. Represents an internal service call parameter, which is part of a
 * microservice model.
 *
 */
public class InternalCallParam {

  private String modelId;
  private String name;


  /**
   * 
   * Creates a new {@link InternalCallParam}.
   * 
   * @param node a {@link i5.cae.simpleModel.node.SimpleNode} containing the call
   * 
   * @throws ModelParseException if something goes wrong during the parsing of the InternalCallParam
   *         information
   * 
   */
  public InternalCallParam(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "name":
          this.name = attribute.getValue();
          break;
        default:
          throw new ModelParseException(
              "Unknown Internal Call Parameter attribute: " + attribute.getName());
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
