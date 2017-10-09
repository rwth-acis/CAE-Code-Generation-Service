package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import java.util.ArrayList;

import com.google.common.collect.ImmutableMap;
import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
import java.util.Map;
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
    input, table, br, button, p, div, textarea, CUSTOM, a, img, audio, video, span, iframe, radio, checkbox, ul, ol, dl, canvas, svg
  }

  private static final Map<ElementType, String> codeSample = ImmutableMap.of(
          ElementType.ul, "  <li>Item 1</li>\n<li>Item 2</li>\n<li>Item 3</li> ",
          ElementType.ol, "  <li>Item 1</li>\n<li>Item 2</li>\n<li>Item 3</li> ",
          ElementType.dl, " <dt>Item 1</dt>\n     <dd>- Description of Item 1</dd>\n" +
                              "<dt>Item 2</dt>\n      <dd>- Description of Item 2</dd>",
          ElementType.table, "  <tr>\n    <th>Column 1</th>\n    <th>Column 2</th> \n    <th>Column 3</th>\n  </tr>\n" +
                  "  <tr>\n    <td>Edit me!</td>\n    <td>Edit me!</td> \n    <td>Edit me!</td>\n  </tr>\n" +
                  "  <tr>\n    <td>Edit me!</td>\n    <td>Edit me!</td> \n    <td>Edit me!</td>\n  </tr>\n" +
                  "  <tr>\n    <td>Edit me!</td>\n    <td>Edit me!</td> \n    <td>Edit me!</td>\n  </tr>",
          ElementType.svg, "  <ellipse cx=\"100\" cy=\"70\" rx=\"85\" ry=\"55\" fill=\"blue\"/>\n  <text fill=\"#ffffff\" font-size=\"45\" font-family=\"Verdana\"\n  x=\"50\" y=\"86\">SVG</text>\nSorry, your browser does not support inline SVG."

  );


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
  private boolean isContentEditable = false;
  private boolean ignoreSize = false;
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
              this.isContentEditable = true;
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
            case "radio":
              this.ignoreSize = true;
              this.type = ElementType.radio;
              break;
            case "checkbox":
              this.ignoreSize = true;
              this.type = ElementType.checkbox;
              break;
            case "ol":
              this.isContentEditable = true;
              this.type = ElementType.ol;
              break;
            case "ul":
              this.isContentEditable = true;
              this.type = ElementType.ul;
              break;
            case "dl":
              this.isContentEditable = true;
              this.type = ElementType.dl;
              break;
            case "canvas":
              this.ignoreSize = true;
              this.type = ElementType.canvas;
              break;
            case "svg":
              this.ignoreSize = true;
              this.isContentEditable = true;
              this.type = ElementType.svg;
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
              if(label == null)
                label = "";
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
    //non attributes for polymer elements
    if(this.type.equals(ElementType.CUSTOM))
      return " ";

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
      if (width.length() > 0 && !this.ignoreSize)
        code.append("width: ").append(width).append("px; ");
      String height = geometry.get("height");
      if (height.length() > 0 && !this.ignoreSize)
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

  public boolean isContentEditable(){
    return this.isContentEditable;
  }

  public String getCodeSample(){
    return codeSample.getOrDefault(this.type, " ");
  }

  public String getAttributeValue(String name){
    return this.attributes.getOrDefault(name, "");
  }

  public String getGeometryAttributeValue(String name){
    return this.geometry.getOrDefault(name, "");
  }
}
