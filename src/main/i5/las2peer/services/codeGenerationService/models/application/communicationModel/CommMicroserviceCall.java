package i5.las2peer.services.codeGenerationService.models.application.communicationModel;

import i5.las2peer.services.codeGenerationService.models.frontendComponent.MicroserviceCall;

/**
 * 
 * Microservice Call entity of the CAE Communication View Model.
 * 
 * Currently, all entities of this model only have a label, besides the version attribute of the
 * application environment entity.
 * 
 */
public class CommMicroserviceCall {

  private String label;

  public CommMicroserviceCall(MicroserviceCall microserviceCall) {
    this.label = microserviceCall.getPath();
    // add a "/" to empty paths
    if (this.label.length() == 0) {
      this.label = "/";
    }
  }


  public String getLabel() {
    return this.label;
  }

}
