package i5.las2peer.services.codeGenerationService.models.microservice;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;

public class Column {
  private String modelId;
  private String name;
  private String type;
  private boolean primaryKey;

  public Column(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "name":
          this.name = attribute.getValue();
          break;
        case "type":
          this.type = attribute.getValue();
          break;
        case "primaryKey":
          this.primaryKey = Boolean.parseBoolean(attribute.getValue());
          break;
        default:
          throw new ModelParseException("Unknown Column attribute: " + attribute.getName());
      }
    }
  }

  public String getModelId() {
    return modelId;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public boolean isPrimaryKey() {
    return primaryKey;
  }

}
