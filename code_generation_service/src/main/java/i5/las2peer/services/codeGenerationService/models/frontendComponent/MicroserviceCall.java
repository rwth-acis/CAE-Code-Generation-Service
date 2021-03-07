package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import java.util.ArrayList;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
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
  private ContentType contentType;
  private ArrayList<InputParameter> inputParameters;

  /**
   * 
   * Represents the three different content types a {@link MicroserviceCall} can have.
   * 
   */
  public enum ContentType {
    json, text, CUSTOM;

    @Override
    public String toString() {
      switch (this) {
        case json:
          return "application/json";
        case text:
          return "text/plain";
        case CUSTOM:
          return "CUSTOM";
        default:
          return "Unknown";
      }
    }

  }


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
    // initialize list
    this.inputParameters = new ArrayList<InputParameter>();

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
        case "contentType":
          switch (attribute.getValue()) {
            case "application/json":
              this.contentType = ContentType.json;
              break;
            case "text/plain":
              this.contentType = ContentType.text;
              break;
            case "CUSTOM":
              this.contentType = ContentType.CUSTOM;
              break;
            default:
              throw new ModelParseException(
                  "Unknown MicroserviceCall contentType: " + attribute.getValue());
          }
          break;
        default:
          throw new ModelParseException(
              "Unknown MicroserviceCall attribute: " + attribute.getName());
      }
    }
  }


  public String getModelId() {
    return this.modelId;
  }


  public String getPath() {
    return this.path;
  }


  public boolean isAuthorize() {
    return this.authorize;
  }


  public String getContent() {
    return this.content;
  }


  public ContentType getContentType() {
    return this.contentType;
  }


  public MethodType getMethodType() {
    return this.methodType;
  }


  public ArrayList<InputParameter> getInputParameters() {
    return inputParameters;
  }


  /**
   * 
   * Adds an {@link InputParameter} to the input parameter list.
   * 
   * @param parameter an {@link InputParameter}
   * 
   */
  public void addInputParameter(InputParameter parameter) {
    this.inputParameters.add(parameter);
  }

}
