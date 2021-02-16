package i5.las2peer.services.codeGenerationService.utilities;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

/**
 * A special git class that always close the connected repository when it is closed. We need this
 * special class as this behavior is not possible with the original git class of jgit.
 * 
 * @author Thomas Winkler
 *
 */

public class AutoCloseGit extends Git {

  public AutoCloseGit(Repository repo) {
    super(repo);
  }

  @Override
  public void close() {
    // always close the repository connected to
    this.getRepository().close();
  }

}
