package i5.las2peer.services.codeGenerationService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import i5.las2peer.api.Context;
import i5.las2peer.api.ServiceException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHelperException;
import i5.las2peer.services.codeGenerationService.utilities.GitUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/")
public class RESTResources {
	private final CodeGenerationService service = (CodeGenerationService) Context.getCurrent().getService();
	protected GitUtility gitUtility;
	private boolean useModelCheck;
	private String gitUser;
	private String gitUserMail;
	private GitProxy gitProxy;

	public RESTResources() {
		gitUtility = service.getGitUtility();
		useModelCheck = service.isUseModelCheck();
		gitUser = service.getGitUser();
		gitUserMail = service.getGitUserMail();
		gitProxy = service.getGitProxy();
	}

	@GET
	@Path("test/")
	public Response baseTest() {
		return Response.ok("CodeGen service").build();
	}

	/*--------------------------------------------
	 * REST endpoints (github proxy functionality)
	 * -------------------------------------------
	 */
	
	/**
	 * Tags the commit with the given sha identifier with the given tag and pushes the new tag.
	 * @param repositoryName Name of the repository, where a commit should be tagged.
	 * @param jsonInput JSON object containing the "tag" and "commitSha".
	 * @return
	 */
	@POST
	@Path("{repositoryName}/tags")
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Pushes the given tag to the given commit.")
	@ApiResponses(value = {@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK"),
			               @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error")})
	public Response addTag(@PathParam("repositoryName") String repositoryName, String jsonInput) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "tags: trying to add tag to repository with name: " + repositoryName);
		
		
		JSONObject json = (JSONObject) JSONValue.parse(jsonInput);
		String versionTag = (String) json.get("tag");
		String commitSha = (String) json.get("commitSha");
		int versionedModelId = ((Long) json.get("versionedModelId")).intValue();
		
		Repository repository = null;
		RevWalk revWalk = null;
		
		String masterBranch = repositoryName.startsWith("frontend") ? "gh-pages" : "master";
		
		try (Git git = gitUtility.getLocalGit(repositoryName, masterBranch)) {
			RefSpec specTags = new RefSpec("refs/tags/" + versionTag + ":refs/tags/" + versionTag);
			
			// use gitAdapter from service
			BaseGitHostAdapter gitAdapter = (BaseGitHostAdapter) service.getGitAdapter();
			
			repository = git.getRepository();
			
			CredentialsProvider credentialsProvider =
			        new UsernamePasswordCredentialsProvider(gitAdapter.getGitUser(), gitAdapter.getGitPassword());
			
			// get the commit by the given commit sha identifier
			ObjectId commitId = repository.resolve(commitSha);
			revWalk = new RevWalk(repository);
		    RevCommit commit = revWalk.parseCommit(commitId);
		    
		    StoredConfig config = git.getRepository().getConfig();
		    RemoteConfig remoteConfig = new RemoteConfig(config, "Remote");
		    remoteConfig.addURI(new URIish(gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + repositoryName + ".git"));
	        remoteConfig.update(config);
			
	        // set tag
			Git.wrap(repository).tag().setObjectId(commit).setName(versionTag).call();
			// push tag
			Git.wrap(repository).push().setForce(true).setRemote("Remote").setPushTags().setCredentialsProvider(credentialsProvider)
	        .setRefSpecs(specTags).call();
			
			// also store commit into database
			String response = (String) Context.getCurrent().invoke(
					"i5.las2peer.services.modelPersistenceService.ModelPersistenceService@0.1", "addTagToCommit",
					new Serializable[]{commitSha, versionedModelId, versionTag});
			
			if(response.equals("done")) return Response.ok().build();
			else return Response.serverError().build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().entity(e.getMessage()).build();
		} finally {
		    if(repository != null) repository.close();
		    if(revWalk != null) revWalk.close();
		}
	}

	/**
	 * Merges the development branch of the given repository with the
	 * master/gh-pages branch and pushes the changes to the remote repository.
	 * 
	 * @param repositoryName
	 *            The name of the repository to push the local changes to
	 * @return HttpResponse containing the status code of the request or the
	 *         result of the model violation if it fails
	 * @throws ServiceException thrown incase of error in service
	 */

	@SuppressWarnings("unchecked")
	@PUT
	@Path("{repositoryName}/push/")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Merge and push the commits to the remote repository", notes = "Push the commits to the remote repo.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response pushToRemote(@PathParam("repositoryName") String repositoryName) throws ServiceException {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "push: trying to push repository with name: " + repositoryName);
		
		try {
			// determine which branch to merge in
			boolean isFrontend = repositoryName.startsWith("frontendComponent-");
			String masterBranchName = isFrontend ? "gh-pages" : "master";

			gitUtility.mergeIntoMasterBranch(repositoryName, masterBranchName, null);
			JSONObject result = new JSONObject();
			result.put("status", "ok");
			return Response.ok(result.toJSONString()).build();
		} catch (Exception e) {
			service.getLogger().log(Level.FINER, e.getMessage());
			throw new InternalServerErrorException("Internal Error");
		}
	}

	/**
	 * Store the content and traces of a file in a repository and commit it to
	 * the local repository.
	 * 
	 * @param repositoryName
	 *            The name of the repository
	 * @param content
	 *            A json string containing the content of the file encoded in
	 *            base64 and its file traces
	 * @return HttpResponse with the status code of the request
	 * @throws ServiceException thrown incase of error in service 
	 */

	@SuppressWarnings("unchecked")
	@PUT
	@Path("{repositoryName}/file/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Stores the content for the given file in the local repository and commits the changes.", notes = "Stores the content for the given file in the local repository and commits the changes.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, file found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "404, file not found") })
	public synchronized Response storeAndCommitFle(@PathParam("repositoryName") String repositoryName, String content) throws ServiceException {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "PUT {repositoryName}/file called with respositoryName: " + repositoryName);
		
		try {
			JSONObject result = new JSONObject();

			JSONParser parser = new JSONParser();
			JSONObject contentObject = (JSONObject) parser.parse(content);
			String filePath = contentObject.get("filename").toString();
			String fileContent = contentObject.get("content").toString();
			String commitMessage = contentObject.get("commitMessage").toString();
			int versionedModelId = Integer.parseInt(contentObject.get("versionedModelId").toString());
			JSONObject traces = (JSONObject) contentObject.get("traces");

			byte[] base64decodedBytes = Base64.getDecoder().decode(fileContent);
			String decodedString = new String(base64decodedBytes, "utf-8");

			try (Git git = gitUtility.getLocalGit(repositoryName, "development");) {

				File file = new File(git.getRepository().getDirectory().getParent(), filePath);
				if (file.exists()) {

					FileWriter fW = new FileWriter(file, false);
					fW.write(decodedString);
					fW.close();
					// call model violation check of the code generation service
					// if enabled
					if (useModelCheck) {
						JSONObject tracedFileObject = new JSONObject();
						tracedFileObject.put("content", fileContent);
						tracedFileObject.put("fileTraces", traces);

						HashMap<String, JSONObject> tracedFile = new HashMap<String, JSONObject>();
						tracedFile.put(filePath, tracedFileObject);

						Serializable[] payload = { gitProxy.getGuidances(git), tracedFile };
						JSONArray feedback = (JSONArray) Context.getCurrent().invoke(
								"i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1", "checkModel",
								payload);
						if (feedback.size() > 0) {
							result.put("status", "Model violation check fails");
							result.put("feedbackItems", feedback);
							return Response.ok(result.toJSONString()).build();
						}
					}
					// check generation id to avoid conflicts
					JSONObject currentTraceFile = gitProxy.getFileTraces(git, filePath);
					if (currentTraceFile != null) {
						String generationId = (String) currentTraceFile.get("generationId");
						String payloadGenerationId = (String) traces.get("generationId");
						if (!generationId.equals(payloadGenerationId)) {
							return Response.status(409).entity("Commit rejected. Wrong generation id").build();
						}
					}

					File traceFile = new File(git.getRepository().getDirectory().getParent(),
							gitProxy.getTraceFileName(filePath));

					fW = new FileWriter(traceFile, false);
					fW.write(traces.toJSONString());
					fW.close();
					git.add().addFilepattern(filePath).addFilepattern(gitProxy.getTraceFileName(filePath)).call();
					RevCommit commit = git.commit().setAuthor(gitUser, gitUserMail).setMessage(commitMessage).call();
					String commitSha = commit.getId().getName();
					
					// call Model Persistence Service to store the auto commit
					String response = (String) Context.getCurrent().invoke(
							"i5.las2peer.services.modelPersistenceService.ModelPersistenceService@0.1", "addAutoCommitToVersionedModel",
							new Serializable[]{commitSha, commitMessage, versionedModelId});
					
					if(response.equals("error")) {
						throw new InternalServerErrorException();
					}

					result.put("status", "OK, file stored and commited");
					return Response.ok(result.toJSONString()).build();
				} else {
					throw new NotFoundException();
				}

			}

		} catch (Exception e) {
			service.getLogger().log(Level.FINER, e.getMessage());
			throw new InternalServerErrorException(e.getMessage());
		}
	}

	/**
	 * Calculate and returns the file name and segment id for a given model id.
	 * 
	 * @param repositoryName
	 *            The name of the repository
	 * @param modelId
	 *            The id of the model.
	 * @return HttpResponse with the status code of the request and the file
	 *         name and segment id of the model
	 * @throws ServiceException 
	 * 			Thrown if something goes wrong with the Github Service
	 */

	@SuppressWarnings("unchecked")
	@GET
	@Path("{repositoryName}/segment/{modelId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Returns the segment id and filename for the given model id.", notes = "Returns the segment id and filename.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, segment found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "404, segment not found") })
	public Response getSegmentOfModelId(@PathParam("repositoryName") String repositoryName,
			@PathParam("modelId") String modelId) throws ServiceException {

		try (Git git = gitUtility.getLocalGit(repositoryName, "development");) {

			JSONObject resultObject = new JSONObject();

			JSONObject traceModel = gitProxy.getTraceModel(git);
			JSONObject modelsToFiles = (JSONObject) traceModel.get("modelsToFile");

			if (modelsToFiles != null) {
				if (modelsToFiles.containsKey(modelId)) {
					JSONArray fileList = (JSONArray) ((JSONObject) modelsToFiles.get(modelId)).get("files");
					String fileName = (String) fileList.get(0);
					JSONObject fileTraceModel = gitProxy.getFileTraces(git, fileName);
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

			return Response.ok(resultObject.toJSONString()).build();
		} catch (FileNotFoundException fileNotFoundException) {
			throw new NotFoundException();
		} catch (Exception e) {
			service.getLogger().log(Level.FINER, e.getMessage());
			throw new InternalServerErrorException();
		}

	}

	/**
	 * Get the files needed for the live preview widget collected in one
	 * response
	 * 
	 * @param repositoryName
	 *            The name of the repository
	 * @return HttpResponse containing the status code of the request and the
	 *         content of the needed files for the live preview widget encoded
	 *         in base64 if everything was fine.
	 * @throws ServiceException 
	 * 			Thrown if something goes wrong with the Github Service
	 */

	@SuppressWarnings("unchecked")
	@GET
	@Path("{repositoryName}/livePreviewFiles/")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Returns all needed files for the live preview widget of the given repository encoded in Base64.", notes = "Returns all needed files for the live preview widget.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, file found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "404, file not found") })
	public Response getLivePreviewFiles(@PathParam("repositoryName") String repositoryName) throws ServiceException {
		if (repositoryName.startsWith("frontendComponent")) {

			try (Git git = gitUtility.getLocalGit(repositoryName, "development")) {
				if (git.getRepository().getBranch().equals("development")) {

					JSONObject result = new JSONObject();
					JSONArray fileList = new JSONArray();

					String[] neededFileNames = { "index.html", "js/applicationScript.js" };

					for (String fileName : neededFileNames) {
						String content = gitUtility.getFileContent(git.getRepository(), fileName);
						String contentBase64 = Base64.getEncoder().encodeToString(content.getBytes("utf-8"));

						JSONObject fileObject = new JSONObject();
						fileObject.put("fileName", fileName);
						fileObject.put("content", contentBase64);
						fileList.add(fileObject);
					}

					result.put("files", fileList);
					return Response.ok(result.toJSONString()).build();
				} else {
					throw new ServiceUnavailableException(repositoryName + " currently unavailable");
				}
			} catch (GitHelperException e) {
				File repo = GitUtility.getRepositoryPath(repositoryName);
				// repo might got cloned, but is empty
				// so delete it
				deleteFolder(repo);
				throw new InternalServerErrorException();
			} catch (FileNotFoundException e) {
				service.getLogger().info(repositoryName + " not found");
				throw new NotFoundException(repositoryName + " not found");
			} catch (Exception e) {
				e.printStackTrace();
				service.getLogger().log(Level.FINER, e.getMessage());
				throw new InternalServerErrorException();
			}
		} else {
			throw new NotAcceptableException("Only frontend components are supported");
		}

	}
	
	public static void deleteFolder(File folder) {
		// get all files in folder
	    File[] files = folder.listFiles();
	    if(files != null) {
	        for(File file : files) {
	            if(file.isDirectory()) {
	                deleteFolder(file);
	            } else {
	                file.delete();
	            }
	        }
	    }
	    folder.delete();
	}

	/**
	 * Returns the content encoded in base64 of a file in a repository
	 * 
	 * @param repositoryName
	 *            The name of the repository
	 * @param fileName
	 *            The absolute path of the file
	 * @return HttpResponse containing the status code of the request and the
	 *         content of the file encoded in base64 if everything was fine.
	 * @throws ServiceException 
	 * 			Thrown if something goes wrong with the Github Service 
	 */

	@SuppressWarnings("unchecked")
	@GET
	@Path("{repositoryName}/file/")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Returns the content of the given file within the specified repository encoded in Base64.", notes = "Returns the content of the given file within the specified repository.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, file found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "404, file not found") })
	public Response getFileInRepository(@PathParam("repositoryName") String repositoryName,
			@QueryParam("file") String fileName) throws ServiceException {

		try (Git git = gitUtility.getLocalGit(repositoryName, "development")) {

			JSONObject fileTraces = gitProxy.getFileTraces(git, fileName);

			String content = gitUtility.getFileContent(git.getRepository(), fileName);
			String contentBase64 = Base64.getEncoder().encodeToString(content.getBytes("utf-8"));

			JSONObject resultObject = new JSONObject();
			resultObject.put("content", contentBase64);

			// add file traces to the json response if one exists
			if (fileTraces != null) {
				resultObject.put("traceModel", fileTraces);
			}
			return Response.ok(resultObject.toJSONString()).build();
		} catch (FileNotFoundException fileNotFoundException) {
			throw new NotFoundException();
		} catch (Exception e) {
			service.getLogger().log(Level.FINER, e.getMessage());
			throw new InternalServerErrorException();
		}

	}

	/*
	 * -------------------------------------------------------------------------
	 * ----------------- Main CAE methods
	 * -------------------------------------------------------------------------
	 * ------------------
	 */

	/**
	 * List all files of a folder of a repository.
	 * 
	 * @param repositoryName
	 *            the name of the repository
	 * @param path
	 *            the path of the folder whose files should be listed
	 * @return HttpResponse containing the files of the given repository as a
	 *         json string
	 *@throws ServiceException 
	 * 			Thrown if something goes wrong with the Github Service 
	 * 
	 */

	@SuppressWarnings("unchecked")
	@GET
	@Path("/{repoName}/files")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Lists all files of a folder of the given repository.", notes = "Lists all files of the given repository.")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, repository of the model found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response listFilesInRepository(@PathParam("repoName") String repositoryName,
			@QueryParam("path") String path) throws ServiceException {

		if (path == null) {
			path = "";
		} else if (path.equals("/")) {
			path = "";
		}

		JSONObject jsonResponse = new JSONObject();
		JSONArray files = new JSONArray();
		jsonResponse.put("files", files);
		try (Git git = gitUtility.getLocalGit(repositoryName, "development");) {

			JSONArray tracedFiles = (JSONArray) gitProxy.getTraceModel(git).get("tracedFiles");
			TreeWalk treeWalk = gitUtility.getRepositoryTreeWalk(git.getRepository());

			if (path.isEmpty()) {
				while (treeWalk.next()) {
					gitProxy.addFile(treeWalk, files, tracedFiles);
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
						gitProxy.addFiletoFileList(treeWalk, files, tracedFiles, path);
					}
				}
			}

		} catch (GitHelperException e) {
			File repo = GitUtility.getRepositoryPath(repositoryName);
			// repo might got cloned, but is empty
			// so delete it
			deleteFolder(repo);
			throw new InternalServerErrorException(e.getMessage());
		} catch (Exception e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "getModelFiles: exception fetching files: " + e);
			service.getLogger().log(Level.FINER, e.getMessage());
			throw new InternalServerErrorException("IO error!");
		}
		return Response.ok(jsonResponse.toString().replace("\\", "")).build();
	}

	/**
	 * Deletes a local repository
	 * 
	 * @param repositoryName
	 *            The repository to delete
	 * @return HttpResponse containing a status code
	 */

	@GET
	@Path("/{repoName}/delete")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Deletes the local repository of the given repository name", notes = "Deletes the local repository.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, local repository deleted"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response deleteLocalRepositoryREST(@PathParam("repoName") String repositoryName) {
		/*
		 * try { FileUtils.deleteDirectory(new File(repositoryName)); } catch
		 * (IOException e) { e.printStackTrace(); logger.printStackTrace(e);
		 * return new HttpResponse(e.getMessage(),
		 * HttpURLConnection.HTTP_INTERNAL_ERROR); } return new
		 * HttpResponse("Ok", HttpURLConnection.HTTP_OK);
		 */
		String result = service.deleteLocalRepository(repositoryName);
		if (Objects.equals(result, "done")) {
			return Response.ok().build();
		} else {
			throw new InternalServerErrorException();
		}
	}

}
