/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import org.apache.tools.ant.BuildException;

import com.leanpulse.bibant.pdf.PdfProcessor;
import com.leanpulse.bibant.pdf.SetOpenAction;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class SetOpenActionTask extends PdfProcessingTask {
	
	private String action = null;
	
	public void setOpenaction(String action) {
		this.action = action;
	}
	
	public void addText(String action) {
		this.action = action.trim();
	}
	
	/* (non-Javadoc)
	 * @see com.leanpulse.pdf.ant.PdfProcessingTask#getProcessor()
	 */
	@Override
	protected PdfProcessor getProcessor() throws BuildException {
		if(action == null)
			throw new BuildException("No open action to set.");
		return new SetOpenAction(action);
	}

}
