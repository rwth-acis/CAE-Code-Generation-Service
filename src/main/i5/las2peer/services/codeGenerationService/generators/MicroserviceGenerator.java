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

  /**
   * 
   * Currently more of a dirty test & hack thing, needs refactoring!
   * 
   * @param microservice
   * @param client
   * @param gitHubOrganization
   * @param templateRepositoryName
   * 
   * @throws GitHubException
   * 
   */
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
        RepositoryContents content = contents.get(i);
        // get microservice folder
        if (content.getName().equals("backend")) {
          System.out.println(content.getPath());
          List<RepositoryContents> microserviceContents =
              contentsService.getContents(templateRepository, content.getPath());
          for (int j = 0; j < microserviceContents.size(); j++) {
            RepositoryContents microserviceContent = microserviceContents.get(j);
            if (microserviceContent.getName().equals(".project")) {
              String contentSha = microserviceContent.getSha();
              Blob file = dataService.getBlob(templateRepository, contentSha);
              byte[] decodedFile = EncodingUtils.fromBase64(file.getContent());
              String decodedFileUTF8 = new String(decodedFile, "UTF-8");
              decodedFileUTF8 =
                  decodedFileUTF8.replace("$Microservice_Name$", microservice.getName());
              System.out.println(decodedFileUTF8);
            }
          }
        }
        // // don't print out directories..
        // if (content.getSize() != 0) {
        // String contentSha = content.getSha();
        // Blob file = dataService.getBlob(templateRepository, contentSha);
        // byte[] decodedFile = EncodingUtils.fromBase64(file.getContent());
        // String decodedFileUTF8 = new String(decodedFile, "UTF-8");
        // System.out.println(decodedFileUTF8);
        // }

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
