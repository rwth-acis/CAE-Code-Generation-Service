package i5.las2peer.services.codeGenerationService.generators;

import java.io.IOException;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

public class MicroserviceGenerator {

  public static void createSourceCode(Microservice microservice, GitHubClient client) {
    RepositoryService service = new RepositoryService();
    try {
      // prints out the name of the user's repositories
      for (Repository repo : service.getRepositories(client.getUser()))
        System.out.println(repo.getName());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
