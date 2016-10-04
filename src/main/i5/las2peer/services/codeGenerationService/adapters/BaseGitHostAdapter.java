package i5.las2peer.services.codeGenerationService.adapters;

public abstract class BaseGitHostAdapter implements GitHostAdapter {
	protected String baseURL = "";

	protected BaseGitHostAdapter(String baseURL) {
		super();
		this.baseURL = baseURL;
	}
}
