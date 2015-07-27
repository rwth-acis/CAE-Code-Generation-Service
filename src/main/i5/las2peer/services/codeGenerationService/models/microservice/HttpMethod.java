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
  private ArrayList<String> internalCalls = new ArrayList<String>();
  private ArrayList<HttpPayload> payloads = new ArrayList<HttpPayload>();
  private ArrayList<HttpResponse> responses = new ArrayList<HttpResponse>();

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
              throw new ModelParseException(
                  "Unknown HTTPMethod methodType: " + attribute.getValue());
          }
          break;
        case "name":
          this.name = attribute.getValue();
          break;
        case "path":
          this.path = attribute.getValue();
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


  public ArrayList<String> getInternalCalls() {
    return this.internalCalls;
  }


  public MethodType getMethodType() {
    return this.methodType;
  }


  public String getModelId() {
    return this.modelId;
  }


  public ArrayList<HttpPayload> getHttpPayloads() {
    return this.payloads;
  }


  public ArrayList<HttpResponse> getHttpResponses() {
    return this.responses;
  }


  public void addInternalCall(String targetMethodId) {
    this.internalCalls.add(targetMethodId);
  }


  public void addHttpPayload(HttpPayload payload) {
    this.payloads.add(payload);
  }


  public void addHttpResponse(HttpResponse response) {
    this.responses.add(response);
  }


  /**
   * 
   * Checks the (until now added) payloads and responses for (semantical) correctness.
   * 
   * @throws ModelParseException the check revealed incorrectness.
   * 
   */
  public void checkPayloadAndResponses() throws ModelParseException {
    // no payload but at least one response is ok
    if (this.payloads.isEmpty() && !this.responses.isEmpty()) {
      return;
    }
    // has to have at least one response
    if (this.responses.isEmpty()) {
      throw new ModelParseException("Http Method " + this.name + " contains no response!");
    }
    // TODO check more?
  }

}
