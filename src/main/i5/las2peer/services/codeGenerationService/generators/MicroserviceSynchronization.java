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

import i5.las2peer.api.Service;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModelFactory;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.templateEngine.InitialGenerationStrategy;
import i5.las2peer.services.codeGenerationService.templateEngine.SynchronizationStrategy;
import i5.las2peer.services.codeGenerationService.templateEngine.Template;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateEngine;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateStrategy;

/**
 * 
 * Synchronizes the source code of a
 * {@link i5.las2peer.services.codeGenerationService.models.microservice.Microservice} model with an
 * updated version of that model
 */

public class MicroserviceSynchronization extends MicroserviceGenerator {

  private static final L2pLogger logger =
      L2pLogger.getInstance(ApplicationGenerator.class.getName());

  /**
   * Synchronize the source code of a
   * {@link i5.las2peer.services.codeGenerationService.models.microservice.Microservice} model with
   * an updated version of that model
   * 
   * @param microservice The updated microservice model
   * @param oldMicroservice The current/old microservice model
   * @param files The traced files with the current source code
   * @param service An instance of {@link i5.las2peer.api.Service} needed to invoke the GitHubProxy
   *        service
 * @throws ModelParseException 
   */

  public static void synchronizeSourceCode(Microservice microservice, Microservice oldMicroservice,
      HashMap<String, JSONObject> files, BaseGitHostAdapter gitAdapter, Service service) throws ModelParseException {

    // first load the needed templates from the template repository

    // variables holding the template source code
    String serviceClass = null;
    String serviceTest = null;
    String serviceProperties = null;
    String genericHttpMethod = null;
    String genericHttpMethodBody = null;
    String genericApiResponse = null;
    String genericHttpResponse = null;
    String genericTestCase = null;
    String databaseConfig = null;
    String databaseInstantiation = null;
    String serviceInvocation = null;
    String databaseScript = null;
    String genericTable = null;
    String databaseManager = null;
    String guidances = null;

    try (TreeWalk treeWalk =
        getTemplateRepositoryContent(gitAdapter)) {
      // now load the TreeWalk containing the template repository content
      treeWalk.setFilter(PathFilter.create("backend/"));
      ObjectReader reader = treeWalk.getObjectReader();
      // walk through the tree and retrieve the needed templates
      while (treeWalk.next()) {
        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = reader.open(objectId);
        switch (treeWalk.getNameString()) {
          case "genericHTTPMethod.txt":
            genericHttpMethod = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericHTTPMethodBody.txt":
            genericHttpMethodBody = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericHTTPResponse.txt":
            genericHttpResponse = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericApiResponse.txt":
            genericApiResponse = new String(loader.getBytes(), "UTF-8");
            break;
          case "ServiceClass.java":
            serviceClass = new String(loader.getBytes(), "UTF-8");
            break;
          case "ServiceTest.java":
            serviceTest = new String(loader.getBytes(), "UTF-8");
            break;
          case "i5.las2peer.services.servicePackage.ServiceClass.properties":
            serviceProperties = new String(loader.getBytes(), "UTF-8");
          case "genericTestMethod.txt":
            genericTestCase = new String(loader.getBytes(), "UTF-8");
            break;
          case "guidances.json":
            guidances = new String(loader.getBytes(), "UTF-8");
            break;
          case "databaseConfig.txt":
            databaseConfig = new String(loader.getBytes(), "UTF-8");
            break;
          case "databaseInstantiation.txt":
            databaseInstantiation = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericServiceInvocation.txt":
            serviceInvocation = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericTable.txt":
            genericTable = new String(loader.getBytes(), "UTF-8");
            break;
          case "DatabaseManager.java":
            databaseManager = new String(loader.getBytes(), "UTF-8");
            break;
          case "database.sql":
            databaseScript = new String(loader.getBytes(), "UTF-8");
            break;
        }
      }
    } catch (Exception e) {
      logger.printStackTrace(e);
    }



    // new file names
    String serviceFileName = getServiceFileName(microservice);
    String servicePropertiesFileName = getServicePropertiesFileName(microservice);

    String serviceTestFileName = getServiceTestFileName(microservice);
    String databaseScriptFileName = getDatabaseScriptFileName(microservice);
    String newDatabaseManagerFileName = "src/main/i5/las2peer/services/"
        + getPackageName(microservice) + "/database/DatabaseManager.java";

    // old file names
    String serviceOldFileName = getServiceFileName(oldMicroservice);
    String serviceOldPropertiesFileName = getServicePropertiesFileName(oldMicroservice);
    String serviceOldTestFileName = getServiceTestFileName(oldMicroservice);
    String databaseOldScriptFileName = getDatabaseScriptFileName(oldMicroservice);
    String oldDatabaseManagerFileName = "src/main/i5/las2peer/services/"
        + getPackageName(oldMicroservice) + "/database/DatabaseManager.java";

    // if the old service file was renamed, we need to rename it in the local repo
    if (!serviceFileName.equals(serviceOldFileName)) {
      renameFileInRepository(getRepositoryName(oldMicroservice), serviceFileName,
          serviceOldFileName, service);
    }

    // if the old service test file was renamed, we need to rename it in the local repo
    if (!serviceTestFileName.equals(serviceOldTestFileName)) {
      renameFileInRepository(getRepositoryName(oldMicroservice), serviceTestFileName,
          serviceOldTestFileName, service);
    }

    // if the old service properties file was renamed, we need to rename it in the local repo
    if (!servicePropertiesFileName.equals(serviceOldPropertiesFileName)) {
      renameFileInRepository(getRepositoryName(oldMicroservice), servicePropertiesFileName,
          serviceOldPropertiesFileName, service);
    }

    // now loop through the traced files and synchronize them

    Iterator<String> it = files.keySet().iterator();

    TraceModel traceModel = new TraceModel();


    // special case for database manager, as it is not always traced
    if (!files.containsKey(oldDatabaseManagerFileName) && microservice.getDatabase() != null) {
      generateOtherArtifacts(
          Template.createInitialTemplateEngine(traceModel, newDatabaseManagerFileName),
          microservice, gitAdapter.getGitOrganization(), databaseManager);
    } else if (files.containsKey(oldDatabaseManagerFileName)
        && microservice.getDatabase() == null) {
      deleteFileInLocalRepository(getRepositoryName(oldMicroservice), oldDatabaseManagerFileName,
          service);
    } else if (!oldDatabaseManagerFileName.equals(newDatabaseManagerFileName)) {
      renameFileInRepository(getRepositoryName(oldMicroservice), newDatabaseManagerFileName,
          oldDatabaseManagerFileName, service);
    }

    // special case for database script, as it is not always traced
    if (!files.containsKey(databaseOldScriptFileName) && microservice.getDatabase() != null) {
      FileTraceModel databaseScriptTraceModel =
          new FileTraceModel(traceModel, getDatabaseScriptFileName(microservice));
      traceModel.addFileTraceModel(databaseScriptTraceModel);

      TemplateEngine databaseScriptTemplateEngine =
          new TemplateEngine(new InitialGenerationStrategy(), databaseScriptTraceModel);

      generateDatabaseScript(databaseScriptTemplateEngine, databaseScript, genericTable,
          microservice);

    } else if (files.containsKey(databaseOldScriptFileName) && microservice.getDatabase() == null) {
      deleteFileInLocalRepository(getRepositoryName(oldMicroservice), databaseOldScriptFileName,
          service);
    } else if (!databaseOldScriptFileName.equals(databaseScriptFileName)) {

      renameFileInRepository(getRepositoryName(oldMicroservice), databaseScriptFileName,
          databaseOldScriptFileName, service);
    }


    while (it.hasNext()) {
      String fileName = it.next();
      JSONObject fileObject = files.get(fileName);
      String content = (String) fileObject.get("content");
      byte[] base64decodedBytes = Base64.getDecoder().decode(content);

      try {
        L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Synchronizing " + fileName + " now ...");
        content = new String(base64decodedBytes, "utf-8");
        JSONObject fileTraces = (JSONObject) fileObject.get("fileTraces");
        FileTraceModel oldFileTraceModel = FileTraceModelFactory
            .createFileTraceModelFromJSON(content, fileTraces, traceModel, fileName);
        TemplateStrategy strategy = new SynchronizationStrategy(oldFileTraceModel);

        TemplateEngine templateEngine = new TemplateEngine(strategy, oldFileTraceModel);

        if (fileName.equals(serviceOldFileName)) {
          oldFileTraceModel.setFileName(serviceFileName);
          
          String repositoryLocation =
              gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + getRepositoryName(microservice);

          generateNewServiceClass(templateEngine, serviceClass, microservice, repositoryLocation,
              genericHttpMethod, genericHttpMethodBody, genericApiResponse, genericHttpResponse,
              databaseConfig, databaseInstantiation, serviceInvocation);
        } else if (fileName.equals(serviceOldTestFileName)) {
          oldFileTraceModel.setFileName(serviceTestFileName);
          generateNewServiceTest(templateEngine, serviceTest, microservice, genericTestCase);
        } else if (fileName.equals(databaseOldScriptFileName)) {
          if (microservice.getDatabase() == null) {
            templateEngine = null;
          } else {
            oldFileTraceModel.setFileName(databaseScriptFileName);
            generateDatabaseScript(templateEngine, databaseScript, genericTable, microservice);
          }
        } else if (fileName.equals(oldDatabaseManagerFileName)) {
          if (microservice.getDatabase() == null) {
            templateEngine = null;
          } else {
            oldFileTraceModel.setFileName(newDatabaseManagerFileName);
            generateOtherArtifacts(templateEngine, microservice, gitAdapter.getGitOrganization(), content);
          }
        } else if (fileName.equals(serviceOldPropertiesFileName)) {
          content = serviceProperties;
          oldFileTraceModel.setFileName(servicePropertiesFileName);
          generateOtherArtifacts(templateEngine, microservice, gitAdapter.getGitOrganization(), content);
        } else {
          generateOtherArtifacts(templateEngine, microservice, gitAdapter.getGitOrganization(), content);
        }

        L2pLogger.logEvent(Event.SERVICE_MESSAGE, "... " + fileName + " synchronized.");

        // finally add the file trace model to the global trace model
        if (templateEngine != null) {
          traceModel.addFileTraceModel(templateEngine.getFileTraceModel());
        }
      } catch (UnsupportedEncodingException e) {
        logger.printStackTrace(e);
      }

    }

    try {
      // commit changes
      updateTracedFilesInRepository(getUpdatedTracedFilesForRepository(traceModel, guidances),
          getRepositoryName(microservice), service);
    } catch (UnsupportedEncodingException e) {
      logger.printStackTrace(e);
    }

  }

  public static boolean existsRemoteRepositoryForModel(Microservice microservice, BaseGitHostAdapter gitAdapter) {
    return existsRemoteRepository(getRepositoryName(microservice), gitAdapter);
  }
}
