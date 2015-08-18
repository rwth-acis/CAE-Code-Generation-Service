package i5.las2peer.services.codeGenerationService.generators;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import i5.las2peer.services.codeGenerationService.generators.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.Event;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.Function;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.HtmlElement;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.IWCCall;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.IWCResponse;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.InputParameter;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.MicroserviceCall;

/**
 * 
 * Generates frontend component source code from passed on
 * {@link i5.las2peer.services.codeGenerationService.models.frontendComponent} models.
 * 
 */
public class FrontendComponentGenerator extends Generator {

  /**
   * 
   * Creates source code from a CAE frontend component model and pushes it to GitHub.
   * 
   * @param frontendComponent the frontend component model
   * @param templateRepositoryName the name of the template repository on GitHub
   * @param gitHubOrganization the organization that is used in the CAE
   * @param gitHubUser the CAE user
   * @param gitHubUserMail the mail of the CAE user
   * @param gitHubPassword the password of the CAE user
   * 
   * @throws GitHubException thrown if anything goes wrong during this process. Wraps around all
   *         other exceptions and prints their message.
   * 
   */
  public static void createSourceCode(FrontendComponent frontendComponent,
      String templateRepositoryName, String gitHubOrganization, String gitHubUser,
      String gitHubUserMail, String gitHubPassword) throws GitHubException {

    // variables to be closed in the final block
    Repository frontendComponentRepository = null;
    TreeWalk treeWalk = null;

    // helper variables
    // variables holding content to be modified and added to repository later
    String widget = null;
    String applicationScript = null;
    String las2peerWidgetLibrary = null;
    String style = null;
    String readMe = null;
    BufferedImage logo = null;
    String htmlElementTemplate = null;
    String functionTemplate = null;
    String microserviceCallTemplate = null;
    String iwcResponseTemplate = null;
    String eventTemplate = null;

    try {
      PersonIdent caeUser = new PersonIdent(gitHubUser, gitHubUserMail);
      String repositoryName = "frontendComponent-" + frontendComponent.getName().replace(" ", "-");
      frontendComponentRepository =
          generateNewRepository(repositoryName, gitHubOrganization, gitHubUser, gitHubPassword);
      try {
        // now load the TreeWalk containing the template repository content
        treeWalk = getTemplateRepositoryContent(templateRepositoryName, gitHubOrganization);
        treeWalk.setFilter(PathFilter.create("frontend/"));
        ObjectReader reader = treeWalk.getObjectReader();
        // walk through the tree and retrieve the needed templates
        while (treeWalk.next()) {
          ObjectId objectId = treeWalk.getObjectId(0);
          ObjectLoader loader = reader.open(objectId);

          switch (treeWalk.getNameString()) {
            case "widget.xml":
              widget = new String(loader.getBytes(), "UTF-8");
              widget = widget.replace("$Widget_Title$", frontendComponent.getWidgetName());
              widget =
                  widget.replace("$Widget_Description$", frontendComponent.getWidgetDescription());
              widget = widget.replace("$Widget_Developer_Name$",
                  frontendComponent.getWidgetDeveloperName());
              widget = widget.replace("$Widget_Developer_Mail$",
                  frontendComponent.getWidgetDeveloperMail());
              widget = widget.replace("$Widget_Width$", frontendComponent.getWidgetWidth() + "");
              widget = widget.replace("$Widget_Height$", frontendComponent.getWidgetHeight() + "");
              String widgetHome = "http://" + gitHubOrganization + ".github.io/" + repositoryName;
              widget = widget.replace("$Widget_Home$", widgetHome);
              break;
            case "genericHtmlElement.txt":
              htmlElementTemplate = new String(loader.getBytes(), "UTF-8");
              break;
            case "genericEvent.txt":
              eventTemplate = new String(loader.getBytes(), "UTF-8");
              break;
            case "applicationScript.js":
              applicationScript = new String(loader.getBytes(), "UTF-8");
              applicationScript = applicationScript.replace("$Microservice_Endpoint_Url$",
                  frontendComponent.getMicroserviceAddress());
              break;
            case "genericFunction.txt":
              functionTemplate = new String(loader.getBytes(), "UTF-8");
              break;
            case "genericMicroserviceCall.txt":
              microserviceCallTemplate = new String(loader.getBytes(), "UTF-8");
              break;
            case "genericIWCResponse.txt":
              iwcResponseTemplate = new String(loader.getBytes(), "UTF-8");
              break;
            case "las2peerWidgetLibrary.js":
              las2peerWidgetLibrary = new String(loader.getBytes(), "UTF-8");
              break;
            case "style.css":
              style = new String(loader.getBytes(), "UTF-8");
              break;
            case "README.md":
              readMe = new String(loader.getBytes(), "UTF-8");
              readMe = readMe.replace("$Repository_Name$", repositoryName);
              readMe = readMe.replace("$Widget_Name$", frontendComponent.getName());
              readMe = readMe.replace("$Organization_Name$", gitHubOrganization);
              break;
            case "logo_services.png":
              logo = ImageIO.read(loader.openStream());
              break;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // add html elements to widget source code
      widget = addHtmlElements(widget, htmlElementTemplate, frontendComponent);
      // add functions to application script
      applicationScript = addFunctions(applicationScript, functionTemplate,
          microserviceCallTemplate, iwcResponseTemplate, frontendComponent);
      // add events to elements
      applicationScript = addEvents(applicationScript, eventTemplate, frontendComponent);
      applicationScript = removeRemainingPlaceholder(applicationScript);
      // add files to new repository
      frontendComponentRepository =
          createTextFileInRepository(frontendComponentRepository, "", "widget.xml", widget);
      frontendComponentRepository = createTextFileInRepository(frontendComponentRepository, "js/",
          "applicationScript.js", applicationScript);
      frontendComponentRepository = createTextFileInRepository(frontendComponentRepository, "js/",
          "las2peerWidgetLibrary.js", las2peerWidgetLibrary);
      frontendComponentRepository =
          createTextFileInRepository(frontendComponentRepository, "css/", "style.css", style);
      frontendComponentRepository =
          createTextFileInRepository(frontendComponentRepository, "", "README.md", readMe);
      frontendComponentRepository =
          createImageFileInRepository(frontendComponentRepository, "img/", "logo.png", logo);

      // commit files
      try {
        Git.wrap(frontendComponentRepository).commit()
            .setMessage("Generated frontend component " + frontendComponent.getVersion())
            .setCommitter(caeUser).call();
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // push (local) repository content to GitHub repository "gh-pages" branch
      try {
        pushToRemoteRepository(frontendComponentRepository, gitHubUser, gitHubPassword, "gh-pages");
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // close all open resources
    } finally {
      frontendComponentRepository.close();
      treeWalk.close();
    }
  }


  /**
   * 
   * Adds HTML elements according to a frontend component model to the passed widget (code).
   * 
   * @param widget the widget code as a string
   * @param htmlElementTemplate the HTML element template as a string
   * @param frontendComponent a {@link FrontendComponent}
   * 
   * @return the widget code with the inserted HTML elements
   * 
   */
  private static String addHtmlElements(String widget, String htmlElementTemplate,
      FrontendComponent frontendComponent) {
    Map<String, HtmlElement> htmlElementsToAdd = new HashMap<String, HtmlElement>();
    // add all static elements
    for (HtmlElement element : frontendComponent.getHtmlElements().values()) {
      if (element.isStaticElement()) {
        htmlElementsToAdd.put(element.getId(), element);
      }
    }
    // now, get all "updated", but not "created" elements (since these are there "from the start")
    ArrayList<String> idsToAdd = new ArrayList<String>();
    ArrayList<String> htmlElementsCreated = new ArrayList<String>();
    for (Function function : frontendComponent.getFunctions().values()) {
      idsToAdd.addAll(function.getHtmlElementUpdates());
      htmlElementsCreated.addAll(function.getHtmlElementCreations());
    }
    idsToAdd.removeAll(htmlElementsCreated);
    for (String idToAdd : idsToAdd) {
      htmlElementsToAdd.put(idToAdd, frontendComponent.getHtmlElements().get(idToAdd));
    }
    // now we got all elements that are there from the start on, so add them to the widget code
    for (HtmlElement element : htmlElementsToAdd.values()) {
      String elementCode = createElementCode(element, htmlElementTemplate);
      widget = widget.replace("$Main_Content$", elementCode);
    }
    // remove last element placeholder
    widget = widget.replace("$Main_Content$\n", "");
    return widget;
  }


  /**
   * 
   * Creates the code for an HTML element.
   * 
   * @param element the element
   * @param htmlElementTemplate a template for an HTML element
   * 
   * @return the code for an HTML element
   * 
   */
  private static String createElementCode(HtmlElement element, String htmlElementTemplate) {
    String elementCode = htmlElementTemplate;
    switch (element.getType()) {
      case CUSTOM:
        elementCode = htmlElementTemplate;
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case br:
        elementCode = htmlElementTemplate;
        elementCode = elementCode.replace("$Closing_Element$", "");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case button:
        elementCode = htmlElementTemplate;
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case div:
        elementCode = htmlElementTemplate;
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case input:
        elementCode = htmlElementTemplate;
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case p:
        elementCode = htmlElementTemplate;
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case table:
        elementCode = htmlElementTemplate;
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case textarea:
        elementCode = htmlElementTemplate;
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      default:
        break;
    }
    return elementCode;
  }


  /**
   * 
   * Adds functions to the passed application script code, according to a frontend component model.
   * 
   * @param applicationScript the application script source code
   * @param functionTemplate a template representing a generic function
   * @param microserviceCallTemplate a template representing a generic microservice call
   * @param iwcResponseTemplate a template representing a generic IWC response (with function call)
   * @param frontendComponent a {@link FrontendComponent}
   * 
   * @return the application script source code with inserted functions
   * 
   */
  private static String addFunctions(String applicationScript, String functionTemplate,
      String microserviceCallTemplate, String iwcResponseTemplate,
      FrontendComponent frontendComponent) {
    String functionCode = "";
    String microserviceCallCode = "";
    for (Function function : frontendComponent.getFunctions().values()) {

      // start with (potential) IWC response creation
      for (IWCResponse response : function.getIwcResponses()) {
        String iwcResponseCode = iwcResponseTemplate;
        iwcResponseCode = iwcResponseCode.replace("$Intent_Action$", response.getIntentAction());
        iwcResponseCode = iwcResponseCode.replace("$Function_Name$", function.getName());
        // add IWC response to application script
        applicationScript = applicationScript.replace("$IWC_Responses$", iwcResponseCode);
      }

      functionCode = functionTemplate;
      functionCode = functionCode.replace("$Function_Name$", function.getName());

      if (!function.getIwcResponses().isEmpty()) {
        // all content names are equal (checked in model creation process), so just take the first
        functionCode = functionCode.replace("$Function_Parameters$",
            function.getIwcResponses().get(0).getContent());
      }
      // treat special case when function does not have any input parameter
      else if (function.getInputParameters().isEmpty()) {
        functionCode = functionCode.replace("$Function_Parameters$", "");
      } else {
        for (InputParameter parameter : function.getInputParameters()) {
          functionCode = functionCode.replace("$Function_Parameters$",
              parameter.getName() + ", $Function_Parameters$");
        }
        // remove last input parameter placeholder
        functionCode = functionCode.replace(", $Function_Parameters$", "");
      }
      // check for return parameter (if there is one, else just remove placeholder)
      if (!function.getReturnParameter().equals("")) {
        // add variable initialization
        functionCode = functionCode.replace("$Function_Body$",
            "  var " + function.getReturnParameter() + " = null;\n$Function_Body$");
        functionCode = functionCode.replace("$Function_Return_Parameter$",
            "  return " + function.getReturnParameter() + ";");
      } else {
        functionCode = functionCode.replace("$Function_Return_Parameter$\n", "");
      }

      for (MicroserviceCall microserviceCall : function.getMicroserviceCalls()) {
        microserviceCallCode = microserviceCallTemplate;
        microserviceCallCode = microserviceCallCode.replace("$Method_Type$",
            microserviceCall.getMethodType().toString());
        microserviceCallCode =
            microserviceCallCode.replace("$Method_Path$", microserviceCall.getPath());
        if (!microserviceCall.getContent().isEmpty()) {
          // add variable initialization
          functionCode = functionCode.replace("$Function_Body$",
              "  var " + microserviceCall.getContent() + " = null;\n$Function_Body$");
          microserviceCallCode =
              microserviceCallCode.replace("$Content$", microserviceCall.getContent());
          microserviceCallCode = microserviceCallCode.replace("$Content_Type$",
              microserviceCall.getContentType().toString());
        } else {
          // no content specified, just remove placeholder / insert empty entries
          microserviceCallCode = microserviceCallCode.replace("$Content$", "\"\"");
          microserviceCallCode = microserviceCallCode.replace("$Content_Type$", "");
        }
        microserviceCallCode =
            microserviceCallCode.replace("$Method_Path$", microserviceCall.getPath());
        functionCode = functionCode.replace("$Function_Body$", microserviceCallCode);
      }

      for (String elementId : function.getHtmlElementCreations()) {
        functionCode = functionCode.replace("$Function_Body$",
            "$( \".container\" ).append(\"<strong>TODO</strong>\" );");
      }

      for (String elementId : function.getHtmlElementUpdates()) {
      }

      for (IWCCall call : function.getIwcCalls()) {
        functionCode = functionCode.replace("$Function_Body$",
            "  var " + call.getContent() + " = \"initialized\";\n$Function_Body$");
        functionCode = functionCode.replace("$Function_Body$", "  client.sendIntent(\""
            + call.getIntentAction() + "\", " + call.getContent() + ");\n$Function_Body$");
      }

      // remove last function body placeholder
      functionCode = functionCode.replace("$Function_Body$\n", "");
      // add function to application script
      applicationScript = applicationScript.replace("$Functions$", functionCode);
    }
    return applicationScript;
  }


  /**
   * 
   * Adds events to the passed application script for all HTML elements.
   * 
   * @param applicationScript the application script code
   * @param eventTemplate a template for an event
   * @param frontendComponent a {@link FrontendComponent}
   * 
   * @return the updated application script code
   * 
   */
  private static String addEvents(String applicationScript, String eventTemplate,
      FrontendComponent frontendComponent) {
    String eventCode = "";
    for (HtmlElement element : frontendComponent.getHtmlElements().values()) {
      for (Event event : element.getEvents()) {
        eventCode = eventTemplate;
        eventCode = eventCode.replace("$Html_Element_Id$", element.getId());
        eventCode = eventCode.replace("$Event_Type$", event.getEventCause().toString());
        Function function = frontendComponent.getFunctions().get(event.getCalledFunctionId());
        eventCode = eventCode.replace("$Function_Name$", function.getName());
        for (InputParameter parameter : function.getInputParameters()) {
          eventCode = eventCode.replace("$Parameter_Init$",
              "var " + parameter.getName() + " = null;\n    $Parameter_Init$");
          eventCode = eventCode.replace("$Function_Parameter$",
              parameter.getName() + ", $Function_Parameter$");
        }
        // remove last parameter placeholder
        eventCode = eventCode.replace("    $Parameter_Init$\n", "");
        eventCode = eventCode.replace(", $Function_Parameter$", "");
        // special case: no parameter
        eventCode = eventCode.replace("$Function_Parameter$", "");

        applicationScript = applicationScript.replace("$Events$", eventCode);
      }
    }
    return applicationScript;
  }


  /**
   * 
   * Removes all remaining placeholder from the application script.
   * 
   * @param applicationScript the application script source code
   * 
   * @return the updated application script source code
   * 
   */
  private static String removeRemainingPlaceholder(String applicationScript) {
    applicationScript = applicationScript.replace("$Functions$\n\n", "");
    applicationScript = applicationScript.replace("$IWC_Responses$\n", "");
    applicationScript = applicationScript.replace("$Events$\n", "");
    return applicationScript;
  }
}
