package i5.las2peer.services.codeGenerationService.templateEngine;

import org.json.simple.JSONObject;

import i5.las2peer.services.codeGenerationService.traces.segments.SegmentFactory;

/**
 * A class representing a single guidance. An abstraction of the json object of the guidance
 * 
 * @author Thomas Winkler
 */
public class ViolationRule {
  private JSONObject jsonObject;

  /**
   * Creates a new guidance
   * 
   * @param guidance The original guidance json object
   */
  public ViolationRule(JSONObject guidance) {
    this.jsonObject = guidance;
  }

  /**
   * Get the type of the guidance
   * 
   * @return The type of the guidance
   */
  public String getType() {
    return (String) this.jsonObject.get("type");
  }

  /**
   * Get the regular expression of the guidance
   * 
   * @return The regular expression of the guidance
   */
  public String getRegex() {
    return (String) this.jsonObject.get("regex");
  }

  /**
   * Get the group of the regular expression that represents the not allowed code
   * 
   * @return The group of the regular expression
   * 
   */
  public int getGroup() {
    Long group = SegmentFactory.getLong(this.jsonObject, "group");
    return Math.toIntExact(group);
  }


  /**
   * Get the message of the guidance, e.g. a error message
   * 
   * @return The guidance message
   */

  public String getMessage() {
    return (String) this.jsonObject.get("message");
  }

}
