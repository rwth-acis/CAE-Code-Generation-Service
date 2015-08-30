package i5.las2peer.services.codeGenerationService.models.application.communicationModel;

import i5.las2peer.services.codeGenerationService.models.frontendComponent.HtmlElement;

/**
 * 
 * Collaborative Element entity of the CAE Communication View Model.
 * 
 * Currently, all entities of this model only have a label, besides the version attribute of the
 * application environment entity.
 * 
 */
public class CommCollaborativeElement {

  private String label;

  public CommCollaborativeElement(HtmlElement element) {
    this.label = element.getId();
  }


  public String getLabel() {
    return this.label;
  }

}
