package i5.las2peer.services.codeGenerationService.generators;

import java.io.IOException;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import i5.las2peer.services.codeGenerationService.models.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

public class MicroserviceGenerator {

  public static void createSourceCode(Microservice microservice, GitHubClient client,
      String gitHubOrganization, String templateRepositoryName) throws GitHubException {
    RepositoryService repService = new RepositoryService(client);
    try {
      Repository templateRepository =
          repService.getRepository(gitHubOrganization, templateRepositoryName);
      System.out.println(templateRepository.getName());
    } catch (IOException e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }
    Repository repository = new Repository();
    repository.setName("microservice " + microservice.getName());
    try {
      repService.createRepository(gitHubOrganization, repository);
    } catch (IOException e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }
  }

}
