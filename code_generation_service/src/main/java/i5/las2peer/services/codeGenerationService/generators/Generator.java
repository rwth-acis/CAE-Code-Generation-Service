package i5.las2peer.services.codeGenerationService.generators;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.CodeGenerationService;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHostException;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;

/**
 * 
 * Abstract class providing means to create local repositories, add files to them and push them to a
 * remote GitHub repository. Does not provide means to commit files (please do manually).
 *
 */
public abstract class Generator {


  private static final L2pLogger logger =
      L2pLogger.getInstance(Generator.class.getName());

  /**
   * 
   * Generates a new (local) repository to add files to. Also creates a (remote) GitHub repository
   * with the same name and adds it to the (local) repository's configuration to be later used as
   * its remote entry to push to.
   * 
   * @param name the name of the repository to be created
   * @param gitAdapter The {@link i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter} for connecting to a git provider
   * @return a {@link org.eclipse.jgit.lib.Repository}
   * 
   * @throws GitHostException if anything goes wrong during this creation process
   * 
   */
  public static Repository generateNewRepository(String name, BaseGitHostAdapter gitAdapter) throws GitHostException {

    Git git = null;
    File localPath = null;

    // prepare a new folder for the new repository
    try {
      String localGitPath = ((CodeGenerationService) Context.getCurrent().getService()).getLocalGitPath();
      if(localGitPath != null && localGitPath.length() > 0)
        localPath = new File(localGitPath + name);
      else
        localPath = File.createTempFile(name, "");
      localPath.delete();
    } catch (IOException e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }

    // add a remote configuration (origin) to the newly created repository
    try {
      git = Git.init().setDirectory(localPath).call();
      StoredConfig config = git.getRepository().getConfig();
      
      RemoteConfig remoteConfig = null;
      
      remoteConfig = new RemoteConfig(config, "Remote");
      remoteConfig.addURI(new URIish(gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + name + ".git"));
		
      
      remoteConfig.update(config);
      config.save();
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }

    // now create empty GitHub/GitLab repository with an HTTP request, using the GitHub/GitLab API directly
    // because jGit does not support direct GitHub/GitLab repository creation..
    // .. we use the adapter
    return git.getRepository();
  }


  /**
   * 
   * Clones the template repository from GitHub to the local machine and returns a
   * {@link org.eclipse.jgit.treewalk.TreeWalk} that can be used to retrieve the repository's
   * content. Repository is used "read-only" here.
   * @param gitAdapter The {@link i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter} for connecting to a git provider
   * 
   * @return a {@link org.eclipse.jgit.treewalk.TreeWalk}
   * 
   * @throws GitHostException if anything goes wrong during retrieving the repository's content
   * 
   */
  public static TreeWalk getTemplateRepositoryContent(BaseGitHostAdapter gitAdapter) throws GitHostException {
	if(gitAdapter == null) {
		throw new GitHostException("Adapter is null");
	}
    Repository templateRepository = getRemoteRepository(gitAdapter.getTemplateRepository(), gitAdapter);
    
    if (templateRepository == null) {
    	throw new GitHostException("Template repository is null!");
    }
    
    // get the content of the repository
    RevWalk revWalk = null;
    TreeWalk treeWalk = null;
    
    try {
      ObjectId lastCommitId = templateRepository.resolve(Constants.HEAD);
      
      if (lastCommitId == null) {
		throw new GitHostException("lastCommit is null, template repo is probably empty");
      }
      
      treeWalk = new TreeWalk(templateRepository);
      revWalk = new RevWalk(templateRepository);
      RevTree tree = revWalk.parseCommit(lastCommitId).getTree();
      treeWalk.addTree(tree);
      treeWalk.setRecursive(true);
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    } finally {
      templateRepository.close();
      revWalk.close();
    }
    return treeWalk;
  }



  /**
   * 
   * Clones a repository from GitHub to the local machine and returns a
   * {@link org.eclipse.jgit.treewalk.TreeWalk} that can be used to retrieve the repository's
   * content. Repository is used "read-only" here.
   * 
   * @param repositoryName the name of the template repository
   * @param gitAdapter adapter for git
   * @param selectedCommitSha The sha identifier of the commit where the content of the repository should be received from.
   * @return a {@link org.eclipse.jgit.treewalk.TreeWalk}
   * 
   * @throws GitHostException if anything goes wrong during retrieving the repository's content
   * 
   */
  public static TreeWalk getRepositoryContent(String repositoryName, BaseGitHostAdapter gitAdapter, String selectedCommitSha)
      throws GitHostException {
    Repository repository = getRemoteRepository(repositoryName, gitAdapter);
    // get the content of the repository
    RevWalk revWalk = null;
    TreeWalk treeWalk = null;

    try {
      treeWalk = new TreeWalk(repository);
      revWalk = new RevWalk(repository);
      ObjectId id = ObjectId.fromString(selectedCommitSha);
      RevTree tree = revWalk.parseCommit(id).getTree();
      treeWalk.addTree(tree);
      treeWalk.setRecursive(true);
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    } finally {
      repository.close();
      revWalk.close();
    }
    return treeWalk;
  }



  /**
   * 
   * Clones a repository from GitHub to the local machine and returns it.
   * 
   * @param repositoryName the name of the repository
   * @param gitAdapter adapter for Git
   * @return a {@link org.eclipse.jgit.lib.Repository}
   * 
   * @throws GitHostException if anything goes wrong during retrieving the repository's content
   * 
   */
  protected static Repository getRemoteRepository(String repositoryName, BaseGitHostAdapter gitAdapter)
      throws GitHostException {
	  String repositoryAddress;
	repositoryAddress = gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + repositoryName + ".git";

    Repository repository = null;
    // prepare a new folder for the template repository (to be cloned)
    File localPath = null;
    try {
      localPath = File.createTempFile(repositoryName, "");
    } catch (IOException e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }
    localPath.delete();

    // then clone
    try {
      CredentialsProvider prov = new UsernamePasswordCredentialsProvider(gitAdapter.getGitUser(), gitAdapter.getGitPassword());
      
      repository = Git
    		  .cloneRepository().setCredentialsProvider(prov)
    		  .setURI(repositoryAddress).setDirectory(localPath)
    		  .call().getRepository();
      
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }

    return repository;
  }



  /**
   * 
   * Adds a text (source code-)file to the repository. Beware of side effects, due to adding all
   * files in main folder to staged area currently.
   * 
   * @param repository the repository the file should be added to
   * @param relativePath the relative path the file should reside at; without first separator
   * @param fileName the file name
   * @param content the content the file should have
   * 
   * @return the {@link org.eclipse.jgit.lib.Repository}, now containing one more file
   * 
   * @throws GitHostException if anything goes wrong during the creation of the file
   * 
   */
  public static Repository createTextFileInRepository(Repository repository, String relativePath,
      String fileName, String content) throws GitHostException {

    File dirs = new File(repository.getDirectory().getParent() + "/" + relativePath);
    dirs.mkdirs();

    try {
      OutputStream file = new FileOutputStream(
          repository.getDirectory().getParent() + "/" + relativePath + fileName);
      OutputStream buffer = new BufferedOutputStream(file);
      PrintStream printStream = new PrintStream(buffer);
      printStream.print(content);
      printStream.close();
    } catch (IOException e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }

    // stage file
    try {
      Git.wrap(repository).add().addFilepattern(".").call();
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }
    return repository;

  }


  /**
   * 
   * Adds a binary file to the repository. Beware of side effects, due to adding all files in main
   * folder to staged area currently.
   * 
   * @param repository the repository the file should be added to
   * @param relativePath the relative path the file should reside at; without first separator
   * @param fileName the file name
   * @param content the content the file should have
   * 
   * @return the {@link org.eclipse.jgit.lib.Repository}, now containing one more file
   * 
   * @throws GitHostException if anything goes wrong during the creation of the file
   * 
   */
  public static Repository createBinaryFileInRepository(Repository repository, String relativePath,
      String fileName, Object content) throws GitHostException {

    File dirs = new File(repository.getDirectory().getParent() + "/" + relativePath);
    dirs.mkdirs();

    try {
      OutputStream file = new FileOutputStream(
          repository.getDirectory().getParent() + "/" + relativePath + fileName);
      OutputStream buffer = new BufferedOutputStream(file);
      ObjectOutput output = new ObjectOutputStream(buffer);
      output.writeObject(content);
      output.close();
    } catch (IOException e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }

    // stage file
    try {
      Git.wrap(repository).add().addFilepattern(".").call();
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }
    return repository;

  }


  /**
   * 
   * Adds an image file to the repository. Beware of side effects, due to adding all files in main
   * folder to staged area currently.
   * 
   * @param repository the repository the file should be added to
   * @param relativePath the relative path the file should reside at; without first separator
   * @param fileName the file name
   * @param content the content the image should have
   * 
   * @return the {@link org.eclipse.jgit.lib.Repository}, now containing one more file
   * 
   * @throws GitHostException if anything goes wrong during the creation of the file
   * 
   */
  public static Repository createImageFileInRepository(Repository repository, String relativePath,
      String fileName, BufferedImage content) throws GitHostException {

    File dirs = new File(repository.getDirectory().getParent() + "/" + relativePath);
    dirs.mkdirs();

    try {
      File file = new File(repository.getDirectory().getParent() + "/" + relativePath + fileName);
      ImageIO.write(content, fileName.substring(fileName.lastIndexOf(".") + 1), file);
    } catch (IOException e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }

    // stage file
    try {
      Git.wrap(repository).add().addFilepattern(".").call();
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }
    return repository;

  }


  /**
   * 
   * Pushes a local repository (from and) to the "master" branch on GitHub. This method only works
   * with repositories previously created by {@link #generateNewRepository}.
   * 
   * @param repository the {@link org.eclipse.jgit.lib.Repository} to be pushed to GitHub
   * @param gitAdapter The {@link i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter} for connecting to a git provider
   * @return the {@link org.eclipse.jgit.lib.Repository} that was pushed
   * 
   * @throws GitHostException if anything goes wrong during the push command
   * 
   */
  public static Repository pushToRemoteRepository(Repository repository, BaseGitHostAdapter gitAdapter, String versionTag) throws GitHostException {
    return pushToRemoteRepository(repository, "master", "master", gitAdapter, versionTag, false);
  }
  
  public static Repository pushToRemoteRepository(Repository repository, BaseGitHostAdapter gitAdapter, String versionTag,
		  boolean forcePush) throws GitHostException {
	  return pushToRemoteRepository(repository, "master", "master", gitAdapter, versionTag, forcePush);
  }


  /**
   * 
   * Pushes a local repository to GitHub. This method only works with repositories previously
   * created by {@link #generateNewRepository}.
   * 
   * @param repository the {@link org.eclipse.jgit.lib.Repository} to be pushed to GitHub
   * @param localBranchName the name of the branch that should be pushed from
   * @param remoteBranchName the name of the branch that should be pushed to
   * @param gitAdapter adapter for Git
   * @param versionTag String which should be used as the tag when commiting. May be null.
   * @param forcePush set t/f value
   * @return the {@link org.eclipse.jgit.lib.Repository} that was pushed
   * 
   * @throws GitHostException if anything goes wrong during the push command
   * 
   */
  public static Repository pushToRemoteRepository(Repository repository, String localBranchName, 
		  String remoteBranchName, BaseGitHostAdapter gitAdapter, String versionTag, boolean forcePush)
      throws GitHostException {
    CredentialsProvider credentialsProvider =
        new UsernamePasswordCredentialsProvider(gitAdapter.getGitUser(), gitAdapter.getGitPassword());
    try {
    	RefSpec spec = new RefSpec("refs/heads/" + localBranchName + ":refs/heads/" + remoteBranchName);
    	RefSpec specTags = new RefSpec("refs/tags/" + versionTag + ":refs/tags/" + versionTag);
    	if(forcePush){
    		if(versionTag != null) {
    			// push version tag and code
    		    Git.wrap(repository).push().setForce(true).setRemote("Remote").setPushTags().setCredentialsProvider(credentialsProvider)
    		        .setRefSpecs(spec, specTags).call();
    		} else {
    		    // only push code
    			Git.wrap(repository).push().setPushTags().setForce(true).setRemote("Remote").setCredentialsProvider(credentialsProvider).setRefSpecs(spec).call();
    		}
    	} else {
    		// the "setRemote" parameter name is defined in the generateNewRepository method
    		if(versionTag != null) {
    			// push version tag and code
    		    Git.wrap(repository).push().setRemote("Remote").setPushTags().setCredentialsProvider(credentialsProvider)
    		        .setRefSpecs(spec, specTags).call();
    		} else {
    			// only push code
    			Git.wrap(repository).push().setRemote("Remote").setCredentialsProvider(credentialsProvider).setRefSpecs(spec).call();
    		}
    	}
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new GitHostException(e.getMessage());
    }
    return repository;
  }


  /**
   * 
   * Deletes a repository on GitHub, given by its name.
   * 
   * @param name the name of the repository
   * @param gitAdapter adapter for git
   * @throws GitHostException if deletion was not successful
   * 
   */
  public static void deleteRemoteRepository(String name, BaseGitHostAdapter gitAdapter) throws GitHostException {
	//See new adapter class
	gitAdapter.deleteRepo(name);
  }

  /**
   * Checks whether a remote repository of the given name in the given github organization exists.
   * Uses the ls remote git command to determine if the repository exists.
   * 
   * @param name The name of the repository
   * @param gitAdapter adapter for Git
   * @return True, if the repository exists, otherwise false
   */

  public static boolean existsRemoteRepository(String name, BaseGitHostAdapter gitAdapter) {
    CredentialsProvider credentialsProvider =
        new UsernamePasswordCredentialsProvider(gitAdapter.getGitUser(), gitAdapter.getGitPassword());
    LsRemoteCommand lsCmd = new LsRemoteCommand(null);
    
    String url;
     
    url = gitAdapter.getBaseURL() + gitAdapter.getGitOrganization() + "/" + name + ".git";
    
    lsCmd.setRemote(url);
    lsCmd.setHeads(true);
    lsCmd.setCredentialsProvider(credentialsProvider);
    boolean exists = true;
    try {
      lsCmd.call();
    } catch (Exception e) {
      // ignore the exception, as this is the way we determine if a remote repository exists
      exists = false;
    }
    return exists;
  }

  /**
   * Creates the traced files contained in the given global trace model in the given repository
   * 
   * @param traceModel The global trace model containing the traced files that should be created in
   *        the repository
   * @param repository The repository in which the files should be created
   * 
   * @return The {@link org.eclipse.jgit.lib.Repository}, now containing the traced files of the
   *         trace model
   * @throws GitHostException if anything goes wrong during the creation of the traced files
   */

  protected static Repository createTracedFilesInRepository(TraceModel traceModel,
      Repository repository) throws GitHostException {
    Map<String, FileTraceModel> fileTraceMap = traceModel.getFilenameToFileTraceModelMap();

    for (String fullPath : fileTraceMap.keySet()) {
      FileTraceModel fileTraceModel = fileTraceMap.get(fullPath);

      String fileName = fullPath;
      String relativePath = "";
      int index = fullPath.lastIndexOf(File.separator);
      if (index > -1) {
        fileName = fullPath.substring(index + 1);
        relativePath = fullPath.substring(0, index) + "/";
      }

      repository = createTextFileInRepository(repository, relativePath, fileName,
          fileTraceModel.getContent());

      repository = createTextFileInRepository(repository, "traces/" + relativePath,
          fileName + ".traces", fileTraceModel.toJSONObject().toJSONString());
    }

    repository = createTextFileInRepository(repository, "traces/", "tracedFiles.json",
        traceModel.toJSONObject().toJSONString().replace("\\", ""));

    return repository;
  }

  /**
   * Commit multiple files to the github repository. Like
   * commitFile, but without any trace information and for multiple files
   * 
   * @param repositoryName The name of the repository
   * @param commitMessage A commit message
   * @param versionTag String which should be used as the tag when commiting. May be null.
   * @param files An array containing the file names and file contents
   */
  private static String commitMultipleFilesRaw(String repositoryName, String commitMessage, String versionTag,
      String[][] files) {
    try {
    	return ((CodeGenerationService) Context.getCurrent().getService()).storeAndCommitFilesRaw(repositoryName, 
    			commitMessage, versionTag, files);
    } catch (Exception e) {
      logger.printStackTrace(e);
      return "";
    }
  }

  /**
   * Updates a given list of traced files in a local repository of the GitHub proxy service
   * 
   * @param fileList A list containing the files that should be updated
   * @param repositoryName The name of the repository
   * @param versionTag String which should be used as the tag when commiting. May be null.
   * @param service Name of the service
   */

  protected static String updateTracedFilesInRepository(List<String[]> fileList,
      String repositoryName, Service service, String commitMessage, String versionTag) {
    return commitMultipleFilesRaw(repositoryName, commitMessage, versionTag,
        fileList.toArray(new String[][] {}));
  }

  /**
   * Creates a list of the traced files contained in a trace model
   * 
   * @param traceModel A trace model that contains the traced files
   * @param guidances The feedback rules used to perform the model violation detection.
   * @return A list of the traced files contained in the trace model
   * @throws UnsupportedEncodingException Thrown for errors during the encoding of the content of
   *         files
   */

  protected static List<String[]> getUpdatedTracedFilesForRepository(TraceModel traceModel,
      String guidances) throws UnsupportedEncodingException {
    Map<String, FileTraceModel> fileTraceMap = traceModel.getFilenameToFileTraceModelMap();

    List<String[]> fileList = new ArrayList<String[]>();

    for (String fullPath : fileTraceMap.keySet()) {
      FileTraceModel fileTraceModel = fileTraceMap.get(fullPath);

      String fileName = fullPath;
      String relativePath = "";
      int index = fullPath.lastIndexOf(File.separator);
      if (index > -1) {
        fileName = fullPath.substring(index + 1);
        relativePath = fullPath.substring(0, index) + "/";
      }

      String content = fileTraceModel.getContent();
      String fileTraceContent = fileTraceModel.toJSONObject().toJSONString();

      fileList.add(new String[] {"traces/" + relativePath + fileName + ".traces",
          Base64.getEncoder().encodeToString(fileTraceContent.getBytes("utf-8"))});
      fileList.add(new String[] {relativePath + fileName,
          Base64.getEncoder().encodeToString(content.getBytes("utf-8"))});

    }

    String tracedFiles = traceModel.toJSONObject().toJSONString().replace("\\", "");
    fileList.add(new String[] {"traces/tracedFiles.json",
        Base64.getEncoder().encodeToString(tracedFiles.getBytes("utf-8"))});

    fileList.add(new String[] {"traces/guidances.json",
        Base64.getEncoder().encodeToString(guidances.getBytes("utf-8"))});

    return fileList;

  }

  /**
   * Rename a file in the local repository hold by the GitHub proxy service
   * 
   * @param repositoryName The name of the repository
   * @param newFileName The new file name
   * @param oldFileName The old file name
   */

  protected static void renameFileInRepository(String repositoryName, String newFileName,
      String oldFileName) {
    try {
    	((CodeGenerationService) Context.getCurrent().getService()).getGitUtility().renameFile(repositoryName, newFileName, oldFileName);
    } catch (Exception e) {
      logger.printStackTrace(e);
    }
  }

  /**
   * Delete a file in the local repository hold by the GitHub proxy service
   * 
   * @param repositoryName The name of the repository
   * @param fileName The name of the file that must be deleted
   */

  protected static void deleteFileInLocalRepository(String repositoryName, String fileName) {
    try {
    	((CodeGenerationService) Context.getCurrent().getService()).getGitUtility().deleteFile(repositoryName, fileName);
    } catch (Exception e) {
      logger.printStackTrace(e);
    }
  }

  /**
   * Replace an exactly matching placeholder of the form %%something%%, otherwise nothing will be changed
   * @param text The text to replace in
   * @param placeholder The string to replace, format: %%something%%
   * @param replacement the replacement text
   * @return The string with replacements
   */
  protected static String replaceExactMatch(String text, String placeholder, String replacement) {
	  if(placeholder.startsWith("%%") && placeholder.endsWith("%%")){
		  text = text.replace(placeholder, replacement);
		  return text;
	  } else {
		  return text;
	  }
  }
}
