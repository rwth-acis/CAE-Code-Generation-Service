package i5.las2peer.services.codeGenerationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
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
    String modelPath1 = "./testModels/My First Testservice.model";
    String modelPath2 = "./testModels/My First Testservice without DB.model";
    String modelPath3 = "./testModels/My Test Widget.model";
    String[] modelPathes4 = new String[7];
    modelPathes4[0] = "./testModels/applicationTestModel/CAE Example Application.model";
    modelPathes4[1] = "./testModels/applicationTestModel/Graph Widget.model";
    modelPathes4[2] = "./testModels/applicationTestModel/LAS2peer Load Store Graph Service.model";
    modelPathes4[3] = "./testModels/applicationTestModel/LAS2peer Video List Service.model";
    modelPathes4[4] = "./testModels/applicationTestModel/Load Store Widget.model";
    modelPathes4[5] = "./testModels/applicationTestModel/Video List Widget.model";
    modelPathes4[6] = "./testModels/applicationTestModel/Video Player Widget.model";

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
      model4 = new SimpleModel[modelPathes4.length];
      int i = 0;
      for (String modelPath4 : modelPathes4) {
        InputStream file4 = new FileInputStream(modelPath4);
        InputStream buffer4 = new BufferedInputStream(file4);
        ObjectInput input4 = new ObjectInputStream(buffer4);
        model4[i] = (SimpleModel) input4.readObject();
        input4.close();
        i++;
      }
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
    String model1GitHubName = "microservice-" + model1.getName().replace(" ", "-");
    String model2GitHubName = "microservice-" + model2.getName().replace(" ", "-");
    String model3GitHubName = "frontendComponent-" + model3.getName().replace(" ", "-");
    String model4GitHubName = "application-" + model4[0].getName().replace(" ", "-");

    Thread.sleep(5000);
    
    try {
      Generator.deleteRemoteRepository(model1GitHubName, (BaseGitHostAdapter) gitAdapter);
    } catch (GitHostException e) {
      e.printStackTrace();
      // that's ok, maybe some error / failure in previous tests caused this
      // catch it, to make sure that every other repository gets deleted
    }
    try {
      Generator.deleteRemoteRepository(model2GitHubName, (BaseGitHostAdapter) gitAdapter);
    } catch (GitHostException e) {
      e.printStackTrace();
      // that's ok, maybe some error / failure in previous tests caused this
      // catch it, to make sure that every other repository gets deleted
    }
    try {
      Generator.deleteRemoteRepository(model3GitHubName, (BaseGitHostAdapter) gitAdapter);
    } catch (GitHostException e) {
      e.printStackTrace();
      // that's ok, maybe some error / failure in previous tests caused this
      // catch it, to make sure that every other repository gets deleted
    }
    try {
      gitAdapter.deleteRepo("Test123");
      Generator.deleteRemoteRepository(model4GitHubName, (BaseGitHostAdapter) gitAdapter);
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
  @Test
  public void testCreateMicroservice() {
    Serializable[] content = {(Serializable) model1};
    Serializable[] parameters = {content};
    try {
      String returnMessage = (String) node.invoke(testService,serviceNameVersion, "createFromModel",parameters);
      assertEquals("done", returnMessage);
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
  
  public void testCreateApplication() {
    Serializable[] parameters = {(Serializable) model4};
    try {
      String returnMessage = (String) node.invoke(testService,
    		  serviceNameVersion, "createFromModel",
          parameters);
      assertEquals("done", returnMessage);
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
    Serializable[] parameters = {(Serializable) model4};
    try {
      SimpleModel simpleModel = (SimpleModel) node.invoke(testService,
    		  serviceNameVersion, "getCommunicationViewOfApplicationModel", parameters);
      assertEquals("CAE Example Application", simpleModel.getName());
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
    Serializable[] content = {(Serializable) model3};
    Serializable[] parameters = {content};
    try {
      String returnMessage = (String) node.invoke(testService,
    		  serviceNameVersion, "createFromModel",
          parameters);
      assertEquals("done", returnMessage);
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
  @Test
  public void testDeleteModel() {
    Serializable[] content = {(Serializable) model2};
    Serializable[] parameters = {content};
    try {
      String returnMessage = (String) node.invoke(testService,
    		  serviceNameVersion, "createFromModel",
          parameters);
      assertEquals("done", returnMessage);
      returnMessage = (String) node.invoke(testService,
          serviceNameVersion, "deleteRepositoryOfModel", parameters);
      assertEquals("done", returnMessage);
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }


  /**
   * 
   * Posts a new model to the service and then tries to update it (with the same model).
   * 
   */
  @Test
  public void testUpdate() {
    Serializable[] content = {(Serializable) model2};
    Serializable[] parameters = {content};
    try {
      String returnMessage = (String) node.invoke(testService,serviceNameVersion, "createFromModel",parameters);
      assertEquals("done", returnMessage);
      returnMessage = (String) node.invoke(testService,serviceNameVersion, "updateRepositoryOfModel", parameters);
      assertEquals("done", returnMessage);
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
  
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
