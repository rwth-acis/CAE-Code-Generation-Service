package i5.las2peer.services.codeGenerationService.models.microservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.SimpleModel;
import i5.cae.simpleModel.edge.SimpleEdge;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;

/**
 * Microservice data class. Currently, edges are only used for creating simple 1 to 1 dependencies
 * between objects, without any attributes added to them.
 * 
 */
public class Microservice {

  // TODO: create sanity checks for variable content of all entries (non-empty values for example..)

  // id of resource node in SyncMeta, needed for model correctness checking
  private String microserviceModelId;
  private String name; // name of complete service "construct"
  private String resourceName; // name of concrete las2peer service
  private String path;
  private String developer;
  private Map<String, HttpMethod> httpMethods;
  private Database database;
  private long version;

  /**
   * 
   * Creates a new microservice.
   * 
   * @param model a {@link i5.cae.simpleModel.SimpleModel} containing the microservice
   * 
   * @throws ModelParseException if something goes wrong during parsing
   * 
   */
  public Microservice(SimpleModel model) throws ModelParseException {
    this.httpMethods = new HashMap<String, HttpMethod>();

    // used for checking node to edge dependencies for correctness
    Map<String, Table> tempTables = new HashMap<String, Table>();
    Map<String, Column> tempColumns = new HashMap<String, Column>();

    this.name = model.getName();

    // metadata of model (currently only version)
    for (int attributeIndex = 0; attributeIndex < model.getAttributes().size(); attributeIndex++) {
      if (model.getAttributes().get(attributeIndex).getName().equals("version")) {
        this.setVersion(Long.getLong(model.getAttributes().get(attributeIndex).getValue()));
      }
    }

    // go through the nodes and create objects
    ArrayList<SimpleNode> nodes = model.getNodes();
    for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
      SimpleNode node = nodes.get(nodeIndex);
      ArrayList<SimpleEntityAttribute> nodeAttributes = node.getAttributes();
      switch (node.getType()) {
        case "RESTful Resource":

          if (this.microserviceModelId == null) {
            this.microserviceModelId = node.getId();
          } else {
            throw new ModelParseException("More than one RESTful Resource in microservice model");
          }
          for (int attributeIndex = 0; attributeIndex < nodeAttributes.size(); attributeIndex++) {
            SimpleEntityAttribute attribute = nodeAttributes.get(attributeIndex);
            switch (attribute.getName()) {
              case "name":
                this.resourceName = attribute.getValue();
                break;
              case "path":
                this.path = attribute.getValue();
                break;
              case "developer":
                this.developer = attribute.getValue();
                break;
              default:
                throw new ModelParseException(
                    "Unknown attribute typ of RESTfulResource: " + attribute.getName());
            }
          }
          break;
        case "HTTP Method":
          httpMethods.put(node.getId(), new HttpMethod(node));
          break;
        case "Database":
          if (this.database == null) {
            this.database = new Database(node);
          } else {
            throw new ModelParseException("More than one Database in microservice model");
          }
          break;
        case "Table":
          tempTables.put(node.getId(), new Table(node));
          break;
        case "Column":
          tempColumns.put(node.getId(), new Column(node));
          break;
        default:
          throw new ModelParseException("Unknown node type: " + node.getType());
      }
    }

    // now to the edges

    // used to determine if the model is connected (assumed that SyncMeta does not allow multiple
    // edges between the same objects)
    int httpMethodToResourceEdges = 0;
    int tableToDatabaseEdges = 0;
    boolean databaseToResourceEdge = false;

    ArrayList<SimpleEdge> edges = model.getEdges();
    for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
      String currentEdgeSource = edges.get(edgeIndex).getSourceNode();
      String currentEdgeTarget = edges.get(edgeIndex).getTargetNode();
      String currentEdgeType = edges.get(edgeIndex).getType();

      switch (currentEdgeType) {
        case "Internal Call":
          if (httpMethods.containsKey(currentEdgeSource)
              && httpMethods.containsKey(currentEdgeTarget)
              && !currentEdgeSource.equals(currentEdgeTarget)) {
            // add call to source method
            httpMethods.get(currentEdgeSource).addInternalCall(currentEdgeTarget);
          } else {
            throw new ModelParseException("Internal call reference broken!");
          }
          break;
        case "RESTful Resource to HTTP Method":
          if (!this.microserviceModelId.equals(currentEdgeSource)
              || !httpMethods.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong RESTful Resource to HTTP Method edge!");
          } else {
            httpMethodToResourceEdges++;
          }
          break;
        case "RESTful Resource to Database":
          if (!this.microserviceModelId.equals(currentEdgeSource)
              || !this.database.getModelId().equals(currentEdgeTarget)) {
            throw new ModelParseException("Wrong RESTful Resource to Database edge!");
          } else {
            databaseToResourceEdge = true;
          }
          break;
        case "Database to Table":
          if (!this.database.getModelId().equals(currentEdgeSource)
              || !tempTables.containsKey(currentEdgeTarget)) {
            throw new ModelParseException("Wrong Database to Table edge!");
          } else {
            tableToDatabaseEdges++;
          }
          break;
        // add column to table and remove from tempColumns if edge was validated successfully
        case "Table to Column":
          Table currentTable = tempTables.get(currentEdgeSource);
          Column currentColumn = tempColumns.get(currentEdgeTarget);
          if (currentTable == null || currentColumn == null) {
            throw new ModelParseException("Wrong Table to Column edge!");
          }
          currentTable.addColumn(currentColumn);
          tempColumns.remove(currentEdgeTarget);
          break;
        default:
          throw new ModelParseException("Unknown edge type: " + currentEdgeType);
      }
    }

    // check for correct edge counts
    if (!(httpMethodToResourceEdges == this.httpMethods.size()) || !databaseToResourceEdge
        || !(tableToDatabaseEdges == tempTables.size())) {
      throw new ModelParseException("Model is not fully connected!");
    }

    // check if all columns were correctly connected to a table
    if (!tempColumns.isEmpty()) {
      throw new ModelParseException("All columns must be connected to a table!");
    }

    // finally, give tables the signal that they can check their columns for correctness
    for (Map.Entry<String, Table> tempTable : tempTables.entrySet()) {
      tempTable.getValue().checkColumns();
    }
    // if that has worked, add them to database
    this.database.addTables((Table[]) tempTables.values().toArray(new Table[tempTables.size()]));
  }


  public String getName() {
    return this.name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public String getPath() {
    return this.path;
  }


  public void setPath(String path) {
    this.path = path;
  }


  public String getDeveloper() {
    return this.developer;
  }


  public void setDeveloper(String developer) {
    this.developer = developer;
  }


  public Map<String, HttpMethod> getHttpMethods() {
    return this.httpMethods;
  }


  public void setHttpMethods(Map<String, HttpMethod> httpMethods) {
    this.httpMethods = httpMethods;
  }


  public Database getDatabase() {
    return this.database;
  }


  public void setDatabase(Database database) {
    this.database = database;
  }


  public String getResourceName() {
    return this.resourceName;
  }


  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }


  public long getVersion() {
    return version;
  }


  public void setVersion(long version) {
    this.version = version;
  }


}
