/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.File;
import java.util.Iterator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileUtils;

import com.leanpulse.bibant.pdf.PdfProcessor;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 * 
 */
public abstract class PdfProcessingTask extends Task {

	protected File file = null;
	protected Union resources = null;
	
	protected boolean preserveLastModified = false;
	protected boolean failonerror = true;

	/**
	 * File for which the regular expression should be replaced; required unless
	 * a nested fileset is supplied.
	 * 
	 * @param file
	 *            The file for which the reg exp should be replaced.
	 */
	public void setFile(File file) {
		this.file = file;
	}
	
	/**
     * Give the processed files the same last modified time as the original files.
     * @param preserve if true preserve the modified time; default is false.
     */
    public void setPreservelastmodified(boolean preserve) {
        preserveLastModified = preserve;
    }
	
	/**
     * Set whether to fail when errors are encountered. If false, note errors
     * to the output but keep going. Default is true.
     * @param failonerror true or false.
     */
    public void setFailonerror(boolean failonerror) {
        this.failonerror = failonerror;
    }
    
	/**
     * List files to apply the processor to
     * @param set the fileset element
     */
    public void addFileset(FileSet set) {
        addConfigured(set);
    }

    /**
     * Support arbitrary file system based resource collections.
     */
    public void addConfigured(ResourceCollection rc) {
        if(!rc.isFilesystemOnly())
            throw new BuildException("Only filesystem resources are supported.");
        if(resources == null)
            resources = new Union();
        resources.add(rc);
    }
    
    /**
     * This will perform the execution.
     */
    public void execute() throws BuildException {
    	PdfProcessor proc = getProcessor();
    	if(file != null) {
    		if(file.exists()) {
    			log("Processing " + file, Project.MSG_INFO);
				long lastmod = file.lastModified();
				try {
					PDDocument doc = null;
			        try {
			            doc = PDDocument.load(file);
			            proc.processDocument(doc);
			            doc.save(file.getAbsolutePath());
			        } finally {
			        	if( doc != null )
			                doc.close();
			        }
			        if(preserveLastModified) 
			        	FileUtils.getFileUtils().setFileLastModified(file,lastmod);
	            } catch(Exception e) {
	            	handleError("An error occurred while processing " + file + " - " + e.getClass().getName() + ":" + e.getMessage(), e);
	            }
    		} else {
    			handleError("Warning: Could not find file " + file + " to process.", null);
    		}
    	}
    	if (resources != null) {
    		int size = resources.size();
    		int count = 1;
            for(Iterator<?> i = resources.iterator(); i.hasNext(); ) {
                FileProvider fp = (FileProvider) ((Resource)i.next()).as(FileProvider.class);
                File f = fp.getFile();
                log(count++ + "/" + size + " Processing " + f, Project.MSG_INFO);
            	long lastmod = f.lastModified();
                try {
                	PDDocument doc = null;
    		        try {
    		            doc = PDDocument.load(f);
    		            proc.processDocument(doc);
    		            doc.save(f.getAbsolutePath());
    		        } finally {
    		        	if( doc != null )
    		                doc.close();
    		        }
    		        if(preserveLastModified)
    		        	FileUtils.getFileUtils().setFileLastModified(f,lastmod);
                } catch(Exception e) {
                	handleError("An error occurred while processing " + f + " - " + e.getClass().getName() + ":" + e.getMessage(), e);
                }
            }
        }
    }
    
    protected void handleError(String message, Throwable t) throws BuildException {
    	if(!failonerror) {
            log(message, t, Project.MSG_ERR);
    	} else {
    		throw new BuildException(message, t);
    	}
    }
    
    protected abstract PdfProcessor getProcessor() throws BuildException;
    
}
