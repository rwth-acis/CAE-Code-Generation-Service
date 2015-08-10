package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;
import i5.las2peer.services.codeGenerationService.models.microservice.HttpMethod.MethodType;

/**
 * 
 * Microservice Call data class. Represents a microservice call, which is part of a frontend
 * component model.
 *
 */
public class MicroserviceCall {


  private String modelId;
  private String path;
  private boolean authorize;
  private String content;
  private MethodType methodType;

  /**
   *
   * Event constructor. Takes a {@link SimpleNode} and parses it to a MicroserviceCall.
   * 
   * @param node the node representing the MicroserviceCall
   * 
   * @throws ModelParseException if some error comes up during parsing the node
   * 
   */
  public MicroserviceCall(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "path":
          this.path = attribute.getValue();
          break;
        case "authorize":
          this.authorize = Boolean.parseBoolean(attribute.getValue());
          break;
        case "content":
          this.content = attribute.getValue();
          break;
        case "methodType":
          switch (attribute.getValue()) {
            case "GET":
              this.methodType = MethodType.GET;
              break;
            case "POST":
              this.methodType = MethodType.POST;
              break;
            case "PUT":
              this.methodType = MethodType.PUT;
              break;
            case "DELETE":
              this.methodType = MethodType.DELETE;
              break;
            default:
              throw new ModelParseException(
                  "Unknown MicroserviceCall methodType: " + attribute.getValue());
          }
          break;
        default:
          throw new ModelParseException(
              "Unknown MicroserviceCall attribute: " + attribute.getName());
      }
    }
  }


  public String getModelId() {
    return modelId;
  }


  public String getPath() {
    return path;
  }


  public boolean isAuthorize() {
    return authorize;
  }


  public String getContent() {
    return content;
  }


  public MethodType getMethodType() {
    return methodType;
  }

}
