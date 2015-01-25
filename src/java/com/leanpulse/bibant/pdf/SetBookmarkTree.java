/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.pdf;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.common.filespecification.PDSimpleFileSpecification;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionRemoteGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Processor to set the bookmark tree of a PDF document.
 * 
 * <p>
 * This processor takes as input an XML file whose schema must conform to the XSL-FO
 * <code>fo:bookmark-tree</code> element definition.
 * </p>
 * <p>An example is given below:</p>
 * <pre>
 * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 * &lt;fo:bookmark-tree xmlns:fo=&quot;http://www.w3.org/1999/XSL/Format&quot;&gt;
 *    &lt;fo:bookmark external-destination=&quot;../Root.pdf&quot;&gt;
 *       &lt;fo:bookmark-title&gt;Documentation Root&lt;/fo:bookmark-title&gt;
 *    &lt;/fo:bookmark&gt;
 *    &lt;fo:bookmark external-destination=&quot;Volume_1.pdf&quot;&gt;
 *       &lt;fo:bookmark-title&gt;Volume 1&lt;/fo:bookmark-title&gt;
 *       &lt;fo:bookmark external-destination=&quot;Chapter_1.pdf&quot;&gt;
 *          &lt;fo:bookmark-title&gt;Chapter 1&lt;/fo:bookmark-title&gt;
 *          &lt;fo:bookmark external-destination=&quot;Document_1.pdf&quot;&gt;
 *             &lt;fo:bookmark-title&gt;Document 1&lt;/fo:bookmark-title&gt;
 *          &lt;/fo:bookmark&gt;
 *          &lt;fo:bookmark external-destination=&quot;Document_2.pdf&quot;&gt;
 *             &lt;fo:bookmark-title&gt;Document 2&lt;/fo:bookmark-title&gt;
 *          &lt;/fo:bookmark&gt;
 *          &lt;fo:bookmark external-destination=&quot;Document_3.pdf&quot;&gt;
 *             &lt;fo:bookmark-title&gt;Document 3&lt;/fo:bookmark-title&gt;
 *          &lt;/fo:bookmark&gt;
 *          &lt;fo:bookmark external-destination=&quot;Document_4.pdf&quot;&gt;
 *             &lt;fo:bookmark-title&gt;Document 4&lt;/fo:bookmark-title&gt;
 *          &lt;/fo:bookmark&gt;
 *       &lt;/fo:bookmark&gt;
 *    &lt;/fo:bookmark&gt;
 * &lt;/fo:bookmark-tree&gt;</pre>
 * 
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 */
public class SetBookmarkTree implements PdfProcessor {
	
	private PDDocumentOutline outline;
	
	/**
	 * Creates the processor.
	 * 
	 * @param xmlFile
	 *            the XML file defining the bookmark tree to set
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public SetBookmarkTree(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xmlFile);
		Element root = doc.getDocumentElement();
		
		outline = new PDDocumentOutline();
		
		NodeList childNodes = root.getChildNodes();
		for(int i=0; i < childNodes.getLength() ; i++) {
			Node node = childNodes.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element) node;
				String elName = el.getNodeName();
				if(elName.equals("fo:bookmark")) {
					outline.appendChild(getBookmark(el));
				}
			}
		}
	}
	
	private PDOutlineItem getBookmark(Element el) {
		PDOutlineItem item = new PDOutlineItem();
		if(el.hasAttribute("external-destination")) {
			PDSimpleFileSpecification fileSpec = new PDSimpleFileSpecification();
			fileSpec.setFile(el.getAttribute("external-destination"));
			PDActionRemoteGoTo action = new PDActionRemoteGoTo();
			action.setFile(fileSpec);
			item.setAction(action);
		} else if(el.hasAttribute("internal-destination")) {
			PDNamedDestination dest = new PDNamedDestination(el.getAttribute("internal-destination"));
			item.setDestination(dest);
		}
		NodeList childNodes = el.getChildNodes();
		for(int i=0; i < childNodes.getLength() ; i++) {
			Node node = childNodes.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) node;
				String childName = child.getNodeName();
				if(childName.equals("fo:bookmark")) {
					item.appendChild(getBookmark(child));
				} else if(childName.equals("fo:bookmark-title")) {
					item.setTitle(child.getTextContent());
				}
			}
		}
		return item;
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.pdf.PdfProcessor#processDocument(org.apache.pdfbox.pdmodel.PDDocument)
	 */
	public void processDocument(PDDocument doc) throws IOException {
		PDDocumentCatalog catalog = doc.getDocumentCatalog();
		catalog.setDocumentOutline(outline);
		catalog.setPageMode(PDDocumentCatalog.PAGE_MODE_USE_OUTLINES);
	}

}
