package i5.las2peer.services.codeGenerationService.models.microservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.SimpleModel;
import i5.cae.simpleModel.edge.SimpleEdge;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;

/**
 * 
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
  private String version;
  private String metadataDocString;

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
    Map<String, HttpPayload> tempHttpPayloads = new HashMap<String, HttpPayload>();
    Map<String, HttpResponse> tempHttpResponses = new HashMap<String, HttpResponse>();
    Map<String, InternalCall> tempInternalCalls = new HashMap<String, InternalCall>();
    Map<String, InternalCallParam> tempInternalCallParameters =
        new HashMap<String, InternalCallParam>();

    this.name = model.getName();

    // metadata of model (currently only version)
    for (int attributeIndex = 0; attributeIndex < model.getAttributes().size(); attributeIndex++) {
      if (model.getAttributes().get(attributeIndex).getName().equals("version")) {
        try {
          this.setVersion(model.getAttributes().get(attributeIndex).getValue());
        }/* catch (NumberFormatException e) {
          throw new ModelParseException("Microservice version is not a number!");
        } */ catch(Exception e) {
        	throw new ModelParseException("Something is wrong with the version number!");
        }
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
                if (this.resourceName.contains(" ")) {
                  throw new ModelParseException(
                      "Resource name contains invalid characters: " + this.resourceName);
                }
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
        case "HTTP Payload":
          tempHttpPayloads.put(node.getId(), new HttpPayload(node));
          break;
        case "HTTP Response":
          tempHttpResponses.put(node.getId(), new HttpResponse(node));
          break;
        case "Internal Service Call":
          tempInternalCalls.put(node.getId(), new InternalCall(node));
          break;
        case "Service Call Parameter":
          tempInternalCallParameters.put(node.getId(), new InternalCallParam(node));
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
        case "Parameter to Internal Service Call":
          if (tempInternalCalls.containsKey(currentEdgeSource)
              && tempInternalCallParameters.containsKey(currentEdgeTarget)) {
            // Add parameter to internal call
            tempInternalCalls.get(currentEdgeSource)
                .addInternalCallParam(tempInternalCallParameters.get(currentEdgeTarget));
            tempInternalCallParameters.remove(currentEdgeTarget);
            // check if the internal call parameter is (at least) in temp list and internal call
            // itself was already added to http method
          } else if (tempInternalCallParameters.containsKey(currentEdgeTarget)) {
            for (HttpMethod method : this.httpMethods.values()) {
              for (InternalCall call : method.getInternalCalls()) {
                if (call.getModelId().equals(currentEdgeSource)) {
                  call.addInternalCallParam(tempInternalCallParameters.get(currentEdgeTarget));
                  tempInternalCallParameters.remove(currentEdgeTarget);
                  break;
                }
              }
            }
          } else {
            throw new ModelParseException("Wrong Parameter to Internal Service Call edge!");
          }
          break;
        case "Internal Call":
          if (this.httpMethods.containsKey(currentEdgeSource)
              && tempInternalCalls.containsKey(currentEdgeTarget)) {
            this.httpMethods.get(currentEdgeSource)
                .addInternalCall(tempInternalCalls.get(currentEdgeTarget));
            tempInternalCalls.remove(currentEdgeTarget);
          }
          break;
        case "RESTful Resource to HTTP Method":
          if (!this.microserviceModelId.equals(currentEdgeSource)
              || !this.httpMethods.containsKey(currentEdgeTarget)) {
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
        // add payload to http method and remove from temp payloads if edge was validated
        // successfully
        case "HTTP Method to HTTP Payload":
          HttpMethod currentHttpMethod = this.httpMethods.get(currentEdgeSource);
          HttpPayload currentHttpPayload = tempHttpPayloads.get(currentEdgeTarget);
          String httpPayloadId = currentEdgeTarget;
          if (currentHttpMethod == null || currentHttpPayload == null) {
            throw new ModelParseException("Wrong HTTP Method to HTTP Payload Edge!");
          }
          currentHttpMethod.addHttpPayload(currentHttpPayload);
          currentHttpMethod.addNodeIdPayload(httpPayloadId, currentHttpPayload);
          tempHttpPayloads.remove(currentEdgeTarget);
          break;
        // add response to http method and remove from temp responses if edge was validated
        // successfully
        case "HTTP Method to HTTP Response":
        
        	if(tempHttpResponses.isEmpty()) {
        		throw new ModelParseException("There are no HTTP Responses left, an edge might be duplicated! From: " +
        				this.httpMethods.get(currentEdgeSource).getName());
        	}
        	
          currentHttpMethod = this.httpMethods.get(currentEdgeSource);
          HttpResponse currentHttpResponse = tempHttpResponses.get(currentEdgeTarget);
          String httpResponseId = currentEdgeTarget;
          
          if (currentHttpMethod == null || currentHttpResponse == null) {
            throw new ModelParseException("Wrong HTTP Method to HTTP Response Edge!");
          }

          currentHttpMethod.addHttpResponse(currentHttpResponse);
          currentHttpMethod.addNodeIdResponse(httpResponseId, currentHttpResponse);
          tempHttpResponses.remove(currentEdgeTarget);
          break;
        default:
          throw new ModelParseException("Unknown microservice edge type: " + currentEdgeType);
      }
    }

    // check for correct edge counts
    if (!(httpMethodToResourceEdges == this.httpMethods.size())) {
      throw new ModelParseException("Not enough http method to resource edges with http methods size. Model is not fully connected!");
    }
    // check for correct edge counts
    if (!(tableToDatabaseEdges == tempTables.size())) {
      throw new ModelParseException("Not enough table to database edges. Model is not fully connected!");
    }
    // check database edges
    if ((!databaseToResourceEdge && this.database != null)) {
      throw new ModelParseException("No database to resource edge and database is not null. Model is not fully connected!");
    }
    // check if all columns were correctly connected to a table
    if (!tempColumns.isEmpty()) {
      throw new ModelParseException("All columns must be connected to a table!");
    }
    // check if all payloads were correctly connected to an http method
    if (!tempHttpPayloads.isEmpty()) {
      throw new ModelParseException("All http payloads must be connected to an http method!");
    }
    // check, if all responses were correctly connected to an http method
    if (!tempHttpResponses.isEmpty()) {
      throw new ModelParseException("All http responses must be connected to an http method!");
    }
    // check, if all internal call parameters were correctly connected to an internal call
    if (!tempInternalCallParameters.isEmpty()) {
      throw new ModelParseException("All call parameters must be connected to an internal call!");
    }
    // check, if all internal calls were correctly connected to an http method
    if (!tempInternalCalls.isEmpty()) {
      throw new ModelParseException("All internal calls must be connected to an http method!");
    }
    // give the http methods the signal that they can check their payloads and responses
    for (Map.Entry<String, HttpMethod> httpMethod : httpMethods.entrySet()) {
      httpMethod.getValue().checkPayloadAndResponses();
    }
    // finally, give tables the signal that they can check their columns for correctness
    for (Map.Entry<String, Table> tempTable : tempTables.entrySet()) {
      tempTable.getValue().checkColumns();
    }
    // if that has worked, add them to database (if database exists;-) )
    if (this.database != null) {
      this.database.addTables((Table[]) tempTables.values().toArray(new Table[tempTables.size()]));
    }
  }


  public void setMicroserviceModelId(String microserviceModelId) {
    this.microserviceModelId = microserviceModelId;
  }


  public String getMicroserviceModelId() {
    return microserviceModelId;
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


  public String getVersion() {
    return this.version;
  }


  public void setVersion(String version) {
    this.version = version;
  }

  public void setMetadataDocString(String metadataDocString) {
    this.metadataDocString = metadataDocString;
  }

  public String getMetadataDocString() {
    return this.metadataDocString;
  } 

}
