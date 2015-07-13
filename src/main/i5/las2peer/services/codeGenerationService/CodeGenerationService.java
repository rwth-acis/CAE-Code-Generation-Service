package i5.las2peer.services.codeGenerationService;

import java.io.Serializable;

import org.eclipse.egit.github.core.client.GitHubClient;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.api.Service;
import i5.las2peer.services.codeGenerationService.generators.ApplicationGenerator;
import i5.las2peer.services.codeGenerationService.generators.FrontendComponentGenerator;
import i5.las2peer.services.codeGenerationService.generators.MicroserviceGenerator;
import i5.las2peer.services.codeGenerationService.models.application.Application;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

/**
 * CAE Code Generation Service
 * 
 * A LAS2peer service used for generating code from send models. Part of the CAE.
 * 
 */
public class CodeGenerationService extends Service {

  private String gitHubUser;
  private String gitHubPassword;
  public GitHubClient client;

  public CodeGenerationService() {
    // read and set properties values
    setFieldValues();
    this.client = new GitHubClient();
    this.client.setCredentials(gitHubUser, gitHubPassword);
  }



  /**
   * 
   * Creates a new GitHub repository with the source code according to the passed on model.
   * 
   * @param serializedModel a @link{i5.cae.simpleModel.SimpleModel} that contains the model
   * 
   * @return a string containing either the message "done" or, in case of an error, the error
   *         message
   * 
   */
  public String createFromModel(Serializable serializedModel) {
    SimpleModel model = (SimpleModel) serializedModel;
    // // write as file for (later) testing purposes
    // try {
    // OutputStream file = new FileOutputStream("testModels/" + model.getName() + ".model");
    // OutputStream buffer = new BufferedOutputStream(file);
    // ObjectOutput output = new ObjectOutputStream(buffer);
    // output.writeObject(model);
    // output.close();
    // } catch (IOException ex) {
    // }
    logMessage("Received model with name " + model.getName());
    // find out what type of model we got (microservice, frontend component or application)
    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        switch (type) {
          case "microservice":
            Microservice microservice = new Microservice(model);
            MicroserviceGenerator.createSourceCode(microservice, this.client);
            return "done";
          case "frontend-component":
            FrontendComponent frontendComponent = new FrontendComponent(model);
            FrontendComponentGenerator.createSourceCode(frontendComponent, this.client);
            return "done";
          case "application":
            Application application = new Application(model);
            ApplicationGenerator.createSourceCode(application, this.client);
            return "done";
        }
      }
    }

    return "Error!";
  }

}
