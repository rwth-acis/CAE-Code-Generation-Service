package i5.las2peer.services.codeGenerationService;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Properties;

import i5.las2peer.services.codeGenerationService.adapters.GitHostAdapter;
import i5.las2peer.services.codeGenerationService.adapters.GitHubAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHostException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;

public class GitHubAdapterTest {

	private static String usedGitHost = null;
	private static String gitOrganization = null;
	private static String gitUser = null;
	private static String gitPassword = null;
	private static String gitUserMail = null;
	private static String templateRepository = null;

	private static String baseURL = null;
	private static String token = null;
	
	
	@BeforeClass
	public static void prepareTest() {
		Properties properties = new Properties();
	    String propertiesFile = "./etc/i5.las2peer.services.codeGenerationService.CodeGenerationService.properties";
	    
	    try {
	    FileReader reader = new FileReader(propertiesFile);
	    properties.load(reader);
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    gitUser = properties.getProperty("gitUser");
	    gitUserMail = properties.getProperty("gitUserMail");
	    gitOrganization = properties.getProperty("gitOrganization");
	    templateRepository = properties.getProperty("templateRepository");
	    gitPassword = properties.getProperty("gitPassword");
	    usedGitHost = properties.getProperty("usedGitHost");
	      
	    baseURL = properties.getProperty("baseURL");
	    token = properties.getProperty("token");
	}
	
	@Test
	public void createRepoTest() {
		GitHostAdapter gitAdapter = null;
		try{
			gitAdapter =  new GitHubAdapter(gitUser, gitPassword, token, gitOrganization, templateRepository, gitUserMail);
			gitAdapter.createRepo("Testrepo", "testdescription");
		} catch (GitHostException e) {
			e.printStackTrace();
			fail();
		}
		
		//Compare with result from GitHub API
		try {
			URL url = new URL("https://api.github.com/repos/" + gitOrganization + "/" + "Testrepo");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setUseCaches(false);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			JSONParser parser = new JSONParser();
			JSONObject object = (JSONObject) parser.parse(response.toString());
			
			assertEquals("Testrepo", object.get("name"));
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		try {
			gitAdapter.deleteRepo("Testrepo");
		}catch(GitHostException e) {
			e.printStackTrace();
			fail();
		} catch(NullPointerException e) {
			e.printStackTrace();
			fail();
		}
		
	}
	
	@Test
	public void deleteRepoTest() {
		GitHostAdapter gitAdapter = null;
		try {
			gitAdapter = new GitHubAdapter(gitUser, gitPassword, token, gitOrganization, templateRepository, gitUserMail);
		} catch (GitHostException e) {
			e.printStackTrace();
			fail();
		}
		
		try {
			gitAdapter.createRepo("Testrepo", "test");
		} catch(GitHostException e) {
			e.printStackTrace();
			fail();
		} catch(NullPointerException e) {
			e.printStackTrace();
			fail();
		}
		
		try {
			gitAdapter.deleteRepo("Testrepo");
		} catch(GitHostException e) {
			e.printStackTrace();
			fail();
		} catch(NullPointerException e) {
			e.printStackTrace();
			fail();
		}
		
		try {
			URL url = new URL("https://api.github.com/repos/" + gitOrganization + "/Testrepo");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("DELETE");
			connection.setUseCaches(false);
			
			String authString = gitUser + ":" + gitPassword;
			byte[] authEncBytes = Base64.getEncoder().encode(authString.getBytes());
			String authStringEnc = new String(authEncBytes,"UTF-8");
			
			connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
			
			assertEquals(404, connection.getResponseCode());
		} catch(IOException e) {
			e.printStackTrace();
			fail();
		}
		
	}
	
}
