/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSArrayList;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDFieldFactory;
import org.apache.pdfbox.util.PDFCloneUtility;

/**
 * Merges PDF files to a single document. 
 * 
 * @author <a href="mailto:a.prost@leanpulse.com">Aur�lien PROST</a>
 */
public class PdfMerger {

	private List<InputStream> sources;
	private String destinationFileName;
	private OutputStream destinationStream;
	private boolean ignoreAcroFormErrors = false;

	/**
	 * Instantiate a new PDFMergerUtility.
	 */
	public PdfMerger() {
		sources = new ArrayList<InputStream>();
	}

	/**
	 * Get the name of the destination file.
	 * 
	 * @return Returns the destination.
	 */
	public String getDestinationFileName() {
		return destinationFileName;
	}

	/**
	 * Set the name of the destination file.
	 * 
	 * @param destination
	 *            The destination to set.
	 */
	public void setDestinationFileName(String destination) {
		this.destinationFileName = destination;
	}

	/**
	 * Get the destination OutputStream.
	 * 
	 * @return Returns the destination OutputStream.
	 */
	public OutputStream getDestinationStream() {
		return destinationStream;
	}

	/**
	 * Set the destination OutputStream.
	 * 
	 * @param destination
	 *            The destination to set.
	 */
	public void setDestinationStream(OutputStream destinationStream) {
		this.destinationStream = destinationStream;
	}

	/**
	 * Add a source file to the list of files to merge.
	 * 
	 * @param source
	 *            Full path and file name of source document.
	 */
	public void addSource(String source) {
		try {
			sources.add(new FileInputStream(new File(source)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add a source file to the list of files to merge.
	 * 
	 * @param source
	 *            File representing source document
	 */
	public void addSource(File source) {
		try {
			sources.add(new FileInputStream(source));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add a source to the list of documents to merge.
	 * 
	 * @param source
	 *            InputStream representing source document
	 */
	public void addSource(InputStream source) {
		sources.add(source);
	}

	/**
	 * Add a list of sources to the list of documents to merge.
	 * 
	 * @param source
	 *            List of InputStream objects representing source documents
	 */
	public void addSources(List<InputStream> sources) {
		this.sources.addAll(sources);
	}

	/**
	 * Merge the list of source documents, saving the result in the destination
	 * file.
	 * 
	 * @throws IOException
	 *             If there is an error saving the document.
	 * @throws COSVisitorException
	 *             If an error occurs while saving the destination file.
	 */
	public void mergeDocuments() throws IOException, COSVisitorException {
		PDDocument destination = null;
		InputStream sourceFile;
		PDDocument source = null;
		if (sources != null && sources.size() > 0) {
			try {
				Iterator<InputStream> sit = sources.iterator();
				sourceFile = sit.next();
				destination = PDDocument.load(sourceFile);

				while (sit.hasNext()) {
					sourceFile = sit.next();
					source = PDDocument.load(sourceFile);
					appendDocument(destination, source);
					source.close();
				}
				if (destinationStream == null) {
					destination.save(destinationFileName);
				} else {
					destination.save(destinationStream);
				}
			} finally {
				if(destination != null)
					destination.close();
				if(source != null)
					source.close();
				destination = null;
				sourceFile = null;
				source = null;
				System.gc();
			}
		}
	}

	/**
	 * Append all pages from source to destination.
	 * 
	 * @param destination
	 *            the document to receive the pages
	 * @param source
	 *            the document originating the new pages
	 * 
	 * @throws IOException
	 *             If there is an error accessing data from either document.
	 */
	public void appendDocument(PDDocument destination, PDDocument source)
			throws IOException {
		if (destination.isEncrypted()) {
			throw new IOException(
					"Error: destination PDF is encrypted, can't append encrypted PDF documents.");
		}
		if (source.isEncrypted()) {
			throw new IOException(
					"Error: source PDF is encrypted, can't append encrypted PDF documents.");
		}
		PDDocumentInformation destInfo = destination.getDocumentInformation();
		PDDocumentInformation srcInfo = source.getDocumentInformation();
		destInfo.getDictionary().mergeInto(srcInfo.getDictionary());

		PDDocumentCatalog destCatalog = destination.getDocumentCatalog();
		PDDocumentCatalog srcCatalog = source.getDocumentCatalog();

		if (destCatalog.getOpenAction() == null) {
			destCatalog.setOpenAction(srcCatalog.getOpenAction());
		}

		PDFCloneUtility cloner = new PDFCloneUtility(destination);

		try {
			PDAcroForm destAcroForm = destCatalog.getAcroForm();
			PDAcroForm srcAcroForm = srcCatalog.getAcroForm();
			if (destAcroForm == null) {
				cloner.cloneForNewDocument(srcAcroForm);
				destCatalog.setAcroForm(srcAcroForm);
			} else {
				if (srcAcroForm != null) {
					mergeAcroForm(cloner, destAcroForm, srcAcroForm);
				}
			}
		} catch (Exception e) {
			// if we are not ignoring exceptions, we'll re-throw this
			if (!ignoreAcroFormErrors) {
				throw (IOException) e;
			}
		}

		COSArray destThreads = (COSArray) destCatalog.getCOSDictionary()
				.getDictionaryObject(COSName.THREADS);
		COSArray srcThreads = (COSArray) cloner.cloneForNewDocument(destCatalog
				.getCOSDictionary().getDictionaryObject(COSName.THREADS));
		if (destThreads == null) {
			destCatalog.getCOSDictionary().setItem(COSName.THREADS, srcThreads);
		} else {
			destThreads.addAll(srcThreads);
		}

		PDDocumentNameDictionary destNames = destCatalog.getNames();
		PDDocumentNameDictionary srcNames = srcCatalog.getNames();
		if (srcNames != null) {
			if (destNames == null) {
				destCatalog.getCOSDictionary().setItem(COSName.NAMES,
						cloner.cloneForNewDocument(srcNames));
			} else {
				cloner.cloneMerge(srcNames, destNames);
			}

		}

		destCatalog.setDocumentOutline(null);
		destCatalog.setPageMode(PDDocumentCatalog.PAGE_MODE_USE_NONE);

		COSDictionary destLabels = (COSDictionary) destCatalog
				.getCOSDictionary().getDictionaryObject(COSName.PAGE_LABELS);
		COSDictionary srcLabels = (COSDictionary) srcCatalog.getCOSDictionary()
				.getDictionaryObject(COSName.PAGE_LABELS);
		if (srcLabels != null) {
			int destPageCount = destination.getNumberOfPages();
			COSArray destNums = null;
			if (destLabels == null) {
				destLabels = new COSDictionary();
				destNums = new COSArray();
				destLabels.setItem(COSName.NUMS, destNums);
				destCatalog.getCOSDictionary().setItem(COSName.PAGE_LABELS,
						destLabels);
			} else {
				destNums = (COSArray) destLabels
						.getDictionaryObject(COSName.NUMS);
			}
			COSArray srcNums = (COSArray) srcLabels
					.getDictionaryObject(COSName.NUMS);
			if (srcNums != null) {
				for (int i = 0; i < srcNums.size(); i += 2) {
					COSNumber labelIndex = (COSNumber) srcNums.getObject(i);
					long labelIndexValue = labelIndex.intValue();
					destNums.add(COSInteger
							.get(labelIndexValue + destPageCount));
					destNums.add(cloner.cloneForNewDocument(srcNums
							.getObject(i + 1)));
				}
			}
		}

		COSStream destMetadata = (COSStream) destCatalog.getCOSDictionary()
				.getDictionaryObject(COSName.METADATA);
		COSStream srcMetadata = (COSStream) srcCatalog.getCOSDictionary()
				.getDictionaryObject(COSName.METADATA);
		if (destMetadata == null && srcMetadata != null) {
			PDStream newStream = new PDStream(destination,
					srcMetadata.getUnfilteredStream(), false);
			newStream.getStream().mergeInto(srcMetadata);
			newStream.addCompression();
			destCatalog.getCOSDictionary().setItem(COSName.METADATA, newStream);
		}

		// finally append the pages
		List<?> pages = source.getDocumentCatalog().getAllPages();
		Iterator<?> pageIter = pages.iterator();
		while (pageIter.hasNext()) {
			PDPage page = (PDPage) pageIter.next();
			PDPage newPage = new PDPage(
					(COSDictionary) cloner.cloneForNewDocument(page
							.getCOSDictionary()));
			newPage.setCropBox(page.findCropBox());
			newPage.setMediaBox(page.findMediaBox());
			newPage.setRotation(page.findRotation());
			destination.addPage(newPage);
		}
	}

	private int nextFieldNum = 1;

	/**
	 * Merge the contents of the source form into the destination form for the
	 * destination file.
	 * 
	 * @param cloner
	 *            the object cloner for the destination document
	 * @param destAcroForm
	 *            the destination form
	 * @param srcAcroForm
	 *            the source form
	 * @throws IOException
	 *             If an error occurs while adding the field.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void mergeAcroForm(PDFCloneUtility cloner, PDAcroForm destAcroForm,
			PDAcroForm srcAcroForm) throws IOException {
		List destFields = destAcroForm.getFields();
		List srcFields = srcAcroForm.getFields();
		if (srcFields != null) {
			if (destFields == null) {
				destFields = new COSArrayList();
				destAcroForm.setFields(destFields);
			}
			Iterator srcFieldsIterator = srcFields.iterator();
			while (srcFieldsIterator.hasNext()) {
				PDField srcField = (PDField) srcFieldsIterator.next();
				PDField destField = PDFieldFactory.createField(destAcroForm,
						(COSDictionary) cloner.cloneForNewDocument(srcField
								.getDictionary()));
				// if the form already has a field with this name then we need
				// to rename this field
				// to prevent merge conflicts.
				if (destAcroForm.getField(destField.getFullyQualifiedName()) != null) {
					destField.setPartialName("dummyFieldName"
							+ (nextFieldNum++));
				}
				destFields.add(destField);
			}
		}
	}

	public boolean isIgnoreAcroFormErrors() {
		return ignoreAcroFormErrors;
	}

	public void setIgnoreAcroFormErrors(boolean ignoreAcroFormErrors) {
		this.ignoreAcroFormErrors = ignoreAcroFormErrors;
	}

}
