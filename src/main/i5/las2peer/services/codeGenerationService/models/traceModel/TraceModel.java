package i5.las2peer.services.codeGenerationService.models.traceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Global trace model that provides the information about which model elements are located in which
 * files
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


  public void addFileTraceModel(String fileName, FileTraceModel fileTraceModel) {
    this.filenameToFileTraceModel.put(fileName, fileTraceModel);
  }

  public List<String> getFilenameListByModelId(String modelId) {
    if (this.modelIdToFilenames.containsKey(modelId)) {
      return this.modelIdToFilenames.get(modelId);
    } else {
      List<String> list = new ArrayList<String>();
      this.modelIdToFilenames.put(modelId, list);
      return list;
    }
  }

  public void addModelElement(String modelId, String fileName, int position) {
    List<String> list = this.getFilenameListByModelId(modelId);

    // if position is lower than 0, we put the new filename at the end of the list
    if (position < 0) {
      position = list.size() - 1;
    }

    // only add a filename once
    if (!list.contains(fileName)) {
      list.add(position, fileName);
    }

  }

  public void addModelElement(String modelId, String fileName) {
    // put the new filename at the end of the list
    this.addModelElement(modelId, fileName, -1);
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
    for (String id : this.filenameToFileTraceModel.keySet()) {
      jArray.add(id);
    }
    jObj.put("id", this.randomId);
    jObj.put("tracedFiles", jArray);
    return jObj;
  }
}
