package i5.las2peer.services.codeGenerationService.generators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHostException;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
import i5.las2peer.services.codeGenerationService.models.microservice.*;
import i5.las2peer.services.codeGenerationService.models.microservice.HttpPayload.PayloadType;
import i5.las2peer.services.codeGenerationService.models.microservice.HttpResponse.ResultType;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.templateEngine.InitialGenerationStrategy;
import i5.las2peer.services.codeGenerationService.templateEngine.Template;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateEngine;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateStrategy;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    return "db/" + microservice.getVersionedModelId().replace(" ", "_") + "_create_tables.sql";
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
     * Get the classes file name of a microservice model
     *
     * @param microservice A microservice model
     * @return The file name of the classes
     */

    protected static String getClassesFileName(Microservice microservice) {
        return "src/main/i5/las2peer/services/" + getPackageName(microservice) + "/classes.java";
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
    String repositoryName = "microservice-" + microservice.getVersionedModelId();
    return repositoryName;
  }

  /**
   * 
   * Creates source code from a CAE microservice model and pushes it to GitHub.
   * 
   * @param microservice the microservice model
 * @param templateRepositoryName the name of the template repository on GitHub
 * @param gitAdapter The gitAdapter that manages operations on GitHub/GitLab etc.
 * @param forcePush boolean value t/f
 * @param versionTag String which should be used as the tag when commiting. May be null.
 * @return Commit sha identifier
   * @throws GitHostException thrown if anything goes wrong during this process. Wraps around all
   *         other exceptions and prints their message.
   * 
   */
  
  public static String createSourceCode(Microservice microservice, 
		  String templateRepositoryName, BaseGitHostAdapter gitAdapter, String commitMessage, String versionTag,
		  boolean forcePush, String metadataDoc) throws GitHostException {
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
	            case "i5.las2peer.connectors.webConnector.WebConnector.properties":
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
	        throw new GitHostException(e.getMessage());
	      }

	      if (!forcePush) {
	    	  microserviceRepository = generateNewRepository(repositoryName, gitAdapter);
	      } else {
	    	  microserviceRepository = getRemoteRepository(repositoryName, gitAdapter);
	    	  Git git = Git.wrap(microserviceRepository);
	          StoredConfig config = git.getRepository().getConfig();
	          
	          RemoteConfig remoteConfig = null;
	          
	          try {
	          remoteConfig = new RemoteConfig(config, "Remote");
	          remoteConfig.addURI(new URIish(gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + repositoryName + ".git"));
	    		
	          
	          remoteConfig.update(config);
	          config.save();
	          } catch (URISyntaxException e) {
	        	  throw new GitHostException("Malformed url: " + e.getMessage());
	          } catch (IOException e) {
	        	  throw new GitHostException("IO exception: " + e.getMessage());
	          }
	      }
	      
	      // generate service class and test
	      String repositoryLocation = gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + repositoryName;

	      FileTraceModel serviceClassTraceModel =
	          new FileTraceModel(traceModel, getServiceFileName(microservice));
	      traceModel.addFileTraceModel(serviceClassTraceModel);

	      TemplateStrategy strategy = new InitialGenerationStrategy();
	      TemplateEngine serviceTemplateEngine = new TemplateEngine(strategy, serviceClassTraceModel);

	      generateNewServiceClass(serviceTemplateEngine, serviceClass, microservice, repositoryLocation,
	          genericHttpMethod, genericHttpMethodBody, genericApiResponse, genericHttpResponse,
						genericCustomMessageDescription, genericCustomMessageLog, genericLogStringPayload,
						genericLogStringPayloadDescription, genericLogStringResponse, genericLogStringResponseDescription,
						genericLogTimeDifference, genericLogTimeDifferenceDescription, genericMeasureTime, genericMeasureTimeDifference,
	          databaseConfig, databaseInstantiation, serviceInvocation, metadataDoc);

          // generate classes schema
          FileTraceModel classesTraceModel =
              new FileTraceModel(traceModel, getClassesFileName(microservice));
          traceModel.addFileTraceModel(classesTraceModel);

          TemplateEngine classesTemplateEngine = new TemplateEngine(new InitialGenerationStrategy(), classesTraceModel);

          generateNewClasses(classesTemplateEngine, classes, microservice, repositoryLocation,
              genericClassBody, genericClassProperty, metadataDoc);

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
	      String commitSha = "";
	      try {
	        RevCommit commit = Git.wrap(microserviceRepository).commit()
	            .setMessage(commitMessage)
	            .setCommitter(caeUser).call();
	        Ref head = microserviceRepository.getAllRefs().get("HEAD");
            commitSha = head.getObjectId().getName();
            
            if(versionTag != null) {
            	Git.wrap(microserviceRepository).tag().setObjectId(commit).setName(versionTag).call();	
            }
	      } catch (Exception e) {
	        logger.printStackTrace(e);
	        throw new GitHostException(e.getMessage());
	      }

	      // push (local) repository content to GitHub repository
	      try {
	        pushToRemoteRepository(microserviceRepository, gitAdapter, versionTag, forcePush);
	      } catch (Exception e) {
	        logger.printStackTrace(e);
	        throw new GitHostException(e.getMessage());
	      }
	      
	      return commitSha;

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
  
  public static void createSourceCode(Microservice microservice, String templateRepositoryName,
      BaseGitHostAdapter gitAdapter, String commitMessage, String versionTag, String metadataDoc)
      throws GitHostException {
	  createSourceCode(microservice, templateRepositoryName, gitAdapter, commitMessage, versionTag, false, metadataDoc);
    
  }

  protected static void generateOtherArtifacts(TemplateEngine templateEngine,
      Microservice microservice, String gitHubOrganization, String templateContent) throws ModelParseException{
    String repositoryName = getRepositoryName(microservice);
    String packageName = getPackageName(microservice);
    String port = "8080";
    
    
    // get the port: skip first 6 characters for search (http: / https:)
    try {
    	if(microservice.getPath().contains("http:") || microservice.getPath().contains("https:")) {
    		port = String.valueOf(new URL(microservice.getPath()).getPort());
    	}else {
    		port = String.valueOf(new URL("http://"+microservice.getPath()).getPort());	
    	}
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
        template.setVariable("$Microservice_Name$", microservice.getVersionedModelId());
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
              "<dependency org=\"mysql\" name=\"mysql-connector-java\" rev=\"5.1.6\" conf=\"bundle->default\"/>\n"
                  + "    <dependency org=\"org.apache.commons\" name=\"commons-pool2\" rev=\"2.2\" conf=\"bundle->default\"/>\n"
                  + "    <dependency org=\"org.apache.commons\" name=\"commons-dbcp2\" rev=\"2.0\" conf=\"bundle->default\"/>");
        } else {
          template.setVariable("$MySQL_Dependencies$", "");
        }
        break;
      case "i5.las2peer.services.servicePackage.ServiceClass.properties":

        break;
      case "i5.las2peer.connectors.webConnector.WebConnector.properties":
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
																								String genericCustomMessageDescription, String genericCustomMessageLog,
																								String genericLogStringPayload, String genericLogStringPayloadDescription,
																								String genericLogStringResponse, String genericLogStringResponseDescription,
																								String genericLogTimeDifference, String genericLogTimeDifferenceDescription,
																								String genericMeasureTime, String genericMeasureTimeDifference,
																								String databaseConfig, String databaseInstantiation, String serviceInvocation,
																								String metadataDoc) {
    // helper variables
    String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1);
    String relativeResourcePath =
        microservice.getPath().substring(microservice.getPath().indexOf("/", 8) + 1);
    boolean hasServiceInvocations = false;
		Map<String, String> customMessageDescriptions = new HashMap<>();

    // create template and add to template engine
    Template serviceClassTemplate =
        templateEngine.createTemplate(microservice.getMicroserviceModelId(), serviceClass);
    templateEngine.addTemplate(serviceClassTemplate);

    templateEngine.addTrace(microservice.getMicroserviceModelId(), "RESTful Resource",
        microservice.getResourceName(), serviceClassTemplate);

    // service name for documentation
    serviceClassTemplate.setVariable("$Microservice_Name$", microservice.getVersionedModelId());
    // relative resource path (resource base path)
    serviceClassTemplate.setVariable("$Relative_Resource_Path$", relativeResourcePath);
    // version
    serviceClassTemplate.setVariable("$Microservice_Version$", microservice.getVersion() + "");
    // developer
    serviceClassTemplate.setVariable("$Developer$", microservice.getDeveloper());
    // link to license file
    serviceClassTemplate.setVariable("$License_File_Address$",
        repositoryLocation + "/blob/master/LICENSE.txt");

    // PARSE METADATA DOC
    ObjectMapper mapper = new ObjectMapper();
    JsonNode metadataTree = null;
    JsonNode nodeDetails = null;

    if (!Strings.isNullOrEmpty(metadataDoc)) {
        try {
            metadataTree = mapper.readTree(metadataDoc);
            System.out.println("===Parsed json ");
            System.out.println(metadataTree);
            if (metadataTree.hasNonNull("nodes"))
                nodeDetails = metadataTree.get("nodes");
        } catch (IOException ex) {
            System.out.println("Exception on metadata tree user input metadata doc");
            ex.printStackTrace();
        }
    }

    if (metadataTree != null) {
        try {
            // get info node
            if (metadataTree.hasNonNull("info")) {
                JsonNode infoNode = metadataTree.get("info");
                String description = infoNode.get("description").asText();
                String version = infoNode.get("version").asText();
                String termsOfService = infoNode.get("termsOfService").asText();

                System.out.println("===Parsed information ");
                System.out.println(description);
                System.out.println(version);
                System.out.println(termsOfService);

                serviceClassTemplate.setVariable("$Metadata_Version$", version);
                serviceClassTemplate.setVariable("$Metadata_Description$", description);
                serviceClassTemplate.setVariable("$Metadata_Terms$", termsOfService);

            }
        } catch (Exception ex) {
            System.out.println("Exception on parsing user input metadata doc");
            serviceClassTemplate.setVariable("$Metadata_Version$", "1.0");
            serviceClassTemplate.setVariable("$Metadata_Description$", "Generated Microservice from CAE");
            serviceClassTemplate.setVariable("$Metadata_Terms$", "");
        }
    }
    
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

      System.out.println("[Microservice generator HTTP Method] Processing http method " + currentMethod.getName());
      System.out.println(currentMethod.getMethodType().toString());
      System.out.println(currentMethod.getPath());

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
      // responses
      HashMap<String, HttpResponse> httpResponses = currentMethod.getNodeIdResponses();
      int httpResponseIndex = 0;

      for (HashMap.Entry<String, HttpResponse> entry: httpResponses.entrySet()) {

        String responseNodeId = entry.getKey();
        boolean isLastResponse = httpResponseIndex == currentMethod.getHttpResponses().size() - 1;

        String description = "";
        String schemaName = "";

        if (nodeDetails != null) {
          if (nodeDetails.hasNonNull(responseNodeId)) {
            JsonNode nodeDetail = nodeDetails.get(responseNodeId); 
            if (nodeDetail.hasNonNull("description")) {
                description = nodeDetail.get("description").asText();
            }

            if (nodeDetail.hasNonNull("schema")) {
                schemaName = nodeDetail.get("schema").asText();
            }
          }
        }

        currentMethodTemplate.setVariable("$Response_Description$", description);

        HttpResponse currentResponse = entry.getValue();
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

        if (!description.equals(""))
            apiResponseTemplate.setVariable("$HTTPResponse_Name$", description);
        else
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

          // if schema is available
          if (schemaName != null && !schemaName.equals("")) {
            httpResponseTemplate.setVariable("$HTTP_Response_Result_Init$", "new classes().new " + schemaName + "().toJSON()");
          } else {
            httpResponseTemplate.setVariable("$HTTP_Response_Result_Init$", "new JSONObject()");
          }

        }
        // check for custom return type and mark it in produces annotation if found
        if (currentResponse.getResultType() == ResultType.CUSTOM) {
          producesAnnotation = "CUSTOM";
          httpResponseTemplate.setVariable("$HTTP_Response_Result_Init$", "CUSTOM");
        }
        if (currentResponse.getResultType() == ResultType.String) {
          httpResponseTemplate.setVariable("$HTTP_Response_Result_Init$", "\"Some String\"");
        }

				generateResponseLogging(templateEngine, customMessageDescriptions, httpResponseTemplate, microservice, currentMethod, currentResponse, genericCustomMessageLog,
						genericLogStringResponse, genericLogStringResponseDescription);

				httpResponseIndex += 1;
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
      HashMap<String, HttpPayload> httpPayloads = currentMethod.getNodeIdPayloads();
      int httpPayloadIndex = 0;

      for (HashMap.Entry<String, HttpPayload> entry: httpPayloads.entrySet()) {

        boolean isLast = httpPayloadIndex == currentMethod.getHttpPayloads().size() - 1;

        String payloadNodeId = entry.getKey();
        String description = "";
        String schemaName = "";

        HttpPayload currentPayload = entry.getValue();
        // add param for JavaDoc
        // dirty, but works:-)
        String type = currentPayload.getPayloadType().toString();
        if (type.equals("PATH_PARAM")) {
          type = "String";
        }

        if (nodeDetails != null) {
          if (nodeDetails.hasNonNull(payloadNodeId)) {

            System.out.println("[MICROSERVICE GENERATOR] Node details available");
            System.out.println(nodeDetails);

            JsonNode nodeDetail = nodeDetails.get(payloadNodeId); 
            if (nodeDetail.hasNonNull("description")) {
                description = nodeDetail.get("description").asText();
            }

            // get schema name if available
            if (nodeDetail.hasNonNull("schema")) {
                schemaName = nodeDetail.get("schema").asText();
            }
          }
        }

        Template paramTemplate = currentMethodTemplate.createTemplate(
            currentPayload.getModelId() + ":param", "   * @param $name$ $description$ a $type$");

        paramTemplate.setVariable("$name$", currentPayload.getName());
        paramTemplate.setVariable("$type$", type);
        paramTemplate.setVariable("$description$", description);

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
        	// TODO workaround
          // consumesAnnotation = "MediaType.APPLICATION_JSON";
          consumesAnnotation = "MediaType.TEXT_PLAIN";
          
          // if schema is available
          System.out.println("[MICROSERVICE GENERATOR] Schema name available " + schemaName);
          if (schemaName != null && !schemaName.equals("")) {
            System.out.println("[MICROSERVICE GENERATOR] Schema name not null " + schemaName);
            // pass parameter as string and add casting from json
            //parameterCode += "classes." + schemaName + " " + currentPayload.getName() + ", ";
            parameterCode += "String " + currentPayload.getName() + ", ";

            Template castTemplate = templateEngine.createTemplate(
                    currentPayload.getModelId() + ":castSchema",
                    "   classes.$Schema_Name$ payload$Payload_Name$Object = new classes().new $Schema_Name$();\n" +
                    "   try { \n" +
                    "       payload$Payload_Name$Object.fromJSON($Payload_Name$);\n" +
                    "   } catch (Exception e) { \n" +
                    "       e.printStackTrace();\n" +
                    "       JSONObject result = new JSONObject();\n" + 
                    "       return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(\"Cannot convert json to object\").build();\n" +
                    "   }");
            castTemplate.setVariable("$Payload_Name$", currentPayload.getName());
            castTemplate.setVariable("$Schema_Name$", schemaName);
            currentMethodTemplate.appendVariable("$HTTPMethod_Casts$", castTemplate);

            System.out.println("[CAST TEMPLATE]");
            System.out.print(castTemplate.getContent());

            System.out.println("[CURRENT METHOD TEMPLATE]");
            System.out.print(currentMethodTemplate.getContent());

          } else {
            System.out.println("[MICROSERVICE GENERATOR] Schema null go string " + schemaName);
            parameterCode += "String " + currentPayload.getName() + ", ";

            Template castTemplate = templateEngine.createTemplate(
                    currentPayload.getModelId() + ":cast",
                    "    JSONObject $Payload_Name$_JSON = (JSONObject) JSONValue.parse($Payload_Name$);\n");
            castTemplate.setVariable("$Payload_Name$", currentPayload.getName());
            currentMethodTemplate.appendVariable("$HTTPMethod_Casts$", castTemplate);
          }

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

			// log content of each payload to MobSOS if configured
			generatePayloadLogging(templateEngine, customMessageDescriptions, currentMethodTemplate, microservice, currentMethod, genericCustomMessageLog,
					genericLogStringPayload, genericLogStringPayloadDescription);

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

			// log the processing time of all invocations to MobSOS if configured
			generateTimeLogging(templateEngine, customMessageDescriptions, currentMethodBodyTemplate, microservice,
					currentMethod, genericCustomMessageLog, genericMeasureTime, genericMeasureTimeDifference,
					genericLogTimeDifference, genericLogTimeDifferenceDescription);

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

		// add unconnected MobSOSLogs to descriptions
		addUnconnectedMobSOSLogs(microservice, customMessageDescriptions);

		// add custom message descriptions for logging
		generateCustomMessageDescriptions(templateEngine, customMessageDescriptions, serviceClassTemplate, genericCustomMessageDescription);
	}

	private static String generateLoggingCall(TemplateEngine templateEngine, String genericCustomMessageLog, MobSOSLog mobSOSLog, String customMessageContent) {
		Template loggingCallTemplate = templateEngine.createTemplate(mobSOSLog.getModelId() + ":logMesssage", genericCustomMessageLog);
		loggingCallTemplate.setVariable("$CUSTOM_MESSAGE_ID$", String.valueOf(mobSOSLog.getCustomMessageID()));
		loggingCallTemplate.setVariable("$CUSTOM_MESSAGE_CONTENT$", customMessageContent);
		loggingCallTemplate.setVariable("$Include_Acting_Agent$", Boolean.toString(mobSOSLog.isIncludeActingAgent()));
		return loggingCallTemplate.getContent();
	}

	private static void generateResponseLogging(TemplateEngine templateEngine, Map<String, String> customMessageDescriptions,
																							Template httpResponseTemplate, Microservice microservice,
																							HttpMethod httpMethod, HttpResponse response,
																							String genericCustomMessageLog, String genericLogStringResponse,
																							String genericLogStringResponseDescription) {
		MobSOSLog mobSOSLog = response.getMobSOSLog();
		String logResponseCode;
		if (mobSOSLog == null) {
			logResponseCode = "";
		} else {
			Template logResponseTemplate = templateEngine.createTemplate(mobSOSLog.getModelId() + ":logResponse", genericLogStringResponse);
			String responseLogVariable = "responseLogObj";
			logResponseTemplate.setVariable("$Response_Json_Name$", responseLogVariable);
			logResponseTemplate.setVariable("$Response_Code$", "\"" + response.getReturnStatusCode().getCode() + "\"");
			logResponseTemplate.setVariable("$Http_Method_Name$", "\"" + httpMethod.getMethodType().name() + "\"");
			logResponseTemplate.setVariable("$Resource_Method_Name$", "\"" + microservice.getResourceName() + "\"");
			logResponseTemplate.setVariable(
					"$Log_Call$",
					generateLoggingCall(templateEngine, genericCustomMessageLog, mobSOSLog, responseLogVariable + ".toJSONString()")
			);
			switch (response.getResultType()) {
				case String:
				case CUSTOM:
					logResponseTemplate.setVariable("$Response_Variable_Name$", response.getResultName());
					break;
				case JSONObject:
					logResponseTemplate.setVariable("$Response_Variable_Name$", response.getResultName() + ".toJSONString()");
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + response.getResultType());
			}
			logResponseCode = logResponseTemplate.getContent();

			// add description for the getCustomMessageDescriptions method
			Template messageDescriptionTemplate = templateEngine.createTemplate(mobSOSLog.getModelId() + ":logResponseDescription", genericLogStringResponseDescription);
			messageDescriptionTemplate.setVariable("$Method_Name$", httpMethod.getName());
			messageDescriptionTemplate.setVariable("$Method_Type$", httpMethod.getMethodType().name());
			messageDescriptionTemplate.setVariable("$Message_ID$", String.valueOf(mobSOSLog.getCustomMessageID()));
			customMessageDescriptions.put("SERVICE_CUSTOM_MESSAGE_" + mobSOSLog.getCustomMessageID(), messageDescriptionTemplate.getContent());
		}
		httpResponseTemplate.setVariable("$Log_Response$", logResponseCode);
	}

	private static void generatePayloadLogging(TemplateEngine templateEngine, Map<String, String> customMessageDescriptions,
																						 Template httpMethodTemplate, Microservice microservice, HttpMethod httpMethod,
																						 String genericCustomMessageLog, String genericLogStringPayload,
																						 String genericLogStringPayloadDescription) {
		StringBuilder logPayloadCode = new StringBuilder();
		for (HttpPayload payload : httpMethod.getNodeIdPayloads().values()) {
			MobSOSLog mobSOSLog = payload.getMobSOSLog();
			if (mobSOSLog != null) {
				Template logPayloadTemplate = templateEngine.createTemplate(mobSOSLog.getModelId() + ":logPayload", genericLogStringPayload);
				String payloadJsonVariable = payload.getName() + "LogObj";
				logPayloadTemplate.setVariable("$Payload_Json_Name$", payloadJsonVariable);
				logPayloadTemplate.setVariable("$Payload_Variable_Name$", payload.getName());
				logPayloadTemplate.setVariable("$Http_Method_Name$", "\"" + httpMethod.getMethodType().name() + "\"");
				logPayloadTemplate.setVariable("$Resource_Method_Name$", "\"" + microservice.getResourceName() + "\"");
				logPayloadTemplate.setVariable(
						"$Log_Call$",
						generateLoggingCall(templateEngine, genericCustomMessageLog, mobSOSLog, payloadJsonVariable + ".toJSONString()")
				);
				logPayloadCode.append(logPayloadTemplate.getContent()).append("\n");

				// add description for the getCustomMessageDescriptions method
				Template messageDescriptionTemplate = templateEngine.createTemplate(mobSOSLog.getModelId() + ":logPayloadDescription", genericLogStringPayloadDescription);
				messageDescriptionTemplate.setVariable("$Method_Name$", httpMethod.getName());
				messageDescriptionTemplate.setVariable("$Method_Type$", httpMethod.getMethodType().name());
				messageDescriptionTemplate.setVariable("$PAYLOAD_NAME$", payload.getName());
				messageDescriptionTemplate.setVariable("$Message_ID$", String.valueOf(mobSOSLog.getCustomMessageID()));
				customMessageDescriptions.put("SERVICE_CUSTOM_MESSAGE_" + mobSOSLog.getCustomMessageID(), messageDescriptionTemplate.getContent());
			}
		}
		httpMethodTemplate.setVariable("$Log_Payloads$", logPayloadCode.toString());
	}

	private static void generateTimeLogging(TemplateEngine templateEngine, Map<String, String> customMessageDescriptions,
																					Template methodBodyTemplate, Microservice microservice, HttpMethod httpMethod,
																					String genericCustomMessageLog, String genericMeasureTime,
																					String genericMeasureTimeDifference, String genericLogTimeDifference,
																					String genericLogTimeDifferenceDescription) {
		MobSOSLog mobSOSLog = httpMethod.getMobSOSLog();
		if (mobSOSLog == null) {
			methodBodyTemplate.setVariable("$Measure_Start_Time$", "");
			methodBodyTemplate.setVariable("$Measure_Finish_Time$", "");
			methodBodyTemplate.setVariable("$Measure_Delta_Time$", "");
			methodBodyTemplate.setVariable("$Log_Time_Difference$", "");
		} else {
			// init templates
			Template measureStartTimeTemplate = templateEngine.createTemplate(mobSOSLog.getModelId() + ":measureStartTime", genericMeasureTime);
			Template measureFinishTimeTemplate = templateEngine.createTemplate(mobSOSLog.getModelId() + ":measureFinishTime", genericMeasureTime);
			Template measureTimeDifferenceTemplate = templateEngine.createTemplate(mobSOSLog.getModelId() + ":measureTimeDifference", genericMeasureTimeDifference);
			Template logTimeDifferenceTemplate = templateEngine.createTemplate(mobSOSLog.getModelId() + ":logTimeDifference", genericLogTimeDifference);

			// variable names
			String startTimeVariable = "processingStart";
			String finishTimeVariable = "processingFinished";
			String differenceVariable = "processingDuration";
			String differenceJsonVariable = differenceVariable + "Obj";

			// set template variables
			measureStartTimeTemplate.setVariable("$Time_Variable_Name$", startTimeVariable);
			measureFinishTimeTemplate.setVariable("$Time_Variable_Name$", finishTimeVariable);
			measureTimeDifferenceTemplate.setVariable("$Time_Delta_Variable_Name$", differenceVariable);
			measureTimeDifferenceTemplate.setVariable("$Start_Time_Variable_Name$", startTimeVariable);
			measureTimeDifferenceTemplate.setVariable("$Stop_Time_Variable_Name$", finishTimeVariable);
			logTimeDifferenceTemplate.setVariable("$Time_Delta_Json_Name$", differenceJsonVariable);
			logTimeDifferenceTemplate.setVariable("$Time_Delta_Variable_Name$", differenceVariable);
			logTimeDifferenceTemplate.setVariable("$Http_Method_Name$", "\"" + httpMethod.getMethodType().name() + "\"");
			logTimeDifferenceTemplate.setVariable("$Resource_Method_Name$", "\"" + microservice.getResourceName() + "\"");
			logTimeDifferenceTemplate.setVariable(
					"$Log_Call$",
					generateLoggingCall(templateEngine, genericCustomMessageLog, mobSOSLog, differenceJsonVariable + ".toJSONString()")
			);

			String indent = "    ";
			// insert results into body template
			methodBodyTemplate.setVariable("$Measure_Start_Time$", indent + measureStartTimeTemplate.getContent());
			methodBodyTemplate.setVariable("$Measure_Finish_Time$", indent + measureFinishTimeTemplate.getContent());
			methodBodyTemplate.setVariable("$Measure_Delta_Time$", indent + measureTimeDifferenceTemplate.getContent());
			methodBodyTemplate.setVariable("$Log_Time_Difference$", logTimeDifferenceTemplate.getContent());

			// add description for the getCustomMessageDescriptions method
			Template messageDescriptionTemplate = templateEngine.createTemplate(mobSOSLog.getModelId() + ":logTimeDescription", genericLogTimeDifferenceDescription);
			messageDescriptionTemplate.setVariable("$Method_Name$", httpMethod.getName());
			messageDescriptionTemplate.setVariable("$Method_Type$", httpMethod.getMethodType().name());
			messageDescriptionTemplate.setVariable("$Message_ID$", String.valueOf(mobSOSLog.getCustomMessageID()));
			customMessageDescriptions.put("SERVICE_CUSTOM_MESSAGE_" + mobSOSLog.getCustomMessageID(), messageDescriptionTemplate.getContent());
		}
	}

	private static void generateCustomMessageDescriptions(TemplateEngine templateEngine, Map<String, String> customMessageDescriptions, Template serviceClassTemplate, String genericCustomMessageDescription) {
		StringBuilder customMessageDescriptionCode = new StringBuilder();
		for (Map.Entry<String, String> entry : customMessageDescriptions.entrySet()) {
			Template customMessageDescriptionTemplate = templateEngine.createTemplate(entry.getKey() + ":customMessageDescription", genericCustomMessageDescription);
			customMessageDescriptionTemplate.setVariable("$CUSTOM_MESSAGE_ID$", "\"" + entry.getKey() + "\"");
			customMessageDescriptionTemplate.setVariable("$CUSTOM_MESSAGE_DESCRIPTION$", entry.getValue());
			customMessageDescriptionCode.append(customMessageDescriptionTemplate.getContent());
		}
		serviceClassTemplate.setVariable("$Custom_Message_Descripions$", customMessageDescriptionCode.toString());
	}

	private static void addUnconnectedMobSOSLogs(Microservice microservice, Map<String, String> customMessageDescriptions) {
		for (MobSOSLog mobSOSLog : microservice.getMobSOSLogs().values()) {
			String customMessageID = "SERVICE_CUSTOM_MESSAGE_" + mobSOSLog.getCustomMessageID();
			if (!customMessageDescriptions.containsKey(customMessageID)) {
				customMessageDescriptions.put(customMessageID, "\"" + mobSOSLog.getDescriptionMarkdown() + "\"");
			}
		}
	}

	/**
     * Generates classes based on schema.
     *
     * @param templateEngine        the template engine to use
     * @param classes               the classes template file
     * @param microservice          the microservice model
     * @param repositoryLocation    the location of the service's repository
     * @param genericClassBody      a generic class body template
     * @param genericClassProperty  a generic class property template
     * @param metadataDoc           metadata information that will be used to generate schemas
     */
    protected static void generateNewClasses(TemplateEngine templateEngine, String classes,
                                                    Microservice microservice, String repositoryLocation, 
                                                    String genericClassBody, String genericClassProperty,
                                                    String metadataDoc) {
        
        System.out.println("[Microservice Generator - generateNewClasses] Generate new service class for " + classes);
        System.out.println("[Microservice Generator - generateNewClasses] Repository name " + repositoryLocation);

        // helper variables
        String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
                + microservice.getResourceName().substring(1);

        // PARSE METADATA DOC
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadataTree = null;
        JsonNode nodeDefinitions = null;

        if (!Strings.isNullOrEmpty(metadataDoc)) {
            try {
                metadataTree = mapper.readTree(metadataDoc);
                System.out.println("===Parsed json ");
                System.out.println(metadataTree);
                if (metadataTree.hasNonNull("definitions"))
                    nodeDefinitions = metadataTree.get("definitions");
            } catch (IOException ex) {
                System.out.println("Exception on metadata tree user input metadata doc");
                ex.printStackTrace();
            }
        }

        // create template and add to template engine
        Template classesTemplate =
                templateEngine.createTemplate(microservice.getMicroserviceModelId(), classes);
        templateEngine.addTemplate(classesTemplate);

        System.out.println("====CLASSES TEMPLATE====");
        System.out.println(classesTemplate.getContent());

        System.out.println("[Microservice Generator - generateNewClasses] Template engine file name " + templateEngine.getFileName());
        System.out.println("[Microservice Generator - generateNewClasses] Classes template file name " + classesTemplate.getTemplateFileName());

        templateEngine.addTrace(microservice.getMicroserviceModelId(), "Definitions schemas",
                microservice.getResourceName(), classesTemplate);

        // package and import paths
        classesTemplate.setVariable("$Lower_Resource_Name$", packageName);

        // iterate for every definitions
        if (nodeDefinitions != null) {
            System.out.println("[Microservice Generator - generateNewClasses] Iterate through definitions in metadata");

            Iterator<Map.Entry<String, JsonNode>> nodes = nodeDefinitions.fields();
            while (nodes.hasNext()) {
                        
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes.next();
                String className = entry.getKey();
                JsonNode entryValue = entry.getValue();

                // create new template for the current method
                Template currentClassBodyTemplate = templateEngine
                        .createTemplate( packageName + className + ":class", "    " + genericClassBody);

                // iterate through properties and data type
                Iterator<Map.Entry<String, JsonNode>> propertyNodes = entryValue.fields();
                System.out.println("[Microservice Generator - generateNewClasses] Iterate through properties in class");

                while (propertyNodes.hasNext()) {
                    Map.Entry<String, JsonNode> propertyEntry = (Map.Entry<String, JsonNode>) propertyNodes.next();
                    String propertyName = propertyEntry.getKey();
                    JsonNode propertyValue = propertyEntry.getValue();

                    // get type
                    String propertyType = propertyValue.get("type").asText();
                    propertyType = swaggerTypeToJavaType(propertyType, propertyValue, 0);

                    System.out.println("[Microservice Generator - generateNewClasses] Property name " + propertyName + " type " + propertyType);

                    // create new template for the current method
                    Template currentPropertyBodyTemplate = templateEngine
                            .createTemplate( packageName + className + propertyName + propertyType + ":classProperty", "\n" + genericClassProperty);

                    // create template for ToJSON
                    Template currentJsonTemplate =
                        templateEngine.createTemplate(packageName + className + propertyName + propertyType + ":jsonProperty",
                                "        jo.put(\"$Property_Name$\", this.$Property_Name$); \n");

                    // create template for fromJSON
                    Template currentFromJsonTemplate =
                        templateEngine.createTemplate(packageName + className + propertyName + propertyType + ":fromJsonProperty",
                                "        this.$Property_Name$ = $Property_Cast$; \n");

                    String propertyFromJsonCast = "(String) jsonObject.get(\"" + propertyName + "\")";
                    switch (propertyType) {
                        case "int":
                            propertyFromJsonCast = "((Long) jsonObject.get(\"" + propertyName + "\")).intValue()";
                            break;
                        case "boolean":
                            propertyFromJsonCast = "(1 == ((Long) jsonObject.get(\"" + propertyName + "\")).intValue())";
                            break;
                        case "default":
                            break;
                    }

                    currentPropertyBodyTemplate.setVariable("$Property_Name$", propertyName);
                    currentPropertyBodyTemplate.setVariable("$Property_Type$", propertyType);

                    currentJsonTemplate.setVariable("$Property_Name$", propertyName);
                    currentFromJsonTemplate.setVariable("$Property_Name$", propertyName);
                    currentFromJsonTemplate.setVariable("$Property_Cast$", propertyFromJsonCast);

                    char c[] = propertyName.toCharArray();
                    c[0] = Character.toLowerCase(c[0]);
                    String propertyNameLowerCase = new String(c);
                    currentPropertyBodyTemplate.setVariable("$Property_Name_LowerCase$", propertyNameLowerCase);

                    // add template of current method to service class template
                    currentClassBodyTemplate.appendVariable("$Class_Properties$", currentPropertyBodyTemplate);
                    currentClassBodyTemplate.appendVariable("$Class_ToJson$",  currentJsonTemplate);
                    currentClassBodyTemplate.appendVariable("$Json_ToClass$",  currentFromJsonTemplate);
                }

                // remove last placeholder
                currentClassBodyTemplate.setVariableIfNotSet("$Class_Properties$", "");

                // replace variables in body
                currentClassBodyTemplate.setVariable("$Class_Name$", className);

                // add template of current method to service class template
                classesTemplate.appendVariable("$Classes_Body$", currentClassBodyTemplate);
            }
        }

        classesTemplate.setVariableIfNotSet("$Classes_Body$", "");
    }

    private static String swaggerTypeToJavaType(String swaggerType, JsonNode propertyValue, int depth) {
        switch (swaggerType.toLowerCase()) {
            case "string":
                swaggerType = "String"; break;
            case "number":
            case "integer":
                swaggerType = "int"; break;
            case "boolean":
                swaggerType = "boolean"; break;
            case "array":
                // get array type
                if (depth == 0) {
                    String arrayType = propertyValue.get("items").get("type").asText();
                    arrayType = swaggerTypeToJavaType(arrayType, propertyValue, depth + 1);
                    swaggerType = arrayType + "[]";
                } else {
                    swaggerType = "String";
                }
                break;
            default:
                swaggerType = "String";
        }
        return swaggerType;
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
    serviceTestTemplate.setVariable("$Microservice_Name$", microservice.getVersionedModelId());

    serviceTest = serviceTest.replace("$Resource_Name$", microservice.getResourceName());
    serviceTest = serviceTest.replace("$Microservice_Name$", microservice.getVersionedModelId());
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
    databaseTemplate.setVariable("$Service_Name$", microservice.getVersionedModelId());
    databaseTemplate.setVariable("$Database_Schema$", database.getSchema());
    // remove last placeholder
    databaseTemplate.setVariableIfNotSet("$Database_Table$", "");
  }

}