/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class ExtractMetadataTask extends Task {
	
	private File file;
	private File metafile;
	boolean failonerror = false;
	private List<FileSet> filesets = null;
	
	private ParseContext context;
	private Parser parser;
	private ContentHandler handler;
	
	public ExtractMetadataTask() {
		context = new ParseContext();
		Detector detector = new DefaultDetector();
		parser = new AutoDetectParser(detector);
        context.set(Parser.class, parser);
        handler = new DefaultHandler();
	}
	
	public void setFile(File file) {
		this.file = file;
	}
	
	public void setMetafile(File metafile) {
		this.metafile = metafile;
	}
	
	public void setFailonerror(boolean failonerror) {
		this.failonerror = failonerror;
	}
	
	public void addFileset(FileSet set) {
		if(filesets == null)
			filesets = new ArrayList<FileSet>();
	    filesets.add(set);
	}
	
	public void execute() throws BuildException {
		if(metafile == null)
    		throw new BuildException("Attribute 'metafile' has to be defined.");
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new BuildException(e);
		}
		
		Document doc = docBuilder.newDocument();
		Element root = doc.createElement("bib:metaextraction");
		root.setAttribute("xmlns:bib", "http://www.leanpulse.com/schemas/bibant/2012/core");
		doc.appendChild(root);
		
        if(file != null) {
        	Element fileEl = processFile(file, doc);
        	if(fileEl != null)
				root.appendChild(fileEl);
        }
        if(filesets != null) {
			for(int i = 0; i < filesets.size(); i++) {
				FileSet fs = filesets.get(i);
				DirectoryScanner ds = fs.getDirectoryScanner(getProject());
				String[] files = ds.getIncludedFiles();
				for(int j = 0; j < files.length; j++) {
					Element fileEl = processFile(new File(fs.getDir(getProject()), files[j]), doc);
					if(fileEl != null)
						root.appendChild(fileEl);
				}
			}
        }
        
        try {
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer serializer = transformerFactory.newTransformer();
	        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
	        DOMSource source = new DOMSource(doc);
	        StreamResult result =  new StreamResult(new FileOutputStream(metafile));
	        serializer.transform(source, result);
        } catch(Exception e) {
        	throw new BuildException("An error occured while saving the metadata file " + file + " - " + e.getClass().getName() + ":" + e.getMessage(), e);
        }
	}
	
	private Element processFile(File file, Document doc) throws BuildException {
		Metadata metadata = new Metadata();
		try {
            InputStream input = TikaInputStream.get(file, metadata);
            try {
                parser.parse(input, handler, metadata, context);
                Element fileEl = doc.createElement("bib:file");
                String fileName = file.getName();
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot > 0) {
                	fileEl.setAttribute("name", fileName.substring(0, lastDot));
                	fileEl.setAttribute("ext", fileName.substring(lastDot, fileName.length()));
                } else {
                	fileEl.setAttribute("name", fileName);
                	fileEl.setAttribute("ext", "");
                }
                fileEl.setAttribute("path", file.getParent());
                String[] names = metadata.names();
                for(int i=0; i<names.length; i++) {
                	if(names[i].equals("Content-Type")) {
                		fileEl.setAttribute("type", metadata.get(names[i]));
                	} else {
	                	Element metaEl = doc.createElement("bib:metadata");
	                	metaEl.setAttribute("name", names[i]);
	                	metaEl.setTextContent(metadata.get(names[i]));
	                	fileEl.appendChild(metaEl);
                	}
                }
                return fileEl;
            } finally {
                input.close();
            }
		} catch(Exception e) {
			if(failonerror)
				throw new BuildException("An error occurred while extracting metadata from file " + file + " - " + e.getClass().getName() + ":" + e.getMessage(), e);
			else
				log("Warning: " + e.getMessage() == null ? e.toString() : e.getMessage(), Project.MSG_ERR);
		}
		return null;
	}

}
