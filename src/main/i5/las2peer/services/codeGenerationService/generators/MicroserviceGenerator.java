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

    // variables holding content to be modified and added to repository later
    String projectFile = null;
    BufferedImage logo = null;
    String readMe = null;
    String buildFile = null;
    String startScriptWindows = null;
    String startScriptUnix = null;
    String packageName = microservice.getResourceName().substring(0, 1).toLowerCase()
        + microservice.getResourceName().substring(1); // helper variable
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
          }

        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // add files to new repository
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "", ".project", projectFile);
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "", "README.md", readMe);
      microserviceRepository =
          createImageFileInRepository(microserviceRepository, "img/", "logo.png", logo);
      microserviceRepository =
          createTextFileInRepository(microserviceRepository, "", "build.xml", buildFile);
      microserviceRepository = createTextFileInRepository(microserviceRepository, "bin/",
          "start_network.bat", startScriptWindows);
      microserviceRepository = createTextFileInRepository(microserviceRepository, "bin/",
          "start_network.sh", startScriptUnix);

      // Commit files
      try {
        Git.wrap(microserviceRepository).commit().setMessage("Generated Microservice")
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
