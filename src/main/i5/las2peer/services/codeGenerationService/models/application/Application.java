package i5.las2peer.services.codeGenerationService.models.application;

import java.io.Serializable;
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
  private float version;
  HashMap<String, Microservice> microservices = new HashMap<String, Microservice>();
  HashMap<String, FrontendComponent> frontendComponents = new HashMap<String, FrontendComponent>();


  /**
   * 
   * Creates a new application.
   * 
   * @param serializedModelComponents an array containing the application model components
   * 
   * @throws ModelParseException if something goes wrong during parsing
   * 
   */
  public Application(Serializable[] serializedModelComponents) throws ModelParseException {
    SimpleModel[] modelComponents = (SimpleModel[]) serializedModelComponents;

    this.name = modelComponents[0].getName();
    // metadata of model (currently only version)
    for (int attributeIndex = 0; attributeIndex < modelComponents[0].getAttributes()
        .size(); attributeIndex++) {
      if (modelComponents[0].getAttributes().get(attributeIndex).getName().equals("version")) {
        try {
          this.setVersion(
              Float.parseFloat(modelComponents[0].getAttributes().get(attributeIndex).getValue()));
        } catch (NumberFormatException e) {
          throw new ModelParseException("Application version is not a number!");
        }
      }
    }

    // now construct models for all components (starting with the first component, first entry of
    // array is application model itself! TODO: think of different version handling
    for (int i = 1; i < modelComponents.length; i++) {
      for (int j = 0; j < modelComponents[i].getAttributes().size(); j++) {
        if (modelComponents[i].getAttributes().get(j).getName().equals("type")) {
          String type = modelComponents[i].getAttributes().get(j).getValue();
          switch (type) {
            case "microservice":
              if (this.microservices.containsKey(modelComponents[i].getName())) {
                throw new ModelParseException(
                    "Error: More than one microservice with name " + modelComponents[i].getName());
              }
              this.microservices.put(modelComponents[i].getName(),
                  new Microservice(modelComponents[i]));
              break;
            case "frontend-component":
              if (this.frontendComponents.containsKey(modelComponents[i].getName())) {
                throw new ModelParseException("Error: More than one frontend-component with name "
                    + modelComponents[i].getName());
              }
              this.frontendComponents.put(modelComponents[i].getName(),
                  new FrontendComponent(modelComponents[i]));
              break;
            default:
              throw new ModelParseException(
                  "Error: Application can only consist of microservices and frontend components!");
          }
        }
      }
    }
  }


  public float getVersion() {
    return this.version;
  }


  public void setVersion(float version) {
    this.version = version;
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


  public SimpleModel toCommunicationModel() {
    CommunicationModel commViewModel = new CommunicationModel(this.name, this.version,
        this.microservices, this.frontendComponents);
    return commViewModel.toSimpleModel();
  }
}
