package i5.las2peer.services.codeGenerationService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ContentParam;
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
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * 
 * CAE Code Generation Service
 * 
 * A LAS2peer service used for generating code from send models. Part of the CAE.
 * 
 */
public class CodeGenerationService extends RESTService {

  // Git service properties
  private String gitUser;
  private String gitPassword;
  private String gitOrganization;
  private String templateRepository;
  private String gitUserMail;
  private String usedGitHost;
  
  private boolean useModelCheck;
  
  //GitLab specific
  private String baseURL;
  private String token;
  
  //The git service adapter object
  private GitHostAdapter gitAdapter;

  // The git helper utility
  private GitUtility gitUtility;
  
  // jenkins properties
  private String buildJobName;
  private String dockerJobName;
  private String jenkinsUrl;
  private String jenkinsJobToken;

  private boolean useModelSynchronization;
  private final L2pLogger logger = L2pLogger.getInstance(CodeGenerationService.class.getName());
  
  public static final String DEPLOYMENT_REPO = "CAE-Deployment-Temp";
  
  // ftp properties
  private boolean pushToFs;
  private String frontendDirectory;

  public CodeGenerationService() throws GitHostException {
    // read and set properties-file values
    setFieldValues();
    
    ApplicationGenerator.pushToFs = pushToFs;
    ApplicationGenerator.frontendDirectory = frontendDirectory;
    
    // Create git adapter matching the usedGitHost
    if(Objects.equals(usedGitHost, "GitHub")) {
    	this.gitAdapter = new GitHubAdapter(gitUser,gitPassword,gitOrganization,templateRepository,gitUserMail); 
    } else if (Objects.equals(usedGitHost, "GitLab")) {
    	this.gitAdapter = new GitLabAdapter(baseURL,token,gitUser,gitPassword,gitOrganization,templateRepository,gitUserMail);
    } else {
    	// Abort
    	throw new GitHostException("No valid git provider selected");
    }
    this.gitUtility = new GitUtility(gitUser, gitPassword, gitOrganization, baseURL);
  }

  /*--------------------------------------------
   * REST endpoints (github proxy functionality)
   * -------------------------------------------
   */
  
  /**
   * Merges the development branch of the given repository with the master/gh-pages branch and
   * pushes the changes to the remote repository.
   * 
   * @param repositoryName The name of the repository to push the local changes to
   * @return HttpResponse containing the status code of the request or the result of the model
   *         violation if it fails
   */

  @SuppressWarnings("unchecked")
  @PUT
  @Path("{repositoryName}/push/")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Merge and push the commits to the remote repository",
      notes = "Push the commits to the remote repo.")
  @ApiResponses(
      value = {@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK"), @ApiResponse(
          code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error")})
  public HttpResponse pushToRemote(@PathParam("repositoryName") String repositoryName) {
    try {

      // determine which branch to merge in
      boolean isFrontend = repositoryName.startsWith("frontendComponent-");
      String masterBranchName = isFrontend ? "gh-pages" : "master";

      gitUtility.mergeIntoMasterBranch(repositoryName,  masterBranchName);
      JSONObject result = new JSONObject();
      result.put("status", "ok");
      HttpResponse r = new HttpResponse(result.toJSONString(), HttpURLConnection.HTTP_OK);
      return r;
    } catch (Exception e) {
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("Internal Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    }
  }
  
  /**
   * Store the content and traces of a file in a repository and commit it to the local repository.
   * 
   * @param repositoryName The name of the repository
   * @param content A json string containing the content of the file encoded in base64 and its file
   *        traces
   * @return HttpResponse with the status code of the request
   */

  @SuppressWarnings("unchecked")
  @PUT
  @Path("{repositoryName}/file/")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Stores the content for the given file in the local repository and commits the changes.",
      notes = "Stores the content for the given file in the local repository and commits the changes.")
  @ApiResponses(value = {@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, file found"),
      @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error"),
      @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "404, file not found")})
  public synchronized HttpResponse storeAndCommitFle(
      @PathParam("repositoryName") String repositoryName, @ContentParam String content) {
    try {
      JSONObject result = new JSONObject();

      JSONParser parser = new JSONParser();
      JSONObject contentObject = (JSONObject) parser.parse(content);
      String filePath = contentObject.get("filename").toString();
      String fileContent = contentObject.get("content").toString();
      String commitMessage = contentObject.get("commitMessage").toString();
      JSONObject traces = (JSONObject) contentObject.get("traces");

      byte[] base64decodedBytes = Base64.getDecoder().decode(fileContent);
      String decodedString = new String(base64decodedBytes, "utf-8");

      try (Git git = gitUtility.getLocalGit(repositoryName, "development");) {

        File file = new File(git.getRepository().getDirectory().getParent(), filePath);
        if (file.exists()) {

          FileWriter fW = new FileWriter(file, false);
          fW.write(decodedString);
          fW.close();
          // call model violation check of the code generation service if enabled
          if (this.useModelCheck) {
            JSONObject tracedFileObject = new JSONObject();
            tracedFileObject.put("content", fileContent);
            tracedFileObject.put("fileTraces", traces);

            HashMap<String, JSONObject> tracedFile = new HashMap<String, JSONObject>();
            tracedFile.put(filePath, tracedFileObject);

            Serializable[] payload = {getGuidances(git), tracedFile};
            JSONArray feedback = (JSONArray) this.invokeServiceMethod(
                "i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1",
                "checkModel", payload);
            if (feedback.size() > 0) {

              result.put("status", "Model violation check fails");
              result.put("feedbackItems", feedback);
              HttpResponse r = new HttpResponse(result.toJSONString(), HttpURLConnection.HTTP_OK);
              return r;
            }
          }
          // check generation id to avoid conflicts
          JSONObject currentTraceFile = getFileTraces(git, filePath);
          if (currentTraceFile != null) {
            String generationId = (String) currentTraceFile.get("generationId");
            String payloadGenerationId = (String) traces.get("generationId");
            if (!generationId.equals(payloadGenerationId)) {
              HttpResponse r = new HttpResponse("Commit rejected. Wrong generation id",
                  HttpURLConnection.HTTP_CONFLICT);
              return r;
            }
          }

          File traceFile =
              new File(git.getRepository().getDirectory().getParent(), getTraceFileName(filePath));

          fW = new FileWriter(traceFile, false);
          fW.write(traces.toJSONString());
          fW.close();

          git.add().addFilepattern(filePath).addFilepattern(getTraceFileName(filePath)).call();
          git.commit().setAuthor(gitUser, gitUserMail).setMessage(commitMessage).call();

          result.put("status", "OK, file stored and commited");
          HttpResponse r = new HttpResponse(result.toJSONString(), HttpURLConnection.HTTP_OK);
          return r;
        } else {
          HttpResponse r = new HttpResponse("404", HttpURLConnection.HTTP_NOT_FOUND);
          return r;
        }

      }

    } catch (Exception e) {
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("Internal Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    }
  }
  
  /**
   * Calculate and returns the file name and segment id for a given model id.
   * 
   * @param repositoryName The name of the repository
   * @param modelId The id of the model.
   * @return HttpResponse with the status code of the request and the file name and segment id of
   *         the model
   */

  @SuppressWarnings("unchecked")
  @GET
  @Path("{repositoryName}/segment/{modelId}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Returns the segment id and filename for the given model id.",
      notes = "Returns the segment id and filename.")
  @ApiResponses(value = {
      @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, segment found"),
      @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error"),
      @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "404, segment not found")})
  public HttpResponse getSegmentOfModelId(@PathParam("repositoryName") String repositoryName,
      @PathParam("modelId") String modelId) {

    try (Git git = gitUtility.getLocalGit(repositoryName, "development");) {

      JSONObject resultObject = new JSONObject();

      JSONObject traceModel = getTraceModel(git);
      JSONObject modelsToFiles = (JSONObject) traceModel.get("modelsToFile");

      if (modelsToFiles != null) {
        if (modelsToFiles.containsKey(modelId)) {
          JSONArray fileList = (JSONArray) ((JSONObject) modelsToFiles.get(modelId)).get("files");
          String fileName = (String) fileList.get(0);
          JSONObject fileTraceModel = getFileTraces(git, fileName);
          JSONObject fileTraces = (JSONObject) fileTraceModel.get("traces");
          JSONArray segments = (JSONArray) ((JSONObject) fileTraces.get(modelId)).get("segments");
          String segmentId = (String) segments.get(0);

          resultObject.put("fileName", fileName);
          resultObject.put("segmentId", segmentId);

        } else {
          throw new FileNotFoundException();
        }
      } else {
        throw new Exception("Error: modelsToFiles mapping not found!");
      }

      HttpResponse r = new HttpResponse(resultObject.toJSONString(), HttpURLConnection.HTTP_OK);
      return r;
    } catch (FileNotFoundException fileNotFoundException) {
      HttpResponse r = new HttpResponse("Not found", HttpURLConnection.HTTP_NOT_FOUND);
      return r;
    } catch (Exception e) {
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("Internal Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    }

  }

  /**
   * Get the files needed for the live preview widget collected in one response
   * 
   * @param repositoryName The name of the repository
   * @return HttpResponse containing the status code of the request and the content of the needed
   *         files for the live preview widget encoded in base64 if everything was fine.
   */

  @SuppressWarnings("unchecked")
  @GET
  @Path("{repositoryName}/livePreviewFiles/")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Returns all needed files for the live preview widget of the given repository encoded in Base64.",
      notes = "Returns all needed files for the live preview widget.")
  @ApiResponses(value = {@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, file found"),
      @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error"),
      @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "404, file not found")})
  public HttpResponse getLivePreviewFiles(@PathParam("repositoryName") String repositoryName) {
    if (repositoryName.startsWith("frontendComponent")
        && gitUtility.existsLocalRepository(repositoryName)) {

      try (Git git = gitUtility.getLocalGit(repositoryName)) {
        if (git.getRepository().getBranch().equals("development")) {

          JSONObject result = new JSONObject();
          JSONArray fileList = new JSONArray();

          String[] neededFileNames = {"widget.xml", "js/applicationScript.js"};

          for (String fileName : neededFileNames) {
            String content = gitUtility.getFileContent(git.getRepository(), fileName);
            String contentBase64 = Base64.getEncoder().encodeToString(content.getBytes("utf-8"));

            JSONObject fileObject = new JSONObject();
            fileObject.put("fileName", fileName);
            fileObject.put("content", contentBase64);
            fileList.add(fileObject);
          }

          result.put("files", fileList);
          HttpResponse r = new HttpResponse(result.toJSONString(), HttpURLConnection.HTTP_OK);
          return r;
        } else {
          HttpResponse r = new HttpResponse(repositoryName + " currently unavailable",
              HttpURLConnection.HTTP_UNAVAILABLE);
          return r;
        }
      } catch (FileNotFoundException e) {
        logger.info(repositoryName + " not found");
        HttpResponse r =
            new HttpResponse(repositoryName + " not found", HttpURLConnection.HTTP_NOT_FOUND);
        return r;
      } catch (Exception e) {
        logger.printStackTrace(e);
        HttpResponse r = new HttpResponse("Internal Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
        return r;
      }
    } else {
      HttpResponse r = new HttpResponse("Only frontend components are supported",
          HttpURLConnection.HTTP_NOT_ACCEPTABLE);
      return r;
    }

  }

  /**
   * Returns the content encoded in base64 of a file in a repository
   * 
   * @param repositoryName The name of the repository
   * @param fileName The absolute path of the file
   * @return HttpResponse containing the status code of the request and the content of the file
   *         encoded in base64 if everything was fine.
   */

  @SuppressWarnings("unchecked")
  @GET
  @Path("{repositoryName}/file/")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Returns the content of the given file within the specified repository encoded in Base64.",
      notes = "Returns the content of the given file within the specified repository.")
  @ApiResponses(value = {@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, file found"),
      @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error"),
      @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "404, file not found")})
  public HttpResponse getFileInRepository(@PathParam("repositoryName") String repositoryName,
      @QueryParam("file") String fileName) {

    try (Git git = gitUtility.getLocalGit(repositoryName, "development")) {

      JSONObject fileTraces = getFileTraces(git, fileName);

      String content = gitUtility.getFileContent(git.getRepository(), fileName);
      String contentBase64 = Base64.getEncoder().encodeToString(content.getBytes("utf-8"));

      JSONObject resultObject = new JSONObject();
      resultObject.put("content", contentBase64);

      // add file traces to the json response if one exists
      if (fileTraces != null) {
        resultObject.put("traceModel", fileTraces);
      }

      HttpResponse r = new HttpResponse(resultObject.toJSONString(), HttpURLConnection.HTTP_OK);
      return r;
    } catch (FileNotFoundException fileNotFoundException) {
      HttpResponse r = new HttpResponse("Not found", HttpURLConnection.HTTP_NOT_FOUND);
      return r;
    } catch (Exception e) {
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("Internal Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    }

  }

  
  /* ------------------------------------------------------------------------------------------
   * Main CAE methods
   *-------------------------------------------------------------------------------------------
   */
  
  /**
   * List all files of a folder of a repository.
   * 
   * @param repositoryName the name of the repository
   * @param path the path of the folder whose files should be listed
   * @return HttpResponse containing the files of the given repository as a json string
   * 
   */

  @SuppressWarnings("unchecked")
  @GET
  @Path("/{repoName}/files")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Lists all files of a folder of the given repository.",
      notes = "Lists all files of the given repository.")
  @ApiResponses(value = {
      @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, repository of the model found"),
      @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR,
          message = "Internal server error")})
  public HttpResponse listFilesInRepository(@PathParam("repoName") String repositoryName,
      @QueryParam("path") String path) {

    if (path == null) {
      path = "";
    } else if (path.equals("/")) {
      path = "";
    }

    JSONObject jsonResponse = new JSONObject();
    JSONArray files = new JSONArray();
    jsonResponse.put("files", files);
    try (Git git = gitUtility.getLocalGit(repositoryName, "development");) {

      JSONArray tracedFiles = (JSONArray) getTraceModel(git).get("tracedFiles");
      TreeWalk treeWalk = gitUtility.getRepositoryTreeWalk(git.getRepository());

      if (path.isEmpty()) {
        while (treeWalk.next()) {
          addFile(treeWalk, files, tracedFiles);
        }
      } else {

        PathFilter filter = PathFilter.create(path);
        boolean folderFound = false;
        treeWalk.setFilter(filter);

        while (treeWalk.next()) {

          if (!folderFound && treeWalk.isSubtree()) {
            treeWalk.enterSubtree();
          }
          if (treeWalk.getPathString().equals(path)) {
            folderFound = true;
            continue;
          }
          if (folderFound) {
            addFiletoFileList(treeWalk, files, tracedFiles, path);
          }
        }
      }

    } catch (Exception e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "getModelFiles: exception fetching files: " + e);
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("IO error!", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    }

    HttpResponse r =
        new HttpResponse(jsonResponse.toString().replace("\\", ""), HttpURLConnection.HTTP_OK);
    return r;
  }

  /**
   * Deletes a local repository
   * 
   * @param repositoryName The repository to delete
   * @return HttpResponse containing a status code
   */

  @GET
  @Path("/{repoName}/delete")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Deletes the local repository of the given repository name",
      notes = "Deletes the local repository.")
  @ApiResponses(value = {
      @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, local repository deleted"),
      @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR,
          message = "Internal server error")})
  public HttpResponse deleteLocalRepositoryREST(@PathParam("repoName") String repositoryName) {
    /*try {
      FileUtils.deleteDirectory(new File(repositoryName));
    } catch (IOException e) {
      e.printStackTrace();
      logger.printStackTrace(e);
      return new HttpResponse(e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
    }
    return new HttpResponse("Ok", HttpURLConnection.HTTP_OK);
    */
	  String result = deleteLocalRepository(repositoryName);
	  if(Objects.equals(result,"done")) {
		  return new HttpResponse("Ok", HttpURLConnection.HTTP_OK);
	  } else {
		  return new HttpResponse(result, HttpURLConnection.HTTP_INTERNAL_ERROR);
	  }
  }

  
  /*--------------------------------------------
   * Git Host Proxy helper methods
   * -------------------------------------------
   */
  
  private String getTraceFileName(String fileName) {
	  return "traces/" + fileName + ".traces";
  }
  
  /**
   * A private helper method to add the current file or folder of a tree walk to a json array.
   * 
   * @param tw The tree walk which current file/folder should be added to the json array
   * @param files The json array the current file/folder should be added
   * @param path The path of the current file
   */

  @SuppressWarnings("unchecked")
  private void addFiletoFileList(TreeWalk tw, JSONArray fileList, JSONArray tracedFiles,
      String path) {
    String name = tw.getPathString();

    JSONObject fileObject = new JSONObject();

    if (tw.isSubtree()) {
      if (name.equals("traces")) {
        return;
      }
      fileObject.put("type", "folder");
    } else if (!tracedFiles.contains(name)) {
      return;
    } else {
      fileObject.put("type", "file");
    }
    fileObject.put("path", name);
    fileList.add(fileObject);
  }

  /**
   * A private helper method to add the current file or folder of a tree walk to a json array
   * 
   * @param tw The tree walk which current file/folder should be added to the json array
   * @param files The json array the current file/folder should be added
   */

  private void addFile(TreeWalk tw, JSONArray files, JSONArray tracedFiles) {
    addFiletoFileList(tw, files, tracedFiles, "");
  }
  
  /**
   * Get the traces for a file
   * 
   * @param git The git object of the repository of the file
   * @param fullFileName The file name whose traces should be returned. Must be the full file name,
   *        i.e. with full file path
   * @return A JSONObject of the file traces or null if the file does not have any traces
   * @throws Exception Thrown if something went wrong
   */
  @SuppressWarnings("unchecked")
  private JSONObject getFileTraces(Git git, String fullFileName) throws Exception {
    JSONObject traceModel = getTraceModel(git);
    JSONArray tracedFiles = (JSONArray) traceModel.get("tracedFiles");
    JSONObject fileTraces = null;

    if (tracedFiles.contains(fullFileName)) {

      try {
        String content =
            gitUtility.getFileContent(git.getRepository(), getTraceFileName(fullFileName));
        JSONParser parser = new JSONParser();
        fileTraces = (JSONObject) parser.parse(content);
        fileTraces.put("generationId", traceModel.get("id"));
      } catch (GitHelperException e) {
        logger.printStackTrace(e);
        //TODO: Handle exception better
      }
    }
    return fileTraces;
  }
  
  @SuppressWarnings("unchecked")
  private JSONObject getGuidances(Git git) {

    JSONObject guidances = new JSONObject();
    // add empty json array
    guidances.put("guidances", new JSONArray());

    JSONParser parser = new JSONParser();
    String content = "traces/guidances.json";
    if (content.length() > 0) {
      try {
        guidances = (JSONObject) parser.parse(gitUtility.getFileContent(git.getRepository(), content));
      } catch (Exception e) {
        logger.printStackTrace(e);
      }
    }
    return guidances;
  }
  
  /**
   * Get the global trace model of a component
   * 
   * @param git The git object of the repository
   * @return A JSONObject of the trace model.
 * @throws  
   * @throws Exception Thrown if something went wrong.
   */

  @SuppressWarnings("unchecked")
  private JSONObject getTraceModel(Git git) {
    JSONObject result = new JSONObject();
    JSONArray tracedFiles = new JSONArray();
    result.put("tracedFiles", tracedFiles);
    try {
      String jsonCode = gitUtility.getFileContent(git.getRepository(), "traces/tracedFiles.json");
      JSONParser parser = new JSONParser();
      result = (JSONObject) parser.parse(jsonCode);
    } catch (GitHelperException e) {
      // if a global trace model is not found, the error should be logged
      logger.printStackTrace(e);
    } catch (ParseException e) {
    	//TODO: Handle exception
    }

    return result;
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
  public String createFromModel(boolean forcePush, Serializable... serializedModel) {

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
                  (BaseGitHostAdapter) gitAdapter, forcePush);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "createFromModel: Created!");
              return "done";  
              
            case "frontend-component":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating frontend component model now..");
              FrontendComponent frontendComponent = new FrontendComponent(model);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating frontend component source code now..");
              FrontendComponentGenerator.createSourceCode(frontendComponent, (BaseGitHostAdapter) gitAdapter, forcePush);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "createFromModel: Created!");
              return "done";
              
            case "application":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating application model now..");
              Application application = new Application(serializedModel);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "createFromModel: Creating application source code now..");
              ApplicationGenerator.createSourceCode(application, (BaseGitHostAdapter) gitAdapter);
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
        } catch (GitHostException e2) {
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
  
  public String createFromModel(Serializable... serializedModel) {
	  return createFromModel(false, serializedModel);
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
              Generator.deleteRemoteRepository(modelName, (BaseGitHostAdapter)this.gitAdapter);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
              return "done";
            case "frontend-component":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "deleteRepositoryOfModel: Deleting frontend-component repository now..");
              modelName = "frontendComponent-" + modelName.replace(" ", "-");
              Generator.deleteRemoteRepository(modelName, (BaseGitHostAdapter)this.gitAdapter);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
              return "done";
            case "application":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "deleteRepositoryOfModel: Deleting application repository now..");
              modelName = "application-" + modelName.replace(" ", "-");
              Generator.deleteRemoteRepository(modelName, (BaseGitHostAdapter)this.gitAdapter);
              L2pLogger.logEvent(Event.SERVICE_MESSAGE, "deleteRepositoryOfModel: Deleted!");
              return "done";
            default:
              return "Error: Model has to have an attribute 'type' that is either "
                  + "'microservice', 'frontend-component' or 'application'!";
          }
        } catch (GitHostException e) {
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
    L2pLogger.logEvent(Event.SERVICE_MESSAGE,"updateRepositoryOfModel: Received model with name " + modelName);

    // old model only used for microservice and frontend components
    SimpleModel oldModel = null;

    if (serializedModel.length > 1) {
      oldModel = (SimpleModel) serializedModel[1];
      L2pLogger.logEvent(Event.SERVICE_MESSAGE,"updateRepositoryOfModel: Received old model with name " + oldModel.getName());
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
                  && MicroserviceSynchronization.existsRemoteRepositoryForModel(microservice, (BaseGitHostAdapter) gitAdapter)) {
                Microservice oldMicroservice = new Microservice(oldModel);
                L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Using model sync: Old model:" + oldModel.getName());
                L2pLogger.logEvent(Event.SERVICE_MESSAGE, "updateRepositoryOfModel: Calling synchronizeSourceCode now..");

                MicroserviceSynchronization.synchronizeSourceCode(microservice, oldMicroservice,
                    this.getTracedFiles(MicroserviceGenerator.getRepositoryName(microservice)),(BaseGitHostAdapter) gitAdapter,this);

                L2pLogger.logEvent(Event.SERVICE_MESSAGE, "updateRepositoryOfModel: Synchronized!");
                return "done";
                
              } else {

            if(gitAdapter instanceof GitLabAdapter) {
            	// Use pseudo-update to circumvent gitlab deletion/creation problem
            	return pseudoUpdateRepositoryOfModel(serializedModel); 
            	
            } else {
                if (useModelSynchronization) {
                  // inform the GitHubProxy service that we may have deleted a remote repository
                  // it will then delete the local repository
                	L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Using model sync: Deleting local repo");
                  deleteReturnMessage = this.deleteLocalRepository(MicroserviceGenerator.getRepositoryName(microservice));
                  if (!deleteReturnMessage.equals("done")) {
                    return deleteReturnMessage; // error happened
                  }
                }
                
                if (Generator.existsRemoteRepository(MicroserviceGenerator.getRepositoryName(microservice), (BaseGitHostAdapter) gitAdapter)) {
                	L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Using model sync: Remote exits. Calling delete repo of model");
                  deleteReturnMessage = deleteRepositoryOfModel(serializedModel);
                  
                  if (!deleteReturnMessage.equals("done")) {
                    return deleteReturnMessage; // error happened
                  }
                }

                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "updateRepositoryOfModel: Calling createFromModel now..");

                return createFromModel(serializedModel);
              }
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
                      frontendComponent, (BaseGitHostAdapter) gitAdapter)) {
                FrontendComponent oldFrontendComponent = new FrontendComponent(oldModel);

                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "updateRepositoryOfModel: Calling synchronizeSourceCode now..");

                FrontendComponentSynchronization.synchronizeSourceCode(frontendComponent,
                    oldFrontendComponent,
                    this.getTracedFiles(
                        FrontendComponentGenerator.getRepositoryName(frontendComponent)),(BaseGitHostAdapter) gitAdapter, this);

                L2pLogger.logEvent(Event.SERVICE_MESSAGE, "updateRepositoryOfModel: Synchronized!");
                return "done";
              } else {
                if(gitAdapter instanceof GitLabAdapter) {
                	return pseudoUpdateRepositoryOfModel(serializedModel);
                } else {
                if (useModelSynchronization) {                	
                  // inform the GitHubProxy service that we may have deleted a remote repository
                  // it will then delete the local repository
                	L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                            "updateRepositoryOfModel: Calling delete (old) repository method now..");
                  deleteReturnMessage = this.deleteLocalRepository(
                      FrontendComponentGenerator.getRepositoryName(frontendComponent));
                  if (!deleteReturnMessage.equals("done")) {
                    return deleteReturnMessage; // error happened
                  }
                }
                if (Generator.existsRemoteRepository(
                    FrontendComponentGenerator.getRepositoryName(frontendComponent),(BaseGitHostAdapter) gitAdapter)) {
                  deleteReturnMessage = deleteRepositoryOfModel(serializedModel);
                  if (!deleteReturnMessage.equals("done")) {
                    return deleteReturnMessage; // error happened
                  }
                }

                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "updateRepositoryOfModel: Calling createFromModel now..");
                return createFromModel(serializedModel);
                }

              }


            // TODO Check update of applications
              
            case "application":
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "updateRepositoryOfModel: Checking application model now..");
              // check first if model can be constructed
              // (in case of an invalid model, keep the old repository)
              //new Application(serializedModel);
              /*
              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "updateRepositoryOfModel: Calling delete (old) repository method now..");
              deleteReturnMessage = deleteRepositoryOfModel(serializedModel);
              if (!deleteReturnMessage.equals("done")) {
                return deleteReturnMessage; // error happened
              }
              L2pLogger.logEvent(Event.SERVICE_ERROR,
                  "updateRepositoryOfModel: Calling createFromModel now..");
              return createFromModel(serializedModel);
              */
              return pseudoUpdateRepositoryOfModel(serializedModel);
              
            default:
              return "Error: Model has to have an attribute 'type' that is either "
                  + "'microservice', 'frontend-component' or 'application'!";
          }
        } catch (ModelParseException e) {
          L2pLogger.logEvent(Event.SERVICE_ERROR,
              "updateRepositoryOfModel: Model Parsing exception: " + e.getMessage());
          logger.printStackTrace(e);
          return "Error: Parsing model failed with " + e.getMessage();
        } catch (GitHostException e2) {
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

  public String pseudoUpdateRepositoryOfModel(Serializable... serializedModel) {
	  SimpleModel model = (SimpleModel) serializedModel[0];
	  String modelName = model.getName();
	  // force push
	  //TODO: add more cleanup here later
	  // first get repo names / etc. 
	  return createFromModel(true, serializedModel);
  }
  
  /**
   * Deletes a local repository
   * 
   * @param repositoryName The name of the repository to be deleted
   * @return a status string
   */

  private String deleteLocalRepository(String repositoryName) {
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
          "i5.las2peer.services.gitHubProxyService.GitHubProxyService@0.2", "getAllTracedFiles",
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

              String repositoryName = DEPLOYMENT_REPO;

              /**if (Generator.existsRemoteRepository(repositoryName, (BaseGitHostAdapter) this.gitAdapter)) {
                Generator.deleteRemoteRepository(repositoryName, (BaseGitHostAdapter) this.gitAdapter);
                L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                    "deployApplicationModel: Deleted old repository!");
              }*/

              L2pLogger.logEvent(Event.SERVICE_MESSAGE,
                  "deployApplicationModel: Copying repository to deployment repository");
              ApplicationGenerator.createSourceCode(repositoryName, application, (BaseGitHostAdapter) gitAdapter, true);
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
        } catch (GitHostException e2) {
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "createFromModel: GitHub access exception: " + e2.getMessage());
          logger.printStackTrace(e2);
          return "Error: Generating code failed because of failing GitHub access in prepareDeploymentApplicationModel: " + e2.getMessage();
        }
      }
    }
    return "Model has no attribute 'type'!";
  }

}
