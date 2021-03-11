package i5.las2peer.services.codeGenerationService.generators;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHostException;
import i5.las2peer.services.codeGenerationService.models.application.Application;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;

/**
 * 
 * Generates application source code from passed on
 * {@link i5.las2peer.services.codeGenerationService.models.application.Application}
 * models.
 * 
 */
public class ApplicationGenerator extends Generator {

  public static String deploymentRepo;
  private static final L2pLogger logger = L2pLogger.getInstance(ApplicationGenerator.class.getName());

  /**
   * 
   * Creates source code from a CAE application model and pushes it to GitHub.
   * 
   * @param application   the application model
   * 
   * @param gitAdapter    adapter for Git
   * 
   * @param commitMessage Message used as the commit message.
   * @param versionTag    String which should be used as the tag when commiting.
   *                      May be null.
   * 
   * @throws GitHostException thrown if anything goes wrong during this process.
   *                          Wraps around all other exceptions and prints their
   *                          message.
   * 
   */
  public static void createSourceCode(Application application, BaseGitHostAdapter gitAdapter, String commitMessage,
      String versionTag) throws GitHostException {
    if (gitAdapter == null) {
      throw new GitHostException("Adapter is null!");
    }
    String repositoryName = "application-" + application.getVersionedModelId().replace(" ", "-");
    createSourceCode(repositoryName, application, gitAdapter, commitMessage, versionTag, false);
  }

  /**
   * 
   * Creates source code from a CAE application model and pushes it to GitHub.
   * 
   * @param repositoryName the repository of the application to use
   * @param application    the application model
   * @param gitAdapter     adapter for Git
   * @param commitMessage  Message used as the commit message.
   * @param versionTag     String which should be used as the tag when commiting.
   *                       May be null.
   * @param forDeploy      True, if the source code is intended to use for
   *                       deployment purpose, e.g. no gh-pages branch will be
   *                       used
   * @throws GitHostException thrown if anything goes wrong during this process.
   *                          Wraps around all other exceptions and prints their
   *                          message. *
   */
  public static void createSourceCode(String repositoryName, Application application, BaseGitHostAdapter gitAdapter,
      String commitMessage, String versionTag, boolean forDeploy) throws GitHostException {
    if (gitAdapter == null) {
      throw new GitHostException("Adapter is null!");
    }
    if (repositoryName == null || repositoryName.equals("")) {
      throw new GitHostException("Repository is not set!");
    }

    // variables to be closed in the final block
    Repository applicationRepository = null;
    TreeWalk treeWalk = null;
    try {
      PersonIdent caeUser = new PersonIdent(gitAdapter.getGitUser(), gitAdapter.getGitUserMail());

      if (!repositoryName.equals(deploymentRepo)) {
        if (!existsRemoteRepository(repositoryName, gitAdapter)) {
          applicationRepository = generateNewRepository(repositoryName, gitAdapter);
        } else {
          applicationRepository = getRemoteRepository(repositoryName, gitAdapter);
          Git git = Git.wrap(applicationRepository);
          StoredConfig config = git.getRepository().getConfig();

          RemoteConfig remoteConfig = null;

          try {
            remoteConfig = new RemoteConfig(config, "Remote");
            remoteConfig.addURI(
                new URIish(gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + repositoryName + ".git"));

            remoteConfig.update(config);
            config.save();
          } catch (URISyntaxException e) {
            throw new GitHostException("Malformed url: " + e.getMessage());
          } catch (IOException e) {
            throw new GitHostException("IO exception: " + e.getMessage());
          }
        }
      } else {
        if (!existsRemoteRepository(deploymentRepo, gitAdapter)) {
          applicationRepository = generateNewRepository(deploymentRepo, gitAdapter);
        } else {

          applicationRepository = getRemoteRepository(deploymentRepo, gitAdapter);
          Git git = Git.wrap(applicationRepository);
          StoredConfig config = git.getRepository().getConfig();

          RemoteConfig remoteConfig = null;

          try {
            remoteConfig = new RemoteConfig(config, "Remote");
            remoteConfig.addURI(
                new URIish(gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + repositoryName + ".git"));

            remoteConfig.update(config);
            config.save();
          } catch (URISyntaxException e) {
            throw new GitHostException("Malformed url: " + e.getMessage());
          } catch (IOException e) {
            throw new GitHostException("IO exception: " + e.getMessage());
          }

          // Remove all files to clear the repository, this is a workaround for gitlabs
          // delayed repo deletion
          try {
            CredentialsProvider cp = new UsernamePasswordCredentialsProvider(gitAdapter.getGitUser(),
                gitAdapter.getGitPassword());

            TreeWalk rmWalk = new TreeWalk(applicationRepository);
            ObjectId lastCommitId = applicationRepository.resolve(Constants.HEAD);
            RevWalk revWalk = new RevWalk(applicationRepository);
            RevTree tree = revWalk.parseCommit(lastCommitId).getTree();
            rmWalk.addTree(tree);
            rmWalk.setRecursive(true);

            while (rmWalk.next()) {
              git.rm().addFilepattern(rmWalk.getPathString()).call();
            }

            git.commit().setMessage("Clear").call();
            git.push().setForce(false).setCredentialsProvider(cp).call();

            rmWalk.close();
            revWalk.close();
          } catch (IOException | NoWorkTreeException | GitAPIException e) {
            throw new GitHostException("Exception: " + e.getMessage());
          }
        }
      }

      if (applicationRepository == null) {
        throw new GitHostException("Repository reference is null. This should not happen");
      }

      // now we start by adding a readMe from the template repository (and thereby
      // initializing the
      // master branch, which is needed to create a "gh-pages" branch afterwards
      String readMe = null;
      BufferedImage logo = null;
      String getExtDependencies = null;
      treeWalk = getTemplateRepositoryContent(gitAdapter);
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
            readMe = readMe.replace("$Organization_Name$", gitAdapter.getGitOrganization());
            break;
          case "logo_application.png":
            logo = ImageIO.read(loader.openStream());
            break;
          case "get_ext_dependencies.sh":
            getExtDependencies = new String(loader.getBytes(), "UTF-8");

            // split external dependencies by their type
            HashMap<String, String> extDependencies = application.getExternalDependencies();
            HashMap<String, String> frontendDependencies = new HashMap<>();
            HashMap<String, String> microserviceDependencies = new HashMap<>();
            for (String key : extDependencies.keySet()) {
              if (key.startsWith("frontend:")) {
                frontendDependencies.put(key.split("frontend:")[1], extDependencies.get(key));
              } else if (key.startsWith("microservice:")) {
                microserviceDependencies.put(key.split("microservice:")[1], extDependencies.get(key));
              }
            }

            // create folder for frontend external dependencies
            getExtDependencies += System.lineSeparator() + "mkdir frontend && cd frontend" + System.lineSeparator();

            // add git clone commands for every frontend external dependency
            for (String key : frontendDependencies.keySet()) {
              // clone external dependency repository into the extra folder
              String tag = frontendDependencies.get(key);
              if (tag.equals("Latest")) {
                getExtDependencies += "git clone " + key;
              } else {
                getExtDependencies += "git clone -b " + tag + " " + key;
              }

              // go to new line
              getExtDependencies += System.lineSeparator();
            }

            // go back to /dependencies
            getExtDependencies += "cd .." + System.lineSeparator();

            // create folder for microservice external dependencies
            getExtDependencies += "mkdir microservices && cd microservices" + System.lineSeparator();

            // add git clone commands for every microservice external dependency
            for (String key : microserviceDependencies.keySet()) {
              // clone external dependency repository into the extra folder
              String tag = microserviceDependencies.get(key);
              if (tag.equals("Latest")) {
                getExtDependencies += "git clone " + key;
              } else {
                getExtDependencies += "git clone -b " + tag + " " + key;
              }

              // go to new line
              getExtDependencies += System.lineSeparator();
            }

            // go back to /dependencies
            getExtDependencies += "cd .." + System.lineSeparator();

            break;
          }
        }
        // add the two files to the repository and commit them
        applicationRepository = createTextFileInRepository(applicationRepository, "", "README.md", readMe);
        applicationRepository = createImageFileInRepository(applicationRepository, "img/", "logo.png", logo);

        applicationRepository = createTextFileInRepository(applicationRepository, "", "get_ext_dependencies.sh",
            getExtDependencies);

        RevCommit commit = Git.wrap(applicationRepository).commit().setMessage(commitMessage).setCommitter(caeUser)
            .call();

        if (versionTag != null) {
          Git.wrap(applicationRepository).tag().setObjectId(commit).setName(versionTag).call();
        }
      } catch (Exception e) {
        logger.printStackTrace(e);
        throw new GitHostException(e.getMessage());
      }

      if (!forDeploy) {
        // create "gh-pages" branch
        try {
          Git.wrap(applicationRepository).branchCreate().setName("gh-pages").call();
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHostException(e.getMessage());
        }
      }

      // fetch microservice repository contents and add them
      for (String microserviceName : application.getMicroservices().keySet()) {
        String microserviceRepositoryName = "microservice-" + microserviceName.replace(" ", "-");
        String selectedCommitSha = application.getMicroservices().get(microserviceName).getSelectedCommitSha();
        treeWalk = getRepositoryContent(microserviceRepositoryName, gitAdapter, selectedCommitSha);
        reader = treeWalk.getObjectReader();
        try {
          while (treeWalk.next()) {
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = reader.open(objectId);
            // copy the content of the repository and switch out the "old" paths

            String oldLogoAddress = gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/"
                + microserviceRepositoryName + "/blob/master/img/logo.png";
            String newLogoAddress = gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + repositoryName
                + "/blob/master/" + microserviceRepositoryName + "/img/logo.png";
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
              if (fileName.contains(".js") || fileName.contains(".txt") || fileName.contains(".css")
                  || fileName.contains(".html") || fileName.contains(".java") || fileName.contains(".bat")
                  || fileName.contains(".sh") || fileName.contains(".md") || fileName.contains(".classpath")
                  || fileName.contains(".gitignore") || fileName.contains(".project") || fileName.contains(".xml")
                  || fileName.contains(".sql") || fileName.contains(".properties")) {
                String file = new String(loader.getBytes(), "UTF-8");
                applicationRepository = createTextFileInRepository(applicationRepository,
                    microserviceRepositoryName + "/" + treeWalk.getPathString().replace(fileName, ""), fileName, file);
              }
              // image
              else if (fileName.contains(".jpg") || fileName.contains(".jpeg") || fileName.contains(".png")
                  || fileName.contains(".bmp") || fileName.contains(".gif")) {
                BufferedImage image = ImageIO.read(loader.openStream());
                applicationRepository = createImageFileInRepository(applicationRepository,
                    microserviceRepositoryName + "/" + treeWalk.getPathString().replace(fileName, ""), fileName, image);
              }
              // binary
              else {
                Object binaryObject = loader.getBytes();
                applicationRepository = createBinaryFileInRepository(applicationRepository,
                    microserviceRepositoryName + "/" + treeWalk.getPathString().replace(fileName, ""), fileName,
                    binaryObject);
              }
              break;
            }
          }
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHostException(e.getMessage());
        }
        treeWalk.close();
        // commit files
        try {
          Git.wrap(applicationRepository).commit().setMessage("Added microservice " + microserviceName)
              .setCommitter(caeUser).call();
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHostException(e.getMessage());
        }
      }

      if (!forDeploy) {
        // push (local) repository content to GitHub repository "master" branch
        try {
          pushToRemoteRepository(applicationRepository, gitAdapter, versionTag, true);
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHostException(e.getMessage());
        }

        // switch branch to "gh-pages"
        try {
          Git.wrap(applicationRepository).checkout().setName("gh-pages").call();
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHostException(e.getMessage());
        }
      }

      // fetch frontend component repository contents and add them
      for (String frontendComponentName : application.getFrontendComponents().keySet()) {
        String frontendComponentRepositoryName = "frontendComponent-" + frontendComponentName.replace(" ", "-");
        String selectedCommitSha = application.getFrontendComponents().get(frontendComponentName)
            .getSelectedCommitSha();
        treeWalk = getRepositoryContent(frontendComponentRepositoryName, gitAdapter, selectedCommitSha);
        reader = treeWalk.getObjectReader();
        try {
          while (treeWalk.next()) {
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = reader.open(objectId);
            // copy the content of the repository and switch out the "old" paths
            // TODO: URLS
            // String oldWidgetHome =
            // "http://ginkgo.informatik.rwth-aachen.de:9081/"+gitAdapter.getGitOrganization()+"/"+frontendComponentRepositoryName;
            // String newWidgetHome =
            // "http://ginkgo.informatik.rwth-aachen.de:9081/"+gitAdapter.getGitOrganization()+"/"+repositoryName+"/"+frontendComponentRepositoryName;

            String oldWidgetHome = "https://" + gitAdapter.getGitOrganization() + ".github.io/"
                + frontendComponentRepositoryName;
            String newWidgetHome = "https://" + gitAdapter.getGitOrganization() + ".github.io/" + repositoryName + "/"
                + frontendComponentRepositoryName;
            if (forDeploy) {
              // use other url for deployment, replaced later by the dockerfile
              newWidgetHome = "$WIDGET_URL$:$HTTP_PORT$" + "/" + frontendComponentRepositoryName;
            }

            String oldLogoAddress = gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/"
                + frontendComponentRepositoryName + "/blob/gh-pages/img/logo.png";
            String newLogoAddress = gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + repositoryName
                + "/blob/gh-pages/" + frontendComponentRepositoryName + "/img/logo.png";
            switch (treeWalk.getNameString()) {
            case "README.md":
              String frontendReadme = new String(loader.getBytes(), "UTF-8");
              frontendReadme = frontendReadme.replace(oldLogoAddress, newLogoAddress);
              applicationRepository = createTextFileInRepository(applicationRepository,
                  frontendComponentRepositoryName + "/", "README.md", frontendReadme);
              break;
            case "index.html":
              String widget = new String(loader.getBytes(), "UTF-8");
              widget = widget.replace(oldWidgetHome, newWidgetHome);

              applicationRepository = createTextFileInRepository(applicationRepository,
                  frontendComponentRepositoryName + "/", "index.html", widget);
              break;
            case "applicationScript.js":
              String applicationScript = new String(loader.getBytes(), "UTF-8");

              if (forDeploy) {
                FrontendComponent frontendComponent = application.getFrontendComponents().get(frontendComponentName);
                applicationScript = applicationScript.replace("$Microservice_Url$", "$STEEN_URL$:$STEEN_PORT$");
              }

              applicationRepository = createTextFileInRepository(applicationRepository,
                  frontendComponentRepositoryName + "/js/", "applicationScript.js", applicationScript);
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
              if (fileName.contains(".js") || fileName.contains(".txt") || fileName.contains(".css")
                  || fileName.contains(".html") || fileName.contains(".java") || fileName.contains(".bat")
                  || fileName.contains(".sh") || fileName.contains(".md") || fileName.contains(".classpath")
                  || fileName.contains(".gitignore") || fileName.contains(".project") || fileName.contains(".xml")
                  || fileName.contains(".sql") || fileName.contains(".properties")) {
                String file = new String(loader.getBytes(), "UTF-8");
                applicationRepository = createTextFileInRepository(applicationRepository,
                    frontendComponentRepositoryName + "/" + treeWalk.getPathString().replace(fileName, ""), fileName,
                    file);
              }
              // image
              else if (fileName.contains(".jpg") || fileName.contains(".jpeg") || fileName.contains(".png")
                  || fileName.contains(".bmp") || fileName.contains(".gif")) {
                BufferedImage image = ImageIO.read(loader.openStream());
                applicationRepository = createImageFileInRepository(applicationRepository,
                    frontendComponentRepositoryName + "/" + treeWalk.getPathString().replace(fileName, ""), fileName,
                    image);
              }
              // binary
              else {
                Object binaryObject = loader.getBytes();
                applicationRepository = createBinaryFileInRepository(applicationRepository,
                    frontendComponentRepositoryName + "/" + treeWalk.getPathString().replace(fileName, ""), fileName,
                    binaryObject);
              }
              break;
            }
          }
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHostException(e.getMessage());
        }
        treeWalk.close();
        // commit files
        try {
          Git.wrap(applicationRepository).commit().setMessage("Added frontend component " + frontendComponentName)
              .setCommitter(caeUser).call();
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHostException(e.getMessage());
        }
      }
      if (!forDeploy) {
        // push (local) repository content to repository "gh-pages" branch
        try {
          pushToRemoteRepository(applicationRepository, "gh-pages", "gh-pages", gitAdapter, versionTag, true);
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHostException(e.getMessage());
        }
      } else {
        // push (local) repository content to repository "master" branch
        try {
          pushToRemoteRepository(applicationRepository, gitAdapter, versionTag, true);
        } catch (Exception e) {
          logger.printStackTrace(e);
          throw new GitHostException(e.getMessage());
        }
      }

      // close all open resources
    } finally {
      if (applicationRepository != null)
        applicationRepository.close();
      if (treeWalk != null)
        treeWalk.close();
    }
  }

  /**
   * Get the build path of the Jenkins remote api for a queue item. If the queue
   * item is still pending, null is returned.
   * 
   * @param queueItem  The path of the queue item
   * @param jenkinsUrl The base path of Jenkins
   * @return The build path for the queue item, or Null if the is still
   *         pending/waiting
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
      BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      for (String line; (line = reader.readLine()) != null;) {
        message += line + "\n";
      }
      reader.close();
      JSONParser parser = new JSONParser();
      JSONObject result = (JSONObject) parser.parse(message);

      if (result.containsKey("executable")) {
        JSONObject executeable = (JSONObject) result.get("executable");
        String path = new URI((String) executeable.get("url")).getPath();
        if (path.startsWith("jenkins/") || path.startsWith("/jenkins")) {
          path = path.substring("jenkins/".length(), path.length());
        }
        return path;
      } else {
        return "pending";
      }
    }

  }

  /**
   * Get the job console text of a build of a queue item. When the item is still
   * pending, the string "Pending" is returned. This consumes an URL provided by
   * jenkins.
   * 
   * @param queueItem  The path of the queue item
   * @param jenkinsUrl The base path of Jenkins
   * @return The console text of a build of a queue item or "Pending" if the item
   *         is still pending/waiting for its execution
   */

  public static String deployStatus(String queueItem, String jenkinsUrl) {
    try {
      String buildPath = getBuildPath(queueItem, jenkinsUrl);
      if (buildPath == null) {
        return "Done";
      } else if (buildPath.equals("pending")) {
        return "Pending";
      } else {

        URL url = new URL(jenkinsUrl + buildPath + "consoleText");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setUseCaches(false);

        String message = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
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
   * Start a job for the deployment of an application by making a HTTP request to
   * the URL jenkins provides us with. They have to following format:
   * PlaceWhereJenkinsIsHosted/job/[jobName]/build?token=[token] with token being
   * the secret that has been set in Jenkins
   * 
   * @param jenkinsUrl The base path of Jenkins
   * @param jobToken   The token to start the job
   * @param jobName    The name of the job to start
   * @return The path of the queue item of the started job
   */

  public static String deployApplication(String jenkinsUrl, String jobToken, String jobName, String body) {

    try {

      Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "Starting Jenkin job: " + jobName);

      URL url = new URL(jenkinsUrl + "/job/" + jobName + "/buildWithParameters?token=" + jobToken + "&BODY=" + body);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setDoOutput(true);
      connection.setUseCaches(false);

      // forward (in case of) error
      if (connection.getResponseCode() != 201) {
        String message = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        for (String line; (line = reader.readLine()) != null;) {
          message += line;
        }
        reader.close();
        throw new Exception(message);
      } else {
        Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "Job started!");
        URI uri = new URI(connection.getHeaderField("Location"));
        String path = uri.getPath();
        if (path.startsWith("jenkins/") || path.startsWith("/jenkins")) {
          path = path.substring("jenkins/".length(), path.length());
        }
        return path;
      }
    } catch (Exception e) {
      logger.printStackTrace(e);
      return "Error";
    }

  }
}
