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
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.services.codeGenerationService.exception.GitHelperException;
import i5.las2peer.services.gitHubProxyService.GitHubProxyService;
import i5.las2peer.services.gitHubProxyService.gitUtils.AutoCloseGit;
import i5.las2peer.services.gitHubProxyService.gitUtils.GitHelper;

/**
 * 
 * @author Thomas Winkler, Jonas Koenning
 *
 * A helper class providing utilities to work with local repositories
 * This class has been adapted from a static helper class. Most method parameters never actually change
 * after the service is started so they have been refactored to fields which are initalized during startup.
 */
//TODO: Create an instance of this class duing service startup
public class GitUtility {
	private String gitHostOrganization;
	private CredentialsProvider provider;
	private L2pLogger logger;
	
	public GitUtility(String gitUser, String gitPasswort, String gitHostOrganization) {
		 logger = L2pLogger.getInstance(GitHubProxyService.class.getName());
		 this.provider = new UsernamePasswordCredentialsProvider(gitUser, gitPasswort);
		 this.gitHostOrganization = gitHostOrganization;
		 
	}
	
	public void renameFile(String repositoryName,String newFileName, String oldFileName) throws NoFilepatternException, GitAPIException, IOException {
		try(Git git = getLocalGit(repositoryName, "development")){
			File oldFile = new File(getRepositoryPath(repositoryName) + "/" + oldFileName);
		    File newFile = new File(getRepositoryPath(repositoryName) + "/" + newFileName);

		    L2pLogger.logEvent(Event.SERVICE_MESSAGE,"Renaming file " + oldFileName + " to " + newFileName);

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
		}
	}
	
	public void deleteFile(String repositoryName, String fileName) throws FileNotFoundException, IOException, NoFilepatternException, GitAPIException {
		try (Git git = getLocalGit(repositoryName, "development")) {
			File file = new File(getRepositoryPath(repositoryName) + "/" + fileName);
			L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Deleting file " + fileName);
			file.delete();
		    git.rm().addFilepattern(fileName).call();
		}
	}
	
	public void mergeIntoMasterBranch(String repositoryName,String masterBranchName) throws InvalidRemoteException, TransportException, GitAPIException, IOException, GitHelperException   {
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
	    		L2pLogger.logEvent(Event.SERVICE_MESSAGE,"Merged development and master branch successfully");
	    		L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Now pushing the commits...");
	    		PushCommand pushCmd = git.push();
	    		pushCmd.setCredentialsProvider(provider).setForce(true).setPushAll().call();
	    		L2pLogger.logEvent(Event.SERVICE_MESSAGE, "... commits pushed");
	    	} else {
	    		logger.warning("Error during merging of development and master branch");
	    		throw new GitHelperException("Unable to merge master and development branch");
	    	}
	    } finally {
	    	if (git != null) {
	    		// switch back to development branch
	    		GitHelper.switchBranch(git, "development");
	    		git.close();
	    	}
	    }
	}
	
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
	
	public boolean existsLocalRepository(String repositoryName) {
		File localPath;
	    localPath = getRepositoryPath(repositoryName);
	    File repoFile = new File(localPath + "/.git");
	    return repoFile.exists();
	}
	
	public Repository getLocalRepository(String repositoryName) throws IOException {
		File localPath;
	    Repository repository = null;
	    localPath = getRepositoryPath(repositoryName);
	    File repoFile = new File(localPath + "/.git");
	
	    if (!repoFile.exists()) {
	    	repository = createLocalRepository(repositoryName);
	
	    } else {
	    	FileRepositoryBuilder builder = new FileRepositoryBuilder();
	    	repository = builder.setGitDir(repoFile).readEnvironment().findGitDir().build();
	    }
	    return repository;
	}
	
	public Git getLocalGit(String repositoryName) throws IOException {
		Git git = new AutoCloseGit(getLocalRepository(repositoryName));
	    return git;
	}
	
	public Git getLocalGit(String repositoryName, String branchName) throws IOException {
		Git git = getLocalGit(repositoryName);
	    switchBranch(git, branchName);
	    return git;
	}
	
	public Git getLocalGit(Repository repository, String branchName) {
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
	
	public String getFileContent(Repository repository, String fileName) {
		//TODO: Stub
		return null;
	}
	
	public void switchBranch(Git git, String branchName) {
		//TODO: Stub
	}
	
	public boolean indexIsLocked(String repositoryName) {
		//TODO: Stub
		return false;
	}
	
	private Repository createLocalRepository(String repositoryName) {
		//TODO: Stub
		return null;
	}
	
	/**
	 * Get the path for the given repository name
	 * 
	 * @param repositoryName The name of the repository
	 * @return A file pointing to the path of the repository
	 */
	private static File getRepositoryPath(String repositoryName) {
		return new File(repositoryName);
	}
	
}
