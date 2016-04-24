package i5.las2peer.services.codeGenerationService.templateEngine;

import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.traces.segments.CompositeSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.ContentSegment;
import i5.las2peer.services.codeGenerationService.traces.segments.Segment;

public class SynchronizationStrategy extends TemplateStrategy{
	private final FileTraceModel traceModel;
	
	public SynchronizationStrategy(FileTraceModel traceModel){
		this.traceModel = traceModel;
	}
	
	@Override
	public void setSegmentContent(Segment segment, String content, String id) {
		//if the segment is a composition, we need to set the content recursively by the setSegmentContent function
		if(segment instanceof CompositeSegment){
			((CompositeSegment)segment).setSegmentContent(id, content);
		}
		//otherwise we can directly set the new content
		else if(segment instanceof ContentSegment){
			((ContentSegment)segment).setContent(content);
		}
		
	}

	@Override
	public Segment addSegment(String id, Segment segment) {
		//check if a segment with the given id already exists
		Segment result = this.traceModel.getRecursiveSegment(id);
		//if so, return it
		if(result != null){
			return result;
		}
		//otherwise add it to the trace model
		else{
			this.traceModel.addSegment(segment);
		}
		return segment;
	}

}
