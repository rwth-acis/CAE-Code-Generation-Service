package i5.las2peer.services.codeGenerationService;

import java.io.Serializable;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.api.Service;
import i5.las2peer.services.codeGenerationService.generators.ApplicationGenerator;
import i5.las2peer.services.codeGenerationService.generators.FrontendComponentGenerator;
import i5.las2peer.services.codeGenerationService.generators.MicroserviceGenerator;
import i5.las2peer.services.codeGenerationService.generators.exception.GitHubException;
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
  private String gitHubOrganization;
  private String templateRepository;
  private String gitHubUserMail;

  public CodeGenerationService() {
    // read and set properties-file values
    setFieldValues();
  }


  /**
   * 
   * Creates a new GitHub repository with the source code according to the passed on model.
   * 
   * @param serializedModel a {@link i5.cae.simpleModel.SimpleModel} that contains the model
   * 
   * @return a string containing either the message "done" or, in case of an error, the error
   *         message
   * 
   */
  public String createFromModel(Serializable serializedModel) {
    SimpleModel model = (SimpleModel) serializedModel;
    logMessage("CreateFromModel: Received model with name " + model.getName());
    // find out what type of model we got (microservice, frontend-component or application)
    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        try {
          switch (type) {
            case "microservice":
              logMessage("Creating microservice model now..");
              Microservice microservice = new Microservice(model);
              logMessage("Creating microservice source code now..");
              MicroserviceGenerator.createSourceCode(microservice, this.templateRepository,
                  this.gitHubOrganization, this.gitHubUser, this.gitHubUserMail,
                  this.gitHubPassword);
              logMessage("CreateFromModel: Created!");
              return "done";
            case "frontend-component":
              logMessage("Creating frontend component model now..");
              FrontendComponent frontendComponent = new FrontendComponent(model);
              logMessage("Creating frontend component source code now..");
              FrontendComponentGenerator.createSourceCode(frontendComponent,
                  this.templateRepository, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubUserMail, this.gitHubPassword);;
              logMessage("CreateFromModel: Created!");
              return "done";
            case "application":
              logMessage("Creating application model now..");
              Application application = new Application(model);
              logMessage("Creating application source code now..");
              ApplicationGenerator.createSourceCode(application, this.templateRepository,
                  this.gitHubOrganization, this.gitHubUser, this.gitHubUserMail,
                  this.gitHubPassword);
              logMessage("CreateFromModel: Created!");
              return "done";
            default:
              return "Error: Model has to have an attribute 'type' that is either "
                  + "'microservice', 'frontend-component' or 'application'!";
          }
        } catch (ModelParseException e1) {
          logError("CreateFromModel: Model Parsing exception: " + e1.getMessage());
          e1.printStackTrace();
          return "Error: Parsing model failed with " + e1.getMessage();
        } catch (GitHubException e2) {
          logError("CreateFromModel: GitHub access exception: " + e2.getMessage());
          e2.printStackTrace();
          return "Error: Generating code failed because of failing GitHub access: "
              + e2.getMessage();
        }
      }
    }
    return "Error!";

  }

}
