package i5.las2peer.services.codeGenerationService;

import java.io.Serializable;

import org.eclipse.egit.github.core.client.GitHubClient;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.api.Service;
import i5.las2peer.services.codeGenerationService.generators.ApplicationGenerator;
import i5.las2peer.services.codeGenerationService.generators.FrontendComponentGenerator;
import i5.las2peer.services.codeGenerationService.generators.MicroserviceGenerator;
import i5.las2peer.services.codeGenerationService.models.application.Application;
import i5.las2peer.services.codeGenerationService.models.exception.ModelParseException;
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
    // write as file for (later) testing purposes
    // try {
    // OutputStream file = new FileOutputStream("testModels/" + model.getName() + ".model");
    // OutputStream buffer = new BufferedOutputStream(file);
    // ObjectOutput output = new ObjectOutputStream(buffer);
    // output.writeObject(model);
    // output.close();
    // } catch (IOException ex) {
    // }
    logMessage("CreateFromModel: Received model with name " + model.getName());
    // find out what type of model we got (microservice, frontend component or application)
    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        try {
          switch (type) {
            case "microservice":
              logMessage("Creating microservice model now..");
              Microservice microservice = new Microservice(model);
              logMessage("Creating microservice source code now..");
              MicroserviceGenerator.createSourceCode(microservice, this.client);
              logMessage("Created!");
              return "done";
            case "frontend-component":
              logMessage("Creating frontend component model now..");
              FrontendComponent frontendComponent = new FrontendComponent(model);
              logMessage("Creating frontend component source code now..");
              FrontendComponentGenerator.createSourceCode(frontendComponent, this.client);
              logMessage("Created!");
              return "done";
            case "application":
              logMessage("Creating application model now..");
              Application application = new Application(model);
              logMessage("Creating application source code now..");
              ApplicationGenerator.createSourceCode(application, this.client);
              logMessage("Created!");
              return "done";
            default:
              return "Error: Model has to have an attribute 'type' that is either "
                  + "'microservice', 'frontend-component' or 'application'!";
          }
        } catch (ModelParseException e) {
          logError("Model Parsing Exception: " + e.getMessage());
          e.printStackTrace();
          return "Error: Parsing model failed with " + e.getMessage();
        }
      }
    }
    return "Error!";


  }

}
