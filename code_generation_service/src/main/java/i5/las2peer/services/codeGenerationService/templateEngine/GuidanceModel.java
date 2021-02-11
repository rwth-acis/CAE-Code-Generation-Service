package i5.las2peer.services.codeGenerationService.templateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

  private Map<String, List<ViolationRule>> violationRules =
      new HashMap<String, List<ViolationRule>>();

  /**
   * Adds a single rule to the model
   * 
   * @param rule The rule to add
   */
  private void addRule(ViolationRule rule) {
    if (!this.violationRules.containsKey(rule.getType())) {
      ArrayList<ViolationRule> newList = new ArrayList<ViolationRule>();
      this.violationRules.put(rule.getType(), newList);
    }
    List<ViolationRule> list = this.violationRules.get(rule.getType());
    list.add(rule);
  }

  /**
   * Parses and adds the violation rules of the given json file to the model
   * 
   * @param violationRulesObj the rules to add
   */
  public void addRules(JSONObject violationRulesObj) {

    JSONArray violationRules = (JSONArray) violationRulesObj.get("guidances");
    for (int i = 0; i < violationRules.size(); i++) {
      JSONObject rule = (JSONObject) violationRules.get(i);
      this.addRule(new ViolationRule(rule));
    }

  }

  /**
   * Performs the model violation check and creates corresponding guidances if breaking code is
   * found
   * 
   * @param type The type of the model element
   * @param segments The segments whose content should be checked
   * @return A list of guidances as json objects
   */

  @SuppressWarnings("unchecked")
  public List<JSONObject> createFeedback(String type, List<UnprotectedSegment> segments) {
    List<JSONObject> feedback = new ArrayList<JSONObject>();

    // if we don't have any guidance support for the given type we dont need to perform the check
    // and
    // return non guidances
    if (!this.violationRules.containsKey(type)) {
      return feedback;
    }
    List<ViolationRule> rules = this.violationRules.get(type);
    // get the content of the segments
    String content = "";
    for (Segment uSegment : segments) {
      content += uSegment.toString();
    }

    for (ViolationRule rule : rules) {

      Pattern forbiddenPattern = Pattern.compile(rule.getRegex(), Pattern.DOTALL);
      Matcher matcher = forbiddenPattern.matcher(content);

      // loop through all findings
      while (matcher.find()) {

        JSONObject feedbackJsonObject = new JSONObject();
        JSONArray guidanceSegments = new JSONArray();
        int group = rule.getGroup();
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
        feedbackJsonObject.put("segments", guidanceSegments);
        feedbackJsonObject.put("message", rule.getMessage());
        feedback.add(feedbackJsonObject);
      }
    }

    return feedback;
  }

}
