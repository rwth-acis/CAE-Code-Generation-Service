package i5.las2peer.services.codeGenerationService.exception;

/**
 * 
 * Exception thrown when a model is not in the expected format.
 *
 */
public class ModelParseException extends Exception {

  private static final long serialVersionUID = -1622464573552868191L;

  public ModelParseException(String message) {
    super(message);
  }
}
