package i5.las2peer.services.codeGenerationService.models.microservice;

import java.util.ArrayList;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;

/**
 * 
 * HttpMethod data class. Represents an HTTP method, which is part of a microservice model.
 *
 */
public class HttpMethod {

  /**
   * 
   * Represents the four possible method types an {@link HttpMethod} can have.
   * 
   */
  public enum MethodType {
    GET, POST, PUT, DELETE
  }

  private MethodType methodType;

  private String modelId;
  private String name;
  private String path;
  private String payload;
  private String payloadType;
  private ArrayList<String> internalCalls = new ArrayList<String>();

  /**
   * 
   * Creates a new {@link HttpMethod}.
   * 
   * @param node a {@link i5.cae.simpleModel.node.SimpleNode} containing the HttpMethod
   *        representation.
   * 
   * @throws ModelParseException if something goes wrong during parsing the node
   * 
   */
  public HttpMethod(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
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
              throw new ModelParseException("Unknown HTTPMethod type: " + methodType);
          }
          break;
        case "name":
          this.name = attribute.getValue();
          break;
        case "path":
          this.path = attribute.getValue();
          break;
        case "payload":
          this.payload = attribute.getValue();
          break;
        case "payloadType":
          this.payloadType = attribute.getValue();
          break;
        default:
          throw new ModelParseException(
              "Unknown HTTPMethod attribute name: " + attribute.getName());
      }
    }
  }


  public String getName() {
    return this.name;
  }


  public String getPath() {
    return this.path;
  }


  public String getPayload() {
    return this.payload;
  }


  public String getPayloadType() {
    return this.payloadType;
  }


  public ArrayList<String> getInternalCalls() {
    return this.internalCalls;
  }


  public MethodType getMethodType() {
    return this.methodType;
  }


  public String getModelId() {
    return this.modelId;
  }


  public void addInternalCall(String targetMethodId) {
    this.internalCalls.add(targetMethodId);
  }

}
