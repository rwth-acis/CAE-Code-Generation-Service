package i5.las2peer.services.codeGenerationService.traces.segments;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * A factory class to create new compositions of segments by given traces and source code. The
 * compositions are then used for templates.
 * 
 * @author Thomas Winkler
 *
 */

public class CompositeSegmentFactory {
  public static Segment createSegment(JSONObject entry, String id, String content) {
    ContentSegment segment = null;
    String type = (String) entry.get("type");

    if (type.equals("protected")) {
      segment = new ProtectedSegment(id);
      segment.setContent(content);
    } else if (type.equals("unprotected")) {
      segment = new UnprotectedSegment(id);
      segment.setContent(content);

      boolean integrityCheck = (boolean) entry.get("integrityCheck");
      if (integrityCheck) {
        // enable integrity check, but we cannot set the hash value yet
        ((UnprotectedSegment) segment).setHash(null);
      }

    }
    return segment;
  }

  public static CompositeSegment createByTraces(String id, String traces, String source) {
    JSONParser parser = new JSONParser();
    CompositeSegment cS = new CompositeSegment(id);

    try {
      Object obj = parser.parse(traces);
      JSONObject jobj = (JSONObject) obj;
      JSONArray jarray = (JSONArray) jobj.get("traceSegments");
      Long start = 0L;
      Pattern pattern = Pattern.compile("(\\$([a-zA-Z_]*)\\$)");

      for (int i = 0; i < jarray.size(); i++) {
        JSONObject entry = (JSONObject) jarray.get(i);
        Long length = (Long) entry.get("length");
        String idSuffix = (String) entry.get("id");
        String sourcePart =
            source.substring(Math.toIntExact(start), Math.toIntExact(start + length));
        Matcher matcher = pattern.matcher(sourcePart);

        // check for variable name
        if (matcher.find()) {
          // if a variable name was found, we need to check if a segment for that specific variable
          // has already been created
          if (!cS.hasChild(id + ":" + matcher.group(1))) {
            cS.addSegment(createSegment(entry, id + ":" + matcher.group(1), sourcePart));
          } else {
            cS.addSegment(cS.getChild(id + ":" + matcher.group(1)));
          }
        } else {
          // a variable name was not found, so we use the idSuffix
          cS.addSegment(createSegment(entry, id + ":" + idSuffix, sourcePart));
        }

        start += length;
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return cS;

  }

}
