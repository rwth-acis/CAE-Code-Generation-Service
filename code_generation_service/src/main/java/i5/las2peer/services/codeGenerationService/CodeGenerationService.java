package i5.las2peer.services.codeGenerationService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.adapters.GitHostAdapter;
import i5.las2peer.services.codeGenerationService.adapters.GitHubAdapter;
import i5.las2peer.services.codeGenerationService.adapters.GitLabAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHelperException;
import i5.las2peer.services.codeGenerationService.exception.GitHostException;
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
import i5.las2peer.services.codeGenerationService.utilities.GitUtility;
import io.swagger.annotations.Api;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

/**
 * 
 * CAE Code Generation Service
 * 
 * A LAS2peer service used for generating code from send models. Part of the
 * CAE.
 * 
 */
@Api
@SwaggerDefinition(info = @Info(title = "CAE Code Generation Service", version = "0.1", description = "A LAS2peer service used for generating code and managing remote repositories. Part of the CAE.", termsOfService = "none", contact = @Contact(name = "Peter de Lange", url = "https://github.com/PedeLa/", email = "lange@dbis.rwth-aachen.de"), license = @License(name = "BSD", url = "https://github.com/PedeLa/CAE-Model-Persistence-Service//blob/master/LICENSE.txt")))
@ServicePath("CodeGen")
@ManualDeployment
public class CodeGenerationService extends RESTService {

	// Git service properties
	private String gitUser;
	private String gitPassword;
	private String gitOrganization;
	private String templateRepository;
	private String gitUserMail;
	private String usedGitHost;

	private boolean useModelCheck;

	private String baseURL;
	private String token;

	private String oidcProvider;
	
	// The git service adapter object
	private GitHostAdapter gitAdapter;

	// The git helper utility
	private GitUtility gitUtility;

	// Proxy helper
	private GitProxy gitProxy;

	// jenkins properties
	private String buildJobName;
	private String dockerJobName;
	private String jenkinsUrl;
	private String jenkinsJobToken;
	private String deploymentRepo;

	boolean useModelSynchronization;
	private final L2pLogger logger = L2pLogger.getInstance(CodeGenerationService.class.getName());
	
	//The base URL where generated and deployed widget's files are hosted
	private String widgetHomeBaseURL;
	private String localGitPath;

	public CodeGenerationService() throws GitHostException {
		// read and set properties-file values
		setFieldValues();

		ApplicationGenerator.deploymentRepo = deploymentRepo;

		// Check if non-optional properties are set
		// gitUser
		// gitUserMail
		// gitOrganization
		// templateRepository
		// deploymentRepo
		// gitPassword
		// baseURL
		if (Objects.equals(gitUser, "")) {
			throw new GitHostException("Git user not set!");
		}
		if (Objects.equals(gitUserMail, "")) {
			throw new GitHostException("Git user mail not set!");
		}
		if (Objects.equals(gitOrganization, "")) {
			throw new GitHostException("Git organization not set!");
		}
		if (Objects.equals(templateRepository, "")) {
			throw new GitHostException("Template repository not set!");
		}
		if (Objects.equals(deploymentRepo, "")) {
			throw new GitHostException("Deployment repository not set!");
		}
		if (Objects.equals(gitPassword, "")) {
			throw new GitHostException("Git host password not set");
		}
		if (Objects.equals(baseURL, "")) {
			// Empty base url leads to wrong paths later on
			throw new GitHostException("No valid base url specified");
		} else {
			// Check for trailing slash, to prevent annoying errors
			if (!baseURL.endsWith("/")) {
				baseURL = baseURL.concat("/");
			}
		}
		if (Objects.equals(widgetHomeBaseURL, "")) {
			// Warn about empty base path and that github.io is used
			logger.warning("Using github pages base path as no url is specified!");
			//"http://" + gitHubOrganization + ".github.io/"
			widgetHomeBaseURL = "http://" + gitOrganization + ".github.io/";
		} else {
			// Check for trailing slash
			if(!widgetHomeBaseURL.endsWith("/")) {
				baseURL = baseURL.concat("/");
			}
		}

		// Create git adapter matching the usedGitHost
		if (Objects.equals(usedGitHost, "GitHub")) {
			gitAdapter = new GitHubAdapter(gitUser, gitPassword, token, gitOrganization, templateRepository, gitUserMail);
		} else if (Objects.equals(usedGitHost, "GitLab")) {
			gitAdapter = new GitLabAdapter(baseURL, token, gitUser, gitPassword, gitOrganization, templateRepository,
					gitUserMail);
		} else {
			// Abort
			throw new GitHostException("No valid git provider selected");
		}
		gitUtility = new GitUtility(gitUser, gitPassword, gitOrganization, baseURL);
		gitProxy = new GitProxy(gitUtility, logger);
	}

	@Override
	protected void initResources() {
		getResourceConfig().register(RESTResources.class);
	}

	/**
	 * 
	 * Creates a new GitHub repository with the source code according to the
	 * passed on model.
	 * 
	 * @param forcePush boolean value
	 * @param commitMessage Message that should be used for the commit.
	 * @param versionTag String which should be used as the tag when commiting. May be null.
	 * @param serializedModel
	 *            a {@link i5.cae.simpleModel.SimpleModel} that contains the
	 *            model, or in case of an application model also the model
	 *            components as additional models
	 * 
	 * @return a string containing either the message "done" or, in case of an
	 *         error, the error message
	 * 
	 */
	public String createFromModel(boolean forcePush, String commitMessage, String versionTag, String metadataDoc,
			ArrayList<SimpleModel> serializedModel, HashMap<String, String> externalDependencies) {
		
		SimpleModel model = (SimpleModel) serializedModel.get(0);
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "createFromModel: Received model with name " + model.getName());

		// TESTING: write as file
		/*
		 * try { OutputStream file = new FileOutputStream("testModels/" +
		 * model.getName() + ".model"); OutputStream buffer = new
		 * BufferedOutputStream(file); ObjectOutput output = new
		 * ObjectOutputStream(buffer); output.writeObject(model);
		 * output.close(); } catch (IOException ex) {
		 * 
		 * }
		 */

		// find out what type of model we got (microservice, frontend-component
		// or application)
		for (int i = 0; i < model.getAttributes().size(); i++) {
			if (model.getAttributes().get(i).getName().equals("type")) {
				String type = model.getAttributes().get(i).getValue();
				try {
					String commitSha;
					switch (type) {
					case "microservice":
						// Create an object representing the microservice model
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "createFromModel: Creating microservice model now..");
						Microservice microservice = new Microservice(model);
						microservice.setMetadataDocString(metadataDoc);

						// Generate the code (and repositories) for this model
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"createFromModel: Creating microservice source code now..");
						commitSha = MicroserviceGenerator.createSourceCode(microservice, this.templateRepository,
								(BaseGitHostAdapter) gitAdapter, commitMessage, versionTag, forcePush, metadataDoc);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "createFromModel: Created!");
						return "done:" + commitSha;

					// The same for the two other types
					case "frontend-component":
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"createFromModel: Creating frontend component model now..");
						FrontendComponent frontendComponent = new FrontendComponent(model);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"createFromModel: Creating frontend component source code now..");
						commitSha = FrontendComponentGenerator.createSourceCode(frontendComponent, (BaseGitHostAdapter) gitAdapter,
								commitMessage, versionTag, forcePush);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "createFromModel: Created!");
						return "done:" + commitSha;

					case "application":
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "createFromModel: Creating application model now..");
						Application application = new Application(serializedModel, externalDependencies);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"createFromModel: Creating application source code now..");
						ApplicationGenerator.createSourceCode(application, (BaseGitHostAdapter) gitAdapter, commitMessage,
								versionTag);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "createFromModel: Created!");
						return "done";
					default:
						return "Error: Model has to have an attribute 'type' that is either "
								+ "'microservice', 'frontend-component' or 'application'!";
					}
				} catch (ModelParseException e1) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
							"createFromModel: Model parsing exception: " + e1.getMessage());
					logger.printStackTrace(e1);
					return "Error: Parsing model failed with " + e1.getMessage();
				} catch (GitHostException e2) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
							"createFromModel: GitHub access exception: " + e2.getMessage());
					logger.printStackTrace(e2);
					return "Error: Generating code failed because of failing GitHub access: " + e2.getMessage();
				}
			}
		}
		return "Model has no attribute 'type'!";
	}

	public String createFromModel(String commitMessage, String versionTag, String metadataDoc, ArrayList<SimpleModel> serializedModel,
			HashMap<String, String> externalDependencies) {
		if(versionTag.equals("")) versionTag = null;
		return createFromModel(false, commitMessage, versionTag, "", serializedModel, externalDependencies);
	}

	/**
	 * 
	 * Deletes a model's repository from GitHub. Please note, that in this case,
	 * it is not checked for correctness of the model, only the name and type
	 * are extracted and then the repository gets deleted according to it.
	 * 
	 * @param serializedModel
	 *            a {@link i5.cae.simpleModel.SimpleModel} that contains the
	 *            model
	 * 
	 * @return a string containing either the message "done" or, in case of an
	 *         error, the error message
	 * 
	 */
	public String deleteRepositoryOfModel(ArrayList<SimpleModel> serializedModel) {
		SimpleModel model = (SimpleModel) serializedModel.get(0);
		String modelName = model.getName();
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deleteRepositoryOfModel: Received model with name " + modelName);
		for (int i = 0; i < model.getAttributes().size(); i++) {
			if (model.getAttributes().get(i).getName().equals("type")) {
				String type = model.getAttributes().get(i).getValue();
				try {
					switch (type) {
					case "microservice":
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"deleteRepositoryOfModel: Deleting microservice repository now..");
						modelName = "microservice-" + modelName.replace(" ", "-");
						Generator.deleteRemoteRepository(modelName, (BaseGitHostAdapter) this.gitAdapter);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
						return "done";
					case "frontend-component":
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"deleteRepositoryOfModel: Deleting frontend-component repository now..");
						modelName = "frontendComponent-" + modelName.replace(" ", "-");
						Generator.deleteRemoteRepository(modelName, (BaseGitHostAdapter) this.gitAdapter);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
						return "done";
					case "application":
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"deleteRepositoryOfModel: Deleting application repository now..");
						modelName = "application-" + modelName.replace(" ", "-");
						Generator.deleteRemoteRepository(modelName, (BaseGitHostAdapter) this.gitAdapter);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
						return "done";
					default:
						return "Error: Model has to have an attribute 'type' that is either "
								+ "'microservice', 'frontend-component' or 'application'!";
					}
				} catch (GitHostException e) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
							"deleteRepositoryOfModel: GitHub access exception: " + e.getMessage());
					logger.printStackTrace(e);
					return "Error: Deleting repository failed because of failing GitHub access: " + e.getMessage();
				}
			}
		}
		return "Model has no attribute 'type'!";
	}

	/**
	 * 
	 * "Updates" an already existing repository with the new given model. Please
	 * note that the current implementation does not really perform an update,
	 * but just deletes the old repository and replaces it with the contents of
	 * the new model.
	 * 
	 * @param commitMessage Commit message that should be used.
	 * @param versionTag String which should be used as the tag when commiting. May be null.
	 * @param metadataDoc
	 * @param serializedModel
	 *            a {@link i5.cae.simpleModel.SimpleModel} that contains the
	 *            model, or in case of an application model also the model
	 *            components as additional models
	 * 
	 * @return a string containing either the message "done" or, in case of an
	 *         error, the error message
	 * 
	 */
	public String updateRepositoryOfModel(String commitMessage, String versionTag, String metadataDoc, 
			ArrayList<SimpleModel> serializedModel, HashMap<String, String> externalDependencies) {
		SimpleModel model = (SimpleModel) serializedModel.get(0);
		String modelName = model.getName();
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "updateRepositoryOfModel: Received model with name " + modelName);

		if(versionTag.equals("")) versionTag = null;
		
		// old model only used for microservice and frontend components
		SimpleModel oldModel = null;

		if (serializedModel.size() > 1) {
			oldModel = (SimpleModel) serializedModel.get(1);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
					"updateRepositoryOfModel: Received old model with name " + oldModel.getName());
		}

		for (int i = 0; i < model.getAttributes().size(); i++) {
			if (model.getAttributes().get(i).getName().equals("type")) {

				String type = model.getAttributes().get(i).getValue();
				String deleteReturnMessage;
				try {
					switch (type) {
					case "microservice":
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"updateRepositoryOfModel: Checking microservice model now..");
						// check first if model can be constructed
						// (in case of an invalid model, keep the old
						// repository)
						Microservice microservice = new Microservice(model);
						microservice.setMetadataDocString(metadataDoc);

						// only if an old model and a remote repository exist,
						// we can synchronize
						// the model and source code
						if (useModelSynchronization && oldModel != null && MicroserviceSynchronization
								.existsRemoteRepositoryForModel(microservice, (BaseGitHostAdapter) gitAdapter)) {
							Microservice oldMicroservice = new Microservice(oldModel);
							Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
									"Using model sync: Old model:" + oldModel.getName());
							Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
									"updateRepositoryOfModel: Calling synchronizeSourceCode now..");

							String commitSha = MicroserviceSynchronization.synchronizeSourceCode(microservice, oldMicroservice,
									this.getTracedFiles(MicroserviceGenerator.getRepositoryName(microservice)),
									(BaseGitHostAdapter) gitAdapter, CodeGenerationService.this, metadataDoc, 
									gitUtility, commitMessage, versionTag);

							Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "updateRepositoryOfModel: Synchronized!");
							return "done:" + commitSha;

						} else {

							if (gitAdapter instanceof GitLabAdapter) {
								// Use pseudo-update to circumvent gitlab
								// deletion/creation problem
								return pseudoUpdateRepositoryOfModel(commitMessage, versionTag, metadataDoc, 
										serializedModel, externalDependencies);

							} else {
								if (useModelSynchronization) {
									Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "Using model sync: Deleting local repo");
									deleteReturnMessage = this.deleteLocalRepository(
											MicroserviceGenerator.getRepositoryName(microservice));
									if (!deleteReturnMessage.equals("done")) {
										return deleteReturnMessage; // error
																	// happened
									}
								}

								if (Generator.existsRemoteRepository(
										MicroserviceGenerator.getRepositoryName(microservice),
										(BaseGitHostAdapter) gitAdapter)) {
									Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
											"Using model sync: Remote exits. Calling delete repo of model");
									deleteReturnMessage = deleteRepositoryOfModel(serializedModel);

									if (!deleteReturnMessage.equals("done")) {
										return deleteReturnMessage; // error
																	// happened
									}
								}

								Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
										"updateRepositoryOfModel: Calling createFromModel now..");

								return createFromModel(commitMessage, versionTag, metadataDoc, serializedModel, externalDependencies);
							}
						}

					case "frontend-component":
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"updateRepositoryOfModel: Checking frontend-component model now..");
						// check first if model can be constructed
						// (in case of an invalid model, keep the old
						// repository)
						FrontendComponent frontendComponent = new FrontendComponent(model);

						// only if an old model and a remote repository exist,
						// we can synchronize
						// the model and source code

						if (useModelSynchronization && oldModel != null && FrontendComponentSynchronization
								.existsRemoteRepositoryForModel(frontendComponent, (BaseGitHostAdapter) gitAdapter)) {
							FrontendComponent oldFrontendComponent = new FrontendComponent(oldModel);

							Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
									"updateRepositoryOfModel: Calling synchronizeSourceCode now..");

							String commitSha = FrontendComponentSynchronization.synchronizeSourceCode(frontendComponent,
									oldFrontendComponent,
									this.getTracedFiles(
											FrontendComponentGenerator.getRepositoryName(frontendComponent)),
									(BaseGitHostAdapter) gitAdapter, CodeGenerationService.this, metadataDoc, 
									gitUtility, commitMessage, versionTag);

							Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "updateRepositoryOfModel: Synchronized!");
							return "done:" + commitSha;
						} else {
							if (gitAdapter instanceof GitLabAdapter) {
								return pseudoUpdateRepositoryOfModel(commitMessage, versionTag, metadataDoc, 
										serializedModel, externalDependencies);
							} else {
								if (useModelSynchronization) {
									Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
											"updateRepositoryOfModel: Calling delete (old) repository method now..");
									deleteReturnMessage = this.deleteLocalRepository(
											FrontendComponentGenerator.getRepositoryName(frontendComponent));
									if (!deleteReturnMessage.equals("done")) {
										return deleteReturnMessage; // error
																	// happened
									}
								}
								if (Generator.existsRemoteRepository(
										FrontendComponentGenerator.getRepositoryName(frontendComponent),
										(BaseGitHostAdapter) gitAdapter)) {
									deleteReturnMessage = deleteRepositoryOfModel(serializedModel);
									if (!deleteReturnMessage.equals("done")) {
										return deleteReturnMessage; // error
																	// happened
									}
								}

								Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
										"updateRepositoryOfModel: Calling createFromModel now..");
								return createFromModel(commitMessage, versionTag, metadataDoc, serializedModel, externalDependencies);
							}

						}

					case "application":
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"updateRepositoryOfModel: Checking application model now..");
						// check first if model can be constructed
						// (in case of an invalid model, keep the old
						// repository)
						// new Application(serializedModel);
						/*
						 * Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
						 * "updateRepositoryOfModel: Calling delete (old) repository method now.."
						 * ); deleteReturnMessage =
						 * deleteRepositoryOfModel(serializedModel); if
						 * (!deleteReturnMessage.equals("done")) { return
						 * deleteReturnMessage; // error happened }
						 * Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR,
						 * "updateRepositoryOfModel: Calling createFromModel now.."
						 * ); return createFromModel(serializedModel);
						 */
						return pseudoUpdateRepositoryOfModel(commitMessage, versionTag, "", serializedModel, externalDependencies);

					default:
						return "Error: Model has to have an attribute 'type' that is either "
								+ "'microservice', 'frontend-component' or 'application'!";
					}
				} catch (ModelParseException e) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR,
							"updateRepositoryOfModel: Model Parsing exception: " + e.getMessage());
					logger.printStackTrace(e);
					return "Error: Parsing model failed with " + e.getMessage();
				} catch (GitHostException e2) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
							"updateRepositoryOfModel: GitHub access exception: " + e2.getMessage());
					logger.printStackTrace(e2);
					return "Error: Generating code failed because of failing GitHub access: " + e2.getMessage();
				} catch (GitHelperException e) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR,
							"updateRepositoryOfModel: GitHelperException: " + e.getMessage());
					logger.printStackTrace(e);
					return "Error: Pushing failed " + e.getMessage();
				}
			}
		}
		return "Model has no attribute 'type'!";
	}

	public String pseudoUpdateRepositoryOfModel(String commitMessage, String versionTag, String metadataDoc,
			ArrayList<SimpleModel> serializedModel, HashMap<String, String> externalDependencies) {
		SimpleModel model = serializedModel.get(0);
		model.getName();
		// force push
		return createFromModel(true, commitMessage, versionTag, metadataDoc, serializedModel, externalDependencies);
	}

	/**
	 * Deletes a local repository
	 * 
	 * @param repositoryName
	 *            The name of the repository to be deleted
	 * @return a status string
	 */

	public String deleteLocalRepository(String repositoryName) {
		try {
			FileUtils.deleteDirectory(new File(repositoryName));
		} catch (IOException e) {
			e.printStackTrace();
			logger.printStackTrace(e);
			return e.getMessage();
		}
		return "done";
	}

	/**
	 * 
	 * Creates an
	 * {@link i5.las2peer.services.codeGenerationService.models.application.communicationModel.CommunicationModel}
	 * from a passed {@link i5.cae.simpleModel.SimpleModel} containing an
	 * application.
	 * 
	 * @param serializedModel
	 *            the application model
	 * 
	 * @return a {@link i5.cae.simpleModel.SimpleModel} containing the
	 *         communication model
	 * 
	 * @throws ModelParseException
	 *             if the passed model cannot be parsed to an application
	 * 
	 */
	public SimpleModel getCommunicationViewOfApplicationModel(ArrayList<SimpleModel> serializedModel)
			throws ModelParseException {
		SimpleModel model = serializedModel.get(0);
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
				"getCommunicationViewOfApplicationModel: Received model with name " + model.getName());
		for (int i = 0; i < model.getAttributes().size(); i++) {
			if (model.getAttributes().get(i).getName().equals("type")) {
				String type = model.getAttributes().get(i).getValue();
				try {
					if (!type.equals("application")) {
						throw new ModelParseException("Model is not an application!");
					}
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
							"getCommunicationViewOfApplicationModel: Creating application model now..");
					Application application = new Application(serializedModel, new HashMap<String, String>());
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
							"getCommunicationViewOfApplicationModel: Creating communication view model now..");
					SimpleModel commViewModel = application.toCommunicationModel();
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "getCommunicationViewOfApplicationModel: Created!");
					return commViewModel;
				} catch (ModelParseException e) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR,
							"getCommunicationViewOfApplicationModel: Model parsing exception: " + e.getMessage());
					logger.printStackTrace(e);
					throw e;
				}
			}
		}
		throw new ModelParseException("Model has no attribute 'type'!");
	}

	/**
	 * Fetch all traced files of a repository
	 * 
	 * @param repositoryName
	 *            The name of the repository
	 * @return a map containing all traced files
	 */
	private HashMap<String, JSONObject> getTracedFiles(String repositoryName) {
		HashMap<String, JSONObject> files = new HashMap<String, JSONObject>();
		try {
			files = getAllTracedFiles(repositoryName);
		} catch (Exception e) {
			logger.printStackTrace(e);
		}
		return files;
	}

	/**
	 * Get a list containing all traced files of a given repository
	 * 
	 * @param repositoryName
	 *            The name of the repository
	 * @return A list of all traced files
	 */

	@SuppressWarnings("unchecked")
	public HashMap<String, JSONObject> getAllTracedFiles(String repositoryName) {
		HashMap<String, JSONObject> files = new HashMap<String, JSONObject>();

		try (Git git = gitUtility.getLocalGit(repositoryName, "development");) {
			JSONArray tracedFiles = (JSONArray) gitProxy.getTraceModel(git).get("tracedFiles");

			try (TreeWalk treeWalk = gitUtility.getRepositoryTreeWalk(git.getRepository(), true)) {
				while (treeWalk.next()) {
					if (tracedFiles.contains(treeWalk.getPathString())) {
						JSONObject fileObject = new JSONObject();
						String content = gitUtility.getFileContent(git.getRepository(), treeWalk.getPathString());
						JSONObject fileTraces = gitProxy.getFileTraces(git, treeWalk.getPathString());

						fileObject.put("content", Base64.getEncoder().encodeToString(content.getBytes("utf-8")));
						fileObject.put("fileTraces", fileTraces);

						files.put(treeWalk.getPathString(), fileObject);
					}
				}
			}

		} catch (Exception e) {
			logger.printStackTrace(e);
		}

		return files;
	}

	/**
	 * Store the content of the files encoded in based64 to a repository
	 * 
	 * @param repositoryName
	 *            The name of the repository
	 * @param commitMessage
	 *            The commit message to use
	 * 
	 * @param versionTag String which should be used as the tag when commiting. May be null.
	 * @param files
	 *            The file list containing the files to commit
	 * @return A status string
	 */

	public String storeAndCommitFilesRaw(String repositoryName, String commitMessage, String versionTag, String[][] files) {

		try (Git git = gitUtility.getLocalGit(repositoryName, "development");) {
			for (String[] fileData : files) {

				String filePath = fileData[0];
				String content = fileData[1];

				byte[] base64decodedBytes = Base64.getDecoder().decode(content);
				String decodedString = new String(base64decodedBytes, "utf-8");

				File file = new File(git.getRepository().getDirectory().getParent(), filePath);
				if (!file.exists()) {
					File dirs = file.getParentFile();
					dirs.mkdirs();
					file.createNewFile();
				}
				FileWriter fW = new FileWriter(file, false);
				fW.write(decodedString);
				fW.close();

				git.add().addFilepattern(filePath).call();

			}

			RevCommit commit = git.commit().setAuthor(this.gitUser, this.gitUserMail).setMessage(commitMessage).call();
			
			if(versionTag != null) {
				git.tag().setObjectId(commit).setName(versionTag).call();	
			}
			
			Ref head = git.getRepository().getAllRefs().get("HEAD");
            String commitSha = head.getObjectId().getName();
            return commitSha;
		} catch (Exception e) {
			logger.printStackTrace(e);
			return e.getMessage();
		}
	}

	/**
	 * Performs a model violation check against the given files
	 * 
	 * @param violationRules
	 *            A json object containing the violation rules
	 * @param files
	 *            The files to check
	 * @return A json array containing feedback about found violations
	 */
	public JSONArray checkModel(JSONObject violationRules, HashMap<String, JSONObject> files) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "starting model violation detection..");
		return ModelViolationDetection.performViolationCheck(files, violationRules);
	}

	/**
	 * Start a build job for the deployment of an application.
	 * 
	 * @param jobAlias
	 *            The job name/alias of the job to start, either "Build" or
	 *            "Docker"
	 * @return The path of the queue item of the started job
	 */

	public String startJenkinsJob(String jobAlias, String body) {
		String jobName = null;
		switch (jobAlias) {
		case "Build":
			jobName = buildJobName;
			break;
		case "Docker":
			jobName = dockerJobName;
			break;
		default:
			return ApplicationGenerator.deployApplication(jenkinsUrl, jenkinsJobToken, jobAlias, body);
		}

		return ApplicationGenerator.deployApplication(jenkinsUrl, jenkinsJobToken, jobName, body);
	}

	/**
	 * Get the deployment status of the last build from Jenkins
	 * 
	 * @param queueItem
	 *            The queue item path returned by the remote api of Jenkins
	 *            after a build has been started. Needed to get the status of
	 *            the build, i.e. in queue or already started
	 * @return The console text of the last build from Jenkins
	 */

	public String deployStatus(String queueItem) {
		return ApplicationGenerator.deployStatus(queueItem, jenkinsUrl);
	}

	/**
	 * Prepare a deployment of an application model, i.e. the model is copied to
	 * a temp repository which is used later during the deployment
	 * 
	 * @param serializedModel The application model to deploy
	 * @param externalDependencies Map containing GitHub URLs and version tags of external dependencies.
	 *            
	 * @return A status text
	 */
	public String prepareDeploymentApplicationModel(ArrayList<SimpleModel> serializedModel, HashMap<String, String> externalDependencies) {
		SimpleModel model = serializedModel.get(0);

		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
				"deployApplicationModel: Received model with name " + model.getName());

		// find out what type of model we got (microservice, frontend-component
		// or application)
		for (int i = 0; i < model.getAttributes().size(); i++) {
			if (model.getAttributes().get(i).getName().equals("type")) {
				String type = model.getAttributes().get(i).getValue();
				try {
					switch (type) {
					case "application":
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"deployApplicationModel: Creating application model now..");
						Application application = new Application(serializedModel, externalDependencies);

						String repositoryName = deploymentRepo;

						/**
						 * if (Generator.existsRemoteRepository(repositoryName,
						 * (BaseGitHostAdapter) this.gitAdapter)) {
						 * Generator.deleteRemoteRepository(repositoryName,
						 * (BaseGitHostAdapter) this.gitAdapter);
						 * Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
						 * "deployApplicationModel: Deleted old repository!"); }
						 */

						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
								"deployApplicationModel: Copying repository to deployment repository");
						ApplicationGenerator.createSourceCode(repositoryName, application,
								(BaseGitHostAdapter) gitAdapter, "Initialized repository for deployment", null, true); // null = do not use version tag
						Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deployApplicationModel: Copied!");
						return "done";

					default:
						return "Error: Model has to have an attribute 'type' that is either "
								+ "'microservice', 'frontend-component' or 'application'!";
					}
				} catch (ModelParseException e1) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
							"createFromModel: Model parsing exception: " + e1.getMessage());
					logger.printStackTrace(e1);
					return "Error: Parsing model failed with " + e1.getMessage();
				} catch (GitHostException e2) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
							"createFromModel: GitHub access exception: " + e2.getMessage());
					logger.printStackTrace(e2);
					return "Error: Generating code failed because of failing GitHub access in prepareDeploymentApplicationModel: "
							+ e2.getMessage();
				}
			}
		}
		return "Model has no attribute 'type'!";
	}

	/*--------------------------------------------
	 * Getter/Setter
	 * -------------------------------------------
	 */
	public GitUtility getGitUtility() {
		return gitUtility;
	}

	public GitProxy getGitProxy() {
		return gitProxy;
	}
	
	public GitHostAdapter getGitAdapter() {
		return this.gitAdapter;
	}

	public boolean isUseModelCheck() {
		return useModelCheck;
	}

	public String getGitUser() {
		return gitUser;
	}

	public String getGitUserMail() {
		return gitUserMail;
	}

	public String getUsedGitHost() {
		return usedGitHost;
	}
	
	public boolean usingGitHub() {
		if (Objects.equals(usedGitHost, "GitHub")) {
			return true;
		} else {
			return false;
		}
	}
	
	public String getWidgetHomeBaseURL() {
		return widgetHomeBaseURL;
	}

	public String getLocalGitPath() { return localGitPath; }

	public String getOidcProvider() {
		return oidcProvider;
	}
	
	
	
}
