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
import i5.las2peer.services.codeGenerationService.models.application.Application;

/**
 * 
 * Generates application source code from passed on
 * {@link i5.las2peer.services.codeGenerationService.models.application} models.
 * 
 */
public class ApplicationGenerator extends Generator {


  /**
   * 
   * Creates source code from a CAE application model and pushes it to GitHub.
   * 
   * @param application the application model
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
  public static void createSourceCode(Application application, String templateRepositoryName,
      String gitHubOrganization, String gitHubUser, String gitHubUserMail, String gitHubPassword)
          throws GitHubException {
    // variables to be closed in the final block
    Repository applicationRepository = null;
    TreeWalk treeWalk = null;
    try {
      PersonIdent caeUser = new PersonIdent(gitHubUser, gitHubUserMail);
      String repositoryName = "application-" + application.getName().replace(" ", "-");

      applicationRepository =
          generateNewRepository(repositoryName, gitHubOrganization, gitHubUser, gitHubPassword);

      // now we start by adding a readMe from the template repository (and thereby initializing the
      // master branch, which is needed to create a "gh-pages" branch afterwards
      String readMe = null;
      BufferedImage logo = null;
      treeWalk = getTemplateRepositoryContent(templateRepositoryName, gitHubOrganization);
      treeWalk.setFilter(PathFilter.create("application/"));
      ObjectReader reader = treeWalk.getObjectReader();
      // walk through the tree and retrieve the needed templates
      try {
        while (treeWalk.next()) {
          ObjectId objectId = treeWalk.getObjectId(0);
          ObjectLoader loader = reader.open(objectId);
          switch (treeWalk.getNameString()) {
            case "README.md":
              readMe = new String(loader.getBytes(), "UTF-8");
              readMe = readMe.replace("$Repository_Name$", repositoryName);
              readMe = readMe.replace("$Application_Name$", application.getName());
              readMe = readMe.replace("$Organization_Name$", gitHubOrganization);
              break;
            case "logo_application.png":
              logo = ImageIO.read(loader.openStream());
              break;
          }
        }
        // add the two files to the repository and commit them
        applicationRepository =
            createTextFileInRepository(applicationRepository, "", "README.md", readMe);
        applicationRepository =
            createImageFileInRepository(applicationRepository, "img/", "logo.png", logo);
        Git.wrap(applicationRepository).commit()
            .setMessage(
                "Initialized repository for application with version " + application.getVersion())
            .setCommitter(caeUser).call();
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // create "gh-pages" branch
      try {
        Git.wrap(applicationRepository).branchCreate().setName("gh-pages").call();
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // fetch microservice repository contents and add them
      for (String microserviceName : application.getMicroservices().keySet()) {
        String microserviceRepositoryName = "microservice-" + microserviceName.replace(" ", "-");
        treeWalk = getRepositoryContent(microserviceRepositoryName, gitHubOrganization);
        reader = treeWalk.getObjectReader();
        try {
          while (treeWalk.next()) {
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = reader.open(objectId);
            // copy the content of the repository and switch out the "old" paths
            String oldLogoAddress = "https://github.com/" + gitHubOrganization + "/"
                + microserviceRepositoryName + "/blob/master/img/logo.png";
            String newLogoAddress = "https://github.com/" + gitHubOrganization + "/"
                + repositoryName + "/blob/master/" + microserviceRepositoryName + "/img/logo.png";
            switch (treeWalk.getNameString()) {
              case "README.md":
                String frontendReadme = new String(loader.getBytes(), "UTF-8");
                frontendReadme = frontendReadme.replace(oldLogoAddress, newLogoAddress);
                applicationRepository = createTextFileInRepository(applicationRepository,
                    microserviceRepositoryName + "/", "README.md", frontendReadme);
                break;
              default:
                // determine type and then "pass it on"
                // TODO: I'm sure there is a more elegant way to do this
                String fileName = treeWalk.getNameString();
                treeWalk.getObjectReader();
                // text
                if (fileName.contains(".js") || fileName.contains(".txt")
                    || fileName.contains(".css") || fileName.contains(".html")
                    || fileName.contains(".java") || fileName.contains(".bat")
                    || fileName.contains(".sh") || fileName.contains(".md")
                    || fileName.contains(".classpath") || fileName.contains(".gitignore")
                    || fileName.contains(".project") || fileName.contains(".xml")
                    || fileName.contains(".sql") || fileName.contains(".properties")) {
                  String file = new String(loader.getBytes(), "UTF-8");
                  applicationRepository =
                      createTextFileInRepository(applicationRepository, microserviceRepositoryName
                          + "/" + treeWalk.getPathString().replace(fileName, ""), fileName, file);
                }
                // image
                else if (fileName.contains(".jpg") || fileName.contains(".jpeg")
                    || fileName.contains(".png") || fileName.contains(".bmp")
                    || fileName.contains(".gif")) {
                  BufferedImage image = ImageIO.read(loader.openStream());
                  applicationRepository =
                      createImageFileInRepository(applicationRepository, microserviceRepositoryName
                          + "/" + treeWalk.getPathString().replace(fileName, ""), fileName, image);
                }
                // binary
                else {
                  Object binaryObject = loader.getBytes();
                  applicationRepository =
                      createBinaryFileInRepository(applicationRepository,
                          microserviceRepositoryName + "/"
                              + treeWalk.getPathString().replace(fileName, ""),
                          fileName, binaryObject);
                }
                break;
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          throw new GitHubException(e.getMessage());
        }
        treeWalk.close();
        // commit files
        try {
          Git.wrap(applicationRepository).commit()
              .setMessage("Added microservice " + microserviceName).setCommitter(caeUser).call();
        } catch (Exception e) {
          e.printStackTrace();
          throw new GitHubException(e.getMessage());
        }
      }
      // push (local) repository content to GitHub repository "master" branch
      try {
        pushToRemoteRepository(applicationRepository, gitHubUser, gitHubPassword);
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // switch branch to "gh-pages"
      try {
        Git.wrap(applicationRepository).checkout().setName("gh-pages").call();
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // fetch frontend component repository contents and add them
      for (String frontendComponentName : application.getFrontendComponents().keySet()) {
        String frontendComponentRepositoryName =
            "frontendComponent-" + frontendComponentName.replace(" ", "-");
        treeWalk = getRepositoryContent(frontendComponentRepositoryName, gitHubOrganization);
        reader = treeWalk.getObjectReader();
        try {
          while (treeWalk.next()) {
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = reader.open(objectId);
            // copy the content of the repository and switch out the "old" paths
            String oldWidgetHome =
                "http://" + gitHubOrganization + ".github.io/" + frontendComponentRepositoryName;
            String newWidgetHome = "http://" + gitHubOrganization + ".github.io/" + repositoryName
                + "/" + frontendComponentRepositoryName;
            String oldLogoAddress = "https://github.com/" + gitHubOrganization + "/"
                + frontendComponentRepositoryName + "/blob/gh-pages/img/logo.png";
            String newLogoAddress =
                "https://github.com/" + gitHubOrganization + "/" + repositoryName
                    + "/blob/gh-pages/" + frontendComponentRepositoryName + "/img/logo.png";
            switch (treeWalk.getNameString()) {
              case "README.md":
                String frontendReadme = new String(loader.getBytes(), "UTF-8");
                frontendReadme = frontendReadme.replace(oldLogoAddress, newLogoAddress);
                applicationRepository = createTextFileInRepository(applicationRepository,
                    frontendComponentRepositoryName + "/", "README.md", frontendReadme);
                break;
              case "widget.xml":
                String widget = new String(loader.getBytes(), "UTF-8");
                widget = widget.replace(oldWidgetHome, newWidgetHome);
                applicationRepository = createTextFileInRepository(applicationRepository,
                    frontendComponentRepositoryName + "/", "widget.xml", widget);
                break;
              default:
                // determine type and then "pass it on"
                // TODO: I'm sure there is a more elegant way to do this
                String fileName = treeWalk.getNameString();
                treeWalk.getObjectReader();
                // text
                if (fileName.contains(".js") || fileName.contains(".txt")
                    || fileName.contains(".css") || fileName.contains(".html")
                    || fileName.contains(".java") || fileName.contains(".bat")
                    || fileName.contains(".sh") || fileName.contains(".md")
                    || fileName.contains(".classpath") || fileName.contains(".gitignore")
                    || fileName.contains(".project") || fileName.contains(".xml")
                    || fileName.contains(".sql") || fileName.contains(".properties")) {
                  String file = new String(loader.getBytes(), "UTF-8");
                  applicationRepository = createTextFileInRepository(applicationRepository,
                      frontendComponentRepositoryName + "/"
                          + treeWalk.getPathString().replace(fileName, ""),
                      fileName, file);
                }
                // image
                else if (fileName.contains(".jpg") || fileName.contains(".jpeg")
                    || fileName.contains(".png") || fileName.contains(".bmp")
                    || fileName.contains(".gif")) {
                  BufferedImage image = ImageIO.read(loader.openStream());
                  applicationRepository = createImageFileInRepository(applicationRepository,
                      frontendComponentRepositoryName + "/"
                          + treeWalk.getPathString().replace(fileName, ""),
                      fileName, image);
                }
                // binary
                else {
                  Object binaryObject = loader.getBytes();
                  applicationRepository =
                      createBinaryFileInRepository(applicationRepository,
                          frontendComponentRepositoryName + "/"
                              + treeWalk.getPathString().replace(fileName, ""),
                          fileName, binaryObject);
                }
                break;
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          throw new GitHubException(e.getMessage());
        }
        treeWalk.close();
        // commit files
        try {
          Git.wrap(applicationRepository).commit()
              .setMessage("Added frontend component " + frontendComponentName).setCommitter(caeUser)
              .call();
        } catch (Exception e) {
          e.printStackTrace();
          throw new GitHubException(e.getMessage());
        }
      }
      // push (local) repository content to GitHub repository "gh-pages" branch
      try {
        pushToRemoteRepository(applicationRepository, gitHubUser, gitHubPassword, "gh-pages",
            "gh-pages");
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // close all open resources
    } finally {
      applicationRepository.close();
      treeWalk.close();
    }
  }

}
