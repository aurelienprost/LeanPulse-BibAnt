/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.pdf;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Interface to implement some processing on an already parsed PDF document.
 * 
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 */
public interface PdfProcessor {
	
	/**
	 * Process the PDF document.
	 * 
	 * @param doc
	 *            the in-memory representation of the PDF document
	 * @throws IOException
	 *             if an error occurred
	 */
	public void processDocument(PDDocument doc) throws IOException;
	
}
