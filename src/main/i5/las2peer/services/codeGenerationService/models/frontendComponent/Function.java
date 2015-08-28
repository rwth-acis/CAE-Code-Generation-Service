package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import java.util.ArrayList;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;


/**
 * 
 * Function data class. Represents a function, which is part of a frontend component model.
 *
 */
public class Function {


  private String modelId;
  private String name;
  private String returnParameter;
  private ArrayList<String> htmlElementUpdates;
  private ArrayList<String> htmlElementCreations;
  private ArrayList<InputParameter> inputParameters;
  private ArrayList<IWCCall> iwcCalls;
  private ArrayList<IWCResponse> iwcResponses;
  private ArrayList<MicroserviceCall> microserviceCalls;

  /**
   *
   * Event constructor. Takes a {@link SimpleNode} and parses it to a Function.
   * 
   * @param node the node representing the Function
   * 
   * @throws ModelParseException if some error comes up during parsing the node
   * 
   */
  public Function(SimpleNode node) throws ModelParseException {
    // initialize lists
    this.htmlElementUpdates = new ArrayList<String>();
    this.htmlElementCreations = new ArrayList<String>();
    this.inputParameters = new ArrayList<InputParameter>();
    this.iwcCalls = new ArrayList<IWCCall>();
    this.iwcResponses = new ArrayList<IWCResponse>();
    this.microserviceCalls = new ArrayList<MicroserviceCall>();

    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "name":
          this.name = attribute.getValue();
          break;
        case "returnParameter":
          this.returnParameter = attribute.getValue();
          break;
        default:
          throw new ModelParseException("Unknown Function attribute: " + attribute.getName());
      }
    }
  }


  public String getName() {
    return this.name;
  }


  public String getReturnParameter() {
    return this.returnParameter;
  }


  public String getModelId() {
    return this.modelId;
  }


  public ArrayList<String> getHtmlElementUpdates() {
    return this.htmlElementUpdates;
  }


  public ArrayList<String> getHtmlElementCreations() {
    return this.htmlElementCreations;
  }


  public ArrayList<InputParameter> getInputParameters() {
    return this.inputParameters;
  }


  public ArrayList<IWCCall> getIwcCalls() {
    return this.iwcCalls;
  }


  public ArrayList<IWCResponse> getIwcResponses() {
    return this.iwcResponses;
  }


  public ArrayList<MicroserviceCall> getMicroserviceCalls() {
    return this.microserviceCalls;
  }


  /**
   * 
   * Adds an HtmlElement id to the update list.
   * 
   * @param htmlElementId the id of the HtmlElement
   * 
   */
  public void addHtmlElementUpdates(String htmlElementId) {
    this.htmlElementUpdates.add(htmlElementId);
  }


  /**
   * 
   * Adds an HtmlElement id to the creations list.
   * 
   * @param htmlElementId the id of the HtmlElement
   * 
   */
  public void addHtmlElementCreations(String htmlElementId) {
    this.htmlElementCreations.add(htmlElementId);
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


  /**
   * 
   * Adds an {@link IWCCall} to the IWCCall list.
   * 
   * @param call an {@link IWCCall}
   * 
   */
  public void addIwcCall(IWCCall call) {
    this.iwcCalls.add(call);
  }


  /**
   * 
   * Adds an {@link IWCResponse} to the IWCResponse list.
   * 
   * @param response an {@link IWCResponse}
   * 
   */
  public void addIwcResponse(IWCResponse response) {
    this.iwcResponses.add(response);
  }


  /**
   * 
   * Adds an {@link MicroserviceCall} to the MicroserviceCall list.
   * 
   * @param call an {@link MicroserviceCall}
   * 
   */
  public void addMicroserviceCall(MicroserviceCall call) {
    this.microserviceCalls.add(call);
  }


  /**
   * 
   * Checks the (complete) function for (semantical) correctness. Should be called after all
   * microservice calls, IWC events and input parameters are added to the function.
   * 
   * @throws ModelParseException if the function is not (semantical) correct
   * 
   */
  public void checkCorrectness() throws ModelParseException {
    if (!this.iwcResponses.isEmpty()) {
      if (!this.inputParameters.isEmpty()) {
        throw new ModelParseException(
            "Functions that wait for an IWC response may not have additional input parameters!");
      }
      for (IWCResponse response : this.iwcResponses) {
        if (!response.getContent().equals(this.iwcResponses.get(0).getContent())) {
          throw new ModelParseException(
              "Functions that have multiple IWC responses must have the same (iwc-)content variable name!");
        }
      }
    }
  }
}
