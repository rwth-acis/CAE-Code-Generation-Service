package i5.las2peer.services.codeGenerationService.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.CodeGenerationService;
import i5.las2peer.services.codeGenerationService.exception.GitHelperException;

/**
 * 
 * @author Thomas Winkler, Jonas Koenning
 *
 * A helper class providing utilities to work with local repositories
 * This class has been adapted from a static helper class. Most method parameters never actually change
 * after the service is started so they have been refactored to fields which are initalized during startup.
 */
public class GitUtility {
	
	private String baseURL;
	private String gitHostOrganization;
	private CredentialsProvider provider;
	private L2pLogger logger;
	
	public GitUtility(String gitUser, String gitPassword, String gitOrganization, String baseURL) {
		 logger = L2pLogger.getInstance(CodeGenerationService.class.getName());
		 this.provider = new UsernamePasswordCredentialsProvider(gitUser, gitPassword);
		 this.gitHostOrganization = gitOrganization;
		 this.baseURL = baseURL;
	}
	
	
	/**
	   * Rename a file within a repository. This method does not commit the renaming.
	   * 
	   * @param repositoryName The name of the repository
	   * @param newFileName The path of the new file name, relative to the working directory
	   * @param oldFileName The path of the old file t, relative to the working directory
	   * @throws GitHelperException thrown incase of error in git api
	 */
	public void renameFile(String repositoryName,String newFileName, String oldFileName) throws GitHelperException {
		try{
			Git git = getLocalGit(repositoryName, "development");
			File oldFile = new File(getRepositoryPath(repositoryName) + "/" + oldFileName);
		    File newFile = new File(getRepositoryPath(repositoryName) + "/" + newFileName);

		    Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,"Renaming file " + oldFileName + " to " + newFileName);

		    if (newFile.getParentFile() != null) {
		    	newFile.getParentFile().mkdirs();
		    }
		    
		    oldFile.renameTo(newFile);
		    git.add().addFilepattern(newFileName).call();
		    git.rm().addFilepattern(oldFileName).call();
		    
		    while (oldFile.getParentFile() != null) {
		    	File parent = oldFile.getParentFile();
		        if (parent.isDirectory() && parent.list().length == 0) {
		          oldFile = parent;
		          parent.delete();
		        } else {
		          break;
		        }
		    }
		} catch(GitAPIException e) {
			throw new GitHelperException("Error using jGit: " + e.getMessage());
		} 
	}

	/**
	   * Delete a file from a repository. This method does not commit the deletion.
	   * 
	   * @param repositoryName The name of the repository
	   * @param fileName The path of the file to be deleted
	   * @throws GitHelperException 
	   * 	thrown incase of error in git api
	   */
	public void deleteFile(String repositoryName, String fileName) throws GitHelperException {
		try (Git git = getLocalGit(repositoryName, "development")) {
			File file = new File(getRepositoryPath(repositoryName) + "/" + fileName);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "Deleting file " + fileName);
			file.delete();
		    git.rm().addFilepattern(fileName).call();
		} catch (GitAPIException e) {
			throw new GitHelperException("Error using jGit: " + e.getMessage());
		}
	}
	
	/**
	   * Merge the development branch of the repository to the given master branch and push it to the
	   * remote repository
	   * 
	   * @param repositoryName The name of the repository
	   * @param masterBranchName The name of the master branch
	   * @param versionTag String which should be used as the tag when commiting. May be null.
	   * @throws GitHelperException thrown incase of error in git api
	   * 
	   */
	public void mergeIntoMasterBranch(String repositoryName,String masterBranchName, String versionTag) throws GitHelperException {
		Git git = null;

	    try {
	    	git = getLocalGit(repositoryName, masterBranchName);
	    	git.fetch().setCredentialsProvider(provider).call();

	    	MergeCommand mCmd = git.merge();
	    	Ref HEAD = git.getRepository().getRef("refs/heads/development");
	    	mCmd.include(HEAD);
	    	mCmd.setStrategy(MergeStrategy.THEIRS);
	    	MergeResult mRes = mCmd.call();
	    	
	    	if (mRes.getMergeStatus().isSuccessful()) {
	    		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "Merged development and " + masterBranchName + " branch successfully");
	    		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "Now pushing the commits...");
	    		PushCommand pushCmd = git.push();
	    		pushCmd.setCredentialsProvider(provider).setForce(true).setPushAll().call();
	    		
	    		if(versionTag != null) {
	    			RefSpec specTags = new RefSpec("refs/tags/" + versionTag + ":refs/tags/" + versionTag);
	    			git.push().setCredentialsProvider(provider).setForce(true).setPushTags().setRefSpecs(specTags).call();
	    		}
	    		
	    		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "... commits pushed");
	    	} else {
	    		logger.warning("Error during merging of development and " + masterBranchName + " branch");
	    		throw new GitHelperException("Unable to merge " + masterBranchName + " and development branch");
	    	}
	    }catch(GitAPIException e) {
	    	throw new GitHelperException("Error using jGit: " + e.getMessage());
	    }catch(IOException e){
	    	throw new GitHelperException(e.getMessage());
	    }finally {
	    
	    	if (git != null) {
	    		// switch back to development branch
	    		switchBranch(git, "development");
	    		git.close();
	    	}
	    }
	}
	
	/**
	 * Checks if a remote repository exists by issuing a {@link LsRemoteCommand}
	 * @param url url of remote repository
	 * @return A boolean that indicates if the remote exists
	 */
	public boolean existsRemoteRepository(String url) {
		LsRemoteCommand lsCmd = new LsRemoteCommand(null);
	    lsCmd.setRemote(url);
	    lsCmd.setHeads(true);
	    // This is needed for gitlab
	    lsCmd.setCredentialsProvider(provider);
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
	 * Checks if a local repository exists
	 * @param repositoryName name of the repository
	 * @return A boolean that indicates if the repository exists
	 */
	public boolean existsLocalRepository(String repositoryName) {
		File localPath;
	    localPath = getRepositoryPath(repositoryName);
	    File repoFile = new File(localPath + "/.git");
	    return repoFile.exists();
	}
	
	/**
	 * Returns a local {@link Repository}.
	 * @param repositoryName name of the repository
	 * @return The repository
	 * @throws GitHelperException thrown incase of error in git api
	 */
	public Repository getLocalRepository(String repositoryName) throws GitHelperException {
		File localPath;
	    Repository repository = null;
	    localPath = getRepositoryPath(repositoryName);
	    File repoFile = new File(localPath + "/.git");
	
	    if (!repoFile.exists()) {
	    	repository = createLocalRepository(repositoryName);
	
	    } else {
	    	FileRepositoryBuilder builder = new FileRepositoryBuilder();
	    	try {
	    	repository = builder.setGitDir(repoFile).readEnvironment().findGitDir().build();
	    	} catch (IOException e) {
	    		throw new GitHelperException(e.getMessage());
	    	}
	    }
	    return repository;
	}
	
	
	public Git getLocalGit(String repositoryName) throws GitHelperException {
		Git git = new AutoCloseGit(getLocalRepository(repositoryName));
	    return git;
	}
	
	/**
	 * Get a local {@link Git} and switch to the specified branch.
	 * @param repositoryName The name of the repository
	 * @param branchName The name of the desired branch
	 * @return The git object
	 * @throws GitHelperException thrown incase of error in git api
	 */
	public Git getLocalGit(String repositoryName, String branchName) throws GitHelperException {
		Git git = getLocalGit(repositoryName);
	    switchBranch(git, branchName);
	    return git;
	}
	
	public Git getLocalGit(Repository repository, String branchName) throws GitHelperException {
		Git git = new AutoCloseGit(repository);
	    switchBranch(git, branchName);
	    return git;
	}
	
	public  TreeWalk getRepositoryTreeWalk(Repository repository) {
		return getRepositoryTreeWalk(repository, false);
	}
	
	public TreeWalk getRepositoryTreeWalk(Repository repository, boolean recursive) {
		RevWalk revWalk = null;
	    TreeWalk treeWalk = null;
	    try {
	    	ObjectId lastCommitId = repository.resolve(Constants.HEAD);
	    	treeWalk = new TreeWalk(repository);
	    	revWalk = new RevWalk(repository);
	    	RevTree tree = revWalk.parseCommit(lastCommitId).getTree();
	    	treeWalk.addTree(tree);
	    	treeWalk.setRecursive(recursive);
	    } catch (Exception e) {
	      logger.printStackTrace(e);
	    } finally {
	      repository.close();
	      revWalk.close();
	    }
	    return treeWalk;
	}
	
	public String getFileContent(Repository repository, String fileName) throws GitHelperException {
		String content = "";

	    try (TreeWalk treeWalk = getRepositoryTreeWalk(repository)) {

	      treeWalk.setFilter(PathFilter.create(fileName));
	      boolean fileFound = false;

	      while (treeWalk.next()) {

	        if (!fileFound && treeWalk.isSubtree()) {
	          treeWalk.enterSubtree();
	        }
	        if (treeWalk.getPathString().equals(fileName)) {
	          fileFound = true;
	          break;
	        }

	      }
	      if (fileFound) {
	        ObjectReader reader = treeWalk.getObjectReader();
	        ObjectId objectId = treeWalk.getObjectId(0);
	        ObjectLoader loader = reader.open(objectId);
	        content = new String(loader.getBytes(), "UTF-8");
	      } else {
	        throw new FileNotFoundException(fileName + " not found");
	      }

	    } catch (IOException e) {
			throw new GitHelperException(e.getMessage());
		}

	    return content;
	}
	
	public void switchBranch(Git git, String branchName) throws GitHelperException {
		try {
			if (git.getRepository().getBranch().equals(branchName)) {
				return;
			}
			boolean branchExists = git.getRepository().getRef(branchName) != null;
			if (!branchExists) {
				git.branchCreate().setName(branchName).call();
			}
			git.checkout().setName(branchName).call();
		} catch(IOException e) {
			throw new GitHelperException(e.getMessage());
		} catch(GitAPIException  e) {
			throw new GitHelperException("Error using jGit: " + e.getMessage());  
		}
	}
	
	public boolean indexIsLocked(String repositoryName) {
		return getRepositoryPath(repositoryName + "/.git/index.lock").exists();
	}
	
	private Repository createLocalRepository(String repositoryName) throws GitHelperException {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "created new local repository " + repositoryName);
	    String repositoryAddress = baseURL + gitHostOrganization + "/" + repositoryName + ".git";
	    Repository repository = null;

	    boolean isFrontend = repositoryName.startsWith("frontendComponent-");
	    String masterBranchName = isFrontend ? "gh-pages" : "master";

	    if (existsRemoteRepository(repositoryAddress)) {
	    	try {
	    	Git result = Git.cloneRepository().setURI(repositoryAddress).setCredentialsProvider(provider)
	    			.setDirectory(getRepositoryPath(repositoryName)).setBranch(masterBranchName).call();
	        repository = result.getRepository();
	        
	        // get the files of the folder which is used by the local repository
	        String[] files = repository.getDirectory().getParentFile().list();
	        // if there is only one file/folder, then it is the .git folder
	        // if there is only the .git folder, then there are not other files and this means,
	        // that the repository was cloned at a point, where the remote repository already got created but
	        // no code has been pushed yet
	        // thus, we delete the local repository again, so that it gets cloned later again when the code got 
	        // pushed maybe
	        if(files.length == 1) {
	        	// delete locally
                throw new GitHelperException("Cloned remote repo, but the remote repo was empty. Deleted local repo again.");
	        }
	    	} catch (GitAPIException e) {
				throw new GitHelperException("Error using jGit: " + e.getMessage());
			}
	    } else {
	      throw new GitHelperException("Remote repository: " + repositoryAddress + " not found!");
	    }

	    return repository;
	}
	
	/**
	 * Get the path for the given repository name
	 * 
	 * @param repositoryName The name of the repository
	 * @return A file pointing to the path of the repository
	 */
	public static File getRepositoryPath(String repositoryName) {
		return new File(repositoryName);
	}
	
}
