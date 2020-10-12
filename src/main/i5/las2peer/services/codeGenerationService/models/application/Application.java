package i5.las2peer.services.codeGenerationService.models.application;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
import i5.las2peer.services.codeGenerationService.models.application.communicationModel.CommunicationModel;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

/**
 * 
 * Application data class. Provides a "communication-based" view on a complete CAE-generated
 * application, consisting of microservices and frontend components.
 * 
 */
public class Application {
  private String name;
  private String versionedModelId;
  private String version;
  HashMap<String, Microservice> microservices = new HashMap<>();
  HashMap<String, FrontendComponent> frontendComponents = new HashMap<>();
  HashMap<String, String> externalDependencies = new HashMap<>();


  /**
   * 
   * Creates a new application.
   * 
   * @param modelComponents an array containing the application model components
   * @param externalDependencies HashMap containing GitHub URL and version tags of external dependencies.
   * 
   * @throws ModelParseException if something goes wrong during parsing
   * 
   */
  public Application(ArrayList<SimpleModel> modelComponents, HashMap<String, String> externalDependencies) throws ModelParseException {
    this.name = modelComponents.get(0).getName();
    this.externalDependencies = externalDependencies;
    // metadata of model (currently only version)
    for (int attributeIndex = 0; attributeIndex < modelComponents.get(0).getAttributes()
        .size(); attributeIndex++) {
      if (modelComponents.get(0).getAttributes().get(attributeIndex).getName().equals("version")) {
          this.setVersion(modelComponents.get(0).getAttributes().get(attributeIndex).getValue());
      }
      if(modelComponents.get(0).getAttributes().get(attributeIndex).getName().equals("versionedModelId")) {
    	  this.versionedModelId = modelComponents.get(0).getAttributes().get(attributeIndex).getValue();
      }
      if(modelComponents.get(0).getAttributes().get(attributeIndex).getName().equals("componentName")) {
    	  this.name = modelComponents.get(0).getAttributes().get(attributeIndex).getValue();
      }
    }

    // now construct models for all components (starting with the first component, first entry of
    // array is application model itself! TODO: think of different version handling
    //TODO: Check this
    for (int i = 1; i < modelComponents.size(); i++) {
      for (int j = 0; j < modelComponents.get(i).getAttributes().size(); j++) {
        if (modelComponents.get(i).getAttributes().get(j).getName().equals("type")) {
          String type = modelComponents.get(i).getAttributes().get(j).getValue();
          switch (type) {
            case "microservice":
              if (this.microservices.containsKey(modelComponents.get(i).getName())) {
               // throw new ModelParseException(
                    //"Error: More than one microservice with name " + modelComponents[i].getName());
              } else {
              this.microservices.put(modelComponents.get(i).getName(),
                  new Microservice(modelComponents.get(i)));
              }
              break;
            case "frontend-component":
              if (this.frontendComponents.containsKey(modelComponents.get(i).getName())) {
                //throw new ModelParseException("Error in Application constructor: More than one frontend-component with name "
                    //+ modelComponents[i].getName());
              }else {
              this.frontendComponents.put(modelComponents.get(i).getName(),
                  new FrontendComponent(modelComponents.get(i)));
              }
              break;
            default:
              throw new ModelParseException(
                  "Error: Application can only consist of microservices and frontend components!");
          }
        }
      }
    }
  }


  public String getVersion() {
    return this.version;
  }


  public void setVersion(String version) {
    this.version = version;
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


  public HashMap<String, Microservice> getMicroservices() {
    return this.microservices;
  }


  public void setMicroservices(HashMap<String, Microservice> microservices) {
    this.microservices = microservices;
  }


  public HashMap<String, FrontendComponent> getFrontendComponents() {
    return this.frontendComponents;
  }


  public void setFrontendComponents(HashMap<String, FrontendComponent> frontendComponents) {
    this.frontendComponents = frontendComponents;
  }
  
  public HashMap<String, String> getExternalDependencies() {
	  return this.externalDependencies;
  }


  public SimpleModel toCommunicationModel() {
    CommunicationModel commViewModel = new CommunicationModel(this.name, this.version,
        this.microservices, this.frontendComponents);
    return commViewModel.toSimpleModel();
  }
}
