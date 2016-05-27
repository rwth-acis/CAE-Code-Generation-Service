package i5.las2peer.services.codeGenerationService.traces.segments;

import java.math.BigInteger;
import java.security.MessageDigest;

import org.json.simple.JSONObject;

/**
 * A class representing an unprotected segments. Unprotected segments are the segments, which can be
 * edited by a user in the editor.
 * 
 * @author Thomas Winkler
 *
 */

public class UnprotectedSegment extends ContentSegment {
  public static final String TYPE = "unprotected";
  private String content;
  private String hash = null;
  private boolean needsIntegrityCheck = false;

  /**
   * Create a new unprotected segment with the given id
   * 
   * @param id The id of the new unprotected segment
   */

  public UnprotectedSegment(String id) {
    super(id);
  }

  public UnprotectedSegment(JSONObject entry) {
    this((String) entry.get("id"));

    boolean integrityCheck = false;
    if (entry.get("integrityCheck") != null) {
      integrityCheck = (boolean) entry.get("integrityCheck");
    }

    if (integrityCheck) {
      this.enableIntegrityCheck();
      // check if we already have a hash
      if (entry.containsKey("hash")) {
        String hash = (String) entry.get("hash");
        this.setHash(hash);
      }
    }

  }

  /**
   * Enable the integrity check for the unprotected segment. The segment will be protected such
   * that, it is only updated during the model synchronization process if the content of the segment
   * is equal to the content of the last generation / model synchronization process. Therefore, a
   * hash value is used.
   */

  public void enableIntegrityCheck() {
    this.needsIntegrityCheck = true;
  }

  /**
   * Set the hash value of the segment and enable the integrity check
   * 
   * @param hash
   */

  private void setHash(String hash) {
    this.hash = hash;
    this.enableIntegrityCheck();
  }

  public void calculateHash() {
    this.hash = getHash(this.getContent());
  }

  public String getHash() {
    return this.hash;
  }

  public static String getHash(String content) {
    MessageDigest m;
    String hash = "";

    try {
      m = MessageDigest.getInstance("MD5");
      m.update(content.getBytes("utf-8"), 0, content.length());
      hash = new BigInteger(1, m.digest()).toString(16);
    } catch (Exception e) {
    }

    return hash;
  }

  @Override
  public int getLength() {
    return this.getContent().length();
  }


  public void setContent(String content, boolean integrityCheck) {

    if (this.hash != null && integrityCheck) {

      if (!getHash(this.getContent()).equals(this.hash)) {
        // always update hash
        this.setHash(getHash(content));
        return;
      }
    }
    this.content = content;
    this.calculateHash();
  }

  @Override
  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public String getContent() {
    return this.content;
  }

  @SuppressWarnings("unchecked")
  @Override
  public JSONObject toJSONObject() {
    JSONObject jObject = super.toJSONObject();
    // only set hash value if the integrity check is enabled and a hash value is set
    if (this.needsIntegrityCheck && this.getHash() != null) {
      jObject.put("integrityCheck", true);
      jObject.put("hash", this.getHash());
    } else {
      jObject.put("integrityCheck", false);
    }

    return jObject;
  }

  @Override
  public String getTypeString() {
    return UnprotectedSegment.TYPE;
  }
}
