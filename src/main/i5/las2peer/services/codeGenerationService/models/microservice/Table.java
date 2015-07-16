package i5.las2peer.services.codeGenerationService.models.microservice;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;

public class Table {

  private String modelId;
  private String name;

  public Table(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "name":
          this.name = attribute.getValue();
          break;
        default:
          throw new ModelParseException("Unknown Table attribute: " + attribute.getName());
      }
    }
  }

  public String getModelId() {
    return modelId;
  }

  public String getName() {
    return name;
  }

  public void checkColumns() throws ModelParseException {
    // TODO Auto-generated method stub
  }

  public void addColumn(Column column) {
    // TODO Auto-generated method stub
  }

}
