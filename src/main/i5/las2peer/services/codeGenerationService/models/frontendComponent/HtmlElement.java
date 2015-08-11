package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import java.util.ArrayList;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;


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
    input, table, br, button, p, div, textarea, CUSTOM
  }

  private String modelId;
  private String id;
  private ElementType type;
  private boolean staticElement;
  private boolean collaborativeElement;
  private ArrayList<Event> events = new ArrayList<Event>();


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
            default:
              throw new ModelParseException("Unknown HtmlElement type: " + attribute.getValue());
          }
        case "static":
          this.staticElement = Boolean.parseBoolean(attribute.getValue());
          break;
        case "collaborative":
          this.collaborativeElement = Boolean.parseBoolean(attribute.getValue());
          break;
        default:
          throw new ModelParseException("Unknown HtmlElement attribute: " + attribute.getName());
      }
    }
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


  /**
   * 
   * Adds an {@link Event} to the HtmlElement.
   * 
   * @param event an {@link Event}
   * 
   */
  public void addEvent(Event event) {
    this.events.add(event);
  }

}
