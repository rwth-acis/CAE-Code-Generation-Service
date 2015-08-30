package i5.las2peer.services.codeGenerationService;

import java.io.Serializable;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.api.Service;
import i5.las2peer.services.codeGenerationService.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
import i5.las2peer.services.codeGenerationService.generators.ApplicationGenerator;
import i5.las2peer.services.codeGenerationService.generators.FrontendComponentGenerator;
import i5.las2peer.services.codeGenerationService.generators.Generator;
import i5.las2peer.services.codeGenerationService.generators.MicroserviceGenerator;
import i5.las2peer.services.codeGenerationService.models.application.Application;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;

/**
 * 
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
   * @param serializedModel a {@link i5.cae.simpleModel.SimpleModel} that contains the model, or in
   *        case of an application model also the model components as additional models
   * 
   * @return a string containing either the message "done" or, in case of an error, the error
   *         message
   * 
   */
  public String createFromModel(Serializable... serializedModel) {

    SimpleModel model = (SimpleModel) serializedModel[0];
    logMessage("createFromModel: Received model with name " + model.getName());

    // TESTING: write as file
    // try {
    // OutputStream file = new FileOutputStream("testModels/" + model.getName() + ".model");
    // OutputStream buffer = new BufferedOutputStream(file);
    // ObjectOutput output = new ObjectOutputStream(buffer);
    // output.writeObject(model);
    // output.close();
    // } catch (IOException ex) {
    // }

    // find out what type of model we got (microservice, frontend-component or application)
    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        try {
          switch (type) {
            case "microservice":
              logMessage("createFromModel: Creating microservice model now..");
              Microservice microservice = new Microservice(model);
              logMessage("createFromModel: Creating microservice source code now..");
              MicroserviceGenerator.createSourceCode(microservice, this.templateRepository,
                  this.gitHubOrganization, this.gitHubUser, this.gitHubUserMail,
                  this.gitHubPassword);
              logMessage("createFromModel: Created!");
              return "done";
            case "frontend-component":
              logMessage("createFromModel: Creating frontend component model now..");
              FrontendComponent frontendComponent = new FrontendComponent(model);
              logMessage("createFromModel: Creating frontend component source code now..");
              FrontendComponentGenerator.createSourceCode(frontendComponent,
                  this.templateRepository, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubUserMail, this.gitHubPassword);;
              logMessage("createFromModel: Created!");
              return "done";
            case "application":
              logMessage("createFromModel: Creating application model now..");
              Application application = new Application(serializedModel);
              logMessage("createFromModel: Creating application source code now..");
              ApplicationGenerator.createSourceCode(application, this.templateRepository,
                  this.gitHubOrganization, this.gitHubUser, this.gitHubUserMail,
                  this.gitHubPassword);
              logMessage("createFromModel: Created!");
              return "done";
            default:
              return "Error: Model has to have an attribute 'type' that is either "
                  + "'microservice', 'frontend-component' or 'application'!";
          }
        } catch (ModelParseException e1) {
          logError("createFromModel: Model parsing exception: " + e1.getMessage());
          e1.printStackTrace();
          return "Error: Parsing model failed with " + e1.getMessage();
        } catch (GitHubException e2) {
          logError("createFromModel: GitHub access exception: " + e2.getMessage());
          e2.printStackTrace();
          return "Error: Generating code failed because of failing GitHub access: "
              + e2.getMessage();
        }
      }
    }
    return "Model has no attribute 'type'!";
  }


  /**
   * 
   * Deletes a model's repository from GitHub. Please note, that in this case, it is not checked for
   * correctness of the model, only the name and type are extracted and then the repository gets
   * deleted according to it.
   * 
   * @param serializedModel a {@link i5.cae.simpleModel.SimpleModel} that contains the model
   * 
   * @return a string containing either the message "done" or, in case of an error, the error
   *         message
   * 
   */
  public String deleteRepositoryOfModel(Serializable serializedModel) {
    SimpleModel model = (SimpleModel) serializedModel;
    String modelName = model.getName();
    logMessage("deleteRepositoryOfModel: Received model with name " + modelName);
    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        try {
          switch (type) {
            case "microservice":
              logMessage("deleteRepositoryOfModel: Deleting microservice repository now..");
              modelName = "microservice-" + modelName.replace(" ", "-");
              Generator.deleteRemoteRepository(modelName, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubPassword);
              logMessage("deleteRepositoryOfModel: Deleted!");
              return "done";
            case "frontend-component":
              logMessage("deleteRepositoryOfModel: Deleting frontend-component repository now..");
              modelName = "frontendComponent-" + modelName.replace(" ", "-");
              Generator.deleteRemoteRepository(modelName, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubPassword);
              logMessage("deleteRepositoryOfModel: Deleted!");
              return "done";
            case "application":
              logMessage("deleteRepositoryOfModel: Deleting application repository now..");
              modelName = "application-" + modelName.replace(" ", "-");
              Generator.deleteRemoteRepository(modelName, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubPassword);
              logMessage("deleteRepositoryOfModel: Deleted!");
              return "done";
            default:
              return "Error: Model has to have an attribute 'type' that is either "
                  + "'microservice', 'frontend-component' or 'application'!";
          }
        } catch (GitHubException e) {
          logError("deleteRepositoryOfModel: GitHub access exception: " + e.getMessage());
          e.printStackTrace();
          return "Error: Deleting repository failed because of failing GitHub access: "
              + e.getMessage();
        }
      }
    }
    return "Model has no attribute 'type'!";
  }


  /**
   * 
   * "Updates" an already existing repository with the new given model. Please note that the current
   * implementation does not really perform an update, but just deletes the old repository and
   * replaces it with the contents of the new model.
   * 
   * @param serializedModel a {@link i5.cae.simpleModel.SimpleModel} that contains the model, or in
   *        case of an application model also the model components as additional models
   * 
   * @return a string containing either the message "done" or, in case of an error, the error
   *         message
   * 
   */
  public String updateRepositoryOfModel(Serializable... serializedModel) {
    SimpleModel model = (SimpleModel) serializedModel[0];
    String modelName = model.getName();
    logMessage("updateRepositoryOfModel: Received model with name " + modelName);
    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        try {
          switch (type) {
            case "microservice":
              logMessage("updateRepositoryOfModel: Checking microservice model now..");
              // check first if model can be constructed
              // (in case of an invalid model, keep the old repository)
              new Microservice(model);
              logMessage("updateRepositoryOfModel: Calling delete (old) repository method now..");
              String deleteReturnMessage = deleteRepositoryOfModel(serializedModel[0]);
              if (!deleteReturnMessage.equals("done")) {
                return deleteReturnMessage; // error happened
              }
              logMessage("updateRepositoryOfModel: Calling createFromModel now..");
              return createFromModel(serializedModel);
            case "frontend-component":
              logMessage("updateRepositoryOfModel: Checking frontend-component model now..");
              // check first if model can be constructed
              // (in case of an invalid model, keep the old repository)
              new FrontendComponent(model);
              logMessage("updateRepositoryOfModel: Calling delete (old) repository method now..");
              deleteReturnMessage = deleteRepositoryOfModel(serializedModel[0]);
              if (!deleteReturnMessage.equals("done")) {
                return deleteReturnMessage; // error happened
              }
              logMessage("updateRepositoryOfModel: Calling createFromModel now..");
              return createFromModel(serializedModel);
            case "application":
              logMessage("updateRepositoryOfModel: Checking application model now..");
              // check first if model can be constructed
              // (in case of an invalid model, keep the old repository)
              new Application(serializedModel);
              logMessage("updateRepositoryOfModel: Calling delete (old) repository method now..");
              deleteReturnMessage = deleteRepositoryOfModel(serializedModel[0]);
              if (!deleteReturnMessage.equals("done")) {
                return deleteReturnMessage; // error happened
              }
              logMessage("updateRepositoryOfModel: Calling createFromModel now..");
              return createFromModel(serializedModel);
            default:
              return "Error: Model has to have an attribute 'type' that is either "
                  + "'microservice', 'frontend-component' or 'application'!";
          }
        } catch (ModelParseException e) {
          logError("updateRepositoryOfModel: Model Parsing exception: " + e.getMessage());
          e.printStackTrace();
          return "Error: Parsing model failed with " + e.getMessage();
        }
      }
    }
    return "Model has no attribute 'type'!";
  }

  /**
   * 
   * Creates an
   * {@link i5.las2peer.services.codeGenerationService.models.application.communicationModel.CommunicationModel}
   * from a passed {@link i5.cae.simpleModel.SimpleModel} containing an application.
   * 
   * @param serializedModel the application model
   * 
   * @return a {@link i5.cae.simpleModel.SimpleModel} containing the communication model
   * 
   * @throws ModelParseException if the passed model cannot be parsed to an application
   * 
   */
  public SimpleModel getCommunicationViewOfApplicationModel(Serializable... serializedModel)
      throws ModelParseException {
    SimpleModel model = (SimpleModel) serializedModel[0];
    logMessage(
        "getCommunicationViewOfApplicationModel: Received model with name " + model.getName());
    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        try {
          if (!type.equals("application")) {
            throw new ModelParseException("Model is not an application!");
          }
          logMessage("getCommunicationViewOfApplicationModel: Creating application model now..");
          Application application = new Application(serializedModel);
          logMessage(
              "getCommunicationViewOfApplicationModel: Creating communication view model now..");
          SimpleModel commViewModel = application.toCommunicationModel();
          logMessage("getCommunicationViewOfApplicationModel: Created!");
          return commViewModel;
        } catch (ModelParseException e) {
          logError(
              "getCommunicationViewOfApplicationModel: Model parsing exception: " + e.getMessage());
          e.printStackTrace();
          throw e;
        }
      }
    }
    throw new ModelParseException("Model has no attribute 'type'!");
  }

}
