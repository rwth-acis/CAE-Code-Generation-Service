package i5.las2peer.services.codeGenerationService.generators;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.services.codeGenerationService.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.models.application.Application;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;

/**
 * 
 * Generates application source code from passed on
 * {@link i5.las2peer.services.codeGenerationService.models.application.Application} models.
 * 
 */
public class ApplicationGenerator extends Generator {

  private static final L2pLogger logger =
      L2pLogger.getInstance(ApplicationGenerator.class.getName());

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
      String gitHubOrganization, String gitHubUser, String gitHubUserMail, String gitHubPassword, String usedGitHost)
      throws GitHubException {
    String repositoryName = "application-" + application.getName().replace(" ", "-");
    createSourceCode(repositoryName, application, templateRepositoryName, gitHubOrganization,
        gitHubUser, gitHubUserMail, gitHubPassword, usedGitHost, false);
  }

  /**
   * 
   * Creates source code from a CAE application model and pushes it to GitHub.
   * 
   * @param repositoryName the repository of the application to use
   * @param application the application model
   * @param templateRepositoryName the name of the template repository on GitHub
   * @param gitHubOrganization the organization that is used in the CAE
   * @param gitHubUser the CAE user
   * @param gitHubUserMail the mail of the CAE user
   * @param gitHubPassword the password of the CAE user
   * @param forDeploy True, if the source code is intended to use for deployment purpose, e.g. no
   *        gh-pages branch will be used
   * 
   * @throws GitHubException thrown if anything goes wrong during this process. Wraps around all
   *         other exceptions and prints their message.
   * 
   */
  public static void createSourceCode(String repositoryName, Application application,
      String templateRepositoryName, String gitHubOrganization, String gitHubUser,
      String gitHubUserMail, String gitHubPassword, String usedGitHost,boolean forDeploy) throws GitHubException {
    // variables to be closed in the final block
    Repository applicationRepository = null;
    TreeWalk treeWalk = null;
    try {
      PersonIdent caeUser = new PersonIdent(gitHubUser, gitHubUserMail);


      applicationRepository =
          generateNewRepository(repositoryName, gitHubOrganization, gitHubUser, gitHubPassword, usedGitHost);

      // now we start by adding a readMe from the template repository (and thereby initializing the
      // master branch, which is needed to create a "gh-pages" branch afterwards
      String readMe = null;
      BufferedImage logo = null;
      treeWalk = getTemplateRepositoryContent(templateRepositoryName, gitHubOrganization, usedGitHost);
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
        logger.printStackTrace(e);
        throw new GitHubException(e.getMessage());
      }

      if (!forDeploy) {
        // create "gh-pages" branch
        try {
          Git.wrap(applicationRepository).branchCreate().setName("gh-pages").call();
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHubException(e.getMessage());
        }
      }

      // fetch microservice repository contents and add them
      for (String microserviceName : application.getMicroservices().keySet()) {
        String microserviceRepositoryName = "microservice-" + microserviceName.replace(" ", "-");
        treeWalk = getRepositoryContent(microserviceRepositoryName, gitHubOrganization, usedGitHost);
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
                // skip traces
                if (treeWalk.getPathString().contains("traces/")) {
                  continue;
                }
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
          logger.printStackTrace(e);
          throw new GitHubException(e.getMessage());
        }
        treeWalk.close();
        // commit files
        try {
          Git.wrap(applicationRepository).commit()
              .setMessage("Added microservice " + microserviceName).setCommitter(caeUser).call();
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHubException(e.getMessage());
        }
      }

      if (!forDeploy) {
        // push (local) repository content to GitHub repository "master" branch
        try {
          pushToRemoteRepository(applicationRepository, gitHubUser, gitHubPassword, usedGitHost);
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHubException(e.getMessage());
        }

        // switch branch to "gh-pages"
        try {
          Git.wrap(applicationRepository).checkout().setName("gh-pages").call();
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHubException(e.getMessage());
        }
      }

      // fetch frontend component repository contents and add them
      for (String frontendComponentName : application.getFrontendComponents().keySet()) {
        String frontendComponentRepositoryName =
            "frontendComponent-" + frontendComponentName.replace(" ", "-");
        treeWalk = getRepositoryContent(frontendComponentRepositoryName, gitHubOrganization, usedGitHost);
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
            if (forDeploy) {
              // use other url for deployment, replaced later by the dockerfile
              newWidgetHome = "$WIDGET_URL$:$HTTP_PORT$" + "/" + frontendComponentRepositoryName;
            }

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
              case "applicationScript.js":
                String applicationScript = new String(loader.getBytes(), "UTF-8");

                if (forDeploy) {
                  FrontendComponent frontendComponent =
                      application.getFrontendComponents().get(frontendComponentName);
                  applicationScript = applicationScript.replace(
                      frontendComponent.getMicroserviceAddress(), "$STEEN_URL$:$STEEN_PORT$");
                }

                applicationRepository = createTextFileInRepository(applicationRepository,
                    frontendComponentRepositoryName + "/js/", "applicationScript.js",
                    applicationScript);
                break;
              default:
                // skip traces
                if (treeWalk.getPathString().contains("traces/")) {
                  continue;
                }
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
          logger.printStackTrace(e);
          throw new GitHubException(e.getMessage());
        }
        treeWalk.close();
        // commit files
        try {
          Git.wrap(applicationRepository).commit()
              .setMessage("Added frontend component " + frontendComponentName).setCommitter(caeUser)
              .call();
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHubException(e.getMessage());
        }
      }
      if (!forDeploy) {
        // push (local) repository content to GitHub repository "gh-pages" branch
        try {
          pushToRemoteRepository(applicationRepository, gitHubUser, gitHubPassword, "gh-pages",
              "gh-pages", usedGitHost);
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHubException(e.getMessage());
        }
      } else {
        // push (local) repository content to GitHub repository "master" branch
        try {
          pushToRemoteRepository(applicationRepository, gitHubUser, gitHubPassword, usedGitHost);
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHubException(e.getMessage());
        }
      }

      // close all open resources
    } finally {
      applicationRepository.close();
      treeWalk.close();
    }
  }

  /**
   * Get the build path of the Jenkins remote api for a queue item. If the queue item is still
   * pending, null is returned.
   * 
   * @param queueItem The path of the queue item
   * @param jenkinsUrl The base path of Jenkins
   * @return The build path for the queue item, or Null if the is still pending/waiting
   * @throws Exception Possible exceptions from the http request
   */

  private static String getBuildPath(String queueItem, String jenkinsUrl) throws Exception {
    URL url = new URL(jenkinsUrl + queueItem + "api/json");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setDoOutput(true);
    connection.setUseCaches(false);

    if (connection.getResponseCode() != 200) {
      // queue item may be deleted, so its no longer queued
      return null;
    } else {
      String message = "";
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(connection.getInputStream()));
      for (String line; (line = reader.readLine()) != null;) {
        message += line + "\n";
      }
      reader.close();
      JSONParser parser = new JSONParser();
      JSONObject result = (JSONObject) parser.parse(message);


      if (result.containsKey("executable")) {
        JSONObject executeable = (JSONObject) result.get("executable");
        URI uri = new URI((String) executeable.get("url"));
        return uri.getPath();
      } else {
        return null;
      }
    }

  }

  /**
   * Get the job console text of a build of a queue item. When the item is still pending, the string
   * "Pending" is returned.
   * 
   * @param queueItem The path of the queue item
   * @param jenkinsUrl The base path of Jenkins
   * @return The console text of a build of a queue item or "Pending" if the item is still
   *         pending/waiting for its execution
   */

  public static String deployStatus(String queueItem, String jenkinsUrl) {
    try {
      String buildPath = getBuildPath(queueItem, jenkinsUrl);
      if (buildPath == null) {
        return "Pending";
      } else {

        URL url = new URL(jenkinsUrl + buildPath + "consoleText");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setUseCaches(false);

        String message = "";
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(connection.getInputStream()));
        for (String line; (line = reader.readLine()) != null;) {
          message += line + "\n";
        }
        reader.close();
        // forward (in case of) error
        if (connection.getResponseCode() != 200) {
          throw new Exception("Jenkins error: " + message);
        } else {
          return message;
        }

      }

    } catch (Exception e) {
      logger.printStackTrace(e);
      return "Error:" + e.getMessage();
    }
  }

  /**
   * Start a job for the deployment of an application
   * 
   * @param jenkinsUrl The base path of Jenkins
   * @param jobToken The token to start the job
   * @param jobName The name of the job to start
   * @return The path of the queue item of the started job
   */

  public static String deployApplication(String jenkinsUrl, String jobToken, String jobName) {

    try {

      L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Starting Jenkin job: " + jobName);

      URL url = new URL(jenkinsUrl + "/job/" + jobName + "/build?token=" + jobToken);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setDoOutput(true);
      connection.setUseCaches(false);

      // forward (in case of) error
      if (connection.getResponseCode() != 201) {
        String message = "";
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        for (String line; (line = reader.readLine()) != null;) {
          message += line;
        }
        reader.close();
        throw new Exception(message);
      } else {
        L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Job started!");
        URI uri = new URI(connection.getHeaderField("Location"));
        String path = uri.getPath();
        return path;
      }
    } catch (Exception e) {
      logger.printStackTrace(e);
      return "Error";
    }

  }

}
