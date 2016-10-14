package i5.las2peer.services.codeGenerationService.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.gitHubProxyService.GitHubProxyService;

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
	private String gitHubOrganization;
	private CredentialsProvider provider;
	private L2pLogger logger;
	
	public GitUtility() {
		 logger = L2pLogger.getInstance(GitHubProxyService.class.getName());
	}
	
	public void renameFile(String repositoryName,String newFileName, String oldFileName) throws FileNotFoundException {
		// TODO: Stub
	}
	
	public void deleteFile(String repositoryName, String fileName) throws FileNotFoundException, IOException {
		//TODO: Stub
	}
	
	public void mergeIntoMasterBranch(String repositoryName,String masterBranchName) {
		//TODO: Stub
	}
	
	public boolean existsRemoteRepository(String url) {
		//TODO: Stub
		return false;
	}
	
	public boolean existsLocalRepository(String repositoryName) {
		//TODO: Stub
		return false;
	}
	
	public Repository getLocalRepository(String repositoryName) {
		//TODO: Stub
		return null;
	}
	
	public Git getLocalGit(String repositoryName) {
		//TODO: Stub
		return null;
	}
	
	public Git getLocalGit(String repositoryName, String branchName) {
		//TODO: Stub
		return null;
	}
	
	public Git getLocalGit(Repository repository, String branchName) {
		//TODO: Stub
		return null;
	}
	
	public  TreeWalk getRepositoryTreeWalk(Repository repository) {
		//TODO: Stub
	    return null;
	}
	
	public TreeWalk getRepositoryTreeWalk(Repository repository, boolean recursive) {
		//TODO: Stub
		return null;
	}
	
	public String getFileContent(Repository repository, String fileName) {
		//TODO: Stub
		return null;
	}
	
	public void switchBranch(Git git, String branchName) {
		//TODO: Stub
	}
	
	private Repository createLocalRepository(String repositoryName) {
		//TODO: Stub
		return null;
	}
	
	public boolean indexIsLocked(String repositoryName) {
		//TODO: Stub
		return false;
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
