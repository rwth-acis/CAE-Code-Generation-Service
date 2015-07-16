package i5.las2peer.services.codeGenerationService.models.microservice;

import java.util.ArrayList;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;

public class Table {

  private String modelId;
  private String name;
  private ArrayList<Column> columns = new ArrayList<Column>();

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
    if (this.columns.isEmpty()) {
      throw new ModelParseException("Table " + this.name + " contains no columns!");
    }
    boolean onePrimaryKey = false;
    for (int columnIndex = 0; columnIndex < this.columns.size(); columnIndex++) {
      if (columns.get(columnIndex).isPrimaryKey()) {
        if (!onePrimaryKey) {
          onePrimaryKey = true;
        } else {
          throw new ModelParseException("More than one primary key in table " + this.name);
        }
      }
    }
    if (!onePrimaryKey) {
      throw new ModelParseException("No primary key in table " + this.name);
    }
  }

  public void addColumn(Column column) {
    this.columns.add(column);
  }

}
