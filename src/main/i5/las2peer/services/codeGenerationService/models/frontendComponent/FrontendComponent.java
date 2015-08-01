package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;

/**
 * 
 * FrontendComponent data class. Currently, edges are only used for creating simple 1 to 1
 * dependencies between objects, without any attributes added to them.
 * 
 */
public class FrontendComponent {
  private String name;
  private float version;


  /**
   * 
   * Creates a new frontend component.
   * 
   * @param model a {@link i5.cae.simpleModel.SimpleModel} containing the frontend component
   * 
   * @throws ModelParseException if something goes wrong during parsing
   * 
   */
  public FrontendComponent(SimpleModel model) throws ModelParseException {
    this.name = model.getName();

    // metadata of model (currently only version)
    for (int attributeIndex = 0; attributeIndex < model.getAttributes().size(); attributeIndex++) {
      if (model.getAttributes().get(attributeIndex).getName().equals("version")) {
        this.setVersion(Float.parseFloat(model.getAttributes().get(attributeIndex).getValue()));
      }
    }

  }


  public String getName() {
    return this.name;
  }


  public float getVersion() {
    return this.version;
  }


  public void setVersion(float version) {
    this.version = version;
  }
}
