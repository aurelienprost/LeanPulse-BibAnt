/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.File;

import org.apache.tools.ant.BuildException;

import com.leanpulse.bibant.pdf.Decrypt;
import com.leanpulse.bibant.pdf.PdfProcessor;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class DecryptTask extends PdfProcessingTask {
	
	private String password = null;
	private File keystore = null;
	private String alias = null;
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setKeystore(File keystore) {
		this.keystore = keystore;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.pdf.ant.PdfProcessingTask#getProcessor()
	 */
	@Override
	protected PdfProcessor getProcessor() throws BuildException {
		if(password == null)
			throw new BuildException("The password must be specified.");
		return new Decrypt(password, keystore, alias);
	}

}
