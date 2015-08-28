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
    String yjsImports = null;
    String yjs = null;
    String yText = null;
    String yXmpp = null;
    String iwc = null;
    String yjsInit = null;
    String yjsBindCode = null;
    String yjsSyncedCode = null;

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
              break;
            case "genericHtmlElement.txt":
              htmlElementTemplate = new String(loader.getBytes(), "UTF-8");
              break;
            case "genericEvent.txt":
              eventTemplate = new String(loader.getBytes(), "UTF-8");
              break;
            case "applicationScript.js":
              applicationScript = new String(loader.getBytes(), "UTF-8");
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
            case "yjs-imports.txt":
              yjsImports = new String(loader.getBytes(), "UTF-8");
              break;
            case "las2peerWidgetLibrary.js":
              las2peerWidgetLibrary = new String(loader.getBytes(), "UTF-8");
              break;
            case "iwc.js":
              iwc = new String(loader.getBytes(), "UTF-8");
              break;
            case "y.js":
              yjs = new String(loader.getBytes(), "UTF-8");
              break;
            case "y-text.js":
              yText = new String(loader.getBytes(), "UTF-8");
              break;
            case "y-xmpp.js":
              yXmpp = new String(loader.getBytes(), "UTF-8");
              break;
            case "yjsInit.txt":
              yjsInit = new String(loader.getBytes(), "UTF-8");
              break;
            case "yjsBindCode.txt":
              yjsBindCode = new String(loader.getBytes(), "UTF-8");
              break;
            case "yjsSyncedTemplate.txt":
              yjsSyncedCode = new String(loader.getBytes(), "UTF-8");
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
            case "logo_frontend.png":
              logo = ImageIO.read(loader.openStream());
              break;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // add html elements to widget source code
      widget = createWidgetCode(widget, htmlElementTemplate, yjsImports, gitHubOrganization,
          repositoryName, frontendComponent);
      // add functions to application script
      applicationScript = createApplicationScript(applicationScript, functionTemplate,
          microserviceCallTemplate, iwcResponseTemplate, htmlElementTemplate, frontendComponent);
      // add events to elements
      applicationScript =
          addEventsToApplicationScript(applicationScript, eventTemplate, frontendComponent);
      // add (possible) Yjs collaboration stuff
      applicationScript = addYjsCollaboration(applicationScript, yjsInit, yjsBindCode,
          yjsSyncedCode, frontendComponent);
      // and remove remaining placeholer in the end
      applicationScript = removeRemainingAppScriptPlaceholder(applicationScript);
      // add files to new repository
      frontendComponentRepository =
          createTextFileInRepository(frontendComponentRepository, "", "widget.xml", widget);
      frontendComponentRepository = createTextFileInRepository(frontendComponentRepository, "js/",
          "applicationScript.js", applicationScript);
      // libraries
      frontendComponentRepository = createTextFileInRepository(frontendComponentRepository,
          "js/lib/", "las2peerWidgetLibrary.js", las2peerWidgetLibrary);
      frontendComponentRepository =
          createTextFileInRepository(frontendComponentRepository, "js/lib/", "iwc.js", iwc);
      // y-is (if needed)
      if (widget.contains("/js/lib/y.js")) {
        frontendComponentRepository =
            createTextFileInRepository(frontendComponentRepository, "js/lib/", "y.js", yjs);
        frontendComponentRepository =
            createTextFileInRepository(frontendComponentRepository, "js/lib/", "y-text.js", yText);
        frontendComponentRepository =
            createTextFileInRepository(frontendComponentRepository, "js/lib/", "y-xmpp.js", yXmpp);
      }
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
        pushToRemoteRepository(frontendComponentRepository, gitHubUser, gitHubPassword, "master",
            "gh-pages");
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
   * Creates the "widget.xml" code according to a passed frontend component model.
   * 
   * @param widget the widget code as a string
   * @param htmlElementTemplate the HTML element template as a string
   * @param importTemplate a text file containing all additional imports to be added
   * @param gitHubOrganization the organization name (for correct paths)
   * @param repositoryName the repository's name (for correct paths)
   * @param frontendComponent a {@link FrontendComponent}
   * 
   * @return the widget code with the inserted HTML elements
   * 
   */
  private static String createWidgetCode(String widget, String htmlElementTemplate,
      String importTemplate, String gitHubOrganization, String repositoryName,
      FrontendComponent frontendComponent) {

    Map<String, HtmlElement> htmlElementsToAdd = new HashMap<String, HtmlElement>();
    // add all static elements and check for collaborative elements
    for (HtmlElement element : frontendComponent.getHtmlElements().values()) {
      if (element.isStaticElement()) {
        htmlElementsToAdd.put(element.getId(), element);
      }
      if (element.isCollaborativeElement()) {
        // currently, only textareas are supported
        widget = widget.replace("$Additional_Imports$", importTemplate);
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
      String elementCode = createHtmlElementCode(element, htmlElementTemplate);
      widget = widget.replace("$Main_Content$", "    " + elementCode + "\n$Main_Content$");
    }

    // widget meta-data and path replacements
    widget = widget.replace("$Widget_Title$", frontendComponent.getWidgetName());
    widget = widget.replace("$Widget_Description$", frontendComponent.getWidgetDescription());
    widget = widget.replace("$Widget_Developer_Name$", frontendComponent.getWidgetDeveloperName());
    widget = widget.replace("$Widget_Developer_Mail$", frontendComponent.getWidgetDeveloperMail());
    widget = widget.replace("$Widget_Width$", frontendComponent.getWidgetWidth() + "");
    widget = widget.replace("$Widget_Height$", frontendComponent.getWidgetHeight() + "");
    String widgetHome = "http://" + gitHubOrganization + ".github.io/" + repositoryName;
    widget = widget.replace("$Widget_Home$", widgetHome);

    // remove last element placeholder
    widget = widget.replace("$Main_Content$\n", "");
    // remove import placeholder (if existing)
    widget = widget.replace("$Additional_Imports$\n", "");
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
  private static String createHtmlElementCode(HtmlElement element, String htmlElementTemplate) {
    String elementCode = htmlElementTemplate;
    switch (element.getType()) {
      case CUSTOM:
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case br:
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case button:
        elementCode =
            elementCode.replace("$Closing_Element$", element.getId() + "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        elementCode = elementCode.replace("$Additional_Values$", "class=\"btn btn-default\"");
        break;
      case div:
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case input:
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case p:
        elementCode = elementCode.replace("$Closing_Element$", "some paragraph</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case table:
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      case textarea:
        elementCode = elementCode.replace("$Closing_Element$", "</$Element_Type$>");
        elementCode =
            elementCode.replace("$Additional_Values$", "class=\"form-control\" rows=\"5\"");
        elementCode = elementCode.replace("$Element_Type$", element.getType().toString());
        elementCode = elementCode.replace("$Element_Id$", element.getId());
        break;
      default:
        break;
    }
    // remove (possible) remaining placeholder
    // (type and id are needed for every element, rest is optional)
    elementCode = elementCode.replace("$Closing_Element$", "");
    elementCode = elementCode.replace(" $Additional_Values$", "");
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
   * @param htmlElementTemplate a template representing a generic HTML template
   * @param frontendComponent a {@link FrontendComponent}
   * 
   * @return the application script source code with inserted functions
   * 
   */
  private static String createApplicationScript(String applicationScript, String functionTemplate,
      String microserviceCallTemplate, String iwcResponseTemplate, String htmlElementTemplate,
      FrontendComponent frontendComponent) {

    // first the endpoint
    applicationScript = applicationScript.replace("$Microservice_Endpoint_Url$",
        frontendComponent.getMicroserviceAddress());

    // now to the functions
    for (Function function : frontendComponent.getFunctions().values()) {

      // start with (potential) IWC response creation
      for (IWCResponse response : function.getIwcResponses()) {
        String iwcResponseCode = iwcResponseTemplate;
        iwcResponseCode = iwcResponseCode.replace("$Intent_Action$", response.getIntentAction());
        iwcResponseCode = iwcResponseCode.replace("$Function_Name$", function.getName());
        // add IWC response to application script
        applicationScript = applicationScript.replace("$IWC_Responses$", iwcResponseCode);
      }

      // start creating the actual function
      String functionCode = functionTemplate;
      functionCode = functionCode.replace("$Function_Name$", function.getName());

      // function parameters
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

      // microservice calls
      for (MicroserviceCall microserviceCall : function.getMicroserviceCalls()) {
        String microserviceCallCode = microserviceCallTemplate;
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

      // element creations
      for (String elementId : function.getHtmlElementCreations()) {
        HtmlElement element = frontendComponent.getHtmlElements().get(elementId);
        String htmlElementCode = createHtmlElementCode(element, htmlElementTemplate);
        functionCode = functionCode.replace("$Function_Body$",
            "  $( \".container\" ).append(\"" + htmlElementCode + "\");\n$Function_Body$");
      }

      // element updates
      for (String elementId : function.getHtmlElementUpdates()) {
        HtmlElement element = frontendComponent.getHtmlElements().get(elementId);
        functionCode = functionCode.replace("$Function_Body$",
            "  $(\"#" + element.getId() + "\").html(\"Upated Element\");\n$Function_Body$");
      }

      // iwc calls
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
   * Adds the (possible) Yjs collaboration code to the application script.
   * 
   * TODO: Currently, only textareas are supported.
   * 
   * @param applicationScript the application script code
   * @param yjsInit a template for initializing the Yjs connector etc
   * @param yjsTemplate a template for binding an html element
   * @param yjsSyncedCode a template for sync actions of an element
   * @param frontendComponent a {@link FrontendComponent}
   * 
   * @return the updated application script code
   * 
   */
  private static String addYjsCollaboration(String applicationScript, String yjsInit,
      String yjsBindCode, String yjsSyncedCode, FrontendComponent frontendComponent) {
    boolean foundCollaborativeElement = false; // helper so that the code only needs to run once
    for (HtmlElement element : frontendComponent.getHtmlElements().values()) {
      if (element.isCollaborativeElement()) {
        if (!foundCollaborativeElement) {
          applicationScript = applicationScript.replace("$Yjs_Code$", yjsInit);
          foundCollaborativeElement = true;
        }
        applicationScript = applicationScript.replace("$Sync_Code$", yjsSyncedCode);
        applicationScript = applicationScript.replace("$Variable_Init$", "  var " + element.getId()
            + " = document.getElementById(\"" + element.getId() + "\");\n$Variable_Init$");
        applicationScript = applicationScript.replace("$Variable_Init$",
            "  y.val(\"" + element.getId() + "\", new Y.Text(\"\"));\n$Variable_Init$");
        applicationScript = applicationScript.replace("$Variable_Observe_Code$", yjsBindCode);
        applicationScript = applicationScript.replace("$Element_Id$", element.getId());
      }
    }
    // remove yjs placeholder if no yjs collaboration is needed
    if (!foundCollaborativeElement) {
      applicationScript = applicationScript.replace("$Yjs_Code$\n", "");
    }
    // remove last placeholder
    applicationScript = applicationScript.replace("$Variable_Init$\n", "");
    applicationScript = applicationScript.replace("$Variable_Observe_Code$\n", "");
    applicationScript = applicationScript.replace("$Sync_Code$\n", "");

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
  private static String addEventsToApplicationScript(String applicationScript, String eventTemplate,
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
  private static String removeRemainingAppScriptPlaceholder(String applicationScript) {
    applicationScript = applicationScript.replace("$Functions$\n\n", "");
    applicationScript = applicationScript.replace("$IWC_Responses$\n", "");
    applicationScript = applicationScript.replace("$Events$\n\n", "");
    return applicationScript;
  }
}
