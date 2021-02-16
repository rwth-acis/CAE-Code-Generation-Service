package i5.las2peer.services.codeGenerationService.models.application.communicationModel;

/**
 * 
 * A {@link CommEdge} represents an edge in the CAE communication view model.
 *
 */
public class CommEdge {

  /**
   * 
   * Represents the different edge types a {@link CommEdge} can have.
   * 
   */
  public enum EdgeType {
    APPLICATION_COMPONENT("Application Component"), HTTP_CALL("HTTP Call"), IWC_COMMUNICATION(
        "IWC Communication"), WIDGET_TO_MICROSERIVCE_CALL(
            "Widget to Microservice Call"), WIDGET_TO_COLLABORATIVE_ELEMENT(
                "Widget to Collaborative Element"), WIDGET_TO_IWC_CALL(
                    "Widget to IWC Call"), WIDGET_TO_IWC_RESPONSE(
                        "Widget to IWC Response"), INTERNAL_RESOURCE_CALL(
                            "Internal Resource Call"), COMMUNICATES("Communicates");

    private String name;

    EdgeType(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private String source;
  private String target;
  private EdgeType type;


  /**
   * 
   * Adds a new {@link CommEdge}. Source and targets are defined as SyncMeta ids.
   * 
   * @param source the source of the edge
   * @param target the target of the edge
   * @param type an {@link EdgeType} representing the type of the edge
   * 
   */
  public CommEdge(String source, String target, EdgeType type) {
    this.source = source;
    this.target = target;
    this.type = type;
  }


  public String getSource() {
    return source;
  }


  public String getTarget() {
    return target;
  }


  public EdgeType getType() {
    return type;
  }

}
