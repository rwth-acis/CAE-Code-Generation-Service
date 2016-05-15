package i5.las2peer.services.codeGenerationService.traces.segments;

import java.math.BigInteger;
import java.security.MessageDigest;

import org.json.simple.JSONObject;

public class UnprotectedSegment extends ContentSegment {
  private String content;
  private String hash = null;
  private boolean needsIntegrityCheck = false;

  public UnprotectedSegment(String id) {
    super(id);
  }

  public void setHash(String hash) {
    this.hash = hash;
    needsIntegrityCheck = true;
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

  @Override
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
  public void replace(String pattern, String replacement) {}

  @Override
  public String getTypeString() {
    return "unprotected";
  }
}
