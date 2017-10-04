package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import java.util.ArrayList;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
import java.util.HashMap;

/**
 * 
 * HtmlElement data class. Represents an HTML Element, which is part of a frontend component model.
 *
 */
public class HtmlElement {


  /**
   * 
   * Represents the different element types an {@link HtmlElement} can have.
   * 
   */
  public enum ElementType {
    input, table, br, button, p, div, textarea, CUSTOM, a, img, audio, video, span, iframe
  }

  private String modelId;
  private String id;
  private ElementType type;
  private boolean staticElement;
  private boolean collaborativeElement;
  private ArrayList<Event> events = new ArrayList<>();
  private HtmlElement parent = null;
  private ArrayList<HtmlElement> children = new ArrayList<>();

  //Taken from the wireframe
  private HashMap<String, String> attributes = new HashMap<>();
  private HashMap<String, String> geometry = new HashMap<>();
  private String label;

  /**
   *
   * HtmlElement constructor. Takes a {@link SimpleNode} and parses it to an HtmlElement.
   * 
   * @param node the node representing the HtmlElement
   * 
   * @throws ModelParseException if some error comes up during parsing the node
   * 
   */
  public HtmlElement(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "id":
          this.id = attribute.getValue();
          break;
        case "type":
          switch (attribute.getValue()) {
            case "input":
              this.type = ElementType.input;
              break;
            case "table":
              this.type = ElementType.table;
              break;
            case "br":
              this.type = ElementType.br;
              break;
            case "button":
              this.type = ElementType.button;
              break;
            case "p":
              this.type = ElementType.p;
              break;
            case "div":
              this.type = ElementType.div;
              break;
            case "textarea":
              this.type = ElementType.textarea;
              break;
            case "CUSTOM":
              this.type = ElementType.CUSTOM;
              break;
            case "a":
              this.type = ElementType.a;
              break;
            case "img":
              this.type = ElementType.img;
              break;
            case "audio":
              this.type = ElementType.audio;
              break;
            case "video":
              this.type = ElementType.video;
              break;
            case "span":
              this.type = ElementType.span;
              break;
            case "iframe":
              this.type = ElementType.iframe;
              break;
            default:
              throw new ModelParseException("Unknown HtmlElement type: " + attribute.getValue());
          }
          break;
        case "static":
          this.staticElement = Boolean.parseBoolean(attribute.getValue());
          break;
        case "collaborative":
          this.collaborativeElement = Boolean.parseBoolean(attribute.getValue());
          break;
        default:
          if(attribute.getSyncMetaId().contains("ui")){
            if(attribute.getSyncMetaId().contains("uiAttr")){
              attributes.put(attribute.getName(), attribute.getValue());
            }else if(attribute.getSyncMetaId().contains("uiGeo")){
              geometry.put(attribute.getName(), attribute.getValue());
            }
            else if(attribute.getSyncMetaId().contains("uiLabel")){
              label = attribute.getValue();
            }
            else{
              throw new ModelParseException("Unknown HtmlElement attribute: " + attribute.getName());
            }
          }
          else throw new ModelParseException("Unknown HtmlElement attribute: " + attribute.getName());
      }
    }
  }

  public String generateCodeForAttributes() {
    StringBuilder code = new StringBuilder();
    for (String key : attributes.keySet()) {
      String value = attributes.get(key);
      if (value.length() > 0) {
        if (value.equals("true"))
          code.append(" ").append(key).append(" ");
        else if (!value.equals("false")) //if value = false then ignore the attribute
          code.append(" ").append(key).append("=\"").append(value).append("\" ");
      }
    }

    if(this.getType().equals(ElementType.input) && this.getLabel() != null)
      code.append("value=\"").append(this.getLabel()).append("\"");
    return code.toString();
  }

  public String generateCodeForGeometry(){
    StringBuilder code = new StringBuilder();
    // add geometry
    if(geometry.keySet().size() > 0) {
      if(this.hasParent())
        code.append("position: absolute ; ");
      else
        code.append("position: absolute; ");
      String top = geometry.get("x");
      if (top.length() > 0)
        code.append("left: ").append(top).append("px; ");
      String left = geometry.get("y");
      if (left.length() > 0)
        code.append("top: ").append(left).append("px; ");
      String width = geometry.get("width");
      if (width.length() > 0)
        code.append("width: ").append(width).append("px; ");
      String height = geometry.get("height");
      if (height.length() > 0)
        code.append("height: ").append(height).append("px;");
      //code.append("\"");
    }
    return code.toString();
  }

  public String getModelId() {
    return this.modelId;
  }


  public String getId() {
    return this.id;
  }


  public ElementType getType() {
    return this.type;
  }


  public boolean isStaticElement() {
    return this.staticElement;
  }


  public ArrayList<Event> getEvents() {
    return this.events;
  }


  public boolean isCollaborativeElement() {
    return collaborativeElement;
  }

  public HashMap<String, String> getAttributes(){
    return attributes;
  }

  /*public HashMap<String, String> getGeometry() {
    return geometry;
  }*/

  public String getLabel() {
    return label;
  }

  void setParent(HtmlElement element){
    parent = element;
  }

  void addChildren(HtmlElement element){
    children.add(element);
  }

  public ArrayList<HtmlElement> getChildren(){
    return children;
  }

  public boolean hasParent(){
    return parent != null;
  }


  public boolean hasChildren(){
    return !children.isEmpty();
  }

  /**
   * 
   * Adds an {@link Event} to the HtmlElement.
   * 
   * @param event an {@link Event}
   * 
   */
  void addEvent(Event event) {
    this.events.add(event);
  }

}
