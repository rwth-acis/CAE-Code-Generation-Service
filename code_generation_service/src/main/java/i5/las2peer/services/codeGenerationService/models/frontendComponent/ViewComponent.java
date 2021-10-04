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
 * ViewComponent data class. Represents an View Component, which is part of a frontend component model.
 *
 */
public class ViewComponent {


  /**
   *
   * Represents the different element types an {@link ViewComponent} can have.
   *
   */
  public enum ElementType {
    div, ul, ol, form, table
  }

  private static final Map<ElementType, String> codeSample = ImmutableMap.of(
          ElementType.table, "  <tr>\n    <th>Column 1</th>\n    <th>Column 2</th> \n    <th>Column 3</th>\n  </tr>\n" +
                  "  <tr>\n    <td>Edit me!</td>\n    <td>Edit me!</td> \n    <td>Edit me!</td>\n  </tr>\n" +
                  "  <tr>\n    <td>Edit me!</td>\n    <td>Edit me!</td> \n    <td>Edit me!</td>\n  </tr>\n" +
                  "  <tr>\n    <td>Edit me!</td>\n    <td>Edit me!</td> \n    <td>Edit me!</td>\n  </tr>"
  );


  private String modelId;
  private String id;
  private ElementType type;
  private boolean staticElement;
  private boolean collaborativeElement;
  private ArrayList<Event> events = new ArrayList<>();
  private ArrayList<DataBinding> dataBindings = new ArrayList<>();
  private ViewComponent parent = null;
  private ArrayList<ViewComponent> children = new ArrayList<>();

  //Taken from the wireframe
  private HashMap<String, String> attributes = new HashMap<>();
  private HashMap<String, String> geometry = new HashMap<>();
  private String label;
  private boolean isContentEditable = false;
  private boolean ignoreSize = false;
  /**
   *
   * ViewComponent constructor. Takes a {@link SimpleNode} and parses it to an ViewComponent.
   *
   * @param node the node representing the ViewComponent
   *
   * @throws ModelParseException if some error comes up during parsing the node
   *
   */
  public ViewComponent(SimpleNode node) throws ModelParseException {
    this.modelId = node.getId();
    for (int nodeIndex = 0; nodeIndex < node.getAttributes().size(); nodeIndex++) {
      SimpleEntityAttribute attribute = node.getAttributes().get(nodeIndex);
      switch (attribute.getName()) {
        case "id":
          this.id = attribute.getValue();
          break;
        case "type":
          switch (attribute.getValue()) {
            case "div":
              this.type = ElementType.div;
              break;
            case "form":
              this.type = ElementType.form;
              break;
            case "ol":
              this.isContentEditable = true;
              this.type = ElementType.ol;
              break;
            case "ul":
              this.isContentEditable = true;
              this.type = ElementType.ul;
              break;
            case "table":
              this.isContentEditable = true;
              this.type = ElementType.table;
              break;
            default:
              throw new ModelParseException("Unknown ViewComponent type: " + attribute.getValue());
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
              throw new ModelParseException("Unknown ViewComponent attribute: " + attribute.getName());
            }
          }
          else throw new ModelParseException("Unknown ViewComponent attribute: " + attribute.getName());
      }
    }
  }

  public String generateCodeForAttributes() {
    //non attributes for polymer elements
    return " ";
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


  public ArrayList<DataBinding> getDataBindings() {
    return this.dataBindings;
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

  void setParent(ViewComponent element){
    parent = element;
  }

  void addChildren(ViewComponent element){
    children.add(element);
  }

  public ArrayList<ViewComponent> getChildren(){
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

  /**
   *
   * Adds an {@link DataBinding} to the HtmlElement.
   *
   * @param DataBinding an {@link DataBinding}
   *
   */
  void addDataBinding(DataBinding dataBinding) {
    this.dataBindings.add(dataBinding);
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
