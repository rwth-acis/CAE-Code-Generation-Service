package i5.las2peer.services.codeGenerationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Assert;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.cae.simpleModel.SimpleModel;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.adapters.GitHostAdapter;
import i5.las2peer.services.codeGenerationService.adapters.GitHubAdapter;
import i5.las2peer.services.codeGenerationService.adapters.GitLabAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHostException;
import i5.las2peer.services.codeGenerationService.generators.Generator;


/**
 *
 * Central test class for the CAE-Code-Generation-Service.
 *
 */
public class CodeGenerationServiceTest {

  private static LocalNode node;

  private static final String codeGenerationService =
      CodeGenerationService.class.getName();

  private static SimpleModel model1;
  private static SimpleModel model2;
  private static SimpleModel model3;
  private static SimpleModel[] model4;
  private static ServiceAgentImpl testService;
  private static ServiceNameVersion serviceNameVersion;

  private static String usedGitHost = null;
  private static String gitOrganization = null;
  private static String gitUser = null;
  private static String gitPassword = null;
  @SuppressWarnings("unused")
  private static String gitUserMail = null;
  @SuppressWarnings("unused")
  private static String templateRepository = null;

  private static GitHostAdapter gitAdapter;
  private static String baseURL = null;
  private static String token = null;


  /**
   *
   * Called before the tests start. Sets up the node and loads test models for later usage.
   *
   * @throws Exception
   *
   */
  @BeforeClass
  public static void startServer() throws Exception {
    // load models
    // activate a helper function
    String modelPath1 = "./testModels/modeloutput.model";
    String modelPath2 = "./testModels/modeloutput-microservice.model";
    String modelPath3 = "./testModels/modeloutput-app.model";

    Properties properties = new Properties();
    String propertiesFile =
        "./etc/i5.las2peer.services.codeGenerationService.CodeGenerationService.properties";

    try {
      InputStream file1 = new FileInputStream(modelPath1);
      InputStream buffer1 = new BufferedInputStream(file1);
      ObjectInput input1 = new ObjectInputStream(buffer1);
      model1 = (SimpleModel) input1.readObject();
      InputStream file2 = new FileInputStream(modelPath2);
      InputStream buffer2 = new BufferedInputStream(file2);
      ObjectInput input2 = new ObjectInputStream(buffer2);
      model2 = (SimpleModel) input2.readObject();
      InputStream file3 = new FileInputStream(modelPath3);
      InputStream buffer3 = new BufferedInputStream(file3);
      ObjectInput input3 = new ObjectInputStream(buffer3);
      model3 = (SimpleModel) input3.readObject();


      input1.close();
      input2.close();
      input3.close();

      FileReader reader = new FileReader(propertiesFile);
      properties.load(reader);
      gitUser = properties.getProperty("gitUser");
      gitUserMail = properties.getProperty("gitUserMail");
      gitOrganization = properties.getProperty("gitOrganization");
      templateRepository = properties.getProperty("templateRepository");
      gitPassword = properties.getProperty("gitPassword");
      usedGitHost = properties.getProperty("usedGitHost");

      baseURL = properties.getProperty("baseURL");
      token = properties.getProperty("token");


      if (Objects.equals(usedGitHost, "GitHub")) {
      	gitAdapter = new GitHubAdapter(gitUser, gitPassword, token, gitOrganization, templateRepository, gitUserMail);
      } else if (Objects.equals(usedGitHost, "GitLab")) {
    	gitAdapter = new GitLabAdapter(baseURL, token, gitUser, gitPassword, gitOrganization, templateRepository, gitUserMail);
      } else {
    	  fail("Property usedGitHost not valid!");
      }



    } catch (IOException ex) {
      fail("Error reading test models and configuration file!");
    }


    // start node
    node = new LocalNodeManager().newNode();
    node.launch();

    serviceNameVersion = new ServiceNameVersion(codeGenerationService, "0.1");
    testService = ServiceAgentImpl.createServiceAgent(serviceNameVersion, "a pass");
    testService.unlock("a pass");
    node.registerReceiver(testService);

    // waiting here not needed because no connector is running!

  }


  /**
   *
   * Called after the tests have finished. Deletes all test repositories and shuts down the server.
   * Just comment out repositories you want to check on after the tests.
   *
   * @throws Exception
   *
   **/
  @AfterClass
  public static void shutDownServer() throws Exception {
    String model1GitHubName = "frontendComponent-" + model1.getName().replace(" ", "-");
    String model2GitHubName = "microservice-" + model2.getName().replace(" ", "-");
    String model3GitHubName = "application-" + model3.getName().replace(" ", "-");
    System.out.println(model1GitHubName);
    System.out.println(model2GitHubName);
    System.out.println(model3GitHubName);

    Thread.sleep(5000);

    try {
      Generator.deleteRemoteRepository("frontendComponent-2", (BaseGitHostAdapter) gitAdapter);
      // Generator.deleteRemoteRepository("microservice-2", (BaseGitHostAdapter) gitAdapter);
      Generator.deleteRemoteRepository("application-1", (BaseGitHostAdapter) gitAdapter);
      Generator.deleteRemoteRepository("Test123", (BaseGitHostAdapter) gitAdapter);
    } catch (GitHostException e) {
      e.printStackTrace();
      // that's ok, maybe some error / failure in previous tests caused this
      // catch it, to make sure that every other repository gets deleted
    }
    node.shutDown();
    node = null;
  }


  /**
   *
   * Posts a new microservice model to the service.
   *
   */
  // @Test
  // public void testCreateMicroservice() {
  //   ArrayList<SimpleModel> simpleArray2 = new ArrayList<SimpleModel>();
  //   simpleArray2.add(model2);
  //   Serializable[] parameters = {"commit", "0.1", "", simpleArray2, null};
  //   try {
  //     String returnMessage = (String) node.invoke(testService,
  //   		  serviceNameVersion, "createFromModel",
  //         parameters);
  //     assertEquals("done", returnMessage.substring(0,4));
  //   } catch (Exception e) {
  //     e.printStackTrace();
  //     fail(e.getMessage());
  //   }
  // }


  /**
   *
   * Posts a new application model to the service.
   *
   */
  @Test
  public void testCreateApplication() {
    ArrayList<SimpleModel> simpleArray3 = new ArrayList<SimpleModel>();
    simpleArray3.add(model3);
    Serializable[] parameters = {"commit", "0.1", "", simpleArray3, new HashMap<String, String>()};
    try {
      String returnMessage = (String) node.invoke(testService,
    		  serviceNameVersion, "createFromModel",
          parameters);
      assertEquals("done", returnMessage.substring(0,4));
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }


  /**
   *
   * Posts a new application model to the service.
   *
   */
  @Test
  public void testGetCommViewModel() {
    ArrayList<SimpleModel> simpleArray = new ArrayList<SimpleModel>();
    simpleArray.add(model3);

    Serializable[] parameters = {(Serializable) simpleArray};
    try {
      SimpleModel simpleModel = (SimpleModel) node.invoke(testService,
    		  serviceNameVersion, "getCommunicationViewOfApplicationModel", parameters);
      // TODO more attributes of model checking
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }


  /**
   *
   * Posts a new frontend component model to the service.
   *
   */
  @Test
  public void testCreateFrontendComponent() {
    ArrayList<SimpleModel> simpleArray1 = new ArrayList<SimpleModel>();
    simpleArray1.add(model1);
    Serializable[] parameters = {"commit", "0.1", "", simpleArray1, null};
    try {
      String returnMessage = (String) node.invoke(testService,
    		  serviceNameVersion, "createFromModel",
          parameters);
      assertEquals("done", returnMessage.substring(0,4));
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }


  /**
   *
   * Posts a new model to the service and then tries to delete it.
   *
   */
  // @Test
  // public void testDeleteFrontEndModel() {
  //   ArrayList<SimpleModel> simpleArray1 = new ArrayList<SimpleModel>();
  //   simpleArray1.add(model1);
  //   Serializable[] parameters = {"commit", "0.1", "", simpleArray1, null};
  //   Serializable[] parameters2 = {simpleArray1};
  //   try {
  //     String returnMessage = (String) node.invoke(testService,
  //   		  serviceNameVersion, "createFromModel",
  //         parameters);
  //     assertEquals("done", returnMessage.substring(0,4));
  //     String returnMessage2 = (String) node.invoke(testService,
  //         serviceNameVersion, "deleteRepositoryOfModel", parameters2);
  //     assertEquals("done", returnMessage2.substring(0,4));
  //   } catch (Exception e) {
  //     e.printStackTrace();
  //     fail(e.getMessage());
  //   }
  // }


  // /**
  //  *
  //  * Posts a new model to the service and then tries to update it (with the same model).
  //  *
  //  */
  // @Test
  // public void testUpdate() {
  //   Serializable[] content = {(Serializable) model2};
  //   Serializable[] parameters = {content};
  //   try {
  //     String returnMessage = (String) node.invoke(testService,serviceNameVersion, "createFromModel",parameters);
  //     assertEquals("done", returnMessage);
  //     returnMessage = (String) node.invoke(testService,serviceNameVersion, "updateRepositoryOfModel", parameters);
  //     assertEquals("done", returnMessage);
  //   } catch (Exception e) {
  //     e.printStackTrace();
  //     fail(e.getMessage());
  //   }
  // }
  //
  /**
   * Internal adapter tests
   */
  @Test
  public void testRepoCreation() {
	  try{
		  gitAdapter.createRepo("Test123", "test description");
	  } catch (Exception e) {
		  e.printStackTrace();
		  fail(e.getMessage());
	  }
  }

  @Test
  public void testRepoDeletion(){
	  try {
		  gitAdapter.createRepo("delTest123", "Test description");
		  Thread.sleep(5000);
		  gitAdapter.deleteRepo("delTest123");
	  } catch (Exception e) {
		e.printStackTrace();
		fail(e.getMessage());
	}
  }

}
