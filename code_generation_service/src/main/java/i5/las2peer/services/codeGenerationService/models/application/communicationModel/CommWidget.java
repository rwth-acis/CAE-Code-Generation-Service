package i5.las2peer.services.codeGenerationService.models.application.communicationModel;

import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;

public class CommWidget {

  private String label;

  public CommWidget(FrontendComponent frontendComponent) {
    this.label = frontendComponent.getWidgetName();
  }


  public String getLabel() {
    return this.label;
  }
}
