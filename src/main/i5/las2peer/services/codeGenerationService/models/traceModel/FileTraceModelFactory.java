package i5.las2peer.services.codeGenerationService.models.traceModel;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import i5.las2peer.services.codeGenerationService.traces.segments.SegmentFactory;

/**
 * A factory class to create file trace models from json objects
 * 
 * @author Thomas Winkler
 *
 */

public class FileTraceModelFactory {

  public static FileTraceModel createFileTraceModelFromJSON(String source, JSONObject fileTraces,
      TraceModel traceModel, String fileName) {

    FileTraceModel fileTraceModel = new FileTraceModel(traceModel, fileName);

    JSONArray segments = (JSONArray) fileTraces.get("traceSegments");
    fileTraceModel.addSegments(SegmentFactory.createSegments(segments, source, 0L));

    return fileTraceModel;
  }

}
