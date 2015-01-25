/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.pdf;

import java.io.IOException;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Processor to set the default search index file (*.pdx) of a PDF document.
 * 
 * This search index will then be automatically selected by Adobe Reader when the user
 * will perform an advanced search.
 * 
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 */
public class SetSearchIndex implements PdfProcessor {

	private String indexPath;
	
	/**
	 * Creates a new instance of this processor.
	 * 
	 * @param indexPath
	 *            the absolute or relative path to the search index file.
	 */
	public SetSearchIndex(String indexPath) {
		this.indexPath = indexPath;
	}

	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.pdf.PdfProcessor#processDocument(org.apache.pdfbox.pdmodel.PDDocument)
	 */
	public void processDocument(PDDocument doc) throws IOException {
		COSDictionary root = doc.getDocumentCatalog().getCOSDictionary();
		if(indexPath.length() > 0) {
			COSBase indexFile = root.getObjectFromPath("Search/Indexes/[0]/Index");
			if(indexFile instanceof COSDictionary) {
				((COSDictionary)indexFile).setString(COSName.F, indexPath);
			} else {
				indexFile = new COSDictionary();
				((COSDictionary)indexFile).setString(COSName.F, indexPath);
				((COSDictionary)indexFile).setName(COSName.TYPE, "Filespec");
				COSDictionary index = new COSDictionary();
				index.setName(COSName.NAME, "PDX");
				index.setItem("Index", indexFile);
				COSArray indexArray = new COSArray();
				indexArray.add(index);
				COSDictionary indexes = new COSDictionary();
				indexes.setItem("Indexes", indexArray);
				root.setItem("Search", indexes);
			}
		} else {
			root.removeItem(COSName.getPDFName("Search"));
		}
	}

}
