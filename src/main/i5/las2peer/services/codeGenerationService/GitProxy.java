package i5.las2peer.services.codeGenerationService;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.exception.GitHelperException;
import i5.las2peer.services.codeGenerationService.utilities.GitUtility;

public class GitProxy {
	private GitUtility gitUtility;
	private L2pLogger logger;
	
	public GitProxy(GitUtility gitUtility, L2pLogger logger) {
		this.gitUtility = gitUtility;
		this.logger = logger;
	}
	/*--------------------------------------------
	   * Git Host Proxy helper methods
	   * -------------------------------------------
	   */

	public String getTraceFileName(String fileName) {
		return "traces/" + fileName + ".traces";
	}

	/**
	 * A private helper method to add the current file or folder of a tree walk
	 * to a json array.
	 * 
	 * @param tw
	 *            The tree walk which current file/folder should be added to the
	 *            json array
	 * @param fileList
	 *            The json array the current file/folder should be added
	 *            
	 * @param tracedFiles array of files which have been traced
	 * 
	 * @param path
	 *            The path of the current file
	 */

	@SuppressWarnings("unchecked")
	public void addFiletoFileList(TreeWalk tw, JSONArray fileList, JSONArray tracedFiles, String path) {
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
	 * A private helper method to add the current file or folder of a tree walk
	 * to a json array
	 * 
	 * @param tw
	 *            The tree walk which current file/folder should be added to the
	 *            json array
	 * @param files
	 *            The json array the current file/folder should be added
	 *            
	 * @param tracedFiles array of files that have been traced
	 */
	public void addFile(TreeWalk tw, JSONArray files, JSONArray tracedFiles) {
		addFiletoFileList(tw, files, tracedFiles, "");
	}

	/**
	 * Get the traces for a file
	 * 
	 * @param git
	 *            The git object of the repository of the file
	 * @param fullFileName
	 *            The file name whose traces should be returned. Must be the
	 *            full file name, i.e. with full file path
	 * @return A JSONObject of the file traces or null if the file does not have
	 *         any traces
	 * @throws Exception
	 *             Thrown if something went wrong
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getFileTraces(Git git, String fullFileName) throws Exception {
		JSONObject traceModel = getTraceModel(git);
		JSONArray tracedFiles = (JSONArray) traceModel.get("tracedFiles");
		JSONObject fileTraces = null;

		if (tracedFiles.contains(fullFileName)) {

			try {
				String content = gitUtility.getFileContent(git.getRepository(), getTraceFileName(fullFileName));
				JSONParser parser = new JSONParser();
				fileTraces = (JSONObject) parser.parse(content);
				fileTraces.put("generationId", traceModel.get("id"));
			} catch (GitHelperException e) {
				logger.printStackTrace(e);
				// TODO: Handle exception better
			}
		}
		return fileTraces;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getGuidances(Git git) {

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
	 * @param git
	 *            The git object of the repository
	 * @return A JSONObject of the trace model.
	 * @throws GitHelperException 
	 * 			Thrown if something goes wrong.
	 * @throws ParseException
	 * 			Thrown if something goes wrong.
	 */

	@SuppressWarnings("unchecked")
	public JSONObject getTraceModel(Git git) throws GitHelperException, ParseException{
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
			logger.printStackTrace(e);
		}
		return result;
	}

}
