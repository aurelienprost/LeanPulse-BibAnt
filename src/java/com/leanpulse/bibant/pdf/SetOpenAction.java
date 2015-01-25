/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.pdf;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionJavaScript;

/**
 * Processor to set the Javascript action executed when the PDF document will be opened.
 * 
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 */
public class SetOpenAction implements PdfProcessor {

	private String openAction;
	
	/**
	 * Creates the processor.
	 * 
	 * @param openAction
	 *            the Javascript code to be executed when the document is opened
	 */
	public SetOpenAction(String openAction) {
		this.openAction = openAction;
	}

	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.pdf.PdfProcessor#processDocument(org.apache.pdfbox.pdmodel.PDDocument)
	 */
	public void processDocument(PDDocument doc) throws IOException {
		PDActionJavaScript javascript = new PDActionJavaScript(openAction);
		doc.getDocumentCatalog().setOpenAction(javascript);
	}

}
