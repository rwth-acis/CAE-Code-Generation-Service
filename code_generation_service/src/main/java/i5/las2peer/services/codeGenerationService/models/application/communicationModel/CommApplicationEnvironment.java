package i5.las2peer.services.codeGenerationService.models.application.communicationModel;

/**
 * 
 * Application Environment entity of the CAE Communication View Model.
 * 
 * Currently, all entities of this model only have a label, besides the version attribute of the
 * application environment entity.
 * 
 */
public class CommApplicationEnvironment {
  private String label;
  private String version;


  public CommApplicationEnvironment(String label, String version) {
    this.label = label;
    this.version = version;
  }


  public String getLabel() {
    return this.label;
  }


  public String getVersion() {
    return this.version;
  }

}
