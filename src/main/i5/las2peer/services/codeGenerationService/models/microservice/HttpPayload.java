package i5.las2peer.services.codeGenerationService.models.microservice;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;

/**
 * 
 * HttpPayload data class. Represents an HTTP Payload, which is part of a microservice model.
 *
 */
public class HttpPayload {

  /**
   * 
   * Represents the four different payload types an {@link HttpPayload} can have.
   * 
   */
  public enum PayloadType {
    JSONObject, String, PATH_PARAM, CUSTOM
  }

  private String modelId;
  private String name;
  private PayloadType payloadType;
  private MobSOSLog mobSOSLog;

  /**
   * 
   * Creates a new {@link HttpPayload}.
   * 
   * @param node a {@link i5.cae.simpleModel.node.SimpleNode} containing the HttpPayload
   * 
   * @throws ModelParseException if something goes wrong during parsing of the HttpPayload
   *         information
   * 
   */
  public HttpPayload(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "name":
          this.name = attribute.getValue();
          break;
        case "payloadType":
          switch (attribute.getValue()) {
            case "JSON":
              this.payloadType = PayloadType.JSONObject;
              break;
            case "TEXT":
              this.payloadType = PayloadType.String;
              break;
            case "PATH_PARAM":
              this.payloadType = PayloadType.PATH_PARAM;
              break;
            case "CUSTOM":
              this.payloadType = PayloadType.CUSTOM;
              break;
            default:
              throw new ModelParseException(
                  "Unknown HttpPayload payloadType: " + attribute.getValue());
          }
          break;
        default:
          throw new ModelParseException("Unknown HttpPayload attribute: " + attribute.getName());
      }
    }
  }


  public String getModelId() {
    return modelId;
  }


  public String getName() {
    return name;
  }


  public PayloadType getPayloadType() {
    return payloadType;
  }

  public MobSOSLog getMobSOSLog() {
    return mobSOSLog;
  }

  public void setMobSOSLog(MobSOSLog mobSOSLog) {
    this.mobSOSLog = mobSOSLog;
  }

}
