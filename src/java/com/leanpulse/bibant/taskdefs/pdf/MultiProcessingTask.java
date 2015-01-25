/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tools.ant.BuildException;

import com.leanpulse.bibant.pdf.PdfProcessor;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class MultiProcessingTask extends PdfProcessingTask {
	
	private static class MultiPdfProcessor implements PdfProcessor {
		PdfProcessor[] processors;
		
		public MultiPdfProcessor(PdfProcessor[] processors) {
			this.processors = processors;
		}
		
		/* (non-Javadoc)
		 * @see com.leanpulse.bibant.pdf.PdfProcessor#processDocument(org.apache.pdfbox.pdmodel.PDDocument)
		 */
		public void processDocument(PDDocument doc) throws IOException {
			for(int i=0; i<processors.length; i++) {
				processors[i].processDocument(doc);
			}
		}
	}
	
	private List<PdfProcessingTask> procTasks = new ArrayList<PdfProcessingTask>();
	
	public void addConfigured(PdfProcessingTask task) {
		procTasks.add(task);
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.pdf.ant.PdfProcessingTask#getProcessor()
	 */
	@Override
	protected PdfProcessor getProcessor() throws BuildException {
		PdfProcessor[] processors = new PdfProcessor[procTasks.size()];
		
		// Call getProcessor() on each task also ensures all parameters are validated
		for(int i=0; i<procTasks.size(); i++) {
			processors[i] = procTasks.get(i).getProcessor();
		}
		
		return new MultiPdfProcessor(processors);
	}

}
