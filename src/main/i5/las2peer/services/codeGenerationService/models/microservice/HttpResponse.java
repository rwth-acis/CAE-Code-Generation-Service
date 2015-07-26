package i5.las2peer.services.codeGenerationService.models.microservice;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;

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
    OK, CREATED, BAD_REQUEST, UNAUTHORIZED, NOT_FOUND, CONFLICT, INTERNAL_ERROR, CUSTOM
  }

  /**
   * 
   * Represents the three different result types a {@link HttpResponse} can have.
   * 
   */
  public enum ResultType {
    JSON, TEXT, CUSTOM
  }

  private String modelId;
  private String name;
  private String resultName;
  private ReturnStatusCode returnStatusCode;
  private ResultType resultType;

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
              this.returnStatusCode = ReturnStatusCode.OK;
              break;
            case "CREATED":
              this.returnStatusCode = ReturnStatusCode.CREATED;
              break;
            case "BAD_REQUEST":
              this.returnStatusCode = ReturnStatusCode.BAD_REQUEST;
              break;
            case "UNAUTHORIZED":
              this.returnStatusCode = ReturnStatusCode.UNAUTHORIZED;
              break;
            case "NOT_FOUND":
              this.returnStatusCode = ReturnStatusCode.NOT_FOUND;
              break;
            case "CONFLICT":
              this.returnStatusCode = ReturnStatusCode.CONFLICT;
              break;
            case "INTERNAL_ERROR":
              this.returnStatusCode = ReturnStatusCode.INTERNAL_ERROR;
              break;
            case "CUSTOM":
              this.returnStatusCode = ReturnStatusCode.CUSTOM;
              break;
            default:
              throw new ModelParseException(
                  "Unknown HttpResponse returnStatusCode: " + attribute.getValue());
          }
          break;
        case "resultType":
          switch (attribute.getValue()) {
            case "JSON":
              this.resultType = ResultType.JSON;
              break;
            case "TEXT":
              this.resultType = ResultType.TEXT;
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

}
