package i5.las2peer.services.codeGenerationService.templateEngine;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ContentSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

public class Template {
	private CompositeSegment segment;
	private final TemplateEngine templateEngine;
	
	public Template(CompositeSegment segment, TemplateEngine templateEngine){
		this.segment = segment;
		this.templateEngine = templateEngine;
	}
	
	public FileTraceModel getFileTraceModel(){
		return this.templateEngine.getFileTraceModel();
	}
	
	public TemplateEngine getTemplateEngine() {
		return this.templateEngine;
	}
	
	public void setSegment(CompositeSegment segment){
		this.segment=segment;
	}
	
	public CompositeSegment getSegment(){
		return this.segment;
	}
	
	public String getId(){
		return this.segment.getId();
	}
	
	public String getContent(){
		return this.segment.toString();
	}
	
	public void setVariable(String id, String content){
		this.segment.setSegmentContent(id, content);
	}
	
	public void appendVariable(String id, Template file){
		CompositeSegment containerNew = new CompositeSegment(this.getId() + ":" + id);
		CompositeSegment container = (CompositeSegment) templateEngine.getSegment( this.getId() + ":" + id, containerNew );

		CompositeSegment segment = (CompositeSegment) templateEngine.getSegment(file.getSegment().getId(), file.getSegment());
		System.out.println(this.getId() + ":" + id);
		System.out.println(container == containerNew );
		if(segment == file.getSegment()){
			container.add(segment);
		}
			
		this.segment.setSegment(id,container);
	}

	public void setVariableIfNotSet(String id, String content) {
		String segmentId = this.getId() + ":" + id;
		Segment segment = (Segment) templateEngine.getSegment( segmentId );
		
		if(segment.getId().equals( segmentId )){
			if( segment.toString().equals(id) && segment instanceof ContentSegment){
				((ContentSegment)segment).setContent(content);
			}
		}
	}
	
	/**
	 * Creates a new template of the same template engine as this template
	 * 
	 * @param id	The id of the new template
	 * @param sourceCode	The template source code
	 * @return	The new created template 
	 */
	
	public Template createTemplate(String id, String sourceCode){
		return this.templateEngine.createTemplate(id, sourceCode);
	}

	public void insertBreakLine(String idSuffix, String variable) {
		this.appendVariable(variable, this.templateEngine.createTemplate( this.getId() +  ":breakLine:" + idSuffix, "\n"));
	}

	
	/**
	 * Factory method to create new templates more easily
	 */
	
	public static Template createInitialTemplate(String id, String content){
		FileTraceModel traceModel = new FileTraceModel();
		TemplateEngine engine = new TemplateEngine(new InitialGenerationStrategy(traceModel),traceModel);
		Template template = engine.createTemplate(id, content);
		return template;
	}

}
