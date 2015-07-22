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
    // get the port: skip first 6 characters for search(http: / https:)
    String port = microservice.getPath().substring(microservice.getPath().indexOf(":", 6) + 1,
        microservice.getPath().indexOf("/", microservice.getPath().indexOf(":", 6)));
    // variables holding content to be modified and added to repository later
    String projectFile = null;
    BufferedImage logo = null;
    String readMe = null;
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
                ivy = ivy.replace("$MYSQL_DEPENDENCY$",
                    "<dependency org=\"mysql\" name=\"mysql-connector-java\" rev=\"5.1.6\" />");
              } else {
                ivy = ivy.replace("$MYSQL_DEPENDENCY$", "");
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
          }

        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
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
          createImageFileInRepository(microserviceRepository, "img/", "logo.png", logo);

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

}
