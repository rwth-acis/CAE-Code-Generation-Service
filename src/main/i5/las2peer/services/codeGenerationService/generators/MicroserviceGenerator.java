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
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

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
    String genericTestCase = null;
    String databaseConfig = null;
    String databaseInstantiation = null;

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
                ivy = ivy.replace("$MySQL_Dependencies$", "");
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
              webConnectorConfig = webConnectorConfig.replace("$HTTP_PORT$", port);
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
                classpath = classpath.replace("$Database_Libraries$", "");
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
            case "ServiceTest.java":
              serviceTest = new String(loader.getBytes(), "UTF-8");
              break;
            case "databaseConfig.txt":
              databaseConfig = new String(loader.getBytes(), "UTF-8");
              break;
            case "databaseInstantiation.txt":
              databaseInstantiation = new String(loader.getBytes(), "UTF-8");
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
          genericHttpMethod, databaseConfig, databaseInstantiation);
      serviceTest = generateNewServiceTest(serviceTest, microservice, genericTestCase);

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
            "src/main/i5/las2peer/service/" + packageName + "/database/", "DatabaseManager.java",
            databaseManager);
      }
      microserviceRepository = createTextFileInRepository(microserviceRepository,
          "src/main/i5/las2peer/service/" + packageName + "/",
          microservice.getResourceName() + ".java", serviceClass);
      microserviceRepository = createTextFileInRepository(microserviceRepository,
          "src/test/i5/las2peer/service/" + packageName + "/", "ServiceTest.java", serviceTest);

      // Commit files
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
   * @param serviceClass
   * @param microservice
   * @param repositoryLocation
   * @param genericHttpMethod
   * @param databaseConfig
   * @param databaseInstantiation
   * 
   * @return the service class as a string
   * 
   */
  private static String generateNewServiceClass(String serviceClass, Microservice microservice,
      String repositoryLocation, String genericHttpMethod, String databaseConfig,
      String databaseInstantiation) {
    // helper variables
    String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1);
    String relativeResourcePath =
        microservice.getPath().substring(microservice.getPath().indexOf("/", 8) + 1);

    // package and import pathes
    serviceClass = serviceClass.replace("$Lower_Resource_Name$", packageName);
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
      // variable names
      serviceClass = serviceClass.replace("$Database_Configuration$", databaseConfig);
      // instantiation
      serviceClass = serviceClass.replace("$Database_Instantiation$", databaseInstantiation);
    } else {
      // set to empty string
      serviceClass = serviceClass.replace("$Database_Configuration$", "");
      serviceClass = serviceClass.replace("$Database_Instantiation$", "");
    }
    for (int httpMethodIndex = 0; httpMethodIndex < microservice.getHttpMethods()
        .size(); httpMethodIndex++) {
      String currentMethod = genericHttpMethod;
      serviceClass = serviceClass.replace("$Service_Methods$", currentMethod);
    }
    serviceClass = serviceClass.replace("$Service_Methods$", "");

    return serviceClass;
  }


  /**
   * 
   * Generates the service test class.
   * 
   * @param serviceTest
   * @param microservice
   * @param genericTestCase
   * 
   * @return the service test as a string
   * 
   */
  private static String generateNewServiceTest(String serviceTest, Microservice microservice,
      String genericTestCase) {
    // get the resource address: (skip first /)
    String relativeResourcePath =
        microservice.getPath().substring(microservice.getPath().indexOf("/", 8) + 1);

    return serviceTest;
  }

}
