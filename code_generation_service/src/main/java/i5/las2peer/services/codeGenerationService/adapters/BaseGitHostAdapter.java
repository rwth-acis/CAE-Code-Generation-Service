package i5.las2peer.services.codeGenerationService.adapters;

import i5.las2peer.services.codeGenerationService.exception.GitHostException;

/**
 * 
 * @author jonask
 *
 * This abstract superclass provides fields used by all platform-specific subclasses.
 */
public abstract class BaseGitHostAdapter implements GitHostAdapter {
	
	//TODO: Exception handling
	
	// Basic (unspecific) git service properties
	protected String gitUser;
	protected String gitPassword;
	protected String token;
	protected String gitOrganization;
	protected String templateRepository;
	protected String gitUserMail;
	
	protected String baseURL = "";

	protected BaseGitHostAdapter(String gitUser, String gitPassword, String token, String gitOrganization,
			String templateRepository, String gitUserMail, String baseURL) throws GitHostException {
		super();
		this.gitUser = gitUser;
		this.gitPassword = gitPassword;
		this.token = token;
		this.gitOrganization = gitOrganization;
		this.templateRepository = templateRepository;
		this.gitUserMail = gitUserMail;
		this.baseURL = baseURL;
		
		if(gitUser.isEmpty() || 
				gitPassword.isEmpty() || 
				token.isEmpty() || 
				gitOrganization.isEmpty() || 
				templateRepository.isEmpty() ||
				gitUserMail.isEmpty()) {
			throw new GitHostException("Not all required properties are set");
		}
	}
	
	public String getGitUser() {
		return gitUser;
	}
	
	public String getGitPassword() {
		return gitPassword;
	}

	public String getToken() {
		return token;
	}
	
	public String getGitOrganization() {
		return gitOrganization;
	}

	public String getTemplateRepository() {
		return templateRepository;
	}

	public String getGitUserMail() {
		return gitUserMail;
	}

	public String getBaseURL() {
		return baseURL;
	}	
}
