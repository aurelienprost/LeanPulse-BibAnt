/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.File;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.Union;

import com.leanpulse.bibant.pdf.PdfMerger;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class MergeTask extends Task {
	
	private Union resources = null;
	private File destFile = null;
	private boolean force = false;
	
	/**
     * Set the destination file.
     * @param destFile the file to merge to.
     */
	public void setTofile(File destFile) {
		this.destFile = destFile;
	}
	
	/**
     * Set force mode regarding existing destination file.
     * @param force if true force overwriting of destination file
     *                  even if the destination file is younger than
     *                  the source files. Default is false.
     */
    public void setForce(boolean force) {
        this.force = force;
    }
	
	/**
     * List files to apply the merge to
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
    	if(destFile == null)
			throw new BuildException("The destination file must be specified.");
    	if(resources == null)
    		throw new BuildException("The set of files to be merged must be specified.");
    	if(resources.size() < 2)
    		throw new BuildException("Can't merge less than 2 files.");
    	PdfMerger merger = new PdfMerger();
    	merger.setDestinationFileName(destFile.getPath());
    	long lastMod = 0L;
        for(Iterator<?> i = resources.iterator(); i.hasNext(); ) {
            FileProvider fp = (FileProvider) ((Resource)i.next()).as(FileProvider.class);
            File f = fp.getFile();
            try {
            	merger.addSource(f);
            } catch (Exception e) {
            	throw new BuildException("An error occurred while merging " + f + " - " + e.getClass().getName() + ":" + e.getMessage(), e);
            }
            long l = f.lastModified();
            if(l > lastMod)
            	lastMod = l;
        }
        if(force || !destFile.exists() || lastMod > destFile.lastModified()) {
	    	log("Merging " + resources.size() + " documents to " + destFile, Project.MSG_INFO);
	        try {
				merger.mergeDocuments();
			} catch (Exception e) {
				throw new BuildException("The merge to " + destFile + " failed - " + e.getClass().getName() + ":" + e.getMessage(), e);
			}
        } else {
        	log("The merge to " + destFile + " has been skipped.", Project.MSG_VERBOSE);
        }
    }

}
