package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import i5.las2peer.services.codeGenerationService.traces.segments.ContentSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import i5.las2peer.services.codeGenerationService.traces.segments.UnprotectedSegment;

/**
 * The guidance model class performs the actual model check and generates corresponding guidances if
 * some breaking code was found
 * 
 * @author Thomas Winkler
 *
 */

public class GuidanceModel {

  private Map<String, Guidance> guidanceMap = new HashMap<String, Guidance>();

  /**
   * Adds a single guidance to the model
   * 
   * @param guidance The guidance to add
   */
  private void addGuidance(Guidance guidance) {
    this.guidanceMap.put(guidance.getType(), guidance);
  }

  /**
   * Parses and adds the guidances of the given json file to the model
   * 
   * @param guidancesString the json file containing the guidances to add
   */
  public void addGuidances(String guidancesString) {
    JSONParser parser = new JSONParser();
    JSONObject jobj;
    try {
      jobj = (JSONObject) parser.parse(guidancesString);
      JSONArray guidances = (JSONArray) jobj.get("guidances");
      for (int i = 0; i < guidances.size(); i++) {
        JSONObject guidance = (JSONObject) guidances.get(i);
        this.addGuidance(new Guidance(guidance));
      }
    } catch (ParseException e) {
      e.printStackTrace();
    }

  }

  /**
   * Performs the model check and creates corresponding guidances if breaking code is found
   * 
   * @param type The type of the model element
   * @param segments The segments whose content should be checked
   * @return A list of guidances as json objects
   */

  @SuppressWarnings("unchecked")
  public List<JSONObject> createGuidances(String type, List<UnprotectedSegment> segments) {
    List<JSONObject> guidances = new ArrayList<JSONObject>();

    // if we don't have any guidance support for the given type we dont need to perform the check
    // and
    // return non guidances
    if (!this.guidanceMap.containsKey(type)) {
      return guidances;
    }

    Guidance guidance = this.guidanceMap.get(type);

    // get the content of the segments
    String content = "";
    for (Segment uSegment : segments) {
      content += uSegment.toString();
    }

    Pattern forbiddenPattern = Pattern.compile(guidance.getRegex(), Pattern.DOTALL);
    Matcher matcher = forbiddenPattern.matcher(content);

    // loop through all findings
    while (matcher.find()) {

      JSONObject guidanceJsonObject = new JSONObject();
      JSONArray guidanceSegments = new JSONArray();
      int group = guidance.getGroup();
      int start = matcher.start(group);
      int end = matcher.end(group);

      int s = 0;

      // loop through all segments the match was found in
      for (ContentSegment uSegment : segments) {
        int length = uSegment.getLength();
        if (s <= start && s + length >= start) {
          // relative start and end for the segment
          int guidanceStart = Math.max(start - s, 0);
          int guidanceEnd = Math.min(length, end - s);

          JSONObject guidanceSegment = new JSONObject();
          guidanceSegment.put("start", guidanceStart);
          guidanceSegment.put("end", guidanceEnd);
          guidanceSegment.put("segmentId", uSegment.getId());

          guidanceSegments.add(guidanceSegment);
        }
        s += length;
      }
      guidanceJsonObject.put("segments", guidanceSegments);
      guidanceJsonObject.put("message", guidance.getMessage());
      guidanceJsonObject.put("helps", guidance.getHelps());
      guidances.add(guidanceJsonObject);
    }

    return guidances;
  }

}
