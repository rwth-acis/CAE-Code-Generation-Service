package i5.las2peer.services.codeGenerationService.models.microservice;

import java.util.ArrayList;
import java.util.HashMap;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
import i5.las2peer.services.codeGenerationService.models.microservice.HttpPayload.PayloadType;

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
  private ArrayList<InternalCall> internalCalls = new ArrayList<InternalCall>();
  private ArrayList<HttpPayload> payloads = new ArrayList<HttpPayload>();
  private ArrayList<HttpResponse> responses = new ArrayList<HttpResponse>();
  private HashMap<String, HttpPayload> nodeIdPayloads = new HashMap<String, HttpPayload>();
  private HashMap<String, HttpResponse> nodeIdResponses = new HashMap<String, HttpResponse>();
  private MobSOSLog mobSOSLog;

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
          if (this.name.contains(" ")) {
            throw new ModelParseException(
                "HttpMethod name contains invalid characters: " + this.name);
          }
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


  public ArrayList<InternalCall> getInternalCalls() {
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

  public HashMap<String, HttpPayload> getNodeIdPayloads() {
    return this.nodeIdPayloads;
  }


  public ArrayList<HttpResponse> getHttpResponses() {
    return this.responses;
  }

  public HashMap<String, HttpResponse> getNodeIdResponses() {
    return this.nodeIdResponses;
  }


  public void addInternalCall(InternalCall call) {
    this.internalCalls.add(call);
  }


  public void addHttpPayload(HttpPayload payload) {
    this.payloads.add(payload);
  }

  public void addNodeIdPayload(String nodeId, HttpPayload payload) {
    this.nodeIdPayloads.put(nodeId, payload);
  }

  public void addHttpResponse(HttpResponse response) {
    this.responses.add(response);
  }

  public void addNodeIdResponse(String nodeId, HttpResponse payload) {
    this.nodeIdResponses.put(nodeId, payload);
  }


  /**
   * 
   * Checks the (until now added) payloads and responses for (semantical) correctness.
   * 
   * @throws ModelParseException if the check revealed incorrectness
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
    // check responses
    for (int responseIndex = 0; responseIndex < this.responses.size(); responseIndex++) {
      if (responses.get(responseIndex).getName().contains(" ")) {
        throw new ModelParseException("HttpResponse name contains invalid characters: "
            + responses.get(responseIndex).getName());
      }
      if (responses.get(responseIndex).getResultName().contains(" ")) {
        throw new ModelParseException("HttpResponse result name contains invalid characters: "
            + responses.get(responseIndex).getResultName());
      }
    }
    // check payloads
    boolean contentPayloadParsed = false;
    for (int payloadIndex = 0; payloadIndex < this.payloads.size(); payloadIndex++) {
      if (payloads.get(payloadIndex).getName().contains(" ")) {
        throw new ModelParseException("HttpPayload name contains invalid characters: "
            + payloads.get(payloadIndex).getName());
      }
      if (payloads.get(payloadIndex).getPayloadType() != PayloadType.PATH_PARAM) {
        if (contentPayloadParsed) {
          throw new ModelParseException(
              "More than one content parameter in HTTPMethod " + this.name);
        } else {
          contentPayloadParsed = true;
        }
      }
    }
    // TODO check more?
  }

  public MobSOSLog getMobSOSLog() {
    return mobSOSLog;
  }

  public void setMobSOSLog(MobSOSLog mobSOSLog) {
    this.mobSOSLog = mobSOSLog;
  }
}
