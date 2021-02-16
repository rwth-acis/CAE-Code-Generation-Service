package i5.las2peer.services.codeGenerationService.models.traceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A global trace model that provides the information about which model elements are located in
 * which files
 * 
 * @author Thomas Winkler
 *
 */

public class TraceModel {

  private Map<String, FileTraceModel> filenameToFileTraceModel =
      new HashMap<String, FileTraceModel>();
  private Map<String, List<String>> modelIdToFilenames = new HashMap<String, List<String>>();
  private String randomId;

  public TraceModel() {
    // generate a random id to distinguish between different code generations
    UUID id = UUID.randomUUID();
    this.randomId = id.toString();
  }

  /**
   * Add a file trace model the global trace model
   * 
   * @param fileTraceModel The file trace model to add
   */

  public void addFileTraceModel(FileTraceModel fileTraceModel) {
    this.filenameToFileTraceModel.put(fileTraceModel.getFileName(), fileTraceModel);
  }

  /**
   * Get the map containing all traced files
   * 
   * @return The map containing the traced files and their file trace model
   */

  public Map<String, FileTraceModel> getFilenameToFileTraceModelMap() {
    return this.filenameToFileTraceModel;
  }

  /**
   * Generates a JSON representation of the trace model
   * 
   * @return A corresponding JSON Object of the trace model
   */

  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    JSONObject jObj = new JSONObject();
    JSONArray jArray = new JSONArray();
    JSONObject jModel = new JSONObject();

    for (String id : this.filenameToFileTraceModel.keySet()) {
      jArray.add(id);
    }

    for (String modelId : this.modelIdToFilenames.keySet()) {
      List<String> fileList = this.modelIdToFilenames.get(modelId);
      JSONObject jModelObject = new JSONObject();

      JSONArray jFilesArray = new JSONArray();
      jModelObject.put("files", jFilesArray);

      for (String file : fileList) {
        jFilesArray.add(file);
      }

      jModel.put(modelId, jModelObject);
    }

    jObj.put("id", this.randomId);
    jObj.put("tracedFiles", jArray);
    jObj.put("modelsToFile", jModel);
    return jObj;
  }

  /**
   * Add trace information about which model element belongs to the given file name
   * 
   * @param modelId The id of the model element the file name belongs to
   * @param fileName The file name to which the model element belongs o
   */

  public void addTrace(String modelId, String fileName) {

    if (!this.modelIdToFilenames.containsKey(modelId)) {
      List<String> fileList = new ArrayList<String>();
      this.modelIdToFilenames.put(modelId, fileList);
    }

    List<String> fileList = this.modelIdToFilenames.get(modelId);
    fileList.add(fileName);
  }
}
