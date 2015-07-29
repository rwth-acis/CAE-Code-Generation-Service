package i5.las2peer.services.codeGenerationService.generators;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import i5.las2peer.services.codeGenerationService.generators.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;

/**
 * 
 * Generates frontend component source code from passed on
 * {@link i5.las2peer.services.codeGenerationService.models.frontendComponent} models.
 * 
 */
public class FrontendComponentGenerator extends Generator {

  /**
   * 
   * Creates source code from a CAE frontend component model and pushes it to GitHub.
   * 
   * @param frontendComponent the frontend component model
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
  public static void createSourceCode(FrontendComponent frontendComponent,
      String templateRepositoryName, String gitHubOrganization, String gitHubUser,
      String gitHubUserMail, String gitHubPassword) throws GitHubException {

    // variables to be closed in the final block
    Repository frontendComponentRepository = null;
    TreeWalk treeWalk = null;

    // helper variables
    // TODO
    // variables holding content to be modified and added to repository later
    // TODO
    String widget = null;
    try {
      PersonIdent caeUser = new PersonIdent(gitHubUser, gitHubUserMail);
      String repositoryName = "frontendComponent-" + frontendComponent.getName().replace(" ", "-");
      frontendComponentRepository =
          generateNewRepository(repositoryName, gitHubOrganization, gitHubUser, gitHubPassword);
      try {
        // now load the TreeWalk containing the template repository content
        treeWalk = getTemplateRepositoryContent(templateRepositoryName, gitHubOrganization);
        treeWalk.setFilter(PathFilter.create("frontend/"));
        ObjectReader reader = treeWalk.getObjectReader();
        // walk through the tree and retrieve the needed templates
        while (treeWalk.next()) {
          ObjectId objectId = treeWalk.getObjectId(0);
          ObjectLoader loader = reader.open(objectId);

          switch (treeWalk.getNameString()) {
            case "widget.xml":
              widget = new String(loader.getBytes(), "UTF-8");
              break;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }
      // add files to new repository
      frontendComponentRepository =
          createTextFileInRepository(frontendComponentRepository, "", "widget.xml", widget);

      // commit files
      try {
        // TODO
        Git.wrap(frontendComponentRepository).commit().setMessage("Generated frontend component")
            .setCommitter(caeUser).call();
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // push (local) repository content to GitHub repository
      try {
        pushToRemoteRepository(frontendComponentRepository, gitHubUser, gitHubPassword);
      } catch (Exception e) {
        e.printStackTrace();
        throw new GitHubException(e.getMessage());
      }

      // close all open resources
    } finally {
      frontendComponentRepository.close();
      treeWalk.close();
    }

  }

}
