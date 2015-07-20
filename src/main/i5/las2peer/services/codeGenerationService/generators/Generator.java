package i5.las2peer.services.codeGenerationService.generators;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.json.simple.JSONObject;

import i5.las2peer.services.codeGenerationService.generators.exception.GitHubException;

public abstract class Generator {

  /**
   * 
   * Generates a new (local) repository to add files to. Also creates a (remote) GitHub repository
   * with the same name and adds it to the (local) repository's configuration to be later used as
   * its remote entry to push to.
   * 
   * @param name the name of the repository to be created
   * @param gitHubOrganization the organization that is used in the CAE
   * @param gitHubUser the CAE user
   * @param gitHubPassword the password of the CAE user
   * 
   * @return a @link{org.eclipse.jgit.lib.Repository}
   * 
   * @throws GitHubException if anything goes wrong during this creation process
   * 
   */
  @SuppressWarnings("unchecked")
  public static Repository generateNewRepository(String name, String gitHubOrganization,
      String gitHubUser, String gitHubPassword) throws GitHubException {

    Git git = null;
    File localPath = null;

    // prepare a new folder for the new repository
    try {
      localPath = File.createTempFile(name, "");
      localPath.delete();
    } catch (IOException e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }

    // add a remote configuration (origin) to the newly created repository
    try {
      git = Git.init().setDirectory(localPath).call();
      StoredConfig config = git.getRepository().getConfig();
      RemoteConfig remoteConfig = new RemoteConfig(config, "GitHub");
      remoteConfig.addURI(new URIish("https://github.com/" + gitHubOrganization + "/" + name));
      remoteConfig.update(config);
      config.save();
    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }

    // now create empty GitHub repository with an HTTP request, using the GitHub API directly
    // because jGit does not support direct GitHub repository creation..
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("name", name);
      String body = JSONObject.toJSONString(jsonObject);

      String authString = gitHubUser + ":" + gitHubPassword;
      byte[] authEncBytes = Base64.getEncoder().encode(authString.getBytes());
      String authStringEnc = new String(authEncBytes);

      URL url = new URL("https://api.github.com/orgs/" + gitHubOrganization + "/repos");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setUseCaches(false);
      connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
      connection.setRequestProperty("Content-Type", "application/vnd.github.v3+json");
      connection.setRequestProperty("Content-Length", String.valueOf(body.length()));
      connection.setRequestProperty("Authorization", "Basic " + authStringEnc);

      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
      writer.write(body);
      writer.flush();
      writer.close();

      // forward (in case of) error
      if (connection.getResponseCode() != 201) {
        String message = "Error creating repository: ";
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        for (String line; (line = reader.readLine()) != null;) {
          message += line;
        }
        reader.close();
        throw new GitHubException(message);
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }
    return git.getRepository();
  }

  /**
   * 
   * Clones the template repository from GitHub to the local machine and returns
   * a @link{org.eclipse.jgit.treewalk.TreeWalk} that can be used to retrieve the repository's
   * content. Repository is used "read-only" here.
   * 
   * @param templateRepositoryName the name of the template repository
   * @param gitHubOrganization the organization that is used in the CAE
   * 
   * @return a @link{org.eclipse.jgit.treewalk.TreeWalk}
   * 
   * @throws GitHubException if anything goes wrong during retrieving the repository's content
   * 
   */
  public static TreeWalk getTemplateRepositoryContent(String templateRepositoryName,
      String gitHubOrganization) throws GitHubException {
    String templateRepositoryAddress =
        "https://github.com/" + gitHubOrganization + "/" + templateRepositoryName + ".git";

    Repository templateRepository = null;
    // prepare a new folder for the template repository (to be cloned)
    File localPath = null;

    try {
      localPath = File.createTempFile("TemplateRepository", "");
    } catch (IOException e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }
    localPath.delete();

    // then clone
    try {
      templateRepository = Git.cloneRepository().setURI(templateRepositoryAddress)
          .setDirectory(localPath).call().getRepository();
    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }

    // get the content of the repository
    RevWalk revWalk = null;
    TreeWalk treeWalk = null;
    try {
      ObjectId lastCommitId = templateRepository.resolve(Constants.HEAD);

      treeWalk = new TreeWalk(templateRepository);
      revWalk = new RevWalk(templateRepository);

      RevTree tree = revWalk.parseCommit(lastCommitId).getTree();
      treeWalk.addTree(tree);
      treeWalk.setRecursive(true);
    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    } finally {
      templateRepository.close();
      revWalk.close();
    }
    return treeWalk;
  }

  /**
   * 
   * Adds a file to the repository. Beware of side effects, due to adding all files in main folder
   * to staged area currently.
   * 
   * @param repository the repository the file should be added to
   * @param relativePath the relative path the file should reside at; without first separator
   * @param fileName the file name
   * @param content the content the file should have
   * 
   * @return a @link{org.eclipse.jgit.lib.Repository}
   * 
   * @throws GitHubException if anything goes wrong during the creation of the file
   * 
   */
  public static Repository createFileInRepository(Repository repository, String relativePath,
      String fileName, String content) throws GitHubException {

    File dirs = new File(repository.getDirectory().getParent() + "/" + relativePath);
    dirs.mkdirs();

    try {
      OutputStream file = new FileOutputStream(
          repository.getDirectory().getParent() + "/" + relativePath + fileName);
      OutputStream buffer = new BufferedOutputStream(file);
      PrintStream printStream = new PrintStream(buffer);
      printStream.print(content);
      printStream.close();
    } catch (IOException e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }

    // stage file
    try {
      Git.wrap(repository).add().addFilepattern(".").call();
    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }
    return repository;

  }
}
