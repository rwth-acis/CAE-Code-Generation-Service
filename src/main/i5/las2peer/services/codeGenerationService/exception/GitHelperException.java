package i5.las2peer.services.codeGenerationService.exception;

public class GitHelperException extends Exception {
	public GitHelperException(String message) {
	    super("There was an error using the GitHelper: " + message);
	}
}
