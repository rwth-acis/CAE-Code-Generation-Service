package i5.las2peer.services.codeGenerationService.generators;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import i5.las2peer.services.codeGenerationService.generators.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

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
    // some local variables
    String microserviceRepositoryName = "microservice-" + microservice.getName().replace(" ", "-");
    String projectFileContent = null;
    CredentialsProvider credentialsProvider =
        new UsernamePasswordCredentialsProvider(gitHubUser, gitHubPassword);
    PersonIdent caeUser = new PersonIdent(gitHubUser, gitHubUserMail);

    Repository microserviceRepository = generateNewRepository(microserviceRepositoryName,
        gitHubOrganization, gitHubUser, gitHubPassword);

    // now load the TreeWalk containing the template repository content
    TreeWalk treeWalk = getTemplateRepositoryContent(templateRepositoryName, gitHubOrganization);
    try {
      treeWalk.setFilter(PathFilter.create("backend/"));
      ObjectReader reader = treeWalk.getObjectReader();
      // walk through the tree and retrieve the needed templates
      while (treeWalk.next()) {
        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = reader.open(objectId);
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
    } finally {
      treeWalk.close();
    }

    // add files to new repository
    microserviceRepository =
        createFileInRepository(microserviceRepository, "", ".project", projectFileContent);
    microserviceRepository = createFileInRepository(microserviceRepository,
        "myCoolFolder/myEvenCoolerFolder/", "someOtherCoolFile.txt", "blabla");
    // add files to new repository
    microserviceRepository = createFileInRepository(microserviceRepository, "myCoolFolder/",
        "coolestFile.txt", "blabla");
    try {
      // run add command, then commit those files
      // Git.wrap(microserviceRepository).add().addFilepattern(".").call();
      Git.wrap(microserviceRepository).commit().setMessage("Added project file")
          .setCommitter(caeUser).call();
    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }

    // finally push (local) repository content to GitHub repository
    // (the "remote" parameter name is set in the generateNewRepository method)
    try {
      RefSpec spec = new RefSpec("refs/heads/master:refs/heads/master");
      Git.wrap(microserviceRepository).push().setRemote("GitHub")
          .setCredentialsProvider(credentialsProvider).setRefSpecs(spec).call();
    } catch (Exception e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    } finally {
      microserviceRepository.close();
    }
  }

}
