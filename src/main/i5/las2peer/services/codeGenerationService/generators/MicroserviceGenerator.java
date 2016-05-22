package i5.las2peer.services.codeGenerationService.generators;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.models.microservice.Column;
import i5.las2peer.services.codeGenerationService.models.microservice.Database;
import i5.las2peer.services.codeGenerationService.models.microservice.HttpMethod;
import i5.las2peer.services.codeGenerationService.models.microservice.HttpPayload;
import i5.las2peer.services.codeGenerationService.models.microservice.HttpPayload.PayloadType;
import i5.las2peer.services.codeGenerationService.models.microservice.HttpResponse;
import i5.las2peer.services.codeGenerationService.models.microservice.HttpResponse.ResultType;
import i5.las2peer.services.codeGenerationService.models.microservice.InternalCall;
import i5.las2peer.services.codeGenerationService.models.microservice.InternalCallParam;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;
import i5.las2peer.services.codeGenerationService.models.microservice.Table;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.templateEngine.InitialGenerationStrategy;
import i5.las2peer.services.codeGenerationService.templateEngine.Template;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateEngine;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateStrategy;

/**
 * 
 * Generates microservice source code from passed on
 * {@link i5.las2peer.services.codeGenerationService.models.microservice.Microservice} models.
 * 
 */
public class MicroserviceGenerator extends Generator {

  private static final L2pLogger logger =
      L2pLogger.getInstance(ApplicationGenerator.class.getName());


  /**
   * Returns the repository name for the given microservice model
   * 
   * @param microservice the mircoservice model
   * @return The name of the repository
   */

  public static String getRepositoryName(Microservice microservice) {
    String repositoryName = "microservice-" + microservice.getName().replace(" ", "-");
    return repositoryName;
  }

  /**
   * 
   * Creates source code from a CAE microservice model and pushes it to GitHub.
   * 
   * @param microservice the microservice model
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
  public static void createSourceCode(Microservice microservice, String templateRepositoryName,
      String gitHubOrganization, String gitHubUser, String gitHubUserMail, String gitHubPassword)
      throws GitHubException {

    // variables to be closed in the final block
    Repository microserviceRepository = null;
    TreeWalk treeWalk = null;

    // helper variables
    String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1);
    // get the port: skip first 6 characters for search (http: / https:)
    String port = microservice.getPath().substring(microservice.getPath().indexOf(":", 6) + 1,
        microservice.getPath().indexOf("/", microservice.getPath().indexOf(":", 6)));

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

    try {
      PersonIdent caeUser = new PersonIdent(gitHubUser, gitHubUserMail);
      String repositoryName = getRepositoryName(microservice);
      microserviceRepository =
          generateNewRepository(repositoryName, gitHubOrganization, gitHubUser, gitHubPassword);
      TraceModel traceModel = new TraceModel();
      try {

        // now load the TreeWalk containing the template repository content
        treeWalk = getTemplateRepositoryContent(templateRepositoryName, gitHubOrganization);
        treeWalk.setFilter(PathFilter.create("backend/"));
        ObjectReader reader = treeWalk.getObjectReader();
        // walk through the tree and retrieve the needed templates
        while (treeWalk.next()) {
          ObjectId objectId = treeWalk.getObjectId(0);
          ObjectLoader loader = reader.open(objectId);

          switch (treeWalk.getNameString()) {
            // start with the "easy" replacements, and store the other template files for later
            case ".project":
              projectFile = new String(loader.getBytes(), "UTF-8");

              Template projectTemplate =
                  Template.createInitialTemplate(microservice.getMicroserviceModelId() + ":project",
                      projectFile, traceModel, ".project");
              projectTemplate.setVariable("$Microservice_Name$", microservice.getName());

              projectFile = projectTemplate.getTemplateEngine().getContent();

              break;

            case "logo_services.png":
              logo = ImageIO.read(loader.openStream());
              break;
            case "README.md":
              readMe = new String(loader.getBytes(), "UTF-8");

              Template readMeTemplate =
                  Template.createInitialTemplate(microservice.getMicroserviceModelId() + ":readMe",
                      readMe, traceModel, "README.md");
              readMeTemplate.setVariable("$Repository_Name$", repositoryName);
              readMeTemplate.setVariable("$Organization_Name$", gitHubOrganization);
              readMeTemplate.setVariable("$Microservice_Name$", microservice.getName());

              readMe = readMeTemplate.getTemplateEngine().getContent();

              break;
            case "LICENSE.txt":
              license = new String(loader.getBytes(), "UTF-8");
              break;
            case "build.xml":
              buildFile = new String(loader.getBytes(), "UTF-8");

              Template buildFileTemplate = Template.createInitialTemplate(
                  microservice.getMicroserviceModelId() + ":buildFile", buildFile, traceModel,
                  treeWalk.getPathString());
              buildFileTemplate.setVariable("$Microservice_Name$", microservice.getName());

              buildFile = buildFileTemplate.getContent();
              break;
            case "start_network.bat":
              startScriptWindows = new String(loader.getBytes(), "UTF-8");

              Template startScriptWindowsTemplate = Template.createInitialTemplate(
                  microservice.getMicroserviceModelId() + ":startScriptWindows", startScriptWindows,
                  traceModel, treeWalk.getPathString());

              startScriptWindowsTemplate.setVariable("$Resource_Name$",
                  microservice.getResourceName());
              startScriptWindowsTemplate.setVariable("$Lower_Resource_Name$", packageName);
              startScriptWindowsTemplate.setVariable("$Microservice_Version$",
                  microservice.getVersion() + "");

              startScriptWindows = startScriptWindowsTemplate.getContent();
              break;
            case "start_network.sh":
              startScriptUnix = new String(loader.getBytes(), "UTF-8");

              Template startScriptUnixTemplate = Template.createInitialTemplate(
                  microservice.getMicroserviceModelId() + ":startScriptUnix", startScriptUnix,
                  traceModel, treeWalk.getPathString());

              startScriptUnixTemplate.setVariable("$Resource_Name$",
                  microservice.getResourceName());
              startScriptUnixTemplate.setVariable("$Lower_Resource_Name$", packageName);
              startScriptUnixTemplate.setVariable("$Microservice_Version$",
                  microservice.getVersion() + "");

              startScriptUnix = startScriptUnixTemplate.getContent();
              break;
            case "start_UserAgentGenerator.bat":
              userAgentGeneratorWindows = new String(loader.getBytes(), "UTF-8");
              break;
            case "start_UserAgentGenerator.sh":
              userAgentGeneratorUnix = new String(loader.getBytes(), "UTF-8");
              break;
            case "nodeInfo.xml":
              nodeInfo = new String(loader.getBytes(), "UTF-8");

              Template nodeInfoTemplate = Template.createInitialTemplate(
                  microservice.getMicroserviceModelId() + ":nodeInfo", nodeInfo, traceModel,
                  treeWalk.getPathString());


              nodeInfoTemplate.setVariable("$Developer$", microservice.getDeveloper());
              nodeInfoTemplate.setVariable("$Resource_Name$", microservice.getResourceName());

              nodeInfo = nodeInfoTemplate.getContent();
              break;
            case "service.properties":
              antServiceProperties = new String(loader.getBytes(), "UTF-8");

              Template antServicePropertiesTemplate = Template.createInitialTemplate(
                  microservice.getMicroserviceModelId() + ":antServiceProperties",
                  antServiceProperties, traceModel, treeWalk.getPathString());

              antServicePropertiesTemplate.setVariable("$Microservice_Version$",
                  microservice.getVersion() + "");
              antServicePropertiesTemplate.setVariable("$Lower_Resource_Name$", packageName);
              antServicePropertiesTemplate.setVariable("$Resource_Name$",
                  microservice.getResourceName());
              antServicePropertiesTemplate.setVariable("$Microservice_Version$",
                  microservice.getVersion() + "");

              antServiceProperties = antServicePropertiesTemplate.getContent();
              break;
            case "user.properties":
              antUserProperties = new String(loader.getBytes(), "UTF-8");
              break;
            case "ivy.xml":
              ivy = new String(loader.getBytes(), "UTF-8");

              Template ivyTemplate =
                  Template.createInitialTemplate(microservice.getMicroserviceModelId() + ":ivy",
                      ivy, traceModel, treeWalk.getPathString());

              // add mysql dependency only if a database exists
              if (microservice.getDatabase() != null) {
                ivyTemplate.setVariable("$MySQL_Dependencies$",
                    "<dependency org=\"mysql\" name=\"mysql-connector-java\" rev=\"5.1.6\" />\n"
                        + "    <dependency org=\"org.apache.commons\" name=\"commons-pool2\" rev=\"2.2\" />\n"
                        + "    <dependency org=\"org.apache.commons\" name=\"commons-dbcp2\" rev=\"2.0\" />");
              } else {
                ivyTemplate.setVariable("$MySQL_Dependencies", "");
              }
              ivy = ivyTemplate.getContent();
              break;
            case "ivysettings.xml":
              ivySettings = new String(loader.getBytes(), "UTF-8");
              break;
            case "i5.las2peer.services.servicePackage.ServiceClass.properties":
              serviceProperties = new String(loader.getBytes(), "UTF-8");
              // if database does not exist, clear the file
              if (microservice.getDatabase() == null) {
                serviceProperties = "";
              } else {

                Template servicePropertiesTemplate = Template.createInitialTemplate(
                    microservice.getMicroserviceModelId() + ":serviceProperties", serviceProperties,
                    traceModel, treeWalk.getPathString());

                servicePropertiesTemplate.setVariable("$Database_Address$",
                    microservice.getDatabase().getAddress());
                servicePropertiesTemplate.setVariable("$Database_Schema$",
                    microservice.getDatabase().getSchema());
                servicePropertiesTemplate.setVariable("$Database_User$",
                    microservice.getDatabase().getLoginName());
                servicePropertiesTemplate.setVariable("$Database_Password$",
                    microservice.getDatabase().getLoginPassword());

                serviceProperties = servicePropertiesTemplate.getContent();
              }
            case "i5.las2peer.webConnector.WebConnector.properties":
              webConnectorConfig = new String(loader.getBytes(), "UTF-8");
              Template webConnectorConfigTemplate = Template.createInitialTemplate(
                  microservice.getMicroserviceModelId() + ":webConnectorConfig", serviceProperties,
                  traceModel, treeWalk.getPathString());
              webConnectorConfigTemplate.setVariable("$HTTP_Port$", port);
              webConnectorConfig = webConnectorConfigTemplate.getContent();
              break;
            case ".gitignore":
              gitignore = new String(loader.getBytes(), "UTF-8");
              break;
            case ".classpath":
              classpath = new String(loader.getBytes(), "UTF-8");
              Template classpathTemplate = Template.createInitialTemplate(
                  microservice.getMicroserviceModelId() + ":classpath", classpath, traceModel,
                  treeWalk.getPathString());
              if (microservice.getDatabase() != null) {
                classpathTemplate.setVariable("$Database_Libraries$",
                    "<classpathentry kind=\"lib\" path=\"lib/mysql-connector-java-5.1.6.jar\"/>\n"
                        + "  <classpathentry kind=\"lib\" path=\"lib/commons-dbcp2-2.0.jar\"/>");
              } else {
                classpathTemplate.setVariable("$Database_Libraries$", "");
              }
              classpath = classpathTemplate.getContent();
              break;
            case "DatabaseManager.java":
              if (microservice.getDatabase() != null) {
                databaseManager = new String(loader.getBytes(), "UTF-8");
                databaseManager = databaseManager.replace("$Lower_Resource_Name$", packageName);
              }
              break;
            case "ServiceClass.java":
              serviceClass = new String(loader.getBytes(), "UTF-8");
              break;
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
            case "database.sql":
              databaseScript = new String(loader.getBytes(), "UTF-8");
              break;
            case "genericTable.txt":
              genericTable = new String(loader.getBytes(), "UTF-8");
              break;
          }
        }
      } catch (Exception e) {
        logger.printStackTrace(e);
        throw new GitHubException(e.getMessage());
      }

      // generate service class and test
      String repositoryLocation = "https://github.com/" + gitHubOrganization + "/" + repositoryName;

      FileTraceModel serviceClassTraceModel =
          new FileTraceModel(traceModel, "src/main/i5/las2peer/services/" + packageName + "/"
              + microservice.getResourceName() + ".java");
      traceModel.addFileTraceModel(serviceClassTraceModel);

      TemplateStrategy strategy = new InitialGenerationStrategy(serviceClassTraceModel);
      TemplateEngine serviceTemplateEngine = new TemplateEngine(strategy, serviceClassTraceModel);

      serviceClass = generateNewServiceClass(serviceTemplateEngine, serviceClass, microservice,
          repositoryLocation, genericHttpMethod, genericHttpMethodBody, genericApiResponse,
          genericHttpResponse, databaseConfig, databaseInstantiation, serviceInvocation);

      FileTraceModel serviceTestTraceModel =
          new FileTraceModel(traceModel, "src/test/i5/las2peer/services/" + packageName + "/"
              + microservice.getResourceName() + ".java");
      traceModel.addFileTraceModel(serviceTestTraceModel);

      TemplateEngine serviceTestTemplateEngine = new TemplateEngine(
          new InitialGenerationStrategy(serviceTestTraceModel), serviceTestTraceModel);

      serviceTest = generateNewServiceTest(serviceTestTemplateEngine, serviceTest, microservice,
          genericTestCase);
      if (microservice.getDatabase() != null) {
        databaseScript = generateDatabaseScript(databaseScript, genericTable, microservice);
      }
      // add files to new repository
      // configuration and build stuff
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "etc/ivy/", "ivy.xml", ivy);
      microserviceRepository = createTextFileInRepository(microserviceRepository, "etc/ivy/",
          "ivysettings.xml", ivySettings);

      microserviceRepository = createTextFileInRepository(microserviceRepository,
          "etc/ant_configuration/", "user.properties", antUserProperties);
      microserviceRepository = createTextFileInRepository(microserviceRepository,
          "etc/ant_configuration/", "service.properties", antServiceProperties);
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "etc/", "nodeInfo.xml", nodeInfo);


      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "", ".gitignore", gitignore);
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "", ".classpath", classpath);
      // property files
      microserviceRepository = createTextFileInRepository(microserviceRepository, "etc/",
          "i5.las2peer.services." + packageName + "." + microservice.getResourceName()
              + ".properties",
          serviceProperties);
      microserviceRepository = createTextFileInRepository(microserviceRepository, "etc/",
          "i5.las2peer.webConnector.WebConnector.properties", webConnectorConfig);
      // scripts
      microserviceRepository = createTextFileInRepository(microserviceRepository, "bin/",
          "start_network.bat", startScriptWindows);
      microserviceRepository = createTextFileInRepository(microserviceRepository, "bin/",
          "start_network.sh", startScriptUnix);
      microserviceRepository = createTextFileInRepository(microserviceRepository, "bin/",
          "start_UserAgentGenerator.bat", userAgentGeneratorWindows);
      microserviceRepository = createTextFileInRepository(microserviceRepository, "bin/",
          "start_UserAgentGenerator.sh", userAgentGeneratorUnix);
      // doc
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "", "LICENSE.txt", license);
      microserviceRepository =
          createImageFileInRepository(microserviceRepository, "img/", "logo.png", logo);
      // source code
      if (databaseManager != null) {
        microserviceRepository = createTextFileInRepository(microserviceRepository,
            "src/main/i5/las2peer/services/" + packageName + "/database/", "DatabaseManager.java",
            databaseManager);
        // database script (replace spaces in filename for better usability later on)
        microserviceRepository = createTextFileInRepository(microserviceRepository, "db/",
            microservice.getName().replace(" ", "_") + "_create_tables.sql", databaseScript);
      }

      microserviceRepository = createTextFileInRepository(microserviceRepository,
          "src/test/i5/las2peer/services/" + packageName + "/",
          microservice.getResourceName() + "Test.java", serviceTest);

      createTracedFilesInRepository(traceModel, microserviceRepository);

      // commit files
      try {
        Git.wrap(microserviceRepository).commit()
            .setMessage("Generated microservice version " + microservice.getVersion())
            .setCommitter(caeUser).call();
      } catch (Exception e) {
        logger.printStackTrace(e);
        throw new GitHubException(e.getMessage());
      }

      // push (local) repository content to GitHub repository
      try {
        pushToRemoteRepository(microserviceRepository, gitHubUser, gitHubPassword);
      } catch (Exception e) {
        logger.printStackTrace(e);
        throw new GitHubException(e.getMessage());
      }

      // close all open resources
    } finally {
      microserviceRepository.close();
      treeWalk.close();
    }
  }


  /**
   * 
   * Generates the service class.
   * 
   * @param templateEngine the template engine to use
   * @param serviceClass the service class file
   * @param microservice the microservice model
   * @param repositoryLocation the location of the service's repository
   * @param genericHttpMethod a generic http method template
   * @param genericHttpMethodBody a generic http method body template
   * @param genericApiResponse a generic api response template
   * @param genericHttpResponse a generic http response template
   * @param databaseConfig a database configuration (source code) template
   * @param databaseInstantiation a database instantiation (source code) template
   * @param serviceInvocation a service invocation (source code) template
   * 
   * @return the service class as a string
   * 
   */
  public static String generateNewServiceClass(TemplateEngine templateEngine, String serviceClass,
      Microservice microservice, String repositoryLocation, String genericHttpMethod,
      String genericHttpMethodBody, String genericApiResponse, String genericHttpResponse,
      String databaseConfig, String databaseInstantiation, String serviceInvocation) {
    // helper variables
    String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1);
    String relativeResourcePath =
        microservice.getPath().substring(microservice.getPath().indexOf("/", 8) + 1);
    boolean hasServiceInvocations = false;

    // create template and add to template engine
    Template serviceClassTemplate =
        templateEngine.createTemplate(microservice.getMicroserviceModelId(), serviceClass);
    templateEngine.addTemplate(serviceClassTemplate);

    // service name for documentation
    serviceClassTemplate.setVariable("$Microservice_Name$", microservice.getName());
    // relative resource path (resource base path)
    serviceClassTemplate.setVariable("$Relative_Resource_Path$", relativeResourcePath);
    // version
    serviceClassTemplate.setVariable("$Microservice_Version$", microservice.getVersion() + "");
    // developer
    serviceClassTemplate.setVariable("$Developer$", microservice.getDeveloper());
    // link to license file
    serviceClassTemplate.setVariable("$License_File_Address$",
        repositoryLocation + "/blob/master/LICENSE.txt");
    // resource name
    serviceClassTemplate.setVariable("$Resource_Name$", microservice.getResourceName());
    // create database references only if microservice has database
    if (microservice.getDatabase() != null) {
      // import
      serviceClassTemplate.setVariable("$Database_Import$",
          "import i5.las2peer.services." + packageName + ".database.DatabaseManager;");
      // variable names
      serviceClassTemplate.setVariable("$Database_Configuration$", databaseConfig);
      // instantiation
      serviceClassTemplate.setVariable("$Database_Instantiation$", databaseInstantiation);
    } else {
      // set to empty string
      serviceClassTemplate.setVariable("$Database_Configuration$", "");
      serviceClassTemplate.setVariable("$Database_Instantiation$", "");
      serviceClassTemplate.setVariable("$Database_Import$", "");
    }
    // package and import paths
    serviceClassTemplate.setVariable("$Lower_Resource_Name$", packageName);

    // http methods
    HttpMethod[] httpMethods = microservice.getHttpMethods().values().toArray(new HttpMethod[0]);
    for (int httpMethodIndex = 0; httpMethodIndex < httpMethods.length; httpMethodIndex++) {
      String currentMethodCode = genericHttpMethod; // copy content
      HttpMethod currentMethod = httpMethods[httpMethodIndex];

      // create new template for the current method
      Template currentMethodTemplate = templateEngine
          .createTemplate(currentMethod.getModelId() + ":httpMethod", currentMethodCode);

      // add template of current method to service class template
      serviceClassTemplate.appendVariable("$Service_Methods$", currentMethodTemplate);

      // create new template for the body of the current method
      // we will only use the content of that template as the function body is implemented as a
      // unprotected segment with integrity check

      Template currentMethodBodyTemplate = templateEngine
          .createTemplate(currentMethod.getModelId() + ":httpMethodBody", genericHttpMethodBody);

      // replace currentMethodCode placeholder with content of currentMethod
      currentMethodTemplate.setVariable("$HTTPMethod_Name$", currentMethod.getName());
      currentMethodTemplate.setVariable("$HTTP_Method_Type$",
          "@" + currentMethod.getMethodType().toString());
      currentMethodTemplate.setVariable("$HTTPMethod_Path$", "/" + currentMethod.getPath());

      // helper variable for storing the produces annotation
      String producesAnnotation = "";

      for (int httpResponseIndex = 0; httpResponseIndex < currentMethod.getHttpResponses()
          .size(); httpResponseIndex++) {
        boolean isLastResponse = httpResponseIndex == currentMethod.getHttpResponses().size() - 1;
        HttpResponse currentResponse = currentMethod.getHttpResponses().get(httpResponseIndex);
        // start with api response code

        Template apiResponseTemplate =
            templateEngine.createTemplate(currentResponse.getModelId() + ":apiResponse",
                genericApiResponse + (!isLastResponse ? ",\n" : ""));

        // first add the api response template to the current method template
        currentMethodTemplate.appendVariable("$HTTPMethod_Api_Responses$", apiResponseTemplate);

        // replace just inserted placeholder
        apiResponseTemplate.setVariable("$HTTPResponse_Code$",
            currentResponse.getReturnStatusCode().toString());

        apiResponseTemplate.setVariable("$HTTPResponse_Name$", currentResponse.getName());

        // now to the http responses
        Template httpResponseTemplate =
            templateEngine.createTemplate(currentResponse.getModelId() + ":httpResponse",
                genericHttpResponse + (!isLastResponse ? "\n" : ""));

        // first add the http response to the current method template
        currentMethodBodyTemplate.appendVariable("$HTTPMethod_Responses$", httpResponseTemplate);

        httpResponseTemplate.setVariable("$HTTPResponse_Name$", currentResponse.getName());

        httpResponseTemplate.setVariable("$HTTPResponse_ResultName$",
            currentResponse.getResultName());
        httpResponseTemplate.setVariable("$HTTPResponse_ResultName_Argument$",
            currentResponse.getResultName());

        // httpResponsesCode =
        // httpResponsesCode.replace("$HTTPResponse_ResultName$", currentResponse.getResultName());


        httpResponseTemplate.setVariable("$HTTPResponse_Code$",
            currentResponse.getReturnStatusCode().toString());

        httpResponseTemplate.setVariable("$HTTPResponse_ResultType$",
            currentResponse.getResultType().toString());

        // JSON objects have to be transformed to strings first (a bit ugly, but works:-) )
        // additionally, if there exists a JSON response, we can set the produces annotation
        if (currentResponse.getResultType() == ResultType.JSONObject) {

          httpResponseTemplate.setVariable("$HTTPResponse_ResultName_Argument$",
              currentResponse.getResultName() + ".toJSONString()");

          producesAnnotation = "MediaType.APPLICATION_JSON";

          httpResponseTemplate.setVariable("$HTTP_Response_Result_Init$", "new JSONObject()");

        }
        // check for custom return type and mark it in produces annotation if found
        if (currentResponse.getResultType() == ResultType.CUSTOM) {
          producesAnnotation = "CUSTOM";
          httpResponseTemplate.setVariable("$HTTP_Response_Result_Init$", "CUSTOM");
        }
        if (currentResponse.getResultType() == ResultType.String) {
          httpResponseTemplate.setVariable("$HTTP_Response_Result_Init$", "\"Some String\"");
        }
      }
      // if no produces annotation is set until here, we set it to text
      if (producesAnnotation.equals("")) {
        producesAnnotation = "MediaType.TEXT_PLAIN";
      }

      // insert produces annotation
      currentMethodTemplate.setVariable("$HTTPMethod_Produces$",
          "@Produces(" + producesAnnotation + ")");
      currentMethodBodyTemplate.setVariableIfNotSet("$HTTPMethod_Responses$", "");

      // payload
      String consumesAnnotation = "";
      String parameterCode = "";
      for (int httpPayloadIndex = 0; httpPayloadIndex < currentMethod.getHttpPayloads()
          .size(); httpPayloadIndex++) {

        boolean isLast = httpPayloadIndex == currentMethod.getHttpPayloads().size() - 1;

        HttpPayload currentPayload = currentMethod.getHttpPayloads().get(httpPayloadIndex);
        // add param for JavaDoc
        // dirty, but works:-)
        String type = currentPayload.getPayloadType().toString();
        if (type.equals("PATH_PARAM")) {
          type = "String";
        }

        Template paramTemplate = currentMethodTemplate.createTemplate(
            currentPayload.getModelId() + ":param", "   * @param $name$ a $type$-{ }-");

        paramTemplate.setVariable("$name$", currentPayload.getName());
        paramTemplate.setVariable("$type$", type);

        if (!isLast) {
          paramTemplate.removeLastCharacter('\n');
          paramTemplate.appendContent('\n');
        }

        currentMethodTemplate.appendVariable("$HTTPMethod_Params$", paramTemplate);

        // check if payload is a JSON and cast if so
        if (currentPayload.getPayloadType() == PayloadType.JSONObject) {
          consumesAnnotation = "MediaType.APPLICATION_JSON";
          parameterCode += "@ContentParam String " + currentPayload.getName() + ", ";

          Template castTemplate =
              templateEngine.createTemplate(currentPayload.getModelId() + ":cast",
                  "    JSONObject " + currentPayload.getName()
                      + "_JSON = (JSONObject) JSONValue.parse(" + currentPayload.getName()
                      + ");\n");


          currentMethodCode =
              currentMethodCode.replace("$HTTPMethod_Casts$",
                  "    JSONObject " + currentPayload.getName()
                      + "_JSON = (JSONObject) JSONValue.parse(" + currentPayload.getName()
                      + ");\n$HTTPMethod_Casts$");
          currentMethodBodyTemplate.appendVariable("$HTTPMethod_Casts$", castTemplate);

        }
        // string param
        if (currentPayload.getPayloadType() == PayloadType.String) {
          parameterCode += "@ContentParam String " + currentPayload.getName() + ", ";
        }
        // mark custom payload in consumes annotation and parameter type
        if (currentPayload.getPayloadType() == PayloadType.CUSTOM) {
          consumesAnnotation = "CUSTOM";
          parameterCode += "@ContentParam CUSTOM " + currentPayload.getName() + ", ";
        }
        // path param
        if (currentPayload.getPayloadType() == PayloadType.PATH_PARAM) {
          parameterCode += "@PathParam(\"" + currentPayload.getName() + "\") String "
              + currentPayload.getName() + ", ";
        }
      }
      // remove last cast placeholder
      currentMethodCode = currentMethodCode.replace("\n$HTTPMethod_Casts$", "");
      currentMethodBodyTemplate.setVariableIfNotSet("$HTTPMethod_Casts$", "");

      // remove last comma from parameter code (of parameters were inserted before)
      if (parameterCode.length() > 0) {
        parameterCode = parameterCode.substring(0, parameterCode.length() - 2);
      }
      // remove last parameter placeholder (JavaDoc)
      currentMethodCode = currentMethodCode.replace("\n$HTTPMethod_Params$", "");
      currentMethodTemplate.setVariableIfNotSet("$HTTPMethod_Params$", "   *");

      // if no consumes annotation is set until here, we set it to text
      if (consumesAnnotation.equals("")) {
        consumesAnnotation = "MediaType.TEXT_PLAIN";
      }
      // set the consumes annotation
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Consumes$",
          "@Consumes(" + consumesAnnotation + ")");

      Template consumeTemplate = templateEngine
          .createTemplate(currentMethod.getModelId() + ":consumes", "@Consumes(-{$type$}-)");
      consumeTemplate.setVariable("$type$", consumesAnnotation);

      currentMethodTemplate.appendVariable("$HTTPMethod_Consumes$", consumeTemplate);

      // set the parameters
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Parameters$", parameterCode);
      currentMethodTemplate.setVariable("$HTTPMethod_Parameters$", parameterCode);

      // now to the service invocations
      for (InternalCall call : currentMethod.getInternalCalls()) {
        hasServiceInvocations = true; // marker for adding serializable import
        String currentInvocation = serviceInvocation;

        Template currentInvocationTemplate =
            templateEngine.createTemplate(call.getModelId(), currentInvocation);



        currentInvocationTemplate.setVariable("$Return_Variable$", call.getReturnVariableName());
        currentInvocationTemplate.setVariable("$Remove_Service_Name$", call.getServiceClass());
        currentInvocationTemplate.setVariable("$Remote_Service_Method$", call.getMethodName());

        currentInvocation =
            currentInvocation.replace("$Return_Variable$", call.getReturnVariableName());
        currentInvocation =
            currentInvocation.replace("$Remove_Service_Name$", call.getServiceClass());
        currentInvocation =
            currentInvocation.replace("$Remote_Service_Method$", call.getMethodName());
        String internalParameter = "$Parameters$";
        for (InternalCallParam parameter : call.getParameters()) {

          currentInvocationTemplate.appendVariable("$Parameter_Init$",
              templateEngine.createTemplate(parameter.getModelId() + ":InternalParam",
                  "    Serializable " + parameter.getName() + " = null;\n"));

          currentInvocation = currentInvocation.replace("$Parameter_Init$",
              "    Serializable " + parameter.getName() + " = null;\n$Parameter_Init$");
          internalParameter =
              internalParameter.replace("$Parameters$", parameter.getName() + ", $Parameters$");
          currentInvocation =
              currentInvocation.replace("$Parameters$", parameter.getName() + ", $Parameters$");
        }
        // replace last init placeholder
        currentInvocationTemplate.setVariableIfNotSet("$Parameter_Init$", "");
        currentInvocation = currentInvocation.replace("\n$Parameter_Init$", "");
        // replace last parameter placeholder
        currentInvocation = currentInvocation.replace(", $Parameters$", "");
        internalParameter = internalParameter.replace(", $Parameters$", "");
        internalParameter = internalParameter.replace("$Parameters$", "new Serializable[] {}");
        currentInvocationTemplate.setVariable("$Parameters$", internalParameter);

        // add invocation code to current method code
        currentMethodCode = currentMethodCode.replace("$Invocations$", currentInvocation);
        currentMethodBodyTemplate.appendVariable("$Invocations$", currentInvocationTemplate);
      }
      // replace last invocation placeholder
      currentMethodCode = currentMethodCode.replace("$Invocations$\n", "");
      currentMethodBodyTemplate.setVariableIfNotSet("$Invocations$", "");

      // finally insert currentMethodCode into serviceClass
      serviceClass = serviceClass.replace("$Service_Methods$", currentMethodCode);

      currentMethodTemplate.setVariable("$HTTPMethod_Body$",
          currentMethodBodyTemplate.getContent());



    }
    // add serializable import or remove placeholder
    if (hasServiceInvocations) {
      serviceClass = serviceClass.replace("$Additional_Import$", "import java.io.Serializable;");
      serviceClassTemplate.setVariable("$Additional_Import$", "import java.io.Serializable;");
    } else {
      serviceClass = serviceClass.replace("$Additional_Import$\n", "");
      serviceClassTemplate.setVariable("$Additional_Import$", "");
    }
    // remove last placeholder
    serviceClassTemplate.setVariableIfNotSet("$Service_Methods$", "");
    serviceClass = serviceClass.replace("\n\n\n$Service_Methods$", "");

    return serviceClass;
  }


  /**
   * 
   * Generates the service test class.
   * 
   * @param templateEngine The template engine to use
   * @param serviceTest the service test class file
   * @param microservice the microservice model
   * @param genericTestCase a generic test class file
   * 
   * @return the service test as a string
   * 
   */
  private static String generateNewServiceTest(TemplateEngine templateEngine, String serviceTest,
      Microservice microservice, String genericTestCase) {

    // create template and add to template engine
    Template serviceTestTemplate =
        templateEngine.createTemplate(microservice.getMicroserviceModelId(), serviceTest);
    templateEngine.addTemplate(serviceTestTemplate);

    // general replacements

    serviceTestTemplate.setVariable("$Resource_Name$", microservice.getResourceName());
    serviceTestTemplate.setVariable("$Microservice_Name$", microservice.getName());

    serviceTest = serviceTest.replace("$Resource_Name$", microservice.getResourceName());
    serviceTest = serviceTest.replace("$Microservice_Name$", microservice.getName());
    // get the resource address: (skip first /)
    String relativeResourcePath =
        microservice.getPath().substring(microservice.getPath().indexOf("/", 8) + 1);
    serviceTestTemplate.setVariable("$Resource_Path$", relativeResourcePath);
    serviceTest = serviceTest.replace("$Resource_Path$", relativeResourcePath);
    String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1);
    serviceTest = serviceTest.replace("$Lower_Resource_Name$", packageName);
    serviceTestTemplate.setVariable("$Lower_Resource_Name$", packageName);

    // test cases
    HttpMethod[] httpMethods = microservice.getHttpMethods().values().toArray(new HttpMethod[0]);
    for (int httpMethodIndex = 0; httpMethodIndex < httpMethods.length; httpMethodIndex++) {

      String currentMethodCode = genericTestCase; // copy content
      HttpMethod currentMethod = httpMethods[httpMethodIndex];

      Template currentMethodTemplate = templateEngine
          .createTemplate(currentMethod.getModelId() + ":httpMethod", genericTestCase);

      serviceTestTemplate.appendVariable("$Test_Methods$", currentMethodTemplate);

      String content = "\"\"";
      String consumesAnnotation = "";
      // replace placeholder of current method code

      currentMethodTemplate.setVariable("$HTTP_Method_Name$", currentMethod.getName());
      currentMethodTemplate.setVariable("$HTTPMethod_Path$", currentMethod.getPath());

      currentMethodCode = currentMethodCode.replace("$HTTP_Method_Name$", currentMethod.getName());
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Path$", currentMethod.getPath());
      for (int httpPayloadIndex = 0; httpPayloadIndex < currentMethod.getHttpPayloads()
          .size(); httpPayloadIndex++) {
        HttpPayload currentPayload = currentMethod.getHttpPayloads().get(httpPayloadIndex);
        // get the payload and create variables for it, if needed, cast in sendRequest code
        if (currentPayload.getPayloadType() == PayloadType.JSONObject) {
          consumesAnnotation = "MediaType.APPLICATION_JSON";

          Template currentPayloadTemplate =
              templateEngine.createTemplate(currentPayload.getModelId() + ":payloadJSON",
                  "      JSONObject $Payload_Name$ = new JSONObject();");
          currentPayloadTemplate.setVariable("$Payload_Name$", currentPayload.getName());

          currentMethodTemplate.appendVariable("$TestMethod_Variables$", currentPayloadTemplate);

          currentMethodCode =
              currentMethodCode.replace("$TestMethod_Variables$", "      JSONObject "
                  + currentPayload.getName() + " = new JSONObject();\n$TestMethod_Variables$");
          content = currentPayload.getName() + ".toJSONString()";
        }
        // string parameter
        if (currentPayload.getPayloadType() == PayloadType.String) {
          Template currentPayloadTemplate =
              templateEngine.createTemplate(currentPayload.getModelId() + ":payloadString",
                  "      String $Payload_Name$ = \"-{initialized}-\";");
          currentPayloadTemplate.setVariable("$Payload_Name$", currentPayload.getName());

          currentMethodTemplate.appendVariable("$TestMethod_Variables$", currentPayloadTemplate);

          currentMethodCode = currentMethodCode.replace("$TestMethod_Variables$", "      String "
              + currentPayload.getName() + " = \"initialized\";\n$TestMethod_Variables$");
          content = currentPayload.getName();
        }
        // mark custom payload in consumes annotation and parameter type
        if (currentPayload.getPayloadType() == PayloadType.CUSTOM) {
          consumesAnnotation = "CUSTOM";

          Template currentPayloadTemplate =
              templateEngine.createTemplate(currentPayload.getModelId() + ":payloadCustom",
                  "      -{CUSTOM}- $Payload_Name$ = \"-{null-\";");
          currentPayloadTemplate.setVariable("$Payload_Name$", currentPayload.getName());

          currentMethodTemplate.appendVariable("$TestMethod_Variables$", currentPayloadTemplate);

          currentMethodCode = currentMethodCode.replace("$TestMethod_Variables$",
              "      CUSTOM " + currentPayload.getName() + " = null;\n$TestMethod_Variables$");
          content = currentPayload.getName();
        }
        // path param: replace strings in method call path
        if (currentPayload.getPayloadType() == PayloadType.PATH_PARAM) {

          Template currentPayloadTemplate =
              templateEngine.createTemplate(currentPayload.getModelId() + ":payloadPathParam",
                  "      String $Payload_Name$ = \"-{initialized}-\";");
          currentPayloadTemplate.setVariable("$Payload_Name$", currentPayload.getName());

          currentMethodTemplate.appendVariable("$TestMethod_Variables$", currentPayloadTemplate);

          currentMethodCode = currentMethodCode.replace("$TestMethod_Variables$", "      String "
              + currentPayload.getName() + " = \"initialized\";\n$TestMethod_Variables$");
          currentMethodCode = currentMethodCode.replace("{" + currentPayload.getName() + "}",
              "\" + " + currentPayload.getName() + " + \"");
        }
      }
      // no JSON, no custom, set it to text (no payloads, string payload, only path parameters)
      if (consumesAnnotation.equals("")) {
        consumesAnnotation = "MediaType.TEXT_PLAIN";
      }
      // might still be empty, if only path parameter were parsed
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Content$", content);
      currentMethodTemplate.setVariable("$HTTPMethod_Content$", content);
      // remove last method variable placeholder
      currentMethodCode = currentMethodCode.replace("\n$TestMethod_Variables$", "");
      currentMethodCode =
          currentMethodCode.replace("$HTTP_Method_Type$", currentMethod.getMethodType().toString());
      currentMethodTemplate.setVariable("$HTTP_Method_Type$",
          currentMethod.getMethodType().toString());
      // produces annotation
      String producesAnnotation = "";
      for (int httpResponseIndex = 0; httpResponseIndex < currentMethod.getHttpResponses()
          .size(); httpResponseIndex++) {
        HttpResponse currentResponse = currentMethod.getHttpResponses().get(httpResponseIndex);
        if (currentResponse.getResultType() == ResultType.JSONObject) {
          producesAnnotation = "MediaType.APPLICATION_JSON";
        }
        // check for custom return type and mark it if found
        if (currentResponse.getResultType() == ResultType.CUSTOM) {
          producesAnnotation = "CUSTOM";
        }
      }
      // if no produces annotation is set until here, we set it to text
      if (producesAnnotation.equals("")) {
        producesAnnotation = "MediaType.TEXT_PLAIN";
      }
      currentMethodTemplate.setVariable("$HTTPMethod_Produces$", producesAnnotation);
      currentMethodTemplate.setVariable("$HTTPMethod_Consumes$", consumesAnnotation);
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Produces$", producesAnnotation);
      // consumes annotation
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Consumes$", consumesAnnotation);
      // insert into service test class
      serviceTest = serviceTest.replace("$Test_Methods$", currentMethodCode);
    }
    // remove last placeholder
    serviceTest = serviceTest.replace("\n\n\n$Test_Methods$", "");

    return serviceTest;
  }


  /**
   * 
   * Creates the database script according to the passed database.
   * 
   * @param databaseScript a database script (template)
   * @param tableTemplate a table template
   * @param microservice the microservice model
   * 
   * @return the updated database script
   * 
   */
  private static String generateDatabaseScript(String databaseScript, String tableTemplate,
      Microservice microservice) {
    Database database = microservice.getDatabase();
    for (Table table : database.getTables()) {
      String tableCode = tableTemplate;
      tableCode = tableCode.replace("$Database_Table_Name$", table.getName());
      for (Column column : table.getColumns()) {
        tableCode = tableCode.replace("$Column$",
            "  " + column.getName() + " " + column.getType() + ",\n$Column$");
        if (column.isPrimaryKey()) {
          tableCode = tableCode.replace("$PK_Name$", column.getName());

        }
      }
      databaseScript = databaseScript.replace("$Database_Table$", tableCode);
    }
    databaseScript = databaseScript.replace("$Service_Name$", microservice.getName());
    databaseScript = databaseScript.replace("$Database_Schema$", database.getSchema());
    // remove last placeholder
    databaseScript = databaseScript.replace("\n$Column$", "");
    databaseScript = databaseScript.replace("\n$Database_Table$", "");

    return databaseScript;
  }

}
