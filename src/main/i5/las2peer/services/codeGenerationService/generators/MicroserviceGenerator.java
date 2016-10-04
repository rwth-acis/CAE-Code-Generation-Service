package i5.las2peer.services.codeGenerationService.generators;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.GitDateParser;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHostException;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
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
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

/**
 * 
 * Generates microservice source code from passed on
 * {@link i5.las2peer.services.codeGenerationService.models.microservice.Microservice} models.
 * 
 */
public class MicroserviceGenerator extends Generator {

  private static final L2pLogger logger =
      L2pLogger.getInstance(ApplicationGenerator.class.getName());


  protected static String getDatabaseScriptFileName(Microservice microservice) {
    return "db/" + microservice.getName().replace(" ", "_") + "_create_tables.sql";
  }

  /**
   * Get the name of the package for a microservice model
   * 
   * @param microservice A microservice nodel
   * @return The name of the package
   */

  protected static String getPackageName(Microservice microservice) {
    return microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1);
  }

  /**
   * Get the service file name of a microservice model
   * 
   * @param microservice A microservice model
   * @return The file name of the service
   */

  protected static String getServiceFileName(Microservice microservice) {
    return "src/main/i5/las2peer/services/" + getPackageName(microservice) + "/"
        + microservice.getResourceName() + ".java";
  }

  /**
   * Get the file name of the service properties file
   * 
   * @param microservice A microservice model
   * @return The file name of the service properties file
   */

  protected static String getServicePropertiesFileName(Microservice microservice) {
    String packageName = getPackageName(microservice);
    return "etc/i5.las2peer.services." + packageName + "." + microservice.getResourceName()
        + ".properties";
  }

  /**
   * Get the file name of the test file of a microservice model
   * 
   * @param microservice A microservice model
   * @return The file name of the test file
   */

  protected static String getServiceTestFileName(Microservice microservice) {
    return "src/test/i5/las2peer/services/" + getPackageName(microservice) + "/"
        + microservice.getResourceName() + "Test.java";
  }

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
 * @param gitAdapter TODO
   * @throws GitHostException thrown if anything goes wrong during this process. Wraps around all
   *         other exceptions and prints their message.
   * 
   */
  public static void createSourceCode(Microservice microservice, String templateRepositoryName,
      BaseGitHostAdapter gitAdapter)
      throws GitHostException {

    // variables to be closed in the final block
    Repository microserviceRepository = null;
    TreeWalk treeWalk = null;

    // helper variables
    String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1);

    // variables holding content to be modified and added to repository later
    BufferedImage logo = null;
    String license = null;
    String userAgentGeneratorWindows = null;
    String userAgentGeneratorUnix = null;
    String antUserProperties = null;
    String ivySettings = null;
    String gitignore = null;
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
    String guidances = null;

    try {
    	
      PersonIdent caeUser = new PersonIdent(gitAdapter.getGitUser(), gitAdapter.getGitPassword());
      String repositoryName = getRepositoryName(microservice);
      TraceModel traceModel = new TraceModel();
      
      // 
      try {

        // now load the TreeWalk containing the template repository content
        treeWalk = getTemplateRepositoryContent(gitAdapter);
        treeWalk.setFilter(PathFilter.create("backend/"));
        ObjectReader reader = treeWalk.getObjectReader();
        
        // walk through the tree and retrieve the needed templates
        while (treeWalk.next()) {
          ObjectId objectId = treeWalk.getObjectId(0);
          ObjectLoader loader = reader.open(objectId);
          String path = treeWalk.getPathString().replace("backend/", "");
          
          switch (treeWalk.getNameString()) {
            // start with the "easy" replacements, and store the other template files for later
            case ".project":
              String projectFile = new String(loader.getBytes(), "UTF-8");
              generateOtherArtifacts(Template.createInitialTemplateEngine(traceModel, path),
                  microservice, gitAdapter.getGitOrganization(), projectFile);
              break;
            case "guidances.json":
              guidances = new String(loader.getBytes(), "UTF-8");
              break;
            case "logo_services.png":
              logo = ImageIO.read(loader.openStream());
              break;
            case "README.md":
              String readMe = new String(loader.getBytes(), "UTF-8");
              generateOtherArtifacts(Template.createInitialTemplateEngine(traceModel, path),
                  microservice, gitAdapter.getGitOrganization(), readMe);
              break;
            case "LICENSE.txt":
              license = new String(loader.getBytes(), "UTF-8");
              break;
            case "build.xml":
              String buildFile = new String(loader.getBytes(), "UTF-8");
              generateOtherArtifacts(Template.createInitialTemplateEngine(traceModel, path),
                  microservice, gitAdapter.getGitOrganization(), buildFile);
              break;
            case "start_network.bat":
              String startScriptWindows = new String(loader.getBytes(), "UTF-8");
              generateOtherArtifacts(Template.createInitialTemplateEngine(traceModel, path),
                  microservice, gitAdapter.getGitOrganization(), startScriptWindows);
              break;
            case "start_network.sh":
              String startScriptUnix = new String(loader.getBytes(), "UTF-8");
              generateOtherArtifacts(Template.createInitialTemplateEngine(traceModel, path),
                  microservice, gitAdapter.getGitOrganization(), startScriptUnix);
              break;
            case "start_UserAgentGenerator.bat":
              userAgentGeneratorWindows = new String(loader.getBytes(), "UTF-8");
              break;
            case "start_UserAgentGenerator.sh":
              userAgentGeneratorUnix = new String(loader.getBytes(), "UTF-8");
              break;
            case "nodeInfo.xml":
              String nodeInfo = new String(loader.getBytes(), "UTF-8");
              generateOtherArtifacts(Template.createInitialTemplateEngine(traceModel, path),
                  microservice, gitAdapter.getGitOrganization(), nodeInfo);
              break;
            case "service.properties":
              String antServiceProperties = new String(loader.getBytes(), "UTF-8");
              generateOtherArtifacts(Template.createInitialTemplateEngine(traceModel, path),
                  microservice, gitAdapter.getGitOrganization(), antServiceProperties);
              break;
            case "user.properties":
              antUserProperties = new String(loader.getBytes(), "UTF-8");
              break;
            case "ivy.xml":
              String ivy = new String(loader.getBytes(), "UTF-8");
              generateOtherArtifacts(Template.createInitialTemplateEngine(traceModel, path),
                  microservice, gitAdapter.getGitOrganization(), ivy);
              break;
            case "ivysettings.xml":
              ivySettings = new String(loader.getBytes(), "UTF-8");
              break;
            case "i5.las2peer.services.servicePackage.ServiceClass.properties":
              String serviceProperties = new String(loader.getBytes(), "UTF-8");
              TemplateEngine serviceTemplateEngine = Template.createInitialTemplateEngine(
                  traceModel, getServicePropertiesFileName(microservice));
              generateOtherArtifacts(serviceTemplateEngine, microservice, gitAdapter.getGitOrganization(),
                  serviceProperties);
              break;
            case "i5.las2peer.webConnector.WebConnector.properties":
              String webConnectorConfig = new String(loader.getBytes(), "UTF-8");
              generateOtherArtifacts(Template.createInitialTemplateEngine(traceModel, path),
                  microservice, gitAdapter.getGitOrganization(), webConnectorConfig);
              break;
            case ".gitignore":
              gitignore = new String(loader.getBytes(), "UTF-8");
              break;
            case ".classpath":
              String classpath = new String(loader.getBytes(), "UTF-8");
              generateOtherArtifacts(Template.createInitialTemplateEngine(traceModel, path),
                  microservice, gitAdapter.getGitOrganization(), classpath);
              break;
            case "DatabaseManager.java":
              if (microservice.getDatabase() != null) {
                databaseManager = new String(loader.getBytes(), "UTF-8");
                generateOtherArtifacts(
                    Template.createInitialTemplateEngine(traceModel,
                        "src/main/i5/las2peer/services/" + packageName
                            + "/database/DatabaseManager.java"),
                    microservice, gitAdapter.getGitOrganization(), databaseManager);
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
        throw new GitHostException(e.getMessage());
      }

      microserviceRepository = generateNewRepository(repositoryName, gitAdapter);
      
      // generate service class and test
      /*
      String repositoryLocation = "";
      if (Objects.equals(usedGitHost, "GitLab")) {
    	  //TODO: Check url
    	  repositoryLocation = "http://ginkgo.informatik.rwth-aachen.de:4080/" + gitAdapter.getGitOrganization() + "/" + repositoryName;
      }
      if (Objects.equals(usedGitHost, "GitHub")) {
    	  repositoryLocation = "https://github.com/" + gitAdapter.getGitOrganization() + "/" + repositoryName;
      }
      */
      
      String repositoryLocation = gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + repositoryName;

      FileTraceModel serviceClassTraceModel =
          new FileTraceModel(traceModel, getServiceFileName(microservice));
      traceModel.addFileTraceModel(serviceClassTraceModel);

      TemplateStrategy strategy = new InitialGenerationStrategy();
      TemplateEngine serviceTemplateEngine = new TemplateEngine(strategy, serviceClassTraceModel);

      generateNewServiceClass(serviceTemplateEngine, serviceClass, microservice, repositoryLocation,
          genericHttpMethod, genericHttpMethodBody, genericApiResponse, genericHttpResponse,
          databaseConfig, databaseInstantiation, serviceInvocation);

      FileTraceModel serviceTestTraceModel =
          new FileTraceModel(traceModel, getServiceTestFileName(microservice));
      traceModel.addFileTraceModel(serviceTestTraceModel);

      TemplateEngine serviceTestTemplateEngine =
          new TemplateEngine(new InitialGenerationStrategy(), serviceTestTraceModel);

      generateNewServiceTest(serviceTestTemplateEngine, serviceTest, microservice, genericTestCase);

      // add not traced files to new repository, e.g. static files

      // configuration and build stuff
      microserviceRepository = createTextFileInRepository(microserviceRepository, "etc/ivy/",
          "ivysettings.xml", ivySettings);

      microserviceRepository = createTextFileInRepository(microserviceRepository,
          "etc/ant_configuration/", "user.properties", antUserProperties);

      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "", ".gitignore", gitignore);

      // scripts
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
        FileTraceModel databaseScriptTraceModel =
            new FileTraceModel(traceModel, getDatabaseScriptFileName(microservice));
        traceModel.addFileTraceModel(databaseScriptTraceModel);

        TemplateEngine databaseScriptTemplateEngine =
            new TemplateEngine(new InitialGenerationStrategy(), databaseScriptTraceModel);

        microserviceRepository = createTextFileInRepository(microserviceRepository,
            "src/main/i5/las2peer/services/" + packageName + "/database/", "DatabaseManager.java",
            databaseManager);

        generateDatabaseScript(databaseScriptTemplateEngine, databaseScript, genericTable,
            microservice);
      }

      createTextFileInRepository(microserviceRepository, "traces/", "guidances.json", guidances);

      // add traced files to new repository
      createTracedFilesInRepository(traceModel, microserviceRepository);

      // commit files
      try {
        Git.wrap(microserviceRepository).commit()
            .setMessage("Generated microservice version " + microservice.getVersion())
            .setCommitter(caeUser).call();
      } catch (Exception e) {
        logger.printStackTrace(e);
        throw new GitHostException(e.getMessage());
      }

      // push (local) repository content to GitHub repository
      try {
        pushToRemoteRepository(microserviceRepository, gitAdapter);
      } catch (Exception e) {
        logger.printStackTrace(e);
        throw new GitHostException(e.getMessage());
      }

      // close all open resources
    } catch (GitHostException e) {
    	throw e;
    } finally {
      if(microserviceRepository != null) {
    	  microserviceRepository.close();
      }
      treeWalk.close();
    }
  }

  protected static void generateOtherArtifacts(TemplateEngine templateEngine,
      Microservice microservice, String gitHubOrganization, String templateContent) throws ModelParseException{

    String repositoryName = getRepositoryName(microservice);
    String packageName = getPackageName(microservice);
    String port = "8080";
    
    
    // get the port: skip first 6 characters for search (http: / https:)
    try {
    port = String.valueOf(new URL(microservice.getPath()).getPort());
    } catch (Exception e) {
    	throw new ModelParseException(e.getMessage());
    }

    Template template = null;
    String fileName =
        java.nio.file.Paths.get(templateEngine.getFileName()).getFileName().toString();
    // special case service class properties file
    if (templateEngine.getFileName().equals(getServicePropertiesFileName(microservice))) {
      template = templateEngine.createTemplate(
          microservice.getMicroserviceModelId() + ":servicePropertiesFile", "$Properties$-{\n}-");

      templateEngine.addTrace(microservice.getMicroserviceModelId() + ":servicePropertiesFile",
          "Properties", "Service class properties", template);

      if (microservice.getDatabase() == null) {
        template.setVariableIfNotSet("$Properties$", "");
        // template = templateEngine.createTemplate(
        // microservice.getMicroserviceModelId() + ":emptyServiceProperties", "-{}-");
      } else {
        Template propertiesTemplate = templateEngine.createTemplate(
            microservice.getMicroserviceModelId() + ":serviceProperties", templateContent);
        propertiesTemplate.setVariable("$Database_Address$",
            microservice.getDatabase().getAddress());
        propertiesTemplate.setVariable("$Database_Schema$", microservice.getDatabase().getSchema());
        propertiesTemplate.setVariable("$Database_User$",
            microservice.getDatabase().getLoginName());
        propertiesTemplate.setVariable("$Database_Password$",
            microservice.getDatabase().getLoginPassword());
        template.appendVariable("$Properties$", propertiesTemplate);
      }
    }

    switch (fileName) {
      case ".project":
        template = templateEngine.createTemplate(microservice.getMicroserviceModelId() + ":project",
            templateContent);
        template.setVariable("$Microservice_Name$", microservice.getName());
        break;
      case "README.md":
        template = templateEngine.createTemplate(microservice.getMicroserviceModelId() + ":readMe",
            templateContent);
        template.setVariable("$Repository_Name$", repositoryName);
        template.setVariable("$Organization_Name$", gitHubOrganization);
        template.setVariable("$Microservice_Name$", microservice.getName());
        break;
      case "build.xml":
        template = templateEngine
            .createTemplate(microservice.getMicroserviceModelId() + ":buildFile", templateContent);
        template.setVariable("$Microservice_Name$", microservice.getName());
        break;
      case "start_network.bat":
        template = templateEngine.createTemplate(
            microservice.getMicroserviceModelId() + ":startScriptWindows", templateContent);
        template.setVariable("$Resource_Name$", microservice.getResourceName());
        template.setVariable("$Lower_Resource_Name$", packageName);
        template.setVariable("$Microservice_Version$", microservice.getVersion() + "");
        break;
      case "start_network.sh":
        template = templateEngine.createTemplate(
            microservice.getMicroserviceModelId() + ":startScriptUnix", templateContent);
        template.setVariable("$Resource_Name$", microservice.getResourceName());
        template.setVariable("$Lower_Resource_Name$", packageName);
        template.setVariable("$Microservice_Version$", microservice.getVersion() + "");
        break;
      case "nodeInfo.xml":
        template = templateEngine
            .createTemplate(microservice.getMicroserviceModelId() + ":nodeInfo", templateContent);
        template.setVariable("$Developer$", microservice.getDeveloper());
        template.setVariable("$Resource_Name$", microservice.getResourceName());
        break;
      case "service.properties":
        template = templateEngine.createTemplate(
            microservice.getMicroserviceModelId() + ":antServiceProperties", templateContent);
        template.setVariable("$Microservice_Version$", microservice.getVersion() + "");
        template.setVariable("$Lower_Resource_Name$", packageName);
        template.setVariable("$Resource_Name$", microservice.getResourceName());
        template.setVariable("$Microservice_Version$", microservice.getVersion() + "");
        break;
      case "ivy.xml":
        template = templateEngine.createTemplate(microservice.getMicroserviceModelId() + ":ivy",
            templateContent);
        // add mysql dependency only if a database exists
        if (microservice.getDatabase() != null) {
          template.setVariable("$MySQL_Dependencies$",
              "<dependency org=\"mysql\" name=\"mysql-connector-java\" rev=\"5.1.6\" />\n"
                  + "    <dependency org=\"org.apache.commons\" name=\"commons-pool2\" rev=\"2.2\" />\n"
                  + "    <dependency org=\"org.apache.commons\" name=\"commons-dbcp2\" rev=\"2.0\" />");
        } else {
          template.setVariable("$MySQL_Dependencies$", "");
        }
        break;
      case "i5.las2peer.services.servicePackage.ServiceClass.properties":

        break;
      case "i5.las2peer.webConnector.WebConnector.properties":
        template = templateEngine.createTemplate(
            microservice.getMicroserviceModelId() + ":webConnectorConfig", templateContent);
        template.setVariable("$HTTP_Port$", port);
        break;
      case ".classpath":
        template = templateEngine
            .createTemplate(microservice.getMicroserviceModelId() + ":classpath", templateContent);
        if (microservice.getDatabase() != null) {
          template.setVariable("$Database_Libraries$",
              "<classpathentry kind=\"lib\" path=\"lib/mysql-connector-java-5.1.6.jar\"/>\n"
                  + "  <classpathentry kind=\"lib\" path=\"lib/commons-dbcp2-2.0.jar\"/>");
        } else {
          template.setVariable("$Database_Libraries$", "");
        }

        break;
      case "DatabaseManager.java":
        template = templateEngine.createTemplate(
            microservice.getMicroserviceModelId() + ":databaseManager", templateContent);
        template.setVariable("$Lower_Resource_Name$", packageName);
        break;
    }

    if (template != null) {
      templateEngine.addTemplate(template);
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
   */
  protected static void generateNewServiceClass(TemplateEngine templateEngine, String serviceClass,
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

    templateEngine.addTrace(microservice.getMicroserviceModelId(), "RESTful Resource",
        microservice.getResourceName(), serviceClassTemplate);

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
      serviceClassTemplate.setVariable("$Database_Import$", "import i5.las2peer.services."
          + packageName + ".database.DatabaseManager;\nimport java.sql.*;");

      Template databaseConfigurationTpl = templateEngine
          .createTemplate(microservice.getMicroserviceModelId() + ":dbConfig", databaseConfig);
      Template databaseInstantiationTpl = templateEngine.createTemplate(
          microservice.getMicroserviceModelId() + ":Instantiation", databaseInstantiation);
      // variable names
      // serviceClassTemplate.appendVariable("$Database_Configuration$", test);

      serviceClassTemplate.appendVariable("$Database_Configuration$", databaseConfigurationTpl);

      // serviceClassTemplate.setVariable("$Database_Configuration$",
      // new String(" /*\n" + " * Database configuration\n" + " */\n"
      // + " private String jdbcDriverClassName;\n" + " private String jdbcLogin;\n"
      // + " private String jdbcPass;\n" + " private String jdbcUrl;\n"
      // + " private String jdbcSchema;\n" + " private DatabaseManager dbm;\n"));
      // instantiation
      serviceClassTemplate.appendVariable("$Database_Instantiation$", databaseInstantiationTpl);
      // serviceClassTemplate.setVariable("$Database_Instantiation$", new String(
      // " // instantiate a database manager to handle database connection pooling and
      // credentials\n"
      // + " dbm = new DatabaseManager(jdbcDriverClassName, jdbcLogin, jdbcPass,
      // jdbcUrl,jdbcSchema);"));
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
      HttpMethod currentMethod = httpMethods[httpMethodIndex];

      // create new template for the current method
      Template currentMethodTemplate = templateEngine
          .createTemplate(currentMethod.getModelId() + ":httpMethod", genericHttpMethod);

      Segment httpMethodBodySegment = currentMethodTemplate.getSegment()
          .getChildRecursive(currentMethodTemplate.getId() + ":$HTTPMethod_Body$");

      // add template of current method to service class template
      serviceClassTemplate.appendVariable("$Service_Methods$", currentMethodTemplate);

      // create new template for the body of the current method
      // we will only use the content of that template as the function body is implemented as a
      // unprotected segment with integrity check

      Template currentMethodBodyTemplate = templateEngine
          .createTemplate(currentMethod.getModelId() + ":httpMethodBody", genericHttpMethodBody);

      // replace currentMethodCode placeholder with content of currentMethod
      currentMethodTemplate.setVariable("$HTTPMethod_Name_Comment$", currentMethod.getName());
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

        Template apiResponseTemplate = templateEngine
            .createTemplate(currentResponse.getModelId() + ":apiResponse", genericApiResponse);


        // first add the api response template to the current method template
        currentMethodTemplate.appendVariable("$HTTPMethod_Api_Responses$", apiResponseTemplate);
        if (!isLastResponse) {
          Template tmp =
              templateEngine.createTemplate(currentResponse.getModelId() + ":indentation", ",\n");
          currentMethodTemplate.appendVariable("$HTTPMethod_Api_Responses$", tmp);
        }
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

        // add a trace for the response to its method template
        templateEngine.addTrace(currentResponse.getModelId(), "Http Response",
            currentResponse.getName(), httpMethodBodySegment);

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
        // add a trace for the payload to its template
        templateEngine.addTrace(currentPayload.getModelId(), "Payload", currentPayload.getName(),
            paramTemplate);

        // check if payload is a JSON and cast if so
        if (currentPayload.getPayloadType() == PayloadType.JSONObject) {
          consumesAnnotation = "MediaType.APPLICATION_JSON";
          parameterCode += "@ContentParam String " + currentPayload.getName() + ", ";

          Template castTemplate = templateEngine.createTemplate(
              currentPayload.getModelId() + ":cast",
              "    JSONObject $Payload_Name$_JSON = (JSONObject) JSONValue.parse($Payload_Name$);\n");
          castTemplate.setVariable("$Payload_Name$", currentPayload.getName());
          currentMethodTemplate.appendVariable("$HTTPMethod_Casts$", castTemplate);

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
      currentMethodTemplate.setVariableIfNotSet("$HTTPMethod_Casts$", "");

      // remove last comma from parameter code (of parameters were inserted before)
      if (parameterCode.length() > 0) {
        parameterCode = parameterCode.substring(0, parameterCode.length() - 2);
      }
      // remove last parameter placeholder (JavaDoc)
      currentMethodTemplate.setVariableIfNotSet("$HTTPMethod_Params$", "   *");

      // if no consumes annotation is set until here, we set it to text
      if (consumesAnnotation.equals("")) {
        consumesAnnotation = "MediaType.TEXT_PLAIN";
      }
      // set the consumes annotation
      Template consumeTemplate = templateEngine
          .createTemplate(currentMethod.getModelId() + ":consumes", "@Consumes($type$)");
      consumeTemplate.setVariable("$type$", consumesAnnotation);

      currentMethodTemplate.appendVariable("$HTTPMethod_Consumes$", consumeTemplate);

      // set the parameters
      currentMethodTemplate.setVariable("$HTTPMethod_Parameters$", parameterCode);

      // now to the service invocations
      for (InternalCall call : currentMethod.getInternalCalls()) {
        hasServiceInvocations = true; // marker for adding serializable import

        Template currentInvocationTemplate =
            templateEngine.createTemplate(call.getModelId(), serviceInvocation);

        currentInvocationTemplate.setVariable("$Return_Variable$", call.getReturnVariableName());
        currentInvocationTemplate.setVariable("$Remove_Service_Name$", call.getServiceClass());
        currentInvocationTemplate.setVariable("$Remote_Service_Method$", call.getMethodName());

        String internalParameter = ", $Parameters$";
        for (InternalCallParam parameter : call.getParameters()) {
          // add a trace for the call to its method template
          templateEngine.addTrace(parameter.getModelId(), "Service Call Parameter",
              parameter.getName(), httpMethodBodySegment);
          currentInvocationTemplate.appendVariable("$Parameter_Init$",
              templateEngine.createTemplate(parameter.getModelId() + ":InternalParam",
                  "    Serializable " + parameter.getName() + " = null;\n"));

          internalParameter =
              internalParameter.replace("$Parameters$", parameter.getName() + ", $Parameters$");
        }
        // replace last init placeholder
        currentInvocationTemplate.setVariableIfNotSet("$Parameter_Init$", "");
        // replace last parameter placeholder
        internalParameter = internalParameter.replace(", $Parameters$", "");
        currentInvocationTemplate.setVariable("$Parameters$", internalParameter);

        // add invocation code to current method code
        currentMethodBodyTemplate.appendVariable("$Invocations$", currentInvocationTemplate);
        // add a trace for the call to its method template
        templateEngine.addTrace(call.getModelId(), "Internal Service Call", call.getMethodName(),
            httpMethodBodySegment);
      }
      // replace last invocation placeholder
      currentMethodBodyTemplate.setVariableIfNotSet("$Invocations$", "");

      currentMethodTemplate.setVariable("$HTTPMethod_Body$",
          currentMethodBodyTemplate.getContent());

      // add a trace to the segment
      templateEngine.addTrace(currentMethod.getModelId(), "Http Method", currentMethod.getName(),
          httpMethodBodySegment);
      templateEngine.addTrace(currentMethod.getModelId(), "Http Method", currentMethod.getName(),
          currentMethodTemplate);

    }
    // add serializable import or remove placeholder
    if (hasServiceInvocations) {
      serviceClassTemplate.setVariable("$Additional_Import$", "import java.io.Serializable;");
    } else {
      serviceClassTemplate.setVariable("$Additional_Import$", "");
    }
    // remove last placeholder
    serviceClassTemplate.setVariableIfNotSet("$Service_Methods$", "");

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
   */
  protected static void generateNewServiceTest(TemplateEngine templateEngine, String serviceTest,
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

      currentMethodTemplate.setVariable("$HTTPMethod_Content$", content);
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
      currentMethodTemplate.setVariableIfNotSet("$TestMethod_Variables$", "");

    }

    serviceTestTemplate.setVariableIfNotSet("$Test_Methods$", "");
  }


  /**
   * 
   * Creates the database script according to the passed database.
   * 
   * @param templateEngine the template engine to use for the code generation
   * @param databaseScript a database script (template)
   * @param tableTemplate a table template
   * @param microservice the microservice model
   * 
   */
  protected static void generateDatabaseScript(TemplateEngine templateEngine, String databaseScript,
      String tableTemplate, Microservice microservice) {
    Database database = microservice.getDatabase();
    Template databaseTemplate = templateEngine
        .createTemplate(microservice.getMicroserviceModelId() + ":database", databaseScript);
    templateEngine.addTemplate(databaseTemplate);

    templateEngine.addTrace(database.getModelId(), "DatabaseScript", databaseTemplate);

    for (Table table : database.getTables()) {
      Template currentTableTemplate =
          templateEngine.createTemplate(table.getModelId() + ":table", tableTemplate);

      databaseTemplate.appendVariable("$Database_Table$", currentTableTemplate);

      currentTableTemplate.setVariable("$Database_Table_Name$", table.getName());
      for (Column column : table.getColumns()) {
        Template columnTemplate = templateEngine.createTemplate(column.getModelId() + ":column",
            "  $name$ $type$-{ }-,\n");
        columnTemplate.setVariable("$name$", column.getName());
        columnTemplate.setVariable("$type$", column.getType());
        currentTableTemplate.appendVariable("$Column$", columnTemplate);
        if (column.isPrimaryKey()) {
          currentTableTemplate.setVariable("$PK_Name$", column.getName());
        }
      }

      currentTableTemplate.setVariableIfNotSet("$Column$", "");
    }
    databaseTemplate.setVariable("$Service_Name$", microservice.getName());
    databaseTemplate.setVariable("$Database_Schema$", database.getSchema());
    // remove last placeholder
    databaseTemplate.setVariableIfNotSet("$Database_Table$", "");
  }

}
