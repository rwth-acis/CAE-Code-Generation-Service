package i5.las2peer.services.codeGenerationService.templateEngine;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ContentSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

/**
 * A class representing a single template. Provides methods to set variables, defined in template source files
 * 
 * @author Thomas Winkler
 *
 */

public class Template {
	
	//each template holds a private composite segment
	private CompositeSegment segment;
	
	//the reference to its template engine
	private final TemplateEngine templateEngine;
	
	/**
	 * Creates a new Template by a given composite segment and a template engine
	 * 
	 * @param segment					- The private composite segment of the template
	 * @param templateEngine	-	The template engine of the template
	 */
	
	public Template(CompositeSegment segment, TemplateEngine templateEngine){
		this.segment = segment;
		this.templateEngine = templateEngine;
	}
	
	/**
	 * Get the trace model of the template engine
	 * 
	 * @return	The trace model of the template engine
	 */
	
	public FileTraceModel getFileTraceModel(){
		return this.templateEngine.getFileTraceModel();
	}
	
	/**
	 * Get the template engine of the template
	 * 
	 * @return	The template engine of the template
	 */
	
	public TemplateEngine getTemplateEngine() {
		return this.templateEngine;
	}
	
	/**
	 * Sets the composite segment, needed for model synchronization
	 * @param segment	-	The new composite segment
	 */
	
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
	
	
	/**
	 * Sets the content of a variable within this template
	 * 
	 * @param variableName	- The name of the variable to set its content
	 * @param content				-	The content that will be set
	 */
	
	public void setVariable(String variableName, String content){
		this.segment.setSegmentContent(variableName, content);
	}

	/**
	 * Adds a template to a variable within this template
	 * 
	 * @param variableName	-	The name of the variable
	 * @param template			- The template that sould be added to the variable
	 */
	
	public void appendVariable(String variableName, Template template){
		CompositeSegment containerNew = new CompositeSegment(this.getId() + ":" + variableName);
		CompositeSegment container = (CompositeSegment) templateEngine.getSegment( this.getId() + ":" + variableName, containerNew );
		CompositeSegment segment = (CompositeSegment) templateEngine.getSegment(template.getSegment().getId(), template.getSegment());

		if(segment == template.getSegment()){
			container.add(segment);
		}
			
		this.segment.setSegment(variableName,container);
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
