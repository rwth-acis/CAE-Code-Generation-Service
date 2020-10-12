package i5.las2peer.services.codeGenerationService.adapters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import i5.las2peer.services.codeGenerationService.exception.GitHostException;

/**
 * 
 * @author jonask
 *	Manages operations on GitLab that are not directly possible via git. Thus the GitLab api is used.
 */
public class GitLabAdapter extends BaseGitHostAdapter{
	//TODO: Exception handling
		
	public GitLabAdapter(String baseURL, String token, String gitUser, String gitPassword, String gitOrganization,
			String templateRepository, String gitUserMail) throws GitHostException {
		super(gitUser, gitPassword, token, gitOrganization, templateRepository, gitUserMail, baseURL);
	}
	
	/**
	 * Helper method: Gets a web resource as a string.
	 * @param url The url to GET from
	 * @return The response.
	 */
	private String getString(String url) {
		HttpURLConnection c = null;
		
		try {
			URL u = new URL(url);
			c = (HttpURLConnection) u.openConnection();
			c.setRequestMethod("GET");
			c.setRequestProperty("PRIVATE-TOKEN", token);
			c.setUseCaches(false);
			c.connect();
			int status = c.getResponseCode();
			
			switch (status) {
			case 200:
			case 201:
				BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
	            StringBuilder sb = new StringBuilder();
	            String line;
	            while ((line = br.readLine()) != null) {
	                sb.append(line+"\n");
	            }
	            br.close();
	            return sb.toString();
			}
			
		} catch (MalformedURLException e) {
			//TODO: Handle exception
		} catch (IOException e) {
			//TODO: Handle exception
		} finally {
			if (c != null) {
				c.disconnect();
			}
		}
		return "";
	}

	/**
	 * Makes a DELETE request on a resource.
	 * @param url The resource to be deleted.
	 * @throws GitHostException
	 */
	private void deleteResource(String url) throws GitHostException {
		HttpURLConnection c = null;
		
		try {
			URL u = new URL(url);
			c = (HttpURLConnection) u.openConnection();
			c.setRequestMethod("DELETE");
			c.setRequestProperty("PRIVATE-TOKEN", token);
			c.setUseCaches(false);
			c.connect();
			int status = c.getResponseCode();
			
			if(status != 200) {
				c.disconnect();
				throw new GitHostException("failed to delete resource with " + status + " at:" + u.getPath());
			}
			
		} catch (MalformedURLException e) {
			throw new GitHostException("Failed to delete resource");
		} catch (IOException e) {
			throw new GitHostException("Failed to delete resource");
		} finally {
			if (c != null) {
				c.disconnect();
			}
		}
	}

	/**
	 * Creates a resource by POSTing a JSON object.
	 * @param url The url to post to.
	 * @param data The JSON object that represents the data to be used.
	 * @throws GitHostException
	 */
	private void createResource(String url, JSONObject data) throws GitHostException {
		HttpURLConnection c = null;
		
		try {
			String body = data.toJSONString();
			URL u = new URL(url);
			c = (HttpURLConnection) u.openConnection();
			c.setRequestMethod("POST");
			c.setRequestProperty("PRIVATE-TOKEN", token);
			c.setRequestProperty("Content-Type", "application/json");
			c.setDoInput(true);
		    c.setDoOutput(true);
			c.setUseCaches(false);
			
			OutputStreamWriter writer = new OutputStreamWriter(c.getOutputStream());
			writer.write(body);
			writer.flush();
			writer.close();
			
			int status = c.getResponseCode();
	
			if (status != 201){
				// forward (in case of) error	  
			    String message = "Error creating repository at: ";
			    BufferedReader reader =
			        new BufferedReader(new InputStreamReader(c.getErrorStream()));
			    for (String line; (line = reader.readLine()) != null;) {
			      message += line;
			    }
			    reader.close();
			    throw new GitHostException(message);	  
			}
			
		} catch (MalformedURLException e) {
			throw new GitHostException("GitLab repo creation went wrong");
		} catch (IOException e) {
			throw new GitHostException("GitLab repo creation went wrong");
		} finally {
			if (c != null) {
				c.disconnect();
			}
		}
	}

	/**
	 * Get a {@link JSONObject} from an URL by parsing the response.
	 * @param url The url to GET from.
	 * @return The {@link JSONObject} created from the response.
	 */
	private JSONObject getJSONObject(String url) {
		String data = getString(url);
		JSONParser parser = new JSONParser();
		JSONObject obj;
		try {
			obj = (JSONObject) parser.parse(data);
		} catch (ParseException e) {
			obj = null;
			e.printStackTrace();
		}
		return obj;
	}

	/**
	 * Get a {@link JSONArray} from an URL by parsing the response.
	 * @param url The url to GET from
	 * @return The {@link JSONArray} created from the response.
	 * @throws ParseException
	 */
	private JSONArray getJSONArray(String url) throws ParseException {
		String data = getString(url);
		JSONParser parser = new JSONParser();
		JSONArray arr;
		arr = (JSONArray) parser.parse(data);
		return arr;
	}
	
	/**
	 * Delete a remote repository on GitLab using the GitLab API. For this we have to get the id of the project by searching
	 * the projects of the group.
	 * @param name The repo to be deleted.
	 * @throws GitHostException
	 * 		Throws if something goes wrong with the GitHost
	 */
	public void deleteRepo(String name) throws GitHostException {
		long id = -1;
		try {
		JSONArray arr = getJSONArray(baseURL + "api/v4/" + "groups/" + this.gitOrganization + "/projects/");
		// We need to get the id of the repo, search for it
		for(Object obj : arr){
			if (((JSONObject) obj).get("name").toString().equalsIgnoreCase(name)) {
				id = (long) ((JSONObject)obj).get("id");
			}
		}
		
		if (id != -1) {
			// example: http://ginkgo.informatik.rwth-aachen.de:4080/api/v4/projects/2
			deleteResource(baseURL + "api/v4/" + "projects/" + id);
		}
		
		}catch (ParseException e) {
			throw new GitHostException("Failed to delete repo");
		}
	}
	
	/**
	 * Create a repository on GitLab using the GitLab API.
	 * @param name The name of the repository.
	 * @param description The description.
	 */
	@SuppressWarnings("unchecked")
	public void createRepo(String name, String description) throws GitHostException {
		//Get namespace id for group
		JSONObject result = getJSONObject(baseURL + "api/v4/" + "groups/" + this.getGitOrganization());
		long id = (long) result.get("id");
		//Create json object representing new repo
		JSONObject obj = new JSONObject();
		obj.put("name", name);
		obj.put("description", description);
		obj.put("namespace_id", id);
		createResource(baseURL + "api/v4/" + "projects", obj);
	}
	
}
