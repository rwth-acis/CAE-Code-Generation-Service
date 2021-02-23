package i5.las2peer.services.codeGenerationService.models.application.communicationModel;

/**
 * 
 * Other Service entity of the CAE Communication View Model.
 * 
 * Currently, all entities of this model only have a label, besides the version attribute of the
 * application environment entity.
 * 
 */
public class CommOtherService {

  private String label;

  public CommOtherService(String serviceName) {
    this.label = serviceName;
  }

  public String getLabel() {
    return this.label;
  }

}
