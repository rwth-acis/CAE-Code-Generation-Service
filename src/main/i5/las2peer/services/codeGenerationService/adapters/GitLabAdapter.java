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

public class GitLabAdapter extends BaseGitHostAdapter{
	//TODO: Exception handling

	private String token;
		
	public GitLabAdapter(String baseURL, String token, String gitUser, String gitPassword, String gitOrganization, 
			String templateRepository, String gitUserMail) {
		super(gitUser, gitPassword, gitOrganization, templateRepository, gitUserMail, baseURL);
		this.token = token;

	}
	
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

	private boolean createResource(String url, JSONObject data) throws GitHostException {
		HttpURLConnection c = null;
		boolean success = false;
		
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
	
			if (status == 201) {
				success = true;
			}else {
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
		return success;
	}

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

	private JSONArray getJSONArray(String url) throws ParseException {
		String data = getString(url);
		JSONParser parser = new JSONParser();
		JSONArray arr;
		arr = (JSONArray) parser.parse(data);
		return arr;
	}
	
	public void deleteRepo(String name) throws GitHostException {
		long id = -1;
		try {
		JSONArray arr = getJSONArray(baseURL + "groups/" + this.gitOrganization + "/projects/");
		// We need to get the id of the repo, search for it
		for(Object obj : arr){
			if (((JSONObject) obj).get("name").toString().equalsIgnoreCase(name)) {
				id = (long) ((JSONObject)obj).get("id");
			}
		}
		
		if (id == -1) {
			throw new GitHostException("Could not find id for project " + name + " with: " + arr.toJSONString());
		}
		
		}catch (ParseException e) {
			throw new GitHostException("Failed to delete repo");
		}
		// example: http://ginkgo.informatik.rwth-aachen.de:4080/api/v3/projects/2
		deleteResource(baseURL + "projects/" + id);
	}
	
	public void createRepo(String name, String description) throws GitHostException {
		//Get namespace id for group
		JSONObject result = getJSONObject(baseURL + "groups/" + this.getGitOrganization());
		long id = (long) result.get("id");
		//Create json object representing new repo
		JSONObject obj = new JSONObject();
		obj.put("name", name);
		obj.put("description", description);
		obj.put("namespace_id", id);
		createResource(baseURL + "projects", obj);
	}
	
}
