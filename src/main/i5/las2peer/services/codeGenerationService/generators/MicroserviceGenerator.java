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

import i5.las2peer.services.codeGenerationService.generators.exception.GitHubException;
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

/**
 * 
 * Generates microservice source code from passed on
 * {@link i5.las2peer.services.codeGenerationService.models.microservice} models.
 * 
 */
public class MicroserviceGenerator extends Generator {

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
      String repositoryName = "microservice-" + microservice.getName().replace(" ", "-");
      microserviceRepository =
          generateNewRepository(repositoryName, gitHubOrganization, gitHubUser, gitHubPassword);

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
              projectFile = projectFile.replace("$Microservice_Name$", microservice.getName());
              break;
            case "logo_services.png":
              logo = ImageIO.read(loader.openStream());
              break;
            case "README.md":
              readMe = new String(loader.getBytes(), "UTF-8");
              readMe = readMe.replace("$Repository_Name$", repositoryName);
              readMe = readMe.replace("$Organization_Name$", gitHubOrganization);
              readMe = readMe.replace("$Microservice_Name$", microservice.getName());
              break;
            case "LICENSE.txt":
              license = new String(loader.getBytes(), "UTF-8");
              break;
            case "build.xml":
              buildFile = new String(loader.getBytes(), "UTF-8");
              buildFile = buildFile.replace("$Microservice_Name$", microservice.getName());
              break;
            case "start_network.bat":
              startScriptWindows = new String(loader.getBytes(), "UTF-8");
              startScriptWindows =
                  startScriptWindows.replace("$Resource_Name$", microservice.getResourceName());
              startScriptWindows = startScriptWindows.replace("$Lower_Resource_Name$", packageName);
              break;
            case "start_network.sh":
              startScriptUnix = new String(loader.getBytes(), "UTF-8");
              startScriptUnix =
                  startScriptUnix.replace("$Resource_Name$", microservice.getResourceName());
              startScriptUnix = startScriptUnix.replace("$Lower_Resource_Name$", packageName);
              break;
            case "start_UserAgentGenerator.bat":
              userAgentGeneratorWindows = new String(loader.getBytes(), "UTF-8");
              break;
            case "start_UserAgentGenerator.sh":
              userAgentGeneratorUnix = new String(loader.getBytes(), "UTF-8");
              break;
            case "nodeInfo.xml":
              nodeInfo = new String(loader.getBytes(), "UTF-8");
              nodeInfo = nodeInfo.replace("$Developer$", microservice.getDeveloper());
              nodeInfo = nodeInfo.replace("$Resource_Name$", microservice.getResourceName());
              break;
            case "service.properties":
              antServiceProperties = new String(loader.getBytes(), "UTF-8");
              antServiceProperties = antServiceProperties.replace("$Microservice_Version$",
                  microservice.getVersion() + "");
              antServiceProperties =
                  antServiceProperties.replace("$Lower_Resource_Name$", packageName);
              antServiceProperties =
                  antServiceProperties.replace("$Resource_Name$", microservice.getResourceName());
              antServiceProperties = antServiceProperties.replace("$Microservice_Version$",
                  microservice.getVersion() + "");
              break;
            case "user.properties":
              antUserProperties = new String(loader.getBytes(), "UTF-8");
              break;
            case "ivy.xml":
              ivy = new String(loader.getBytes(), "UTF-8");
              // add mysql dependency only if a database exists
              if (microservice.getDatabase() != null) {
                ivy = ivy.replace("$MySQL_Dependencies$",
                    "<dependency org=\"mysql\" name=\"mysql-connector-java\" rev=\"5.1.6\" />\n"
                        + "    <dependency org=\"org.apache.commons\" name=\"commons-pool2\" rev=\"2.2\" />\n"
                        + "    <dependency org=\"org.apache.commons\" name=\"commons-dbcp2\" rev=\"2.0\" />");
              } else {
                ivy = ivy.replace("    $MySQL_Dependencies$\n", "");
              }
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
                serviceProperties = serviceProperties.replace("$Database_Address$",
                    microservice.getDatabase().getAddress());
                serviceProperties = serviceProperties.replace("$Database_Schema$",
                    microservice.getDatabase().getSchema());
                serviceProperties = serviceProperties.replace("$Database_User$",
                    microservice.getDatabase().getLoginName());
                serviceProperties = serviceProperties.replace("$Database_Password$",
                    microservice.getDatabase().getLoginPassword());
              }
            case "i5.las2peer.webConnector.WebConnector.properties":
              webConnectorConfig = new String(loader.getBytes(), "UTF-8");
              webConnectorConfig = webConnectorConfig.replace("$HTTP_Port$", port);
              break;
            case ".gitignore":
              gitignore = new String(loader.getBytes(), "UTF-8");
              break;
            case ".classpath":
              classpath = new String(loader.getBytes(), "UTF-8");
              if (microservice.getDatabase() != null) {
                classpath = classpath.replace("$Database_Libraries$",
                    "<classpathentry kind=\"lib\" path=\"lib/mysql-connector-java-5.1.6.jar\"/>\n"
                        + "  <classpathentry kind=\"lib\" path=\"lib/commons-dbcp2-2.0.jar\"/>");
              } else {
                classpath = classpath.replace("$Database_Libraries$\n", "");
              }
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
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // generate service class and test
      String repositoryLocation = "https://github.com/" + gitHubOrganization + "/" + repositoryName;
      serviceClass = generateNewServiceClass(serviceClass, microservice, repositoryLocation,
          genericHttpMethod, genericApiResponse, genericHttpResponse, databaseConfig,
          databaseInstantiation, serviceInvocation);
      serviceTest = generateNewServiceTest(serviceTest, microservice, genericTestCase);
      if (microservice.getDatabase() != null) {
        databaseScript = generateDatabaseScript(databaseScript, genericTable, microservice);
      }
      // add files to new repository
      // configuration and build stuff
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "etc/ivy/", "ivy.xml", ivy);
      microserviceRepository = createTextFileInRepository(microserviceRepository, "etc/ivy/",
          "ivysettings.xml", ivySettings);
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "", "build.xml", buildFile);
      microserviceRepository = createTextFileInRepository(microserviceRepository,
          "etc/ant_configuration/", "user.properties", antUserProperties);
      microserviceRepository = createTextFileInRepository(microserviceRepository,
          "etc/ant_configuration/", "service.properties", antServiceProperties);
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "etc/", "nodeInfo.xml", nodeInfo);
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "", ".project", projectFile);
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
          createTextFileInRepository(microserviceRepository, "", "README.md", readMe);
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
          "src/main/i5/las2peer/services/" + packageName + "/",
          microservice.getResourceName() + ".java", serviceClass);
      microserviceRepository = createTextFileInRepository(microserviceRepository,
          "src/test/i5/las2peer/services/" + packageName + "/",
          microservice.getResourceName() + "Test.java", serviceTest);
      // commit files
      try {
        Git.wrap(microserviceRepository).commit()
            .setMessage("Generated microservice version " + microservice.getVersion())
            .setCommitter(caeUser).call();
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // push (local) repository content to GitHub repository
      try {
        pushToRemoteRepository(microserviceRepository, gitHubUser, gitHubPassword);
      } catch (Exception e) {
        e.printStackTrace();
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
   * @param serviceClass the service class file
   * @param microservice the microservice model
   * @param repositoryLocation the location of the service's repository
   * @param genericHttpMethod a generic http method template
   * @param genericApiResponse a generic api response template
   * @param generiHttpResponse a generic http response template
   * @param databaseConfig a database configuration (source code) template
   * @param databaseInstantiation a database instantiation (source code) template
   * @param serviceInvocation a service invocation (source code) template
   * 
   * @return the service class as a string
   * 
   */
  private static String generateNewServiceClass(String serviceClass, Microservice microservice,
      String repositoryLocation, String genericHttpMethod, String genericApiResponse,
      String genericHttpResponse, String databaseConfig, String databaseInstantiation,
      String serviceInvocation) {
    // helper variables
    String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1);
    String relativeResourcePath =
        microservice.getPath().substring(microservice.getPath().indexOf("/", 8) + 1);
    boolean hasServiceInvocations = false;

    // service name for documentation
    serviceClass = serviceClass.replace("$Microservice_Name$", microservice.getName());
    // relative resource path (resource base path)
    serviceClass = serviceClass.replace("$Relative_Resource_Path$", relativeResourcePath);
    // version
    serviceClass = serviceClass.replace("$Microservice_Version$", microservice.getVersion() + "");
    // developer
    serviceClass = serviceClass.replace("$Developer$", microservice.getDeveloper());
    // link to license file
    serviceClass = serviceClass.replace("$License_File_Address$",
        repositoryLocation + "/blob/master/LICENSE.txt");
    // resource name
    serviceClass = serviceClass.replace("$Resource_Name$", microservice.getResourceName());
    // create database references only if microservice has database
    if (microservice.getDatabase() != null) {
      // import
      serviceClass = serviceClass.replace("$Database_Import$",
          "import i5.las2peer.services.$Lower_Resource_Name$.database.DatabaseManager;");
      // variable names
      serviceClass = serviceClass.replace("$Database_Configuration$", databaseConfig);
      // instantiation
      serviceClass = serviceClass.replace("$Database_Instantiation$", databaseInstantiation);
    } else {
      // set to empty string
      serviceClass = serviceClass.replace("$Database_Configuration$\n\n\n", "");
      serviceClass = serviceClass.replace("$Database_Instantiation$\n", "");
      serviceClass = serviceClass.replace("$Database_Import$\n", "");
    }
    // package and import paths
    serviceClass = serviceClass.replace("$Lower_Resource_Name$", packageName);
    // http methods
    HttpMethod[] httpMethods = microservice.getHttpMethods().values().toArray(new HttpMethod[0]);
    for (int httpMethodIndex = 0; httpMethodIndex < httpMethods.length; httpMethodIndex++) {
      String currentMethodCode = genericHttpMethod; // copy content
      HttpMethod currentMethod = httpMethods[httpMethodIndex];
      // replace currentMethodCode placeholder with content of currentMethod
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Name$", currentMethod.getName());
      currentMethodCode = currentMethodCode.replace("$HTTP_Method_Type$",
          "@" + currentMethod.getMethodType().toString());
      currentMethodCode =
          currentMethodCode.replace("$HTTPMethod_Path$", "/" + currentMethod.getPath());

      // responses
      String producesAnnotation = "";
      String apiResponseCode = "";
      String httpResponsesCode = "";
      for (int httpResponseIndex = 0; httpResponseIndex < currentMethod.getHttpResponses()
          .size(); httpResponseIndex++) {
        HttpResponse currentResponse = currentMethod.getHttpResponses().get(httpResponseIndex);
        // start with api response code
        apiResponseCode += genericApiResponse + "\n";
        // replace just inserted placeholder
        apiResponseCode = apiResponseCode.replace("$HTTPResponse_Code$",
            currentResponse.getReturnStatusCode().toString());
        apiResponseCode = apiResponseCode.replace("$HTTPResponse_Name$", currentResponse.getName());
        // now to the http responses
        httpResponsesCode += genericHttpResponse + "\n";
        httpResponsesCode =
            httpResponsesCode.replace("$HTTPResponse_Name$", currentResponse.getName());
        httpResponsesCode =
            httpResponsesCode.replace("$HTTPResponse_ResultName$", currentResponse.getResultName());
        httpResponsesCode =
            httpResponsesCode.replace("$HTTPResponse_ResultName$", currentResponse.getResultName());
        httpResponsesCode = httpResponsesCode.replace("$HTTPResponse_Code$",
            currentResponse.getReturnStatusCode().toString());
        httpResponsesCode = httpResponsesCode.replace("$HTTPResponse_ResultType$",
            currentResponse.getResultType().toString());
        // JSON objects have to be transformed to strings first (a bit ugly, but works:-) )
        // additionally, if there exists a JSON response, we can set the produces annotation
        if (currentResponse.getResultType() == ResultType.JSONObject) {
          httpResponsesCode =
              httpResponsesCode.replace("HttpResponse(" + currentResponse.getResultName(),
                  "HttpResponse(" + currentResponse.getResultName() + ".toJSONString()");
          producesAnnotation = "MediaType.APPLICATION_JSON";
          httpResponsesCode =
              httpResponsesCode.replace("$HTTP_Response_Result_Init$", "new JSONObject()");
        }
        // check for custom return type and mark it in produces annotation if found
        if (currentResponse.getResultType() == ResultType.CUSTOM) {
          producesAnnotation = "CUSTOM";
          httpResponsesCode = httpResponsesCode.replace("$HTTP_Response_Result_Init$", "CUSTOM");
        }
        if (currentResponse.getResultType() == ResultType.String) {
          httpResponsesCode =
              httpResponsesCode.replace("$HTTP_Response_Result_Init$", "\"Some String\"");
        }
      }
      // if no produces annotation is set until here, we set it to text
      if (producesAnnotation.equals("")) {
        producesAnnotation = "MediaType.TEXT_PLAIN";
      }
      // remove last comma and empty line from api response string
      apiResponseCode = apiResponseCode.substring(0, apiResponseCode.length() - 2);
      // remove last empty line from http responses string
      httpResponsesCode = httpResponsesCode.substring(0, httpResponsesCode.length() - 1);
      // add both code fragments to method
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Api_Responses$", apiResponseCode);
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Responses$", httpResponsesCode);
      // insert produces annotation
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Produces$",
          "@Produces(" + producesAnnotation + ")");

      // payload
      String consumesAnnotation = "";
      String parameterCode = "";
      for (int httpPayloadIndex = 0; httpPayloadIndex < currentMethod.getHttpPayloads()
          .size(); httpPayloadIndex++) {
        HttpPayload currentPayload = currentMethod.getHttpPayloads().get(httpPayloadIndex);
        // add param for JavaDoc
        // dirty, but works:-)
        String type = currentPayload.getPayloadType().toString();
        if (type.equals("PATH_PARAM")) {
          type = "String";
        }
        currentMethodCode = currentMethodCode.replace("$HTTPMethod_Params$",
            "   * @param " + currentPayload.getName() + " a " + type + "\n$HTTPMethod_Params$");
        // check if payload is a JSON and cast if so
        if (currentPayload.getPayloadType() == PayloadType.JSONObject) {
          consumesAnnotation = "MediaType.APPLICATION_JSON";
          parameterCode += "String " + currentPayload.getName() + ", ";
          currentMethodCode =
              currentMethodCode.replace("$HTTPMethod_Casts$",
                  "    JSONObject " + currentPayload.getName()
                      + "_JSON = (JSONObject) JSONValue.parse(" + currentPayload.getName()
                      + ");\n$HTTPMethod_Casts$");
        }
        // string param
        if (currentPayload.getPayloadType() == PayloadType.String) {
          parameterCode += "String " + currentPayload.getName() + ", ";
        }
        // mark custom payload in consumes annotation and parameter type
        if (currentPayload.getPayloadType() == PayloadType.CUSTOM) {
          consumesAnnotation = "CUSTOM";
          parameterCode += "CUSTOM " + currentPayload.getName() + ", ";
        }
        // path param
        if (currentPayload.getPayloadType() == PayloadType.PATH_PARAM) {
          parameterCode += "@PathParam(\"" + currentPayload.getName() + "\") String "
              + currentPayload.getName() + ", ";
        }
      }
      // remove last cast placeholder
      currentMethodCode = currentMethodCode.replace("\n$HTTPMethod_Casts$", "");
      // remove last comma from parameter code (of parameters were inserted before)
      if (parameterCode.length() > 0) {
        parameterCode = parameterCode.substring(0, parameterCode.length() - 2);
      }
      // remove last parameter placeholder (JavaDoc)
      currentMethodCode = currentMethodCode.replace("\n$HTTPMethod_Params$", "");
      // if no consumes annotation is set until here, we set it to text
      if (consumesAnnotation.equals("")) {
        consumesAnnotation = "MediaType.TEXT_PLAIN";
      }
      // set the consumes annotation
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Consumes$",
          "@Consumes(" + consumesAnnotation + ")");
      // set the parameters
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Parameters$", parameterCode);

      // now to the service invocations
      for (InternalCall call : currentMethod.getInternalCalls()) {
        hasServiceInvocations = true; // marker for adding serializable import
        String currentInvocation = serviceInvocation;
        currentInvocation =
            currentInvocation.replace("$Return_Variable$", call.getReturnVariableName());
        currentInvocation =
            currentInvocation.replace("$Remove_Service_Name$", call.getServiceClass());
        currentInvocation =
            currentInvocation.replace("$Remote_Service_Method$", call.getMethodName());
        for (InternalCallParam parameter : call.getParameters()) {
          currentInvocation = currentInvocation.replace("$Parameter_Init$",
              "    Serializable " + parameter.getName() + " = null;\n$Parameter_Init$");
          currentInvocation =
              currentInvocation.replace("$Parameters$", parameter.getName() + ", $Parameters$");
        }
        // replace last init placeholder
        currentInvocation = currentInvocation.replace("\n$Parameter_Init$", "");
        // replace last parameter placeholder
        currentInvocation = currentInvocation.replace(", $Parameters$", "");
        // add invocation code to current method code
        currentMethodCode = currentMethodCode.replace("$Invocations$", currentInvocation);
      }
      // replace last invocation placeholder
      currentMethodCode = currentMethodCode.replace("$Invocations$\n", "");

      // finally insert currentMethodCode into serviceClass
      serviceClass = serviceClass.replace("$Service_Methods$", currentMethodCode);
    }
    // add serializable import or remove placeholder
    if (hasServiceInvocations) {
      serviceClass = serviceClass.replace("$Additional_Import$", "import java.io.Serializable;");
    } else {
      serviceClass = serviceClass.replace("$Additional_Import$\n", "");
    }
    // remove last placeholder
    serviceClass = serviceClass.replace("\n\n\n$Service_Methods$", "");

    return serviceClass;
  }


  /**
   * 
   * Generates the service test class.
   * 
   * @param serviceTest the service test class file
   * @param microservice the microservice model
   * @param genericTestCase a generic test class file
   * 
   * @return the service test as a string
   * 
   */
  private static String generateNewServiceTest(String serviceTest, Microservice microservice,
      String genericTestCase) {
    // general replacements
    serviceTest = serviceTest.replace("$Resource_Name$", microservice.getResourceName());
    serviceTest = serviceTest.replace("$Microservice_Name$", microservice.getName());
    // get the resource address: (skip first /)
    String relativeResourcePath =
        microservice.getPath().substring(microservice.getPath().indexOf("/", 8) + 1);
    serviceTest = serviceTest.replace("$Resource_Path$", relativeResourcePath);
    String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1);
    serviceTest = serviceTest.replace("$Lower_Resource_Name$", packageName);

    // test cases
    HttpMethod[] httpMethods = microservice.getHttpMethods().values().toArray(new HttpMethod[0]);
    for (int httpMethodIndex = 0; httpMethodIndex < httpMethods.length; httpMethodIndex++) {
      String currentMethodCode = genericTestCase; // copy content
      HttpMethod currentMethod = httpMethods[httpMethodIndex];
      String content = "\"\"";
      String consumesAnnotation = "";
      // replace placeholder of current method code
      currentMethodCode = currentMethodCode.replace("$HTTP_Method_Name$", currentMethod.getName());
      currentMethodCode = currentMethodCode.replace("$HTTPMethod_Path$", currentMethod.getPath());
      for (int httpPayloadIndex = 0; httpPayloadIndex < currentMethod.getHttpPayloads()
          .size(); httpPayloadIndex++) {
        HttpPayload currentPayload = currentMethod.getHttpPayloads().get(httpPayloadIndex);
        // get the payload and create variables for it, if needed, cast in sendRequest code
        if (currentPayload.getPayloadType() == PayloadType.JSONObject) {
          consumesAnnotation = "MediaType.APPLICATION_JSON";
          currentMethodCode =
              currentMethodCode.replace("$TestMethod_Variables$", "      JSONObject "
                  + currentPayload.getName() + " = new JSONObject();\n$TestMethod_Variables$");
          content = currentPayload.getName() + ".toJSONString()";
        }
        // string parameter
        if (currentPayload.getPayloadType() == PayloadType.String) {
          currentMethodCode = currentMethodCode.replace("$TestMethod_Variables$", "      String "
              + currentPayload.getName() + " = \"initialized\";\n$TestMethod_Variables$");
          content = currentPayload.getName();
        }
        // mark custom payload in consumes annotation and parameter type
        if (currentPayload.getPayloadType() == PayloadType.CUSTOM) {
          consumesAnnotation = "CUSTOM";
          currentMethodCode = currentMethodCode.replace("$TestMethod_Variables$",
              "      CUSTOM " + currentPayload.getName() + " = null;\n$TestMethod_Variables$");
          content = currentPayload.getName();
        }
        // path param: replace strings in method call path
        if (currentPayload.getPayloadType() == PayloadType.PATH_PARAM) {
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
      // remove last method variable placeholder
      currentMethodCode = currentMethodCode.replace("\n$TestMethod_Variables$", "");
      currentMethodCode =
          currentMethodCode.replace("$HTTP_Method_Type$", currentMethod.getMethodType().toString());
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
