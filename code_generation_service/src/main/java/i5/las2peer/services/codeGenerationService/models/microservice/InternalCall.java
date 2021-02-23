package i5.las2peer.services.codeGenerationService.models.microservice;

import java.util.ArrayList;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;

/**
 * 
 * InternalCall data class. Represents an internal service call, which is part of a microservice
 * model.
 *
 */
public class InternalCall {

  private String modelId;
  private String returnVariableName;
  private String serviceClass;
  private String methodName;
  private ArrayList<InternalCallParam> parameters;


  /**
   * 
   * Creates a new {@link InternalCall}.
   * 
   * @param node a {@link i5.cae.simpleModel.node.SimpleNode} containing the call
   * 
   * @throws ModelParseException if something goes wrong during the parsing of the InternalCall
   *         information
   * 
   */
  public InternalCall(SimpleNode node) throws ModelParseException {
    this.parameters = new ArrayList<InternalCallParam>();
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "returnVariableName":
          this.returnVariableName = attribute.getValue();
          break;
        case "serviceClass":
          this.serviceClass = attribute.getValue();
          break;
        case "methodName":
          this.methodName = attribute.getValue();
          break;
        default:
          throw new ModelParseException("Unknown Internal Call attribute: " + attribute.getName());
      }
    }
  }


  public String getModelId() {
    return modelId;
  }


  public String getReturnVariableName() {
    return returnVariableName;
  }


  public String getServiceClass() {
    return serviceClass;
  }


  public String getMethodName() {
    return methodName;
  }


  public ArrayList<InternalCallParam> getParameters() {
    return parameters;
  }


  public void addInternalCallParam(InternalCallParam internalCallParam) {
    this.parameters.add(internalCallParam);
  }

}
