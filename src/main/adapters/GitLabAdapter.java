package adapters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GitLabAdapter {
	// TODO: Add token
	private final static String token = "";
	private final static String baseURL = "http://ginkgo.informatik.rwth-aachen.de:4080/api/v3/";

	private static String getString(String url) {
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
	
	private static boolean deleteResource(String url) {
		HttpURLConnection c = null;
		boolean success = false;
		
		try {
			URL u = new URL(url);
			c = (HttpURLConnection) u.openConnection();
			c.setRequestMethod("DELETE");
			c.setRequestProperty("PRIVATE-TOKEN", token);
			c.setUseCaches(false);
			c.connect();
			int status = c.getResponseCode();
			
			switch (status) {
			case 200:
				success = true;
				break;
			default:
				break;
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
		return success;
	}
	
	private static boolean createResource(String url, JSONObject data) {
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
			c.connect();
			int status = c.getResponseCode();
			
			switch (status) {
			case 201:
				success = true;
				break;
			}
			
		} catch (MalformedURLException e) {
			// TODO: handle exception
		} catch (IOException e) {
			// TODO: handle exception
		} finally {
			if (c != null) {
				c.disconnect();
			}
		}
		return success;
	}
	
	private static JSONObject getJSONObject(String url) {
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
	
	private static JSONArray getJSONArray(String url) {
		String data = getString(url);
		JSONParser parser = new JSONParser();
		JSONArray arr;
		try {
			arr = (JSONArray) parser.parse(data);
		} catch (ParseException e) {
			arr = null;
			e.printStackTrace();
		}
		return arr;
	}
	
	public static boolean deleteRepo(String name, String gitHubOrganization) {
		JSONArray arr = getJSONArray(baseURL + "groups/" + gitHubOrganization + "/projects/");
		boolean success = false;
		// We need to get the id of the repo, search for it
		if (arr != null)
		{
			JSONObject obj = null;
			while (arr.iterator().hasNext()) {
				obj = (JSONObject) arr.iterator().next();
				if (obj.get("name").toString() == name) {
					break;
				}
			}
			
			if(obj != null) {
				int id = (int) obj.get("id");
				// example: http://ginkgo.informatik.rwth-aachen.de:4080/api/v3/projects/2
				success = deleteResource(baseURL + "projects/" + id);
			}
		}
		return success;
	}
	
	public static boolean createRepo(String name, String description) {
		boolean success = createResource(baseURL + "projects");
		
		return false;
	}
	
}
