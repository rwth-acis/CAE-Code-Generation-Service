package i5.las2peer.services.codeGenerationService.models.microservice;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;

public class MobSOSLog {


  private String modelId, descriptionMarkdown;
  private boolean includeActingAgent;
  private int customMessageID;

  /**
   * Creates a {@link MobSOSLog}.
   *
   * @param node a {@link i5.cae.simpleModel.node.SimpleNode} containing the MobSOSLog
   *             *        representation.
   * @throws ModelParseException if something goes wrong during parsing the node
   */
  public MobSOSLog(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "includeActingAgent":
          this.includeActingAgent = Boolean.parseBoolean(attribute.getValue());
          break;
        case "customMessageID":
          String idString = attribute.getValue();
          int messageID;
          try {
            messageID = Integer.parseInt(idString);
          } catch (NumberFormatException e) {
            throw new ModelParseException("Failed to parse customMessageID: " + attribute.getValue() + ": " + e);
          }
          if (messageID < 1 || messageID > 99) {
            throw new ModelParseException(
                "customMessageID must be between 1 and 99: " + attribute.getValue());
          }
          this.customMessageID = messageID;
          break;
        case "descriptionMarkdown":
          this.descriptionMarkdown = attribute.getValue();
          break;
        default:
          throw new ModelParseException(
              "Unknown MobSOSLog attribute name: " + attribute.getName());
      }
    }
  }

  public String getModelId() {
    return modelId;
  }

  public int getCustomMessageID() {
    return customMessageID;
  }

  public boolean isIncludeActingAgent() {
    return includeActingAgent;
  }

  public String getDescriptionMarkdown() {
    if (descriptionMarkdown == null) {
      return "";
    }
    return descriptionMarkdown;
  }
}
