package i5.las2peer.services.codeGenerationService.models.traceModel;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import i5.las2peer.services.codeGenerationService.traces.segments.SegmentFactory;

/**
 * A factory class to create file trace models from json objects
 * 
 * @author Thomas Winkler
 *
 */

public class FileTraceModelFactory {

  public static FileTraceModel createFileTraceModelFromJSON(String source, String traceSource,
      TraceModel traceModel, String fileName) {

    FileTraceModel fileTraceModel = new FileTraceModel(traceModel, fileName);
    JSONParser parser = new JSONParser();
    JSONObject jobj;

    try {
      jobj = (JSONObject) parser.parse(traceSource);
      JSONArray segments = (JSONArray) jobj.get("traceSegments");

      fileTraceModel.addSegments(SegmentFactory.createSegments(segments, source, 0L));
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return fileTraceModel;
  }

}
