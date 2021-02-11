package i5.las2peer.services.codeGenerationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
import i5.las2peer.services.codeGenerationService.generators.Generator;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModelFactory;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ContentSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.UnprotectedSegment;


/**
 * 
 * Central test class for the CAE-Code-Generation-Service.
 *
 */
public class ModelSynchronizationTest extends Generator {

  private static LocalNode node;


  private static final String codeGenerationService = CodeGenerationService.class.getCanonicalName();


  private static SimpleModel model1;
  private static SimpleModel updatedModel1;
  private static SimpleModel model2;
  private static SimpleModel model3;
  private static SimpleModel updatedModel3;

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


  private static String commitFiles(String prefix, SimpleModel model, TraceModel traceModel)
      throws Exception {
    String repositoryName = prefix + "-" + model.getName().replace(" ", "-");
    List<String[]> fileList = getUpdatedTracedFilesForRepository(traceModel, "{}");

    return commitFilesRaw(repositoryName, fileList);

  }

  private static String commitFilesRaw(String repositoryName, List<String[]> fileList)
      throws Exception {
    return (String) node.invoke(testService, serviceNameVersion,
        "storeAndCommitFilesRaw", new Serializable[] {repositoryName,
            "Code regeneration/Model synchronization", fileList.toArray(new String[][] {})});
  }

  private static SimpleModel loadModel(String path) throws Exception {
    InputStream file = new FileInputStream(path);
    InputStream buffer = new BufferedInputStream(file);
    ObjectInput input = new ObjectInputStream(buffer);
    SimpleModel model = (SimpleModel) input.readObject();
    input.close();
    return model;
  }

  private static String getContent(String path) {
    String content = "";
    try {
      FileInputStream fis = new FileInputStream(path);
      content = IOUtils.toString(fis, "UTF-8");
    } catch (Exception e) {
      e.printStackTrace();
    }

    return content;
  }

  @SuppressWarnings("unchecked")
  private static HashMap<String, JSONObject> getAllTracedFiles(String prefix, SimpleModel model)
      throws Exception {
    String repositoryName = prefix + "-" + model.getName().replace(" ", "-");
    return (HashMap<String, JSONObject>) node.invoke(testService,
        serviceNameVersion, "getAllTracedFiles", new Serializable[] {repositoryName});
  }

  private static void deleteRepositoryOfModel(String prefix, SimpleModel model) {
    String repositoryName = prefix + "-" + model.getName().replace(" ", "-");
    try {
      Generator.deleteRemoteRepository(repositoryName, (BaseGitHostAdapter) gitAdapter);
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      node.invoke(testService, serviceNameVersion, "deleteLocalRepository",
          new Serializable[] {repositoryName});

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static FileTraceModel createFileTraceModel(String content, String fileName,
      JSONObject traces, TraceModel traceModel) throws Exception {
    return FileTraceModelFactory.createFileTraceModelFromJSON(content, traces, traceModel,
        fileName);
  }

  private static FileTraceModel createFileTraceModel(Map<String, JSONObject> tracedFiles,
      String fileName, TraceModel traceModel) throws Exception {
    JSONObject fileJson = tracedFiles.get(fileName);
    String contentBase64 = (String) fileJson.get("content");
    byte[] asBytes = Base64.getDecoder().decode(contentBase64);
    String content = new String(asBytes, "utf-8");
    return FileTraceModelFactory.createFileTraceModelFromJSON(content,
        (JSONObject) fileJson.get("fileTraces"), traceModel, fileName);
  }

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

    Properties properties = new Properties();
    String propertiesFile =
        "./etc/i5.las2peer.services.codeGenerationService.CodeGenerationService.properties";

    try {
      model1 = loadModel("./testModels/JUnitTestWidget1.model");
      updatedModel1 = loadModel("./testModels/JUnitTestWidget2.model");
      model2 = loadModel("./testModels/JUnitTestWidget3.model");

      model3 = loadModel("./testModels/JUnitTestService1.model");
      updatedModel3 = loadModel("./testModels/JUnitTestService2.model");
      
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
  }


  /**
   * 
   * Called after the tests have finished.
   * 
   * @throws Exception
   * 
   **/
  @AfterClass
  public static void shutDownServer() throws Exception {
    node.shutDown();
    node = null;
  }


  /**
   * 
   * Tests if the type of a html element of a widget is changed correctly without overwritten the
   * content of this html element
   * 
   */
  @Test
  public void changeHtmlElementTypeTest() {
    try {
      String changedContent = "changedContent";

      Serializable[] models = {(Serializable) model1};
      Serializable[] parameters = {models};

      String returnMessage =
          (String) node.invoke(testService, serviceNameVersion, "createFromModel", parameters);
      assertEquals("done", returnMessage);

      HashMap<String, JSONObject> tracedFiles = getAllTracedFiles("frontendComponent", model1);

      TraceModel traceModel = new TraceModel();

      for (String fileName : tracedFiles.keySet()) {
        // we are only testing the synchronization of an widget
        if (fileName.equals("index.html")) {
          continue;
        }

        traceModel.addFileTraceModel(createFileTraceModel(tracedFiles, fileName, traceModel));
      }

      FileTraceModel oldFileTraceModel =
          createFileTraceModel(tracedFiles, "index.html", traceModel);
      traceModel.addFileTraceModel(oldFileTraceModel);

      UnprotectedSegment oldHtmlElementContent = (UnprotectedSegment) oldFileTraceModel
          .getRecursiveSegment("d843d5d05463aae9ba656743:htmlElement:$Element_Content$");
      ContentSegment oldHtmlElementType = (ContentSegment) oldFileTraceModel
          .getRecursiveSegment("d843d5d05463aae9ba656743:htmlElement:$Element_Type$");

      // the html element must be an input field
      assertEquals(oldHtmlElementType.getContent().equals("input"), true);
      // the content must be an unprotected segment with integrity check
      assertEquals(oldHtmlElementContent.getHash() != null, true);

      // change the content
      oldHtmlElementContent.setContent(changedContent);

      assertEquals(oldHtmlElementContent.getContent().equals(changedContent), true);

      returnMessage = commitFiles("frontendComponent", model1, traceModel);

      assertEquals(returnMessage, "done");

      // now perform a model synchronization process

      models = new Serializable[] {(Serializable) updatedModel1, (Serializable) model1};
      parameters = new Serializable[] {models};

      returnMessage = (String) node.invoke(testService, serviceNameVersion,
          "updateRepositoryOfModel", parameters);

      assertEquals("done", returnMessage);

      tracedFiles = getAllTracedFiles("frontendComponent", model1);

      FileTraceModel fileTraceModel =
          createFileTraceModel(tracedFiles, "index.html", new TraceModel());

      UnprotectedSegment htmlElementContent = (UnprotectedSegment) fileTraceModel
          .getRecursiveSegment("d843d5d05463aae9ba656743:htmlElement:$Element_Content$");
      ContentSegment htmlElementType = (ContentSegment) fileTraceModel
          .getRecursiveSegment("d843d5d05463aae9ba656743:htmlElement:$Element_Type$");

      // the type of the element must have changed, but not its content
      assertEquals(htmlElementType.getContent().equals("div"), true);
      assertEquals(htmlElementContent.getContent().equals(oldHtmlElementContent.getContent()),
          true);

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      // delete repository for the next tests
      deleteRepositoryOfModel("frontendComponent", model1);
    }
  }

  /**
   * Tests if files are properly created or deleted when a database was added or removed from the
   * model, respectively.
   */
  @Test
  public void microserviceAddDeletionTest() {
    try {
    } catch (Exception e) {

    }
  }

  /**
   * Tests the correct renaming of files when a microservice resource was renamed
   */

  @Test
  public void microserviceRenameTest() {
    try {
      String repositoryName = "microservice-" + model3.getName().replace(" ", "-");
      File serviceClass =
          new File(repositoryName + "/src/main/i5/las2peer/services/nameBefore/nameBefore.java");
      File serviceClassNew =
          new File(repositoryName + "/src/main/i5/las2peer/services/nameAfter/nameAfter.java");
      File serviceProperties =
          new File(repositoryName + "/etc/i5.las2peer.services.nameBefore.nameBefore.properties");
      File servicePropertiesNew =
          new File(repositoryName + "/etc/i5.las2peer.services.nameAfter.nameAfter.properties");

      Serializable[] models = {(Serializable) model3};
      Serializable[] parameters = {models};

      String returnMessage =
          (String) node.invoke(testService, serviceNameVersion, "createFromModel", parameters);
      assertEquals("done", returnMessage);

      // now perform a model synchronization process

      models = new Serializable[] {(Serializable) updatedModel3, (Serializable) model3};
      parameters = new Serializable[] {models};

      returnMessage = (String) node.invoke(testService, serviceNameVersion,
          "updateRepositoryOfModel", parameters);

      assertEquals("done", returnMessage);

      assertEquals(false, serviceClass.exists());
      assertEquals(true, serviceClassNew.exists());

      assertEquals(false, serviceProperties.exists());
      assertEquals(true, servicePropertiesNew.exists());
    } catch (Exception e) {
      fail(e.getMessage());
    } finally {
      // delete repository for the next tests
      deleteRepositoryOfModel("microservice", model3);
    }
  }

  /**
   * Tests if the order of html elements is preserved during the model synchronization.
   */

  @Test
  public void reorderHtmlElementTypeTest() {
    try {
      String repositoryName = "frontendComponent-" + model2.getName().replace(" ", "-");
      Serializable[] models = {(Serializable) model2};
      Serializable[] parameters = {models};
      
      String returnMessage =
          (String) node.invoke(testService, serviceNameVersion, "createFromModel", parameters);
      assertEquals("done", returnMessage);

      String traces = getContent("./testFiles/ModelSynchronization/TestCase3/index.html.traces");
      JSONParser parser = new JSONParser();
      JSONObject tracesJson = (JSONObject) parser.parse(traces);
      String widget = getContent("./testFiles/ModelSynchronization/TestCase3/index.html");

      FileTraceModel oldFileTraceModel =
          createFileTraceModel(widget, "index.html", tracesJson, new TraceModel());
      CompositeSegment oldMainContentSegment = (CompositeSegment) oldFileTraceModel
          .getRecursiveSegment("57209a05b1734235f7bdbe5d:$Main_Content$");
      List<String> oldOrder = oldMainContentSegment.getChildrenList();

      // commit reordered html elements

      List<String[]> fileList = new ArrayList<String[]>();
      fileList.add(new String[] {"traces/index.html.traces",
          Base64.getEncoder().encodeToString(traces.getBytes("utf-8"))});
      fileList.add(new String[] {"index.html",
          Base64.getEncoder().encodeToString(widget.getBytes("utf-8"))});

      returnMessage = commitFilesRaw(repositoryName, fileList);
      assertEquals("done", returnMessage);

      // now perform a model synchronization process

      models = new Serializable[] {(Serializable) model2, (Serializable) model2};
      parameters = new Serializable[] {models};

      returnMessage = (String) node.invoke(testService, serviceNameVersion,
          "updateRepositoryOfModel", parameters);

      assertEquals("done", returnMessage);

      // updated file trace model
      HashMap<String, JSONObject> tracedFiles = getAllTracedFiles("frontendComponent", model1);
      TraceModel traceModel = new TraceModel();

      FileTraceModel fileTraceModel = createFileTraceModel(tracedFiles, "index.html", traceModel);
      CompositeSegment mainContentSegment = (CompositeSegment) fileTraceModel
          .getRecursiveSegment("57209a05b1734235f7bdbe5d:$Main_Content$");
      List<String> newOrder = mainContentSegment.getChildrenList();



      // check right order
      int i = 0;
      for (i = 0; i < oldOrder.size(); i++) {
        String oldId = oldOrder.get(i);
        String newId = newOrder.get(i);
        assertEquals(oldId, newId);
      }

    } catch (Exception e) {
      fail(e.getMessage());
    } finally {
      // delete repository for the next tests
      deleteRepositoryOfModel("frontendComponent", model2);
    }
  }

}
