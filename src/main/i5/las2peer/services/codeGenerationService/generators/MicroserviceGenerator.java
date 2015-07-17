package i5.las2peer.services.codeGenerationService.generators;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.util.EncodingUtils;

import i5.las2peer.services.codeGenerationService.models.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

public class MicroserviceGenerator {

  public static void createSourceCode(Microservice microservice, GitHubClient client,
      String gitHubOrganization, String templateRepositoryName) throws GitHubException {
    // initialize repository service, contents service and data service
    RepositoryService repService = new RepositoryService(client);
    ContentsService contentsService = new ContentsService(client);
    DataService dataService = new DataService(client);
    // retrieve template repository
    try {
      Repository templateRepository =
          repService.getRepository(gitHubOrganization, templateRepositoryName);
      List<RepositoryContents> contents = contentsService.getContents(templateRepository);
      for (int i = 0; i < contents.size(); i++) {
        System.out.println(contents.get(i).getName());
        System.out.println(contents.get(i).getType());
        System.out.println(contents.get(i).getSize());
        // don't print out directories..
        if (contents.get(i).getSize() != 0) {
          String contentSha = contents.get(i).getSha();
          Blob content = dataService.getBlob(templateRepository, contentSha);
          byte[] decodedContent = EncodingUtils.fromBase64(content.getContent());
          String decodedContentUTF8 = new String(decodedContent, "UTF-8");
          System.out.println(decodedContentUTF8);
        }

      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }

    // put code into new repository
    Repository repository = new Repository();
    repository.setName("microservice-" + microservice.getName());
    try {
      repService.createRepository(gitHubOrganization, repository);
    } catch (IOException e) {
      e.printStackTrace();
      throw new GitHubException(e.getMessage());
    }
  }

}
