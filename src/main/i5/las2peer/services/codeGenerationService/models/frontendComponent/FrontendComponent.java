package i5.las2peer.services.codeGenerationService.models.frontendComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.SimpleModel;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;

/**
 * 
 * FrontendComponent data class. Currently, edges are only used for creating simple 1 to 1
 * dependencies between objects, without any attributes added to them.
 * 
 */
public class FrontendComponent {
  private String widgetId;
  private String name;
  private String widgetName;
  private float version;
  private String widgetDescription;
  private String widgetDeveloperName;
  private String widgetDeveloperMail;
  private int widgetWidth;
  private int widgetHeight;
  private Map<String, HtmlElement> htmlElements;

  /**
   * 
   * Creates a new frontend component.
   * 
   * @param model a {@link i5.cae.simpleModel.SimpleModel} containing the frontend component
   * 
   * @throws ModelParseException if something goes wrong during parsing
   * 
   */
  public FrontendComponent(SimpleModel model) throws ModelParseException {
    this.htmlElements = new HashMap<String, HtmlElement>();
    this.name = model.getName();
    // used to find (possible) duplicate (HTML) ids and report them
    ArrayList<String> ids = new ArrayList<String>();
    // metadata of model (currently only version)
    for (int attributeIndex = 0; attributeIndex < model.getAttributes().size(); attributeIndex++) {
      if (model.getAttributes().get(attributeIndex).getName().equals("version")) {
        try {
          this.setVersion(Float.parseFloat(model.getAttributes().get(attributeIndex).getValue()));
        } catch (NumberFormatException e) {
          throw new ModelParseException("Frontend Component version is not a number!");
        }
      }
    }
    // go through the nodes and create objects
    ArrayList<SimpleNode> nodes = model.getNodes();
    for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
      SimpleNode node = nodes.get(nodeIndex);
      ArrayList<SimpleEntityAttribute> nodeAttributes = node.getAttributes();
      switch (node.getType()) {
        case "Widget":
          if (this.widgetId == null) {
            this.widgetId = node.getId();
          } else {
            throw new ModelParseException("More than one Widget in frontend component model");
          }
          for (int attributeIndex = 0; attributeIndex < nodeAttributes.size(); attributeIndex++) {
            SimpleEntityAttribute attribute = nodeAttributes.get(attributeIndex);
            switch (attribute.getName()) {
              case "name":
                this.widgetName = attribute.getValue();
                break;
              case "description":
                this.widgetDescription = attribute.getValue();
                break;
              case "developerName":
                this.widgetDeveloperName = attribute.getValue();
                break;
              case "developerMail":
                this.widgetDeveloperMail = attribute.getValue();
                break;
              case "height":
                try {
                  this.widgetHeight = Integer.parseInt(attribute.getValue());
                } catch (NumberFormatException e) {
                  throw new ModelParseException("Widget height is not a number!");
                }
                break;
              case "width":
                try {
                  this.widgetWidth = Integer.parseInt(attribute.getValue());
                  break;
                } catch (NumberFormatException e) {
                  throw new ModelParseException("Widget width is not a number!");
                }
              default:
                throw new ModelParseException(
                    "Unknown attribute typ of Widget: " + attribute.getName());
            }
          }
          break;
        case "HTML Element":
          HtmlElement element = new HtmlElement(node);
          this.htmlElements.put(node.getId(), element);
          if (ids.contains(element.getId())) {
            throw new ModelParseException("Duplicate id found: " + node.getId());
          }
          ids.add(element.getId());
          break;
        default:
          throw new ModelParseException("Unknown node type: " + node.getType());
      }
    }
  }


  public String getWidgetId() {
    return widgetId;
  }


  public void setWidgetId(String widgetId) {
    this.widgetId = widgetId;
  }


  public String getName() {
    return this.name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public String getWidgetName() {
    return this.widgetName;
  }


  public void setWidgetName(String name) {
    this.widgetName = name;
  }


  public float getVersion() {
    return this.version;
  }


  public void setVersion(float version) {
    this.version = version;
  }


  public String getWidgetDescription() {
    return widgetDescription;
  }


  public void setWidgetDescription(String widgetDescription) {
    this.widgetDescription = widgetDescription;
  }


  public String getWidgetDeveloperName() {
    return widgetDeveloperName;
  }


  public void setWidgetDeveloperName(String widgetDeveloperName) {
    this.widgetDeveloperName = widgetDeveloperName;
  }


  public String getWidgetDeveloperMail() {
    return widgetDeveloperMail;
  }


  public void setWidgetDeveloperMail(String widgetDeveloperMail) {
    this.widgetDeveloperMail = widgetDeveloperMail;
  }


  public int getWidgetWidth() {
    return widgetWidth;
  }


  public void setWidgetWidth(int widgetWidth) {
    this.widgetWidth = widgetWidth;
  }


  public int getWidgetHeight() {
    return widgetHeight;
  }


  public void setWidgetHeight(int widgetHeight) {
    this.widgetHeight = widgetHeight;
  }

}
