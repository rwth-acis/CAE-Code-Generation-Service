package i5.las2peer.services.codeGenerationService.models.microservice;

import java.util.Map;

import i5.cae.simpleModel.node.SimpleNode;

public class Database {
  private String modelId = null;

  public Database(SimpleNode node) {
    this.modelId = node.getId();
  }

  public String getModelId() {
    return this.modelId;
  }

  public void addTables(Map<String, Table> tempTables) {
    // TODO Auto-generated method stub

  }

}
