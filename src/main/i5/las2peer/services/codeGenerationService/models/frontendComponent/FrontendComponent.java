package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import i5.cae.simpleModel.SimpleModel;

public class FrontendComponent {
  private String name;

  public FrontendComponent(SimpleModel model) {
    this.name = model.getName();
  }

  public String getName() {
    return name;
  }

}
