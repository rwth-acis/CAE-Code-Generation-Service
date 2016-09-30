package i5.las2peer.services.codeGenerationService;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
import i5.las2peer.services.codeGenerationService.generators.FrontendComponentSynchronization;
import i5.las2peer.services.codeGenerationService.generators.Generator;
import i5.las2peer.services.codeGenerationService.generators.MicroserviceGenerator;
import i5.las2peer.services.codeGenerationService.generators.MicroserviceSynchronization;
import i5.las2peer.services.codeGenerationService.models.application.Application;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;
import i5.las2peer.services.codeGenerationService.models.microservice.Microservice;
import i5.las2peer.services.codeGenerationService.templateEngine.ModelViolationDetection;

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
  private String usedGitHost;

  // jenkins properties
  private String buildJobName;
  private String dockerJobName;
  private String jenkinsUrl;
  private String jenkinsJobToken;

  private boolean useModelSynchronization;
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

    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "createFromModel: Received model with name " + model.getName());
    // TESTING: write as file
    /*
    try {
    	OutputStream file = new FileOutputStream("testModels/" + model.getName() + ".model");
    	OutputStream buffer = new BufferedOutputStream(file);
    	ObjectOutput output = new ObjectOutputStream(buffer);
    	output.writeObject(model);
    	output.close();
      } catch (IOException ex) {
    	  
      }
      */

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
                  this.gitHubPassword, this.usedGitHost);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "createFromModel: Created!");
              return "done";  
              
            case "frontend-component":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating frontend component model now..");
              FrontendComponent frontendComponent = new FrontendComponent(model);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating frontend component source code now..");
              FrontendComponentGenerator.createSourceCode(frontendComponent,
                  this.templateRepository, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubUserMail, this.gitHubPassword, this.usedGitHost);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "createFromModel: Created!");
              return "done";
              
            case "application":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating application model now..");
              Application application = new Application(serializedModel);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating application source code now..");
              ApplicationGenerator.createSourceCode(application, this.templateRepository,
                  this.gitHubOrganization, this.gitHubUser, this.gitHubUserMail,
                  this.gitHubPassword, this.usedGitHost);
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
                  this.gitHubPassword, this.usedGitHost);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
              return "done";
            case "frontend-component":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "deleteRepositoryOfModel: Deleting frontend-component repository now..");
              modelName = "frontendComponent-" + modelName.replace(" ", "-");
              Generator.deleteRemoteRepository(modelName, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubPassword, this.usedGitHost);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
              return "done";
            case "application":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "deleteRepositoryOfModel: Deleting application repository now..");
              modelName = "application-" + modelName.replace(" ", "-");
              Generator.deleteRemoteRepository(modelName, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubPassword, this.usedGitHost);
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

              // only if an old model and a remote repository exist, we can synchronize
              // the model and source code

              if (useModelSynchronization && oldModel != null
                  && MicroserviceSynchronization.existsRemoteRepositoryForModel(microservice,
                      this.gitHubOrganization, this.gitHubUser, this.gitHubPassword, this.usedGitHost)) {
                Microservice oldMicroservice = new Microservice(oldModel);

                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "updateRepositoryOfModel: Calling synchronizeSourceCode now..");

                MicroserviceSynchronization.synchronizeSourceCode(microservice, oldMicroservice,
                    this.getTracedFiles(MicroserviceGenerator.getRepositoryName(microservice)),
                    this.templateRepository, this.gitHubOrganization, this.usedGitHost ,this);

                L2pLogger.logEvent(Event.SERVICE_MESSAGE, "updateRepositoryOfModel: Synchronized!");
                return "done";
              } else {

                if (useModelSynchronization) {
                  // inform the GitHubProxy service that we may have deleted a remote repository
                  // it will then delete the local repository
                  deleteReturnMessage = this
                      .deleteLocalRepository(MicroserviceGenerator.getRepositoryName(microservice));
                  if (!deleteReturnMessage.equals("done")) {
                    return deleteReturnMessage; // error happened
                  }
                }
                
                if (Generator.existsRemoteRepository(
                    MicroserviceGenerator.getRepositoryName(microservice), gitHubOrganization,
                    gitHubUser, gitHubPassword, usedGitHost)) {
                	
                  deleteReturnMessage = deleteRepositoryOfModel(serializedModel);
                  
                  if (!deleteReturnMessage.equals("done")) {
                    return deleteReturnMessage; // error happened
                  }
                }

                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "updateRepositoryOfModel: Calling createFromModel now..");

                return createFromModel(serializedModel);
              }

            case "frontend-component":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "updateRepositoryOfModel: Checking frontend-component model now..");
              // check first if model can be constructed
              // (in case of an invalid model, keep the old repository)
              FrontendComponent frontendComponent = new FrontendComponent(model);

              // only if an old model and a remote repository exist, we can synchronize
              // the model and source code

              if (useModelSynchronization && oldModel != null
                  && FrontendComponentSynchronization.existsRemoteRepositoryForModel(
                      frontendComponent, this.gitHubOrganization, this.gitHubUser,
                      this.gitHubPassword, this.usedGitHost)) {
                FrontendComponent oldFrontendComponent = new FrontendComponent(oldModel);

                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "updateRepositoryOfModel: Calling synchronizeSourceCode now..");

                FrontendComponentSynchronization.synchronizeSourceCode(frontendComponent,
                    oldFrontendComponent,
                    this.getTracedFiles(
                        FrontendComponentGenerator.getRepositoryName(frontendComponent)),
                    this.templateRepository, this.gitHubOrganization, this.usedGitHost, this);

                L2pLogger.logEvent(Event.SERVICE_MESSAGE, "updateRepositoryOfModel: Synchronized!");
                return "done";
              } else {

                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "updateRepositoryOfModel: Calling delete (old) repository method now..");

                if (useModelSynchronization) {
                  // inform the GitHubProxy service that we may have deleted a remote repository
                  // it will then delete the local repository
                  deleteReturnMessage = this.deleteLocalRepository(
                      FrontendComponentGenerator.getRepositoryName(frontendComponent));
                  if (!deleteReturnMessage.equals("done")) {
                    return deleteReturnMessage; // error happened
                  }
                }
                if (Generator.existsRemoteRepository(
                    FrontendComponentGenerator.getRepositoryName(frontendComponent),
                    gitHubOrganization, gitHubUser, gitHubPassword, usedGitHost)) {
                  deleteReturnMessage = deleteRepositoryOfModel(serializedModel);
                  if (!deleteReturnMessage.equals("done")) {
                    return deleteReturnMessage; // error happened
                  }
                }

                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "updateRepositoryOfModel: Calling createFromModel now..");
                return createFromModel(serializedModel);

              }



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
        } catch (GitHubException e2) {
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "updateRepositoryOfModel: GitHub access exception: " + e2.getMessage());
          logger.printStackTrace(e2);
          return "Error: Generating code failed because of failing GitHub access: "
              + e2.getMessage();
        }
      }
    }
    return "Model has no attribute 'type'!";
  }

  /**
   * Deletes a local repository by invoking the GitHub proxy service
   * 
   * @param repositoryName The name of the repository to be deleted
   * @return a status string
   */

  private String deleteLocalRepository(String repositoryName) {
    try {
      Serializable[] payload = {repositoryName};

      this.invokeServiceMethod("i5.las2peer.services.gitHubProxyService.GitHubProxyService@0.1",
          "deleteLocalRepository", payload);

    } catch (Exception e) {
      logger.printStackTrace(e);
      return e.getMessage();
    }
    return "done";
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

  /**
   * Fetch all traced files of a repository from the GitHub proxy service
   * 
   * @param repositoryName The name of the repository
   * @return a map containing all traced files
   */

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

  /**
   * Performs a model violation check against the given files
   * 
   * @param violationRules A json object containing the violation rules
   * @param files The files to check
   * @return A json array containing feedback about found violations
   */
  public JSONArray checkModel(JSONObject violationRules, HashMap<String, JSONObject> files) {
    L2pLogger.logEvent(Event.SERVICE_MESSAGE, "starting model violation detection..");
    return ModelViolationDetection.performViolationCheck(files, violationRules);
  }

  /**
   * Start a build job for the deployment of an application.
   * 
   * @param jobAlias The job name/alias of the job to start, either "Build" or "Docker"
   * @return The path of the queue item of the started job
   */

  public String startJenkinsJob(String jobAlias) {
    String jobName = null;
    switch (jobAlias) {
      case "Build":
        jobName = buildJobName;
        break;
      case "Docker":
        jobName = dockerJobName;
        break;
      default:
        return "Error: Unknown job alias given!";
    }

    return ApplicationGenerator.deployApplication(jenkinsUrl, jenkinsJobToken, jobName);
  }

  /**
   * Get the deployment status of the last build from Jenkins
   * 
   * @param queueItem The queue item path returned by the remote api of Jenkins after a build has
   *        been started. Needed to get the status of the build, i.e. in queue or already started
   * @return The console text of the last build from Jenkins
   */

  public String deployStatus(String queueItem) {
    return ApplicationGenerator.deployStatus(queueItem, jenkinsUrl);
  }

  /**
   * Prepare a deployment of an application model, i.e. the model is copied to a temp repository
   * which is used later during the deployment
   * 
   * @param serializedModel The application model to deploy
   * @return A status text
   */

  public String prepareDeploymentApplicationModel(Serializable... serializedModel) {

    SimpleModel model = (SimpleModel) serializedModel[0];

    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "deployApplicationModel: Received model with name " + model.getName());

    // find out what type of model we got (microservice, frontend-component or application)
    for (int i = 0; i < model.getAttributes().size(); i++) {
      if (model.getAttributes().get(i).getName().equals("type")) {
        String type = model.getAttributes().get(i).getValue();
        try {
          switch (type) {
            case "application":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "deployApplicationModel: Creating application model now..");
              Application application = new Application(serializedModel);

              String repositoryName = "CAE-Deployment-Temp";

              if (Generator.existsRemoteRepository(repositoryName, this.gitHubOrganization,
                  this.gitHubUser, this.gitHubPassword, this.usedGitHost)) {
                Generator.deleteRemoteRepository(repositoryName, this.gitHubOrganization,
                    this.gitHubUser, this.gitHubPassword, this.usedGitHost);
                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "deployApplicationModel: Deleted old repository!");
              }

              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "deployApplicationModel: Copying repository to deployment repository");
              ApplicationGenerator.createSourceCode(repositoryName, application,
                  this.templateRepository, this.gitHubOrganization, this.gitHubUser,
                  this.gitHubUserMail, this.gitHubPassword, this.usedGitHost,true);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "deployApplicationModel: Copied!");
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
          return "Error: Generating code failed because of failing GitHub access: " + e2.getMessage();
        }
      }
    }
    return "Model has no attribute 'type'!";
  }

}
