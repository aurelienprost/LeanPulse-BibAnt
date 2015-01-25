/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.File;

import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.tools.ant.BuildException;

import com.leanpulse.bibant.pdf.Encrypt;
import com.leanpulse.bibant.pdf.PdfProcessor;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class EncryptTask extends PdfProcessingTask {

	private String ownerpassword = null;
	private String userpassword = null;
	private File certfile = null;
	private int keylength = 40;
	private AccessPermission ap = new AccessPermission();
	
	public void setOwnerpassword(String ownerpassword) {
		this.ownerpassword = ownerpassword;
	}
	
	public void setUserpassword(String userpassword) {
		this.userpassword = userpassword;
	}
	
	public void setCertfile(File certfile) {
		this.certfile = certfile;
	}
	
	public void setKeylength(int keylength) {
		this.keylength = keylength;
	}
	
	public void setCanassemble(String boolstr) {
		ap.setCanAssembleDocument(boolstr.equalsIgnoreCase("true"));
	}
	
	public void setCanextractcontent(String boolstr) {
		ap.setCanExtractContent(boolstr.equalsIgnoreCase("true"));
	}
	
	public void setCanextractforaccessibility(String boolstr) {
		ap.setCanExtractForAccessibility(boolstr.equalsIgnoreCase("true"));
	}
	
	public void setCanfillinform(String boolstr) {
		ap.setCanFillInForm(boolstr.equalsIgnoreCase("true"));
	}
	
	public void setCanmodify(String boolstr) {
		ap.setCanModify(boolstr.equalsIgnoreCase("true"));
	}
	
	public void setCanmodifyannotations(String boolstr) {
		ap.setCanModifyAnnotations(boolstr.equalsIgnoreCase("true"));
	}
	
	public void setCanprint(String boolstr) {
		ap.setCanPrint(boolstr.equalsIgnoreCase("true"));
	}
	
	public void setCanprintdegraded(String boolstr) {
		ap.setCanPrintDegraded(boolstr.equalsIgnoreCase("true"));
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.pdf.ant.PdfProcessingTask#getProcessor()
	 */
	@Override
	protected PdfProcessor getProcessor() throws BuildException {
		if(certfile == null) {
			if(ownerpassword == null)
				throw new BuildException("The owner password must be specified.");
			if(userpassword == null)
				throw new BuildException("The user password must be specified.");
		}
		return new Encrypt(ownerpassword, userpassword, ap, certfile, keylength);
	}

}
