package i5.las2peer.services.codeGenerationService.exception;

public class GitHelperException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5889548715894964698L;

	public GitHelperException(String message) {
	    super("There was an error using the GitHelper: " + message);
	}
}
