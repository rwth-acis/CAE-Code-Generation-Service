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

  /**
   * Create a {@link i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel}
   * for a file based on its source code and trace information
   * 
   * @param source The source code of the file
   * @param fileTraces The trace information of the file
   * @param traceModel A global trace model
   * @param fileName The file name
   * @return The created file trace model for the file
   */

  public static FileTraceModel createFileTraceModelFromJSON(String source, JSONObject fileTraces,
      TraceModel traceModel, String fileName) {

    FileTraceModel fileTraceModel = new FileTraceModel(traceModel, fileName);

    JSONArray segments = (JSONArray) fileTraces.get("traceSegments");
    fileTraceModel.addSegments(SegmentFactory.createSegments(segments, source, 0L));

    return fileTraceModel;
  }

}
