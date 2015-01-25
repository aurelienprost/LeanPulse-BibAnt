/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.File;

import org.apache.tools.ant.BuildException;

import com.leanpulse.bibant.pdf.PdfProcessor;
import com.leanpulse.bibant.pdf.SetBookmarkTree;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class SetBookmarkTreeTask extends PdfProcessingTask {
	
	private File fofile;
	
	public void setFofile(File fofile) {
		this.fofile = fofile;
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.pdf.PdfProcessingTask#getProcessor()
	 */
	@Override
	protected PdfProcessor getProcessor() throws BuildException {
		if(fofile == null)
			throw new BuildException("The XSL-FO file defining the bookmark tree must be specified.");
		PdfProcessor processor = null;
		try {
			processor = new SetBookmarkTree(fofile);
		} catch (Exception e) {
			throw new BuildException("An error occured while loading the XSL-FO file " + fofile, e);
		}
		return processor;
	}

}
