package i5.las2peer.services.codeGenerationService.models.application.communicationModel;

import i5.las2peer.services.codeGenerationService.models.frontendComponent.IWCResponse;

/**
 * 
 * IWC Response entity of the CAE Communication View Model.
 * 
 * Currently, all entities of this model only have a label, besides the version attribute of the
 * application environment entity.
 * 
 */
public class CommIWCResponse {

  private String label;

  public CommIWCResponse(IWCResponse iwcResponse) {
    this.label = iwcResponse.getIntentAction();
  }


  public String getLabel() {
    return this.label;
  }

}
