package i5.las2peer.services.codeGenerationService.models.microservice;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;

/**
 * 
 * HttpResponse data class. Represents a HttpResponse, which is part of a microservice model.
 *
 */
public class HttpResponse {

  /**
   * 
   * Represents the different return status codes a {@link HttpResponse} can have.
   * 
   */
  public enum ReturnStatusCode {
    HTTP_OK(200), HTTP_CREATED(201), HTTP_BAD_REQUEST(400), HTTP_UNAUTHORIZED(401), HTTP_NOT_FOUND(
        404), HTTP_CONFLICT(409), HTTP_INTERNAL_ERROR(500), HTTP_CUSTOM(-1);

    private int value = -1;


    private ReturnStatusCode(int value) {
      this.value = value;
    }

    public int getCode() {
      return value;
    }
  }

  /**
   * 
   * Represents the three different result types a {@link HttpResponse} can have.
   * 
   */
  public enum ResultType {
    JSONObject, String, CUSTOM
  }

  private String modelId;
  private String name;
  private String resultName;
  private ReturnStatusCode returnStatusCode;
  private ResultType resultType;
  private MobSOSLog mobSOSLog;

  /**
   * 
   * Creates a new {@link HttpResponse}.
   * 
   * @param node a {@link i5.cae.simpleModel.node.SimpleNode} containing the response
   * 
   * @throws ModelParseException if something goes wrong during the parsing of the HttpResponse
   *         information
   * 
   */
  public HttpResponse(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "name":
          this.name = attribute.getValue();
          break;
        case "resultName":
          this.resultName = attribute.getValue();
          break;
        case "returnStatusCode":
          switch (attribute.getValue()) {
            case "OK":
              this.returnStatusCode = ReturnStatusCode.HTTP_OK;
              break;
            case "CREATED":
              this.returnStatusCode = ReturnStatusCode.HTTP_CREATED;
              break;
            case "BAD_REQUEST":
              this.returnStatusCode = ReturnStatusCode.HTTP_BAD_REQUEST;
              break;
            case "UNAUTHORIZED":
              this.returnStatusCode = ReturnStatusCode.HTTP_UNAUTHORIZED;
              break;
            case "NOT_FOUND":
              this.returnStatusCode = ReturnStatusCode.HTTP_NOT_FOUND;
              break;
            case "CONFLICT":
              this.returnStatusCode = ReturnStatusCode.HTTP_CONFLICT;
              break;
            case "INTERNAL_ERROR":
              this.returnStatusCode = ReturnStatusCode.HTTP_INTERNAL_ERROR;
              break;
            case "CUSTOM":
              this.returnStatusCode = ReturnStatusCode.HTTP_CUSTOM;
              break;
            default:
              throw new ModelParseException(
                  "Unknown HttpResponse returnStatusCode: " + attribute.getValue());
          }
          break;
        case "resultType":
          switch (attribute.getValue()) {
            case "JSON":
              this.resultType = ResultType.JSONObject;
              break;
            case "TEXT":
              this.resultType = ResultType.String;
              break;
            case "CUSTOM":
              this.resultType = ResultType.CUSTOM;
              break;
            default:
              throw new ModelParseException(
                  "Unknown HttpResponse resultType: " + attribute.getValue());
          }
          break;
        default:
          throw new ModelParseException("Unknown HttpResponse attribute: " + attribute.getName());
      }
    }
  }


  public String getModelId() {
    return modelId;
  }


  public String getName() {
    return name;
  }


  public String getResultName() {
    return resultName;
  }


  public ReturnStatusCode getReturnStatusCode() {
    return returnStatusCode;
  }


  public ResultType getResultType() {
    return resultType;
  }

  public MobSOSLog getMobSOSLog() {
    return mobSOSLog;
  }

  public void setMobSOSLog(MobSOSLog mobSOSLog) {
    this.mobSOSLog = mobSOSLog;
  }
}
