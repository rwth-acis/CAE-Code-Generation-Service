package i5.las2peer.services.codeGenerationService;

import java.io.Serializable;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.api.Service;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.services.codeGenerationService.exception.GitHubException;
import i5.las2peer.services.codeGenerationService.exception.ModelParseException;
import i5.las2peer.services.codeGenerationService.generators.ApplicationGenerator;
import i5.las2peer.services.codeGenerationService.generators.FrontendComponentGenerator;
import i5.las2peer.services.codeGenerationService.generators.Generator;
import i5.las2peer.services.codeGenerationService.generators.MicroserviceGenerator;
import i5.las2peer.services.codeGenerationService.generators.MicroserviceSynchronization;
import i5.las2peer.services.codeGenerationService.models.application.Application;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;
import i5.las2peer.services.codeGenerationService.templateEngine.ModelChecker;

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
  private final L2pLogger logger = L2pLogger.getInstance(CodeGenerationService.class.getName());

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

    // old model only used for microservice and frontend components
    SimpleModel oldModel = null;

    if (serializedModel.length > 1) {
      oldModel = (SimpleModel) serializedModel[1];
    }

    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "createFromModel: Received model with name " + model.getName());
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
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating microservice model now..");
              Microservice microservice = new Microservice(model);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating microservice source code now..");
              MicroserviceGenerator.createSourceCode(microservice, this.templateRepository,
                  this.gitHubOrganization, this.gitHubUser, this.gitHubUserMail,
                  this.gitHubPassword);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "createFromModel: Created!");
              return "done";
            case "frontend-component":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating frontend component model now..");
              FrontendComponent frontendComponent = new FrontendComponent(model);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating frontend component source code now..");

              // only if there is an old model and a remote repository exists, we can synchronize
              // the model

              if (oldModel != null && Generator.existsRemoteRepository(
                  FrontendComponentGenerator.getRepositoryName(frontendComponent),
                  this.gitHubOrganization, this.gitHubUser, this.gitHubPassword)) {
                FrontendComponent oldFrontendComponent = new FrontendComponent(oldModel);

                FrontendComponentGenerator.synchronizeSourceCode(frontendComponent,
                    oldFrontendComponent,
                    this.getTracedFiles(
                        FrontendComponentGenerator.getRepositoryName(frontendComponent)),
                    this.templateRepository, this.gitHubOrganization, this);

                L2pLogger.logEvent(Event.SERVICE_MESSAGE, "createFromModel: Synchronized!");

              } else {

                FrontendComponentGenerator.createSourceCode(frontendComponent,
                    this.templateRepository, this.gitHubOrganization, this.gitHubUser,
                    this.gitHubUserMail, this.gitHubPassword);;
                L2pLogger.logEvent(Event.SERVICE_MESSAGE, "createFromModel: Created!");

                // inform the GitHubProxy service that we may have deleted an existing repository

                try {
                  Serializable[] payload =
                      {FrontendComponentGenerator.getRepositoryName(frontendComponent)};

                  this.invokeServiceMethod(
                      "i5.las2peer.services.gitHubProxyService.GitHubProxyService@0.1",
                      "deleteLocalRepository", payload);

                } catch (Exception e) {
                  logger.printStackTrace(e);
                }

              }

              return "done";
            case "application":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating application model now..");
              Application application = new Application(serializedModel);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating application source code now..");
              ApplicationGenerator.createSourceCode(application, this.templateRepository,
                  this.gitHubOrganization, this.gitHubUser, this.gitHubUserMail,
                  this.gitHubPassword);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "createFromModel: Created!");
              return "done";
            default:
              return "Error: Model has to have an attribute 'type' that is either "
                  + "'microservice', 'frontend-component' or 'application'!";
          }
        } catch (ModelParseException e1) {
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "createFromModel: Model parsing exception: " + e1.getMessage());
          logger.printStackTrace(e1);
          return "Error: Parsing model failed with " + e1.getMessage();
        } catch (GitHubException e2) {
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "createFromModel: GitHub access exception: " + e2.getMessage());
          logger.printStackTrace(e2);
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
  public String deleteRepositoryOfModel(Serializable... serializedModel) {
    SimpleModel model = (SimpleModel) serializedModel[0];
    String modelName = model.getName();
    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "deleteRepositoryOfModel: Received model with name " + modelName);
    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        try {
          switch (type) {
            case "microservice":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "deleteRepositoryOfModel: Deleting microservice repository now..");
              modelName = "microservice-" + modelName.replace(" ", "-");
              Generator.deleteRemoteRepository(modelName, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubPassword);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
              return "done";
            case "frontend-component":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "deleteRepositoryOfModel: Deleting frontend-component repository now..");
              modelName = "frontendComponent-" + modelName.replace(" ", "-");
              Generator.deleteRemoteRepository(modelName, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubPassword);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
              return "done";
            case "application":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "deleteRepositoryOfModel: Deleting application repository now..");
              modelName = "application-" + modelName.replace(" ", "-");
              Generator.deleteRemoteRepository(modelName, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubPassword);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
              return "done";
            default:
              return "Error: Model has to have an attribute 'type' that is either "
                  + "'microservice', 'frontend-component' or 'application'!";
          }
        } catch (GitHubException e) {
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "deleteRepositoryOfModel: GitHub access exception: " + e.getMessage());
          logger.printStackTrace(e);
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
    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "updateRepositoryOfModel: Received model with name " + modelName);

    // old model only used for microservice and frontend components
    SimpleModel oldModel = null;

    if (serializedModel.length > 1) {
      oldModel = (SimpleModel) serializedModel[1];
      L2pLogger.logEvent(Event.SERVICE_MESSAGE,
          "updateRepositoryOfModel: Received old model with name " + oldModel.getName());
    }

    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        String deleteReturnMessage;
        try {
          switch (type) {
            case "microservice":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "updateRepositoryOfModel: Checking microservice model now..");
              // check first if model can be constructed
              // (in case of an invalid model, keep the old repository)
              Microservice microservice = new Microservice(model);

              // only if there is an old model and a remote repository exists, we can synchronize
              // the model

              if (oldModel != null && Generator.existsRemoteRepository(
                  MicroserviceGenerator.getRepositoryName(microservice), this.gitHubOrganization,
                  this.gitHubUser, this.gitHubPassword)) {
                Microservice oldMicroservice = new Microservice(oldModel);

                MicroserviceSynchronization.synchronizeSourceCode(microservice, oldMicroservice,
                    this.getTracedFiles(MicroserviceGenerator.getRepositoryName(microservice)),
                    this.templateRepository, this.gitHubOrganization, this);

                L2pLogger.logEvent(Event.SERVICE_MESSAGE, "updateRepositoryOfModel: Synchronized!");
                return "done";
              } else {
                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "updateRepositoryOfModel: Calling createFromModel now..");
                return createFromModel(serializedModel);
              }

            case "frontend-component":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "updateRepositoryOfModel: Checking frontend-component model now..");
              // check first if model can be constructed
              // (in case of an invalid model, keep the old repository)
              new FrontendComponent(model);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "updateRepositoryOfModel: Calling delete (old) repository method now..");
              // deleteReturnMessage = deleteRepositoryOfModel(serializedModel);
              // if (!deleteReturnMessage.equals("done")) {
              // return deleteReturnMessage; // error happened
              // }
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "updateRepositoryOfModel: Calling createFromModel now..");
              return createFromModel(serializedModel);
            case "application":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "updateRepositoryOfModel: Checking application model now..");
              // check first if model can be constructed
              // (in case of an invalid model, keep the old repository)
              new Application(serializedModel);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "updateRepositoryOfModel: Calling delete (old) repository method now..");
              deleteReturnMessage = deleteRepositoryOfModel(serializedModel);
              if (!deleteReturnMessage.equals("done")) {
                return deleteReturnMessage; // error happened
              }
              L2pLogger.logEvent(Event.SERVICE_ERROR,
                  "updateRepositoryOfModel: Calling createFromModel now..");
              return createFromModel(serializedModel);
            default:
              return "Error: Model has to have an attribute 'type' that is either "
                  + "'microservice', 'frontend-component' or 'application'!";
          }
        } catch (ModelParseException e) {
          L2pLogger.logEvent(Event.SERVICE_ERROR,
              "updateRepositoryOfModel: Model Parsing exception: " + e.getMessage());
          logger.printStackTrace(e);
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
    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "getCommunicationViewOfApplicationModel: Received model with name " + model.getName());
    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        try {
          if (!type.equals("application")) {
            throw new ModelParseException("Model is not an application!");
          }
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "getCommunicationViewOfApplicationModel: Creating application model now..");
          Application application = new Application(serializedModel);
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "getCommunicationViewOfApplicationModel: Creating communication view model now..");
          SimpleModel commViewModel = application.toCommunicationModel();
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "getCommunicationViewOfApplicationModel: Created!");
          return commViewModel;
        } catch (ModelParseException e) {
          L2pLogger.logEvent(Event.SERVICE_ERROR,
              "getCommunicationViewOfApplicationModel: Model parsing exception: " + e.getMessage());
          logger.printStackTrace(e);
          throw e;
        }
      }
    }
    throw new ModelParseException("Model has no attribute 'type'!");
  }



  public void commitFile(String repositoryName, JSONObject obj) {
    Serializable[] payload = {repositoryName, obj.toJSONString()};
    try {
      this.invokeServiceMethod("i5.las2peer.services.gitHubProxyService.GitHubProxyService@0.1",
          "storeAndCommitFle", payload);
    } catch (Exception e) {
      logger.printStackTrace(e);
    }
  }

  @SuppressWarnings("unchecked")
  private HashMap<String, JSONObject> getTracedFiles(String repositoryName) {
    Serializable[] payload = {repositoryName};
    HashMap<String, JSONObject> files = new HashMap<String, JSONObject>();
    try {
      files = (HashMap<String, JSONObject>) this.invokeServiceMethod(
          "i5.las2peer.services.gitHubProxyService.GitHubProxyService@0.1", "getAllTracedFiles",
          payload);
    } catch (Exception e) {
      logger.printStackTrace(e);
    }
    return files;
  }

  public void commitFilesRaw(String repositoryName, String[][] files) {
    Serializable[] payload = {repositoryName, "", files};
    try {
      this.invokeServiceMethod("i5.las2peer.services.gitHubProxyService.GitHubProxyService@0.1",
          "storeAndCommitFilesRaw", payload);
    } catch (Exception e) {
      logger.printStackTrace(e);
    }
  }


  public void commitFileRaw(String repositoryName, String fileName, String encodeToString) {
    this.commitFilesRaw(repositoryName, new String[][] {new String[] {fileName, encodeToString}});
  }

  /**
   * Performs a model violation check against the files located in the given repository
   * 
   * @param repositoryName The name of the repository
   * @return A json array containing guidances of found violations
   */
  public JSONArray checkModel(String repositoryName) {
    return ModelChecker.performViolationCheck(this.getTracedFiles(repositoryName),
        templateRepository, gitHubOrganization);
  }


  public void renameFile(String repositoryName, String newFileName, String oldFileName) {
    Serializable[] payload = {repositoryName, newFileName, oldFileName};
    try {
      this.invokeServiceMethod("i5.las2peer.services.gitHubProxyService.GitHubProxyService@0.1",
          "renameFile", payload);
    } catch (Exception e) {
      logger.printStackTrace(e);
    }
  }
}
