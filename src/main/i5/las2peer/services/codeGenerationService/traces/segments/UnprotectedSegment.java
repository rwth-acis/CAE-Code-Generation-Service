package i5.las2peer.services.codeGenerationService.traces.segments;

public class UnprotectedSegment extends ContentSegment {
	private String content;
	public UnprotectedSegment(String id) {
		super(id);
	}

	@Override
	public int getLength() {
		return this.getContent().length();
	}

	@Override
	public void setContent(String content) {
		this.content=content;
	}

	@Override
	public String getContent() {
		return this.content;
	}

	@Override
	public void replace(String pattern, String replacement) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getTypeString() {
		return "unprotected";
	}
}
