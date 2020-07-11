package i5.las2peer.services.codeGenerationService.generators;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHelperException;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModelFactory;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.templateEngine.*;
import i5.las2peer.services.codeGenerationService.utilities.GitUtility;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.simple.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;

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
   * @param gitAdapter adapter for git
   * @param service name of the service
   * @param metadataDoc metadata string from swagger
   * @param gitUtility GitUtility used for pushing.
   * @param commitMessage Message that should be used for the commit.
   * @param versionTag String which should be used as the tag when commiting. May be null.
   * @return Commit sha identifier.
   * @throws ModelParseException thrown incase of error in model parsing
 * @throws GitHelperException 
   */

  public static String synchronizeSourceCode(Microservice microservice, Microservice oldMicroservice,
      HashMap<String, JSONObject> files, BaseGitHostAdapter gitAdapter, Service service, String metadataDoc,
      GitUtility gitUtility, String commitMessage, String versionTag) throws ModelParseException, GitHelperException {

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

    // monitoring templates
    String genericCustomMessageDescription = null;
    String genericCustomMessageLog = null;
    String genericLogStringPayload = null;
    String genericLogStringPayloadDescription = null;
    String genericLogStringResponse = null;
    String genericLogStringResponseDescription = null;
    String genericLogTimeDifference = null;
    String genericLogTimeDifferenceDescription = null;
    String genericMeasureTime = null;
    String genericMeasureTimeDifference = null;

    // to generate schema file
    String classes = null;
    String genericClassBody = null;
    String genericClassProperty = null;

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
          case "Classes.java":
            classes = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericClassBody.txt":
            genericClassBody = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericClassProperty.txt":
            genericClassProperty = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericCustomMessageDescription.txt":
            genericCustomMessageDescription = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericCustomMessageLog.txt":
            genericCustomMessageLog = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericLogStringPayload.txt":
            genericLogStringPayload = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericLogStringPayloadDescription.txt":
            genericLogStringPayloadDescription = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericLogStringResponse.txt":
            genericLogStringResponse = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericLogStringResponseDescription.txt":
            genericLogStringResponseDescription = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericLogTimeDifference.txt":
            genericLogTimeDifference = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericLogTimeDifferenceDescription.txt":
            genericLogTimeDifferenceDescription = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericMeasureTime.txt":
            genericMeasureTime = new String(loader.getBytes(), "UTF-8");
            break;
          case "genericMeasureTimeDifference.txt":
            genericMeasureTimeDifference = new String(loader.getBytes(), "UTF-8");
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

    String newClassesFileName = getClassesFileName(microservice);

    // old file names
    String serviceOldFileName = getServiceFileName(oldMicroservice);
    String serviceOldPropertiesFileName = getServicePropertiesFileName(oldMicroservice);
    String serviceOldTestFileName = getServiceTestFileName(oldMicroservice);
    String databaseOldScriptFileName = getDatabaseScriptFileName(oldMicroservice);
    String oldDatabaseManagerFileName = "src/main/i5/las2peer/services/"
        + getPackageName(oldMicroservice) + "/database/DatabaseManager.java";

    String oldClassesFileName = getClassesFileName(oldMicroservice);

    // if the old service file was renamed, we need to rename it in the local repo
    if (!serviceFileName.equals(serviceOldFileName)) {
      renameFileInRepository(getRepositoryName(oldMicroservice), serviceFileName,
          serviceOldFileName);
    }

    // if the old service test file was renamed, we need to rename it in the local repo
    if (!serviceTestFileName.equals(serviceOldTestFileName)) {
      renameFileInRepository(getRepositoryName(oldMicroservice), serviceTestFileName,
          serviceOldTestFileName);
    }
    
    // if the old classes file was renamed, we need to rename it in the local repo
    if (!newClassesFileName.equals(oldClassesFileName)) {
        renameFileInRepository(getRepositoryName(oldMicroservice), newClassesFileName,
                oldClassesFileName);
    }

    // if the old service properties file was renamed, we need to rename it in the local repo
    if (!servicePropertiesFileName.equals(serviceOldPropertiesFileName)) {
      renameFileInRepository(getRepositoryName(oldMicroservice), servicePropertiesFileName,
          serviceOldPropertiesFileName);
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
      deleteFileInLocalRepository(getRepositoryName(oldMicroservice), oldDatabaseManagerFileName);
    } else if (!oldDatabaseManagerFileName.equals(newDatabaseManagerFileName)) {
      renameFileInRepository(getRepositoryName(oldMicroservice), newDatabaseManagerFileName,
          oldDatabaseManagerFileName);
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
      deleteFileInLocalRepository(getRepositoryName(oldMicroservice), databaseOldScriptFileName);
    } else if (!databaseOldScriptFileName.equals(databaseScriptFileName)) {

      renameFileInRepository(getRepositoryName(oldMicroservice), databaseScriptFileName,
          databaseOldScriptFileName);
    }


    while (it.hasNext()) {
      String fileName = it.next();
      JSONObject fileObject = files.get(fileName);
      String content = (String) fileObject.get("content");
      byte[] base64decodedBytes = Base64.getDecoder().decode(content);

      try {
        Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "Synchronizing " + fileName + " now ...");
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
              genericCustomMessageDescription, genericCustomMessageLog, genericLogStringPayload,
              genericLogStringPayloadDescription, genericLogStringResponse, genericLogStringResponseDescription,
              genericLogTimeDifference, genericLogTimeDifferenceDescription, genericMeasureTime, genericMeasureTimeDifference,
              databaseConfig, databaseInstantiation, serviceInvocation, metadataDoc);
        } else if (fileName.equals(serviceOldTestFileName)) {
          oldFileTraceModel.setFileName(serviceTestFileName);
          generateNewServiceTest(templateEngine, serviceTest, microservice, genericTestCase);
        } else if (fileName.equals(oldClassesFileName)) {
            oldFileTraceModel.setFileName(newClassesFileName);
   
            String repositoryLocation =
                    gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + getRepositoryName(microservice);

            generateNewClasses(templateEngine, classes, microservice, repositoryLocation,genericClassBody, genericClassProperty, metadataDoc);
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

        Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "... " + fileName + " synchronized.");

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
      String commitSha = updateTracedFilesInRepository(getUpdatedTracedFilesForRepository(traceModel, guidances),
          getRepositoryName(microservice), service, commitMessage, versionTag);
      
      // merge development and master and push to master
   	  String masterBranchName = "master";
   	  gitUtility.mergeIntoMasterBranch(getRepositoryName(microservice), masterBranchName, versionTag);
   	  
   	  return commitSha;
    } catch (UnsupportedEncodingException e) {
      logger.printStackTrace(e);
      return "";
    }

  }

  public static boolean existsRemoteRepositoryForModel(Microservice microservice, BaseGitHostAdapter gitAdapter) {
    return existsRemoteRepository(getRepositoryName(microservice), gitAdapter);
  }
}