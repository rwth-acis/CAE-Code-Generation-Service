package i5.las2peer.services.codeGenerationService.generators;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.simple.JSONObject;

import i5.las2peer.services.codeGenerationService.generators.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

public class MicroserviceGenerator {

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
   * @throws GitHubException
   * 
   */
  @SuppressWarnings("unchecked")
  public static void createSourceCode(Microservice microservice, String templateRepositoryName,
      String gitHubOrganization, String gitHubUser, String gitHubUserMail, String gitHubPassword)
          throws GitHubException {
    String templateRepositoryAddress =
        "https://github.com/" + gitHubOrganization + "/" + templateRepositoryName + ".git";
    String projectFileContent = null;
    Git templateRepositoryGit = null;
    Git microserviceRepositoryGit = null;
    CredentialsProvider credentialsProvider =
        new UsernamePasswordCredentialsProvider(gitHubUser, gitHubPassword);
    // prepare a new folder for the new microservice repository
    File localPath = null;
    try {
      localPath = File.createTempFile(microservice.getName(), "");
      localPath.delete();
    } catch (IOException e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }
    try {
      microserviceRepositoryGit = Git.init().setDirectory(localPath).call();
      StoredConfig config = microserviceRepositoryGit.getRepository().getConfig();
      RemoteConfig remoteConfig = new RemoteConfig(config, "GitHub");
      remoteConfig.addURI(new URIish("https://github.com/" + gitHubOrganization + "/"
          + "microservice-" + microservice.getName().replace(" ", "-")));
      remoteConfig.update(config);
      config.save();

      System.out.println(
          "Created new repository: " + microserviceRepositoryGit.getRepository().getDirectory());
    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }

    // prepare a new folder for the template repository (to be cloned)
    try {
      localPath = File.createTempFile("TemplateRepository", "");
    } catch (IOException e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }
    localPath.delete();

    // then clone
    System.out.println("Cloning from " + templateRepositoryAddress + " to " + localPath);
    try {
      templateRepositoryGit =
          Git.cloneRepository().setURI(templateRepositoryAddress).setDirectory(localPath).call();
    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }

    // get the content of the repository
    RevWalk revWalk = null;
    TreeWalk treeWalk = null;
    try {
      Repository templateRepository = templateRepositoryGit.getRepository();
      System.out.println("Cloned repository to: " + templateRepository.getDirectory());

      ObjectId lastCommitId = templateRepository.resolve(Constants.HEAD);

      treeWalk = new TreeWalk(templateRepository);
      revWalk = new RevWalk(templateRepository);

      RevTree tree = revWalk.parseCommit(lastCommitId).getTree();
      treeWalk.addTree(tree);
      treeWalk.setRecursive(true);
      treeWalk.setFilter(PathFilter.create("backend/"));

      // walk through the tree and retrieve the needed templates TODO
      while (treeWalk.next()) {
        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = templateRepository.open(objectId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        loader.copyTo(out);

        // only use .project file for now TODO
        if (treeWalk.getNameString().equals(".project")) {
          projectFileContent = new String(loader.getBytes(), "UTF-8");
          projectFileContent =
              projectFileContent.replace("$Microservice_Name$", microservice.getName());
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    } finally

    {
      revWalk.close();
      treeWalk.close();
      templateRepositoryGit.close();
    }

    // add files to new repository
    Repository microserviceRepository = microserviceRepositoryGit.getRepository();
    File projectFile = new File(microserviceRepository.getDirectory().getParent(), ".project");
    FileWriter fileWriter;

    try {
      projectFile.createNewFile();
      fileWriter = new FileWriter(projectFile.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fileWriter);
      bw.write(projectFileContent);
      bw.close();

    } catch (IOException e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }
    System.out.println("Adding .project file to microservice repository");
    try {
      // run the add and commit files
      microserviceRepositoryGit.add().addFilepattern(".").call();
      PersonIdent caeUser = new PersonIdent(gitHubUser, gitHubUserMail);
      microserviceRepositoryGit.commit().setMessage("Added project file").setCommitter(caeUser)
          .call();
    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }

    System.out.println("Committed file " + projectFile + " to repository at "
        + microserviceRepository.getDirectory());

    // now create empty GitHub repository with an HTTP request, using the GitHub API because JGit
    // does not support direct GitHub repository creation
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("name", "microservice-" + microservice.getName().replace(" ", "-"));
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

    // finally push (local) repository content
    try {
      RefSpec spec = new RefSpec("refs/heads/master:refs/heads/master");
      microserviceRepositoryGit.push().setRemote("GitHub")
          .setCredentialsProvider(credentialsProvider).setRefSpecs(spec).call();
    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }

  }

}
