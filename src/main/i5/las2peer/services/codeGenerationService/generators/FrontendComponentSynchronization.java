package i5.las2peer.services.codeGenerationService.generators;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.simple.JSONObject;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.services.codeGenerationService.CodeGenerationService;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHostException;
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
      FrontendComponent oldFrontendComponent, HashMap<String, JSONObject> files,BaseGitHostAdapter gitAdapter,CodeGenerationService service)
      throws GitHostException {
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
    String yjs = null;
    String yArray = null;
    String yText = null;
    String yMemory = null;
    String yWebsockets = null;
    String yjsImports = null;
    String yjsInit = null;
    String guidances = null;

    TemplateEngine applicationTemplateEngine = null;
    TemplateEngine widgetTemplateEngine = null;

    SynchronizationStrategy applicationSynchronizationStrategy = null;
    SynchronizationStrategy widgetSynchronizationStrategy = null;

    try (TreeWalk treeWalk =
        getTemplateRepositoryContent(gitAdapter)) {
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
          case "y.js":
            yjs = new String(loader.getBytes(), "UTF-8");
            break;
          case "y-array.js":
            yArray = new String(loader.getBytes(), "UTF-8");
            break;
          case "y-text.js":
            yText = new String(loader.getBytes(), "UTF-8");
            break;
          case "y-websockets-client.js":
            yWebsockets = new String(loader.getBytes(), "UTF-8");
            break;
          case "y-memory.js":
            yMemory = new String(loader.getBytes(), "UTF-8");
            break;
          case "yjs-imports.txt":
            yjsImports = new String(loader.getBytes(), "UTF-8");
            break;
          case "yjsInit.txt":
            yjsInit = new String(loader.getBytes(), "UTF-8");
            break;
          case "guidances.json":
            guidances = new String(loader.getBytes(), "UTF-8");
            break;
        }
      }
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
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
            widgetSynchronizationStrategy = new SynchronizationOrderedStrategy(oldFileTraceModel);
            widgetTemplateEngine =
                new TemplateEngine(widgetSynchronizationStrategy, oldFileTraceModel);
            break;
          case "js/applicationScript.js":
            applicationSynchronizationStrategy = new SynchronizationStrategy(oldFileTraceModel);
            applicationTemplateEngine =
                new TemplateEngine(applicationSynchronizationStrategy, oldFileTraceModel);
            break;

        }

      } catch (UnsupportedEncodingException e) {
        logger.printStackTrace(e);
      }

    }

    try {
      L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Synchronizing widget now ...");
      applicationSynchronizationStrategy
          .addAditionalOldFileTraceModel(widgetTemplateEngine.getFileTraceModel());
      widgetSynchronizationStrategy
          .addAditionalOldFileTraceModel(applicationTemplateEngine.getFileTraceModel());


      // regenerate widget code
      createWidgetCode(widgetTemplateEngine, widget, htmlElementTemplate, yjsImports,
          gitAdapter.getGitOrganization(), getRepositoryName(frontendComponent), frontendComponent);

      traceModel.addFileTraceModel(widgetTemplateEngine.getFileTraceModel());

      // regenerate applicationScript code
      Template applicationTemplate = applicationTemplateEngine.createTemplate(
          frontendComponent.getWidgetModelId() + ":applicationScript:", applicationScript);

      applicationTemplateEngine.addTemplate(applicationTemplate);

      createApplicationScript(applicationTemplate, functionTemplate, microserviceCallTemplate,
          iwcResponseTemplate, htmlElementTemplate, frontendComponent);

      // add events to elements
      addEventsToApplicationScript(applicationTemplate, applicationTemplateEngine, eventTemplate,
          frontendComponent);

      // add (possible) Yjs collaboration stuff
      addYjsCollaboration(applicationTemplate, applicationTemplateEngine, yjsInit,
          frontendComponent);

      traceModel.addFileTraceModel(applicationTemplateEngine.getFileTraceModel());

      L2pLogger.logEvent(Event.SERVICE_MESSAGE, "... widget synchronized.");

      // commit changes
      List<String[]> fileList = getUpdatedTracedFilesForRepository(traceModel, guidances);

      if (widgetTemplateEngine.getContent().contains("/js/lib/y.js")) {
        fileList.add(new String[] {"js/lib/y.js",
            Base64.getEncoder().encodeToString(yjs.getBytes("utf-8"))});
        fileList.add(new String[] {"js/lib/y-array.js",
            Base64.getEncoder().encodeToString(yArray.getBytes("utf-8"))});
        fileList.add(new String[] {"js/lib/y-text.js",
            Base64.getEncoder().encodeToString(yText.getBytes("utf-8"))});
        fileList.add(new String[] {"js/lib/y-websockets-client.js",
            Base64.getEncoder().encodeToString(yWebsockets.getBytes("utf-8"))});
        fileList.add(new String[] {"js/lib/y-memory.js",
            Base64.getEncoder().encodeToString(yMemory.getBytes("utf-8"))});
      }


      updateTracedFilesInRepository(fileList, getRepositoryName(frontendComponent), service);

    } catch (UnsupportedEncodingException e) {
      logger.printStackTrace(e);
    }
  }

  public static boolean existsRemoteRepositoryForModel(FrontendComponent frontendComponent, BaseGitHostAdapter gitAdapter) {
    return existsRemoteRepository(getRepositoryName(frontendComponent), gitAdapter);
  }

}
