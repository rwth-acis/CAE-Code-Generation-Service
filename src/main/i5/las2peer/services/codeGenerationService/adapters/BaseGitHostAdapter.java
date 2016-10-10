package i5.las2peer.services.codeGenerationService.adapters;

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
	protected String gitOrganization;
	protected String templateRepository;
	protected String gitUserMail;
	
	protected String baseURL = "";

	protected BaseGitHostAdapter(String gitUser, String gitPassword, String gitOrganization, String templateRepository,
			String gitUserMail, String baseURL) {
		super();
		this.gitUser = gitUser;
		this.gitPassword = gitPassword;
		this.gitOrganization = gitOrganization;
		this.templateRepository = templateRepository;
		this.gitUserMail = gitUserMail;
		this.baseURL = baseURL;
	}

	public String getGitUser() {
		return gitUser;
	}

	public String getGitPassword() {
		return gitPassword;
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