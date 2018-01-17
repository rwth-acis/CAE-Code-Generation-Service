package i5.las2peer.services.codeGenerationService.models.application.communicationModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.SimpleModel;
import i5.cae.simpleModel.edge.SimpleEdge;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.application.communicationModel.CommEdge.EdgeType;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.Function;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.HtmlElement;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.IWCCall;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.IWCResponse;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.MicroserviceCall;
import i5.las2peer.services.codeGenerationService.models.microservice.HttpMethod;
import i5.las2peer.services.codeGenerationService.models.microservice.InternalCall;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

/**
 * 
 * A special view on an
 * {@link i5.las2peer.services.codeGenerationService.models.application.Application}. Focuses on
 * representing the communication between its different components.
 * 
 */
public class CommunicationModel {
  // HashMaps have their SyncMeta id as key attribute
  private HashMap<String, CommCollaborativeElement> collaborativeElements;
  private HashMap<String, CommIWCCall> iwcCalls;
  private HashMap<String, CommIWCResponse> iwcResponses;
  private HashMap<String, CommMicroserviceCall> microserviceCalls;
  private HashMap<String, CommOtherService> otherServices;
  private HashMap<String, CommRestfulResource> restfulResources;
  private HashMap<String, CommWidget> widgets;
  private ArrayList<CommEdge> commEdges;
  private CommApplicationEnvironment applicationEnvironment;
  private String applicationEnvironemntId;

  /**
   * 
   * Creates a Communication View Model. The "comm"-elements this model consists of have the same
   * SyncMeta ids as the corresponding elements of the microserivce and frontend-component models.
   * 
   * Please note, that the creation of the model is not optimized in terms of speed, but split up
   * into several loops to provide at least a bit of readability. Should be fast enough though,
   * compared to the GiHub access functionalities of this service, this is still some dimensions
   * faster.
   * 
   * @param name the name of the complete application
   * @param version the application's version
   * @param microservices a map of microservices
   * @param frontendComponents a map of frontend components
   * 
   */
  public CommunicationModel(String name, String version, HashMap<String, Microservice> microservices,
      HashMap<String, FrontendComponent> frontendComponents) {

    // initialize maps, list and give random id to application environment object
    this.collaborativeElements = new HashMap<String, CommCollaborativeElement>();
    this.iwcCalls = new HashMap<String, CommIWCCall>();
    this.iwcResponses = new HashMap<String, CommIWCResponse>();
    this.microserviceCalls = new HashMap<String, CommMicroserviceCall>();
    this.otherServices = new HashMap<String, CommOtherService>();
    this.restfulResources = new HashMap<String, CommRestfulResource>();
    this.widgets = new HashMap<String, CommWidget>();
    this.commEdges = new ArrayList<CommEdge>();
    this.applicationEnvironemntId = generateNewRandomSyncMetaId();

    // NODES
    // create environment
    this.applicationEnvironment = new CommApplicationEnvironment(name, version);
    // extract nodes from microservices
    for (Microservice microservice : microservices.values()) {
      this.restfulResources.put(microservice.getMicroserviceModelId(),
          new CommRestfulResource(microservice));
    }
    // extract nodes from frontend-components
    for (FrontendComponent frontendComponent : frontendComponents.values()) {
      this.widgets.put(frontendComponent.getWidgetModelId(), new CommWidget(frontendComponent));
      for (Function function : frontendComponent.getFunctions().values()) {
        for (MicroserviceCall microserviceCall : function.getMicroserviceCalls()) {
          this.microserviceCalls.put(microserviceCall.getModelId(),
              new CommMicroserviceCall(microserviceCall));
        }
        for (IWCCall iwcCall : function.getIwcCalls()) {
          this.iwcCalls.put(iwcCall.getModelId(), new CommIWCCall(iwcCall));
        }
        for (IWCResponse iwcResponse : function.getIwcResponses()) {
          this.iwcResponses.put(iwcResponse.getModelId(), new CommIWCResponse(iwcResponse));
        }
      }
      for (HtmlElement element : frontendComponent.getHtmlElements().values()) {
        if (element.isCollaborativeElement()) {
          this.collaborativeElements.put(element.getModelId(),
              new CommCollaborativeElement(element));
        }
      }
    }

    // EDGES
    // connect widgets and restful resources to application environment
    for (Entry<String, CommRestfulResource> entry : this.restfulResources.entrySet()) {
      this.commEdges.add(new CommEdge(this.applicationEnvironemntId, entry.getKey(),
          EdgeType.APPLICATION_COMPONENT));
    }
    for (Entry<String, CommWidget> entry : this.widgets.entrySet()) {
      this.commEdges.add(new CommEdge(this.applicationEnvironemntId, entry.getKey(),
          EdgeType.APPLICATION_COMPONENT));
    }

    // go through microservices and add internal calls
    for (Microservice microserviceSource : microservices.values()) {
      for (HttpMethod method : microserviceSource.getHttpMethods().values()) {
        for (InternalCall internalCall : method.getInternalCalls()) {
          String targetServiceClass = internalCall.getServiceClass();
          boolean found = false;
          for (Microservice microserviceTarget : microservices.values()) {
            // a bit complex..but that is how one gets the class name
            String serviceClass = "i5.las2peer.services."
                + microserviceTarget.getResourceName().substring(0, 1).toLowerCase()
                + microserviceTarget.getResourceName().substring(1) + "."
                + microserviceTarget.getResourceName();
            // if target id matches and services are not the same, we found the target, add edge
            // from source service to target service (SyncMeta ids)
            // important: we do not check for existing (http-)methods, since internal invocations
            // can be done on "non-restful" (=not-modeled) methods as well
            if (serviceClass.equals(targetServiceClass) && !(microserviceTarget
                .getMicroserviceModelId().equals(microserviceSource.getMicroserviceModelId()))) {
              this.commEdges.add(new CommEdge(microserviceSource.getMicroserviceModelId(),
                  microserviceTarget.getMicroserviceModelId(), EdgeType.INTERNAL_RESOURCE_CALL));
              found = true;
              break;
            }
          }
          // if service was not found, search other services and add a new one if necessary
          if (!found) {
            found = false; // reset
            for (Entry<String, CommOtherService> otherService : this.otherServices.entrySet()) {
              if (otherService.getValue().getLabel().equals(targetServiceClass)) {
                found = true;
                this.commEdges.add(new CommEdge(microserviceSource.getMicroserviceModelId(),
                    otherService.getKey(), EdgeType.INTERNAL_RESOURCE_CALL));
                break;
              }
            }
            if (!found) {
              String newId = generateNewRandomSyncMetaId();
              otherServices.put(newId, new CommOtherService(targetServiceClass));
              this.commEdges.add(new CommEdge(microserviceSource.getMicroserviceModelId(), newId,
                  EdgeType.INTERNAL_RESOURCE_CALL));
            }
          }
        }
      }
    }

    // go through frontend components
    for (FrontendComponent frontendComponent : frontendComponents.values()) {
      // go through functions of frontend component
      for (Function function : frontendComponent.getFunctions().values()) {
        // widget to microservive call
        for (MicroserviceCall microserviceCall : function.getMicroserviceCalls()) {
          this.commEdges.add(new CommEdge(frontendComponent.getWidgetModelId(),
              microserviceCall.getModelId(), EdgeType.WIDGET_TO_MICROSERIVCE_CALL));
        }
        // widget to IWC calls
        for (IWCCall iwcCall : function.getIwcCalls()) {
          this.commEdges.add(new CommEdge(frontendComponent.getWidgetModelId(),
              iwcCall.getModelId(), EdgeType.WIDGET_TO_IWC_CALL));
        }
        // widget IWC responses
        for (IWCResponse iwcResponse : function.getIwcResponses()) {
          this.commEdges.add(new CommEdge(frontendComponent.getWidgetModelId(),
              iwcResponse.getModelId(), EdgeType.WIDGET_TO_IWC_RESPONSE));
        }
        // http calls (TODO: Check if parameter match)
        String frontendComponentMicroservicePath = frontendComponent.getMicroserviceAddress();
        for (Entry<String, Microservice> microservice : microservices.entrySet()) {
          if (frontendComponentMicroservicePath.equals(microservice.getValue().getPath())) {
            for (MicroserviceCall microserviceCall : function.getMicroserviceCalls()) {
              this.commEdges.add(new CommEdge(microserviceCall.getModelId(),
                  microservice.getValue().getMicroserviceModelId(), EdgeType.HTTP_CALL));
            }
          }
        }
      }
      // widget to collaborative elements
      for (HtmlElement element : frontendComponent.getHtmlElements().values()) {
        if (element.isCollaborativeElement()) {
          this.commEdges.add(new CommEdge(frontendComponent.getWidgetModelId(),
              element.getModelId(), EdgeType.WIDGET_TO_COLLABORATIVE_ELEMENT));
        }
      }
    }
    // iwc communication edges (check for matching intent action)
    for (Entry<String, CommIWCResponse> response : this.iwcResponses.entrySet()) {
      for (Entry<String, CommIWCCall> call : this.iwcCalls.entrySet()) {
        if (response.getValue().getLabel().equals(call.getValue().getLabel())) {
          this.commEdges
              .add(new CommEdge(call.getKey(), response.getKey(), EdgeType.IWC_COMMUNICATION));
        }
      }
    }
    // collaborative (Yjs) edges
    for (Entry<String, CommCollaborativeElement> elementOne : this.collaborativeElements
        .entrySet()) {
      for (Entry<String, CommCollaborativeElement> elementTwo : this.collaborativeElements
          .entrySet()) {
        if (!(elementOne.getKey().equals(elementTwo.getKey()))
            && elementOne.getValue().getLabel().equals(elementTwo.getValue().getLabel())) {
          this.commEdges
              .add(new CommEdge(elementOne.getKey(), elementTwo.getKey(), EdgeType.COMMUNICATES));
        }
      }
    }

  }


  /**
   * 
   * Converts the Communication View Model to a {@link i5.cae.simpleModel.SimpleModel}.
   * 
   * TODO: hardcoded the id's from the meta-model..
   * 
   * @return a {@link i5.cae.simpleModel.SimpleModel} representation of the communication view model
   * 
   */
  public SimpleModel toSimpleModel() {
    // NODES
    ArrayList<SimpleNode> nodes = new ArrayList<SimpleNode>();
    // application environment
    ArrayList<SimpleEntityAttribute> attributes = new ArrayList<SimpleEntityAttribute>();
    attributes.add(new SimpleEntityAttribute(AttributeIds.APPLICATION_ENVIRONMENT_LABEL.toString(),
        "label", this.applicationEnvironment.getLabel()));
    attributes
        .add(new SimpleEntityAttribute(AttributeIds.APPLICATION_ENVIRONMENT_VERSION.toString(),
            "version", this.applicationEnvironment.getVersion() + ""));
    nodes.add(new SimpleNode(applicationEnvironemntId, "Application Environment", attributes));
    // collaborativeElements
    for (Entry<String, CommCollaborativeElement> element : this.collaborativeElements.entrySet()) {
      attributes = new ArrayList<SimpleEntityAttribute>(Arrays
          .asList(new SimpleEntityAttribute(AttributeIds.COLLABORATIVE_ELEMENT_LABEL.toString(),
              "label", element.getValue().getLabel())));
      nodes.add(new SimpleNode(element.getKey(), "Collaborative Element", attributes));
    }
    // iwcCalls
    for (Entry<String, CommIWCCall> element : this.iwcCalls.entrySet()) {
      attributes = new ArrayList<SimpleEntityAttribute>(Arrays.asList(new SimpleEntityAttribute(
          AttributeIds.IWC_CALL_LABEL.toString(), "label", element.getValue().getLabel())));
      nodes.add(new SimpleNode(element.getKey(), "IWC Call", attributes));
    }
    // iwcResponses
    for (Entry<String, CommIWCResponse> element : this.iwcResponses.entrySet()) {
      attributes = new ArrayList<SimpleEntityAttribute>(Arrays.asList(new SimpleEntityAttribute(
          AttributeIds.IWC_RESPONSE_LABEL.toString(), "label", element.getValue().getLabel())));
      nodes.add(new SimpleNode(element.getKey(), "IWC Response", attributes));
    }
    // microserviceCalls
    for (Entry<String, CommMicroserviceCall> element : this.microserviceCalls.entrySet()) {
      attributes = new ArrayList<SimpleEntityAttribute>(
          Arrays.asList(new SimpleEntityAttribute(AttributeIds.MICROSERVICE_CALL_LABEL.toString(),
              "label", element.getValue().getLabel())));
      nodes.add(new SimpleNode(element.getKey(), "Microservice Call", attributes));
    }
    // otherServices
    for (Entry<String, CommOtherService> element : this.otherServices.entrySet()) {
      attributes = new ArrayList<SimpleEntityAttribute>(Arrays.asList(new SimpleEntityAttribute(
          AttributeIds.OTHER_SERVICE_LABEL.toString(), "label", element.getValue().getLabel())));
      nodes.add(new SimpleNode(element.getKey(), "Other Service", attributes));
    }
    // restfulResources
    for (Entry<String, CommRestfulResource> element : this.restfulResources.entrySet()) {
      attributes = new ArrayList<SimpleEntityAttribute>(Arrays.asList(new SimpleEntityAttribute(
          AttributeIds.RESTFUL_RESOURCE_LABEL.toString(), "label", element.getValue().getLabel())));
      nodes.add(new SimpleNode(element.getKey(), "Restful Resource", attributes));
    }
    // widgets
    for (Entry<String, CommWidget> element : this.widgets.entrySet()) {
      attributes = new ArrayList<SimpleEntityAttribute>(Arrays.asList(new SimpleEntityAttribute(
          AttributeIds.WIDGET_LABEL.toString(), "label", element.getValue().getLabel())));
      nodes.add(new SimpleNode(element.getKey(), "Widget", attributes));
    }

    // EDGES
    ArrayList<SimpleEdge> edges = new ArrayList<SimpleEdge>();
    for (CommEdge edge : this.commEdges) {
      edges.add(new SimpleEdge(generateNewRandomSyncMetaId(), edge.getSource(), edge.getTarget(),
          edge.getType().toString(), "", new ArrayList<>()));
    }

    // ATTRIBUTES
    attributes = new ArrayList<SimpleEntityAttribute>();
    attributes.add(new SimpleEntityAttribute(generateNewRandomSyncMetaId(), "type",
        "communication-view-model"));
    attributes.add(new SimpleEntityAttribute(generateNewRandomSyncMetaId(), "version",
        this.applicationEnvironment.getVersion() + ""));
    SimpleModel model =
        new SimpleModel(this.applicationEnvironment.getLabel(), nodes, edges, attributes);
    return model;
  }


  /**
   * 
   * Temporary solution, need to find a better way to do this (sometime..) But currently needed,
   * since SyncMeta attributes rely on their parent meta-model attribute id, which cannot be
   * randomly generated, but has to be passed on to this service from the model (JSON) file.
   * 
   */
  public enum AttributeIds {
    IWC_CALL_LABEL("e06a85d121d12e5c6b4442bb"), IWC_RESPONSE_LABEL(
        "15a477687f37ed702f9ea235"), MICROSERVICE_CALL_LABEL(
            "5789ce97def22cf74c41d454"), APPLICATION_ENVIRONMENT_LABEL(
                "695450a0bbbca8556b24868b"), APPLICATION_ENVIRONMENT_VERSION(
                    "e8ceef5fec841d6db6f65e0f"), RESTFUL_RESOURCE_LABEL(
                        "18ca0a76756191969557c5ee"), WIDGET_LABEL(
                            "f8d9e72ff7f31966fd274c9c"), COLLABORATIVE_ELEMENT_LABEL(
                                "aac860c27f30fd44e43425a8"), OTHER_SERVICE_LABEL(
                                    "158dd42ccdef60b1f1a333de");
    private String name;

    AttributeIds(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }


  /**
   * 
   * Generates a new random SyncMeta id.
   * 
   * @return the id as a String
   * 
   */
  private String generateNewRandomSyncMetaId() {
    char[] chars = "1234567890abcdef".toCharArray();;
    int numOfChars = chars.length;
    int i, rand;
    String id = "";
    int length = 24;
    for (i = 0; i < length; i++) {
      rand = (int) Math.floor(Math.random() * numOfChars);
      id += chars[rand];
    }
    return id;
  }
}
