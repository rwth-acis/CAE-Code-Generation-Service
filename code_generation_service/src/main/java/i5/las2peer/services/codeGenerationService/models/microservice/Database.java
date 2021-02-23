package i5.las2peer.services.codeGenerationService.models.microservice;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;

/**
 * 
 * Database data class. Represents a database, which is part of a microservice model.
 *
 */
public class Database {
  private String modelId = null;
  private String name;
  private String address;
  private String schema;
  private String loginName;
  private String loginPassword;
  private Table[] tables;

  /**
   * 
   * Creates a new {@link Database}.
   * 
   * @param node a {@link i5.cae.simpleModel.node.SimpleNode} containing the database
   *        representation.
   * 
   * @throws ModelParseException if something goes wrong during parsing the node
   * 
   */
  public Database(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "name":
          this.name = attribute.getValue();
          break;
        case "address":
          this.address = attribute.getValue();
          break;
        case "schema":
          this.schema = attribute.getValue();
          break;
        case "loginName":
          this.loginName = attribute.getValue();
          break;
        case "loginPassword":
          this.loginPassword = attribute.getValue();
          break;
        default:
          throw new ModelParseException("Unknown Database attribute: " + attribute.getName());
      }
    }
  }


  public String getModelId() {
    return this.modelId;
  }


  public String getName() {
    return this.name;
  }


  public String getAddress() {
    return address;
  }


  public String getSchema() {
    return this.schema;
  }


  public String getLoginName() {
    return this.loginName;
  }


  public String getLoginPassword() {
    return this.loginPassword;
  }


  public Table[] getTables() {
    return this.tables;
  }


  public void addTables(Table[] tables) {
    this.tables = tables;
  }

}
