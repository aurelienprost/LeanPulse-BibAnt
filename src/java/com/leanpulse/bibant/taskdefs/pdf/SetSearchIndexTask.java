/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import org.apache.tools.ant.BuildException;

import com.leanpulse.bibant.pdf.PdfProcessor;
import com.leanpulse.bibant.pdf.SetSearchIndex;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class SetSearchIndexTask extends PdfProcessingTask {
	
	private String indexPath = null;
	
	public void setPath(String indexPath) {
		this.indexPath = indexPath;
	}
	
	/* (non-Javadoc)
	 * @see com.leanpulse.pdf.ant.PdfProcessingTask#getProcessor()
	 */
	@Override
	protected PdfProcessor getProcessor() throws BuildException {
		if(indexPath == null)
			throw new BuildException("No index to set.");
		return new SetSearchIndex(indexPath);
	}

}
