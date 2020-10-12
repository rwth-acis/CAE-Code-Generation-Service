package i5.las2peer.services.codeGenerationService.adapters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import org.json.simple.JSONObject;

import i5.las2peer.services.codeGenerationService.exception.GitHostException;

/**
 * 
 * @author jonask
 * Manages operations on GitHub that are not directly possible via git. Thus the GitHub api is used.
 */
public class GitHubAdapter extends BaseGitHostAdapter {
	//TODO: Exception handling
	
	private GitHubAdapter(String gitUser, String gitPassword, String personalAccessToken, String gitOrganization,
			String templateRepository, String gitUserMail, String baseURL) throws GitHostException {
		super(gitUser, gitPassword, personalAccessToken, gitOrganization, templateRepository, gitUserMail, baseURL);
	}

	public GitHubAdapter(String gitUser, String gitPassword, String personalAccessToken, String gitOrganization,
			String templateRepository, String gitUserMail) throws GitHostException {
		this(gitUser, gitPassword, personalAccessToken, gitOrganization, templateRepository, gitUserMail, "https://github.com/");
	}

	/**
	 * Creates a repository on GitHub using the GitHub api.
	 * @param name the value to be used as a name for the repository.
	 * @param description the repository description.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void createRepo(String name, String description) throws GitHostException {
		
	  JSONObject jsonObject = new JSONObject();
	  jsonObject.put("name", name);
	  jsonObject.put("description", description);
	  String body = JSONObject.toJSONString(jsonObject);
	
	  String authString = this.getToken();
	  
	  byte[] authEncBytes = Base64.getEncoder().encode(authString.getBytes());
	  String authStringEnc;
	  
	  URL url;
	  try {
		  url = new URL("https://api.github.com/orgs/" + this.gitOrganization + "/repos");
		  authStringEnc = new String(authEncBytes);
	  
		  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		  connection.setRequestMethod("POST");
		  connection.setDoInput(true);
		  connection.setDoOutput(true);
		  connection.setUseCaches(false);
		  connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
		  connection.setRequestProperty("Content-Type", "application/vnd.github.v3+json");
		  connection.setRequestProperty("Content-Length", String.valueOf(body.length()));
		  connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
		
		  OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
		  writer.write(body);
		  writer.flush();
		  writer.close();
		
		  // forward (in case of) error
		  if (connection.getResponseCode() != 201) {
		    String message = "Error creating repository at: ";
		    BufferedReader reader =
		        new BufferedReader(new InputStreamReader(connection.getErrorStream()));
		    for (String line; (line = reader.readLine()) != null;) {
		      message += line;
		    }
		    reader.close();
		    throw new GitHostException(message);
		  }
	  } catch (MalformedURLException e) {
			e.printStackTrace();
			throw new GitHostException(e.getMessage());
	  } catch (IOException e) {
		  	e.printStackTrace();
		  	throw new GitHostException(e.getMessage());
	  }
	}

	/**
	 * Deletes a GitHub repository using the GitHub api.
	 * @param name The repository to delete
	 */
	@Override
	public void deleteRepo(String name) throws GitHostException {
		String authString = this.getToken();
		byte[] authEncBytes = Base64.getEncoder().encode(authString.getBytes());
		String authStringEnc = new String(authEncBytes);
		try {
			URL url = new URL("https://api.github.com/repos/" + this.gitOrganization + "/" + name);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("DELETE");
			connection.setUseCaches(false);
			connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
	
			// forward (in case of) error
			if (connection.getResponseCode() != 204) {
				String message = "Error deleting repository: ";
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
				for (String line; (line = reader.readLine()) != null;) {
					message += line;
				}
				reader.close();
				throw new GitHostException(message);
			}
	  }catch (IOException e) {
		  throw new GitHostException(e.getMessage());
	  }
			
	}
}
