package i5.las2peer.services.codeGenerationService.generators;

import java.io.IOException;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

public class MicroserviceGenerator {

  public static void createSourceCode(Microservice microservice, GitHubClient client,
      String gitHubOrganization, String templateRepository) {
    RepositoryService repService = new RepositoryService(client);
    Repository repository = new Repository();
    repository.setName("microservice " + microservice.getName());
    try {
      repService.createRepository(gitHubOrganization, repository);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
