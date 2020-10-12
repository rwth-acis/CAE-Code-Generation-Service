package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.SimpleModel;
import i5.cae.simpleModel.edge.SimpleEdge;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;

/**
 * 
 * FrontendComponent data class. Currently, edges are only used for creating simple 1 to 1
 * dependencies between objects, without any attributes added to them.
 * 
 */
public class FrontendComponent {
  private String versionedModelId;
  private String widgetModelId;
  private String name;
  private String widgetName;
  private String version;
  private String selectedCommitSha;
  private String widgetDescription;
  private String widgetDeveloperName;
  private String widgetDeveloperMail;
  private int widgetWidth;
  private int widgetHeight;
  private String microserviceAddress;
  private HashMap<String, HtmlElement> htmlElements;
  private HashMap<String, Function> functions;
  private boolean hasPolymerElements = false;

  /**
   * 
   * Creates a new frontend component.
   * 
   * @param model a {@link i5.cae.simpleModel.SimpleModel} containing the frontend component
   * 
   * @throws ModelParseException if something goes wrong during parsing
   * 
   */
  public FrontendComponent(SimpleModel model) throws ModelParseException {
    this.htmlElements = new HashMap<String, HtmlElement>();
    this.functions = new HashMap<String, Function>();

    // some helper fields to check model for correctness
    // used to find (possible) duplicate (HTML) ids and report them
    ArrayList<String> tempIds = new ArrayList<String>();
    // used to first parse all nodes and later add them to their corresponding "parent objects"
    HashMap<String, Event> tempEvents = new HashMap<String, Event>();
    HashMap<String, InputParameter> tempParameters = new HashMap<String, InputParameter>();
    HashMap<String, IWCResponse> tempIwcResponses = new HashMap<String, IWCResponse>();
    HashMap<String, IWCCall> tempIwcCalls = new HashMap<String, IWCCall>();
    HashMap<String, MicroserviceCall> tempMicroserviceCalls =
        new HashMap<String, MicroserviceCall>();

    this.name = model.getName();

    // metadata of model (currently only version)
    for (int attributeIndex = 0; attributeIndex < model.getAttributes().size(); attributeIndex++) {
      if (model.getAttributes().get(attributeIndex).getName().equals("version")) {
        try {
          this.setVersion(model.getAttributes().get(attributeIndex).getValue());
        } catch (NumberFormatException e) {
          throw new ModelParseException("FrontendComponent version is not a number!");
        }
      }
      if(model.getAttributes().get(attributeIndex).getName().equals("versionedModelId")) {
    	this.versionedModelId = model.getAttributes().get(attributeIndex).getValue();
      }
      if(model.getAttributes().get(attributeIndex).getName().equals("commitSha")) {
    	this.selectedCommitSha = model.getAttributes().get(attributeIndex).getValue();
      }
      if(model.getAttributes().get(attributeIndex).getName().equals("componentName")) {
    	this.name = model.getAttributes().get(attributeIndex).getValue();
      }
    }
    // go through the nodes and create objects
    ArrayList<SimpleNode> nodes = model.getNodes();
    for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
      SimpleNode node = nodes.get(nodeIndex);
      ArrayList<SimpleEntityAttribute> nodeAttributes = node.getAttributes();
      switch (node.getType()) {
        case "Widget":
          if (this.widgetModelId == null) {
            this.widgetModelId = node.getId();
          } else {
            throw new ModelParseException("More than one Widget in FrontendComponent model");
          }
          for (int attributeIndex = 0; attributeIndex < nodeAttributes.size(); attributeIndex++) {
            SimpleEntityAttribute attribute = nodeAttributes.get(attributeIndex);
            switch (attribute.getName()) {
              case "name":
                this.widgetName = attribute.getValue();
                break;
              case "description":
                this.widgetDescription = attribute.getValue();
                break;
              case "developerName":
                this.widgetDeveloperName = attribute.getValue();
                break;
              case "developerMail":
                this.widgetDeveloperMail = attribute.getValue();
                break;
              case "height":
                try {
                  this.widgetHeight = Integer.parseInt(attribute.getValue());
                } catch (NumberFormatException e) {
                  throw new ModelParseException("Widget height is not a number!");
                }
                break;
              case "width":
                try {
                  this.widgetWidth = Integer.parseInt(attribute.getValue());
                  break;
                } catch (NumberFormatException e) {
                  throw new ModelParseException("Widget width is not a number!");
                }
              case "microserviceAddress":
                this.microserviceAddress = attribute.getValue();
                break;
              default:
                throw new ModelParseException(
                    "Unknown attribute type of Widget: " + attribute.getName());
            }
          }
          break;
        case "HTML Element":
          HtmlElement element = new HtmlElement(node);

          //element is a polymer element
          if(element.getType().equals(HtmlElement.ElementType.CUSTOM))
            this.setHasPolymerElements(true);

          this.htmlElements.put(node.getId(), element);
          if (tempIds.contains(element.getModelId())) {
            throw new ModelParseException("Duplicate id found: " + element.getModelId());
          }
          tempIds.add(element.getModelId());
          break;
        case "Event":
          Event event = new Event(node);
          tempEvents.put(node.getId(), event);
          break;
        case "Function":
          Function function = new Function(node);
          this.functions.put(node.getId(), function);
          break;
        case "Input Parameter":
          InputParameter parameter = new InputParameter(node);
          tempParameters.put(node.getId(), parameter);
          break;
        case "IWC Response":
          IWCResponse response = new IWCResponse(node);
          tempIwcResponses.put(node.getId(), response);
          break;
        case "IWC Call":
          IWCCall call = new IWCCall(node);
          tempIwcCalls.put(node.getId(), call);
          break;
        case "Microservice Call":
          MicroserviceCall microserviceCall = new MicroserviceCall(node);
          tempMicroserviceCalls.put(node.getId(), microserviceCall);
          break;
        default:
          throw new ModelParseException("Unknown node type: " + node.getType());
      }
    }

    // edges
    ArrayList<SimpleEdge> edges = model.getEdges();
    // helper variables to check for correct edges
    int htmlElementCount = this.htmlElements.size();
    int functionCount = this.functions.size();
    for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
      String currentEdgeSource = edges.get(edgeIndex).getSourceNode();
      String currentEdgeTarget = edges.get(edgeIndex).getTargetNode();
      String currentEdgeType = edges.get(edgeIndex).getType();
      switch (currentEdgeType) {
        case "Widget to HTML Element":
          if (!this.widgetModelId.equals(currentEdgeSource)
              || !this.htmlElements.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong Widget to HTML edge!");
          }
          htmlElementCount--;
          break;
        case "Element Update":
          if (!this.functions.containsKey(currentEdgeSource)
              || !this.htmlElements.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong Element Update edge!");
          }
          // check if element is not static
          if (this.htmlElements.get(currentEdgeTarget).isStaticElement()) {
            throw new ModelParseException("Static HtmlElements cannot be updated by functions: "
                + this.htmlElements.get(currentEdgeTarget).getId());
          }
          this.functions.get(currentEdgeSource).addHtmlElementUpdates(currentEdgeTarget);
          break;
        case "Element Creation":
          if (!this.functions.containsKey(currentEdgeSource)
              || !this.htmlElements.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong Element Creation edge!");
          }
          // check if element is not static
          if (this.htmlElements.get(currentEdgeTarget).isStaticElement()) {
            throw new ModelParseException("Static HtmlElements cannot be created by functions: "
                + this.htmlElements.get(currentEdgeTarget).getId());
          }
          this.functions.get(currentEdgeSource).addHtmlElementCreations(currentEdgeTarget);
          break;
        case "HTML Element to Event":
          if (!this.htmlElements.containsKey(currentEdgeSource)
              || !tempEvents.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong HTML Element to Event edge!");
          }
          this.htmlElements.get(currentEdgeSource).addEvent(tempEvents.get(currentEdgeTarget));
          tempEvents.remove(currentEdgeTarget);
          break;
        case "Parameter Connection":
          // check if parameter is there
          if (!tempParameters.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong Parameter Connection edge!");
          }
          // check for function connection
          if (this.functions.containsKey(currentEdgeSource)) {
            this.functions.get(currentEdgeSource)
                .addInputParameter(tempParameters.get(currentEdgeTarget));
          }
          // if not, check for microservice connection
          // first, check if call is still in tempMicroserviceCalls
          else if (tempMicroserviceCalls.containsKey(currentEdgeSource)) {
            tempMicroserviceCalls.get(currentEdgeSource)
                .addInputParameter(tempParameters.get(currentEdgeTarget));
            // if not, check if in list of a function
          } else {
            boolean found = false;
            for (Function function : this.functions.values()) {
              for (MicroserviceCall call : function.getMicroserviceCalls()) {
                if (call.getModelId().equals(currentEdgeSource)) {
                  call.addInputParameter(tempParameters.get(currentEdgeTarget));
                  found = true;
                }
              }
            }
            // if not, the parameter connection is invalid
            if (!found) {
              throw new ModelParseException("Wrong Parameter Connection edge!");
            }
          }
          tempParameters.remove(currentEdgeTarget);
          break;
        case "Waits for":
          if (!this.functions.containsKey(currentEdgeSource)
              || !tempIwcResponses.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong Waits for edge!");
          }
          this.functions.get(currentEdgeSource)
              .addIwcResponse(tempIwcResponses.get(currentEdgeTarget));
          tempIwcResponses.remove(currentEdgeTarget);
          break;
        case "Initiates":
          if (!this.functions.containsKey(currentEdgeSource)
              || !tempIwcCalls.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong Initiates edge!");
          }
          this.functions.get(currentEdgeSource).addIwcCall(tempIwcCalls.get(currentEdgeTarget));
          tempIwcCalls.remove(currentEdgeTarget);
          break;
        case "Event to Function Call":

          // check if function exists
          if (!this.functions.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong Event to Function Call!");
          }
          // check if event is still in tempEvent list
          if (tempEvents.containsKey(currentEdgeSource)) {
            tempEvents.get(currentEdgeSource).setCalledFunctionId(currentEdgeTarget);
            break;
          } else {
            boolean found = false;
            // now we need to check already parsed events..
            for (HtmlElement element : this.htmlElements.values()) {
              for (Event event : element.getEvents()) {
                if (event.getModelId().equals(currentEdgeSource)) {
                  event.setCalledFunctionId(currentEdgeTarget);
                  found = true;
                }
              }
            }
            if (!found) {
              throw new ModelParseException("Wrong Event to Function Call!");
            }
          }
          break;
        case "Function To Microservice Call":
          if (!this.functions.containsKey(currentEdgeSource)
              || !tempMicroserviceCalls.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong Function To Microservice Call edge!");
          }
          this.functions.get(currentEdgeSource)
              .addMicroserviceCall(tempMicroserviceCalls.get(currentEdgeTarget));
          tempMicroserviceCalls.remove(currentEdgeTarget);
          break;
        case "Widget to Function":
          if (!this.widgetModelId.equals(currentEdgeSource)
              || !this.functions.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong Widget to Function edge!");
          }
          functionCount--;
          break;
        case "hasChild":
            //actually not needed, consider removing this edge
          if(!htmlElements.containsKey(currentEdgeSource) || !htmlElements.containsKey(currentEdgeTarget)){
              throw new ModelParseException("Wrong hasChild edge");
          }
          HtmlElement parent = htmlElements.get(currentEdgeSource);
          HtmlElement child = htmlElements.get(currentEdgeTarget);
          parent.addChildren(child);
          child.setParent(parent);
          break;
        default:
          throw new ModelParseException("Unknown frontend component edge type: " + currentEdgeType);
      }
    }
    // only one widget allowed (checked previously), no multiple edges between two objects in
    // SyncMeta -> element count must be zero now if all elements are connected to the widget
    // also, all temp lists should be empty by now
    if (htmlElementCount != 0 || functionCount != 0 || !tempEvents.isEmpty()
        || !tempParameters.isEmpty() || !tempIwcResponses.isEmpty() || !tempIwcCalls.isEmpty()
        || !tempMicroserviceCalls.isEmpty()) {
      throw new ModelParseException("Model not fully connected!");
    }
    // check functions (now complete with all IWC events, microservice calls and input parameters)
    // for semantical correctness
    for (Function function : this.functions.values()) {
      function.checkCorrectness();
    }
  }

  public String getWidgetModelId() {
    return widgetModelId;
  }


  public void setWidgetModelId(String widgetId) {
    this.widgetModelId = widgetId;
  }


  public String getVersionedModelId() {
    return this.versionedModelId;
  }
  
  public String getName() {
	return this.name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public String getWidgetName() {
    return this.widgetName;
  }


  public void setWidgetName(String name) {
    this.widgetName = name;
  }


  public String getVersion() {
    return this.version;
  }


  public void setVersion(String version) {
    this.version = version;
  }

  public String getSelectedCommitSha() {
	return this.selectedCommitSha;
  }

  public String getWidgetDescription() {
    return widgetDescription;
  }


  public void setWidgetDescription(String widgetDescription) {
    this.widgetDescription = widgetDescription;
  }


  public String getWidgetDeveloperName() {
    return widgetDeveloperName;
  }


  public void setWidgetDeveloperName(String widgetDeveloperName) {
    this.widgetDeveloperName = widgetDeveloperName;
  }


  public String getWidgetDeveloperMail() {
    return widgetDeveloperMail;
  }


  public void setWidgetDeveloperMail(String widgetDeveloperMail) {
    this.widgetDeveloperMail = widgetDeveloperMail;
  }


  public int getWidgetWidth() {
    return widgetWidth;
  }


  public void setWidgetWidth(int widgetWidth) {
    this.widgetWidth = widgetWidth;
  }


  public int getWidgetHeight() {
    return widgetHeight;
  }


  public void setWidgetHeight(int widgetHeight) {
    this.widgetHeight = widgetHeight;
  }


  public Map<String, HtmlElement> getHtmlElements() {
    return this.htmlElements;
  }


  public void setHtmlElements(HashMap<String, HtmlElement> htmlElements) {
    this.htmlElements = htmlElements;
  }


  public Map<String, Function> getFunctions() {
    return this.functions;
  }


  public void setFunctions(HashMap<String, Function> functions) {
    this.functions = functions;
  }


  public String getMicroserviceAddress() {
    return microserviceAddress;
  }


  public void setMicroserviceAddress(String microserviceAddress) {
    this.microserviceAddress = microserviceAddress;
  }

  public boolean hasPolymerElements(){
    return this.hasPolymerElements;
  }

  private void setHasPolymerElements(boolean hasPolymerElements){
    this.hasPolymerElements = hasPolymerElements;
  }
}
