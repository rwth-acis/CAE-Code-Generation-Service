package i5.las2peer.services.codeGenerationService.generators;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.simple.JSONObject;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.CodeGenerationService;
import i5.las2peer.services.codeGenerationService.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModelFactory;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.templateEngine.SynchronizationOrderedStrategy;
import i5.las2peer.services.codeGenerationService.templateEngine.SynchronizationStrategy;
import i5.las2peer.services.codeGenerationService.templateEngine.Template;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateEngine;

/**
 * 
 * Synchronizes the source code of a
 * {@link i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent}
 * with an updated version of that component
 * 
 */
public class FrontendComponentSynchronization extends FrontendComponentGenerator {

  private static final L2pLogger logger =
      L2pLogger.getInstance(ApplicationGenerator.class.getName());

  public static void synchronizeSourceCode(FrontendComponent frontendComponent,
      FrontendComponent oldFrontendComponent, HashMap<String, JSONObject> files,
      String templateRepositoryName, String gitHubOrganization, CodeGenerationService service)
      throws GitHubException {
    // first load the needed templates from the template repository

    // helper variables
    // variables holding content to be modified and added to repository later
    String widget = null;
    String applicationScript = null;
    String htmlElementTemplate = null;
    String functionTemplate = null;
    String microserviceCallTemplate = null;
    String iwcResponseTemplate = null;
    String eventTemplate = null;
    String yjsImports = null;
    String yjsInit = null;

    TemplateEngine applicationTemplateEngine = null;
    TemplateEngine widgetTemplateEngine = null;

    try (TreeWalk treeWalk =
        getTemplateRepositoryContent(templateRepositoryName, gitHubOrganization)) {
      // now load the TreeWalk containing the template repository content
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
          case "yjsInit.txt":
            yjsInit = new String(loader.getBytes(), "UTF-8");
            break;
        }
      }
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new GitHubException(e.getMessage());
    }

    // now loop through the traced files and synchronize them

    Iterator<String> it = files.keySet().iterator();

    TraceModel traceModel = new TraceModel();

    while (it.hasNext()) {
      String fileName = it.next();
      JSONObject fileObject = files.get(fileName);
      String content = (String) fileObject.get("content");
      byte[] base64decodedBytes = Base64.getDecoder().decode(content);

      try {
        content = new String(base64decodedBytes, "utf-8");

        JSONObject fileTraces = (JSONObject) fileObject.get("fileTraces");
        FileTraceModel oldFileTraceModel = FileTraceModelFactory
            .createFileTraceModelFromJSON(content, fileTraces, traceModel, fileName);

        switch (fileName) {
          case "widget.xml":
            widgetTemplateEngine = new TemplateEngine(
                new SynchronizationOrderedStrategy(oldFileTraceModel), oldFileTraceModel);
            break;
          case "js/applicationScript.js":
            applicationTemplateEngine = new TemplateEngine(
                new SynchronizationStrategy(oldFileTraceModel), oldFileTraceModel);
            break;

        }

      } catch (UnsupportedEncodingException e) {
        logger.printStackTrace(e);
      }

    }

    try {
      // regenerate widget code
      FrontendComponentSynchronization.createWidgetCode(widgetTemplateEngine, widget,
          htmlElementTemplate, yjsImports, gitHubOrganization, getRepositoryName(frontendComponent),
          frontendComponent);

      traceModel.addFileTraceModel(widgetTemplateEngine.getFileTraceModel());

      // regenerate applicationScript code
      Template applicationTemplate = applicationTemplateEngine.createTemplate(
          frontendComponent.getWidgetModelId() + ":applicationScript:", applicationScript);

      applicationTemplateEngine.addTemplate(applicationTemplate);

      createApplicationScript(applicationTemplate, functionTemplate, microserviceCallTemplate,
          iwcResponseTemplate, htmlElementTemplate, frontendComponent);

      // add events to elements
      addEventsToApplicationScript(applicationTemplate, widgetTemplateEngine, eventTemplate,
          frontendComponent);

      // add (possible) Yjs collaboration stuff
      addYjsCollaboration(applicationTemplate, applicationTemplateEngine, yjsInit,
          frontendComponent);

      traceModel.addFileTraceModel(applicationTemplateEngine.getFileTraceModel());

      // commit changes
      updateTracedFilesInRepository(traceModel, getRepositoryName(frontendComponent), service);

    } catch (UnsupportedEncodingException e) {
      logger.printStackTrace(e);
    }
  }

  public static boolean existsRemoteRepositoryForModel(FrontendComponent frontendComponent,
      String gitHubOrganization, String gitHubUser, String gitHubPassword) {
    return existsRemoteRepository(getRepositoryName(frontendComponent), gitHubOrganization,
        gitHubUser, gitHubPassword);
  }

}
