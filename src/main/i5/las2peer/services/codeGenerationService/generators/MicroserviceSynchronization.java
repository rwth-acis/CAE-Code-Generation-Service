package i5.las2peer.services.codeGenerationService.generators;

import java.awt.image.BufferedImage;
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
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModelFactory;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.templateEngine.SynchronizationStrategy;
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
   * Synchronizes the source code of a
   * {@link i5.las2peer.services.codeGenerationService.models.microservice.Microservice} model with
   * an updated version of that model
   * 
   * @param microservice The updated microservice model
   * @param oldMicroservice The current microservice model
   * @param files The traced files with the current source code
   * @param templateRepositoryName the name of the template repository on GitHub
   * @param gitHubOrganization the organization that is used in the CAE
   * @param service An instance of
   *        {@link i5.las2peer.services.codeGenerationService.generators.Generator} needed to invoke
   *        the GitHubProxy service
   */

  public static void synchronizeSourceCode(Microservice microservice, Microservice oldMicroservice,
      HashMap<String, JSONObject> files, String templateRepositoryName, String gitHubOrganization,
      CodeGenerationService service) {

    // first load the needed templates from the template repository

    // helper variables
    String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1);

    // variables holding content to be modified and added to repository later
    String projectFile = null;
    BufferedImage logo = null;
    String readMe = null;
    String license = null;
    String buildFile = null;
    String startScriptWindows = null;
    String startScriptUnix = null;
    String userAgentGeneratorWindows = null;
    String userAgentGeneratorUnix = null;
    String nodeInfo = null;
    String antServiceProperties = null;
    String antUserProperties = null;
    String ivy = null;
    String ivySettings = null;
    String serviceProperties = null;
    String webConnectorConfig = null;
    String gitignore = null;
    String classpath = null;
    String databaseManager = null;
    String serviceClass = null;
    String serviceTest = null;
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


    try (TreeWalk treeWalk =
        getTemplateRepositoryContent(templateRepositoryName, gitHubOrganization)) {
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
          case "genericTestMethod.txt":
            genericTestCase = new String(loader.getBytes(), "UTF-8");
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
        }
      }
    } catch (Exception e) {
      logger.printStackTrace(e);
    }

    // new file names
    String serviceFileName = getServiceFileName(microservice);
    String serviceTestFileName = getServiceTestFileName(microservice);

    // old file names
    String serviceOldFileName = getServiceFileName(oldMicroservice);
    String serviceOldTestFileName = getServiceTestFileName(oldMicroservice);

    System.out.println(serviceFileName + "=" + serviceOldFileName);
    System.out.println(serviceTestFileName + "=" + serviceOldTestFileName);

    // if the old service file was renamed, we need to remove it from the local repo
    if (!serviceFileName.equals(serviceOldFileName)) {
      renameFileInRepository(getRepositoryName(oldMicroservice), serviceFileName,
          serviceOldFileName, service);
    }

    // if the old service test file was renamed, we need to remove it from the local repo
    if (!serviceTestFileName.equals(serviceOldTestFileName)) {
      renameFileInRepository(getRepositoryName(oldMicroservice), serviceTestFileName,
          serviceOldTestFileName, service);
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
            .createFileTraceModelFromJSON(content, fileTraces.toJSONString(), traceModel, fileName);
        TemplateStrategy strategy = new SynchronizationStrategy(oldFileTraceModel);

        TemplateEngine templateEngine = new TemplateEngine(strategy, oldFileTraceModel);

        System.out.println("synchronize " + fileName);
        if (fileName.equals(serviceOldFileName)) {
          oldFileTraceModel.setFileName(serviceFileName);
          String repositoryLocation =
              "https://github.com/" + gitHubOrganization + "/" + getRepositoryName(microservice);

          generateNewServiceClass(templateEngine, serviceClass, microservice, repositoryLocation,
              genericHttpMethod, genericHttpMethodBody, genericApiResponse, genericHttpResponse,
              databaseConfig, databaseInstantiation, serviceInvocation);
        } else if (fileName.equals(serviceOldTestFileName)) {
          oldFileTraceModel.setFileName(serviceTestFileName);
          generateNewServiceTest(templateEngine, serviceTest, microservice, genericTestCase);
        } else {
          generateOther(templateEngine, oldMicroservice, gitHubOrganization, content);
        }

        traceModel.addFileTraceModel(templateEngine.getFileTraceModel());
        // commitTracedFile(templateEngine.getFileTraceModel().getFileName(), templateEngine,
        // "code regeneration " + fileName, service, getRepositoryName(microservice));

      } catch (UnsupportedEncodingException e) {
        logger.printStackTrace(e);
      }

    }



    try {
      // commit changes
      updateTracedFilesInRepository(traceModel, getRepositoryName(microservice), service);
      // // commit global trace model
      // String tracedFiles = traceModel.toJSONObject().toJSONString().replace("\\", "");
      // service.commitFileRaw(getRepositoryName(microservice), "traces/tracedFiles.json",
      // Base64.getEncoder().encodeToString(tracedFiles.getBytes("utf-8")));
    } catch (UnsupportedEncodingException e) {
      logger.printStackTrace(e);
    }

  }
}
