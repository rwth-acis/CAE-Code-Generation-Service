package i5.las2peer.services.codeGenerationService.models.application.communicationModel;

import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

/**
 * 
 * Restful Resource entity of the CAE Communication View Model.
 * 
 * Currently, all entities of this model only have a label, besides the version attribute of the
 * application environment entity.
 * 
 */
public class CommRestfulResource {

  private String label;

  public CommRestfulResource(Microservice microservice) {
    this.label = microservice.getResourceName();
  }


  public String getLabel() {
    return this.label;
  }

}
