package i5.las2peer.services.codeGenerationService;

import java.io.IOException;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.api.Service;

/**
 * CAE Code Generation Service
 * 
 * A LAS2peer service used for generating code from send models. Part of the CAE.
 * 
 */
public class CodeGenerationService extends Service {

  public String gitHubUser;
  public String gitHubPassword;

  public CodeGenerationService() {
    // read and set properties values
    setFieldValues();
    GitHubClient client = new GitHubClient();
    client.setCredentials(gitHubUser, gitHubPassword);
    RepositoryService service = new RepositoryService();
    try {
      // prints out the name of the users repositories
      for (Repository repo : service.getRepositories(gitHubUser))
        System.out.println(repo.getName());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * @param model
   * 
   * @return a string
   * 
   */
  public String createSource(SimpleModel model) {
    return null;
  }

}
