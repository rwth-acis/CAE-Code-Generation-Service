package i5.las2peer.services.codeGenerationService.models.application.communicationModel;

import i5.las2peer.services.codeGenerationService.models.frontendComponent.IWCCall;

/**
 * 
 * IWC Call entity of the CAE Communication View Model.
 * 
 * Currently, all entities of this model only have a label, besides the version attribute of the
 * application environment entity.
 * 
 */
public class CommIWCCall {

  private String label;

  public CommIWCCall(IWCCall iwcCall) {
    this.label = iwcCall.getIntentAction();
  }


  public String getLabel() {
    return this.label;
  }

}
