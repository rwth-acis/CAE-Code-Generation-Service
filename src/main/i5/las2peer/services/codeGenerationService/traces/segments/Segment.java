package i5.las2peer.services.codeGenerationService.traces.segments;

import org.json.simple.JSONObject;

public abstract class Segment {
	private String id;
	public Segment(String id){
		this.id=id;
	}
	
	public String getId(){
		return this.id;
	}
	

	public abstract void replace(String pattern, String replacement);
	public abstract String getTypeString();
	
	public abstract Object toJSONObject();
}
