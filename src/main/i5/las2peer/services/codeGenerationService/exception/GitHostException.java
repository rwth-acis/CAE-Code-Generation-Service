package i5.las2peer.services.codeGenerationService.exception;

/**
 * 
 * Exception thrown when something went wrong during GitHub access.
 *
 */
public class GitHostException extends Exception {

  private static final long serialVersionUID = -1622464573552868191L;

  public GitHostException(String message) {
    super(message);
  }
}
