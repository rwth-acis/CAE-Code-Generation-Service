package i5.las2peer.services.codeGenerationService;

import static org.junit.Assert.fail;

import java.io.FileReader;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;


/**
 * 
 * Central test class for the CAE-Code-Generation-Service.
 *
 */
public class CodeGenerationServiceTest {

  private static LocalNode node;
  private static UserAgent testAgent;
  private static final String testPass = "adamspass";

  private static final String testTemplateService = CodeGenerationService.class.getCanonicalName();

  /**
   * 
   * Called before the tests start. Sets up the node and initializes users that can be used
   * throughout the tests.
   * 
   * @throws Exception
   * 
   */
  @BeforeClass
  public static void startServer() throws Exception {
    // paths to properties and models
    Properties properties = new Properties();
    String propertiesFile =
        "./etc/i5.las2peer.services.codeGenerationService.CodeGenerationService.properties";

    // load properties and models
    try {
      FileReader reader = new FileReader(propertiesFile);
      properties.load(reader);
      // set needed properties here
    } catch (Exception e) {
      e.printStackTrace();
      fail("Properties file loading problems: " + e);
    }

    // start node
    node = LocalNode.newNode();
    node.storeAgent(MockAgentFactory.getAdam());
    node.launch();

    ServiceAgent testService = ServiceAgent.generateNewAgent(testTemplateService, "a pass");
    testService.unlockPrivateKey("a pass");

    node.registerReceiver(testService);

    testAgent = MockAgentFactory.getAdam();

    // avoid timing errors: wait for the repository manager to get all services before continuing
    try {
      System.out.println("waiting..");
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * 
   * Called after the tests have finished. Shuts down the server.
   * 
   * @throws Exception
   * 
   */
  @AfterClass
  public static void shutDownServer() throws Exception {
    node.shutDown();
    node = null;
    LocalNode.reset();
  }

  /**
   * 
   * 
   */
  @Test
  public void testModelUpdate() {

  }

}
