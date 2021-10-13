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
public class ParamBinding {


  private String modelId;
  private String input;
  private String output;
  private String functionName;

  private ArrayList<String> viewComponentUpdates;


  /**
   *
   * Event constructor. Takes a {@link SimpleNode} and parses it to a Function.
   *
   * @param node the node representing the Function
   *
   * @throws ModelParseException if some error comes up during parsing the node
   *
   */
  public ParamBinding(SimpleNode node) throws ModelParseException {
    // initialize lists
    this.viewComponentUpdates = new ArrayList<String>();

    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "input":
          this.input = attribute.getValue();
          break;
        case "output":
          this.output = attribute.getValue();
          break;
        case "functionName":
          this.functionName = attribute.getValue();
          break;
        default:
          throw new ModelParseException("Unknown Function attribute: " + attribute.getName());
      }
    }
  }


  public String getModelId() {
    return this.modelId;
  }


  public String getInput() {
    return this.input;
  }


  public String getOutput() {
    return this.output;
  }


  public String getFunctionName() {
    return this.functionName;
  }


  public ArrayList<String> getViewComponentUpdates() {
    return this.viewComponentUpdates;
  }


  /**
   *
   * Adds an ViewComponent id to the update list.
   *
   * @param viewComponentId the id of the ViewComponent
   *
   */
  public void addViewComponentUpdates(String viewComponentId) {
    this.viewComponentUpdates.add(viewComponentId);
  }


}
