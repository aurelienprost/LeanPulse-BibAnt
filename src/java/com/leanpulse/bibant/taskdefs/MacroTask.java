/*********************************************
 * Copyright (c) 2013 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs;

import java.io.File;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.ResourceUtils;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 * 
 */
public abstract class MacroTask extends Task {

	protected File file = null;
	private File destFile = null;
	private File destDir = null;
	private Vector<File> vbFiles;
	private String macroName = null;
	private String macroArgs = null;
	private String returnProperty = null;
	private Mapper mapperElement = null;
	protected String code = null;
	protected boolean interactive = false;
	
	private boolean force = false;
	protected boolean preserveLastModified = false;
	protected boolean failonerror = true;
	private long granularity = 0;
	
	private Vector<ResourceCollection> rcs = new Vector<ResourceCollection>();

	/**
	 * File on which the macro will be executed; required unless
	 * a nested fileset is supplied.
	 * 
	 * @param file
	 *            The file on which the macro will be executed.
	 */
	public void setFile(File file) {
		this.file = file;
	}
	
	/**
     * Set the destination file.
     * @param destFile the file to print to.
     */
	public void setTofile(File destFile) {
		this.destFile = destFile;
	}
	
	/**
     * Set the destination directory.
     * @param destDir the destination directory.
     */
    public void setTodir(File destDir) {
        this.destDir = destDir;
    }
    
    /**
     * Set the VB files to import.
     * @param filenames the VB files to import.
     */
    public void setVbfiles(String filenames) {
		if (filenames != null && filenames.length() > 0) {
			vbFiles = new Vector<File>();
			StringTokenizer tok = new StringTokenizer(filenames, ", \t\n\r\f", false);
			while(tok.hasMoreTokens()) {
				vbFiles.addElement(new File(tok.nextToken()));
			}
		}
    }
    
    /**
     * Set the name of the macro to be executed.
     * @param destDir the name of the macro to be executed.
     */
    public void setMacroname(String macroName) {
        this.macroName = macroName;
    }
    
    /**
     * Set the arguments to be passed to the macro.
     * @param macroArgs the arguments to be passed to the macro.
     */
    public void setMacroargs(String macroArgs) {
        this.macroArgs = macroArgs;
    }
    
    /**
     * Set the name of the property to which the result will be set.
     * @param property the name if the property.
     */
    public void setReturnproperty(String property) {
        this.returnProperty = property;
    }
    
    /**
     * Set if the application will be visible while executing the macro.
     * @param interactive if true the application will be visible.
     */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }
    
    /**
     * Set force mode regarding existing destination file(s).
     * @param force if true force overwriting of destination file(s)
     *                  even if the destination file(s) are younger than
     *                  the corresponding source file. Default is false.
     */
    public void setForce(boolean force) {
        this.force = force;
    }
    
	/**
     * Give the converted files the same last modified time as the original files.
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
     * Set the number of milliseconds leeway to give before deciding a
     * target is out of date.
     *
     * <p>Default is 1 second, or 2 seconds on DOS systems.</p>
     * @param granularity the granularity used to decide if a target is out of date.
     */
    public void setGranularity(long granularity) {
        this.granularity = granularity;
    }
    
    /**
     * Add a set of files to print.
     * @param set a set of files to print.
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
        rcs.add(rc);
    }
    
    /**
     * Define the mapper to map source to destination files.
     * @return a mapper to be configured.
     * @exception BuildException if more than one mapper is defined.
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null)
            throw new BuildException("Cannot define more than one mapper.", getLocation());
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    /**
     * Add a nested filenamemapper.
     * @param fileNameMapper the mapper to add.
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }
    
    /**
     * Set the macro code.
     * @param code the macro code to be executed.
     */
    public void addText(String rawCode) {
    	String trimedCode = rawCode.trim();
    	if(trimedCode.length() > 0) {
	    	code = getProject().replaceProperties(trimedCode);
	    	Pattern p = Pattern.compile("^\\s*", Pattern.MULTILINE);
	    	Matcher m = p.matcher(code);
	    	if(m.find()) {
	        	int lineNb = 1;
	        	int lastEnd = 0;
	        	StringBuffer codeBuffer = new StringBuffer();
	        	do {
	        		String line = code.substring(lastEnd, m.start());
	        		codeBuffer.append(line);
	        		if(!line.trim().endsWith(" _"))
	        			codeBuffer.append(lineNb++);
	        		codeBuffer.append("\t");
	        		lastEnd = m.end();
	        	} while(m.find());
	        	codeBuffer.append(code.substring(lastEnd));
	        	code = codeBuffer.toString();
	    	}
    	}
    }
    
    /**
     * Ensure we have a consistent and legal set of attributes, and set
     * any internal flags necessary based on different combinations
     * of attributes.
     * @exception BuildException if an error occurs.
     */
    protected void validateAttributes() throws BuildException {
    	if(macroName == null && code == null)
        	throw new BuildException("Specifiy the VB code to be executed at least as embedded text in the task or by giving the corresponding macro name.");
    	if(macroName != null && code != null)
    		throw new BuildException("Cannot execute VB code embedded in the task element and from a given macro at the same time.");
    	if(vbFiles != null && macroName == null)
    		throw new BuildException("You must specify the name of the macro to be executed.");
        if (file == null && rcs.size() == 0)
            throw new BuildException("Specify at least one source: a file or a resource collection.");
        if (destFile != null && destDir != null)
            throw new BuildException("Only one of tofile and todir may be set.");
        if (file != null && file.isDirectory())
            throw new BuildException("Use a resource collection to process several files in a directory.");
        if (destFile != null && rcs.size() > 0) {
            if (rcs.size() > 1) {
                throw new BuildException("Cannot concatenate multiple resources into a single file.");
            } else {
                ResourceCollection rc = rcs.elementAt(0);
                if (rc.size() == 0) {
                    throw new BuildException("Cannot perform operation from directory to file.");
                } else if (rc.size() == 1) {
                    Resource res = (Resource) rc.iterator().next();
                    FileProvider r = (FileProvider) res.as(FileProvider.class);
                    if (file == null) {
                        if (r != null)
                            file = r.getFile();
                        else
                        	throw new BuildException("Only filesystem resources are supported.");
                        rcs.removeElementAt(0);
                    } else {
                        throw new BuildException("Cannot concatenate multiple resources into a single file.");
                    }
                } else {
                    throw new BuildException("Cannot concatenate multiple resources into a single file.");
                }
            }
        }
        if (destFile != null) {
            destDir = destFile.getParentFile();
        }
    }
    
    protected abstract ActiveXComponent startApp() throws BuildException;
    
    protected abstract Dispatch getBooks(ActiveXComponent app);
    
    protected void exitApp(ActiveXComponent app) {
		app.invoke("Quit");
    }
    
    /**
     * This will perform the execution.
     */
    public void execute() throws BuildException {
    	validateAttributes();
    	
    	ComThread.InitMTA(true);
		ActiveXComponent app = null;
		Dispatch books = null;
		try {
			app = startApp();
			app.setProperty("DisplayAlerts", false);
			app.setProperty("Visible", interactive);
			
			books = getBooks(app); 
			
			int skippedcount = 0;
			
			// deal with the single file
			if(file != null) {
	            if(file.exists()) {
	            	if(destFile == null)
	                    destFile = new File(destDir == null ? file.getParentFile() : destDir, file.getName());
	            	if(force || !destFile.exists() || file.equals(destFile) || (file.lastModified() - granularity > destFile.lastModified())) {
	                	log("Processing " + file + " to " + destFile, Project.MSG_INFO);
	                	runMacro(app, books, file, destFile);
	                } else
	                	skippedcount++;	
	            } else {
	                String message = "Warning: Could not find file " + file + " to process.";
	                if(!failonerror)
	                    log(message, Project.MSG_ERR);
	                else
	                    throw new BuildException(message);
	            }
	        }
			
			// deal with the ResourceCollections
			FileNameMapper mapper = null;
	        if (mapperElement != null)
	            mapper = mapperElement.getImplementation();
	        else
	            mapper = new IdentityMapper();
	        
	        for(int i = 0; i < rcs.size(); i++) {
				ResourceCollection rc = rcs.elementAt(i);
				if(!rc.isFilesystemOnly())
                    throw new BuildException("Only FileSystem resources are supported.");
				int size = rc.size();
				int count = 1;
				Iterator<?> resources = rc.iterator();
                while(resources.hasNext()) {
                    Resource r = (Resource) resources.next();
                    if(!r.isExists()) {
                        String message = "Warning: Could not find document " + r.toLongString() + " to process.";
                        if (!failonerror)
                           log(message, Project.MSG_ERR);
                        else
                            throw new BuildException(message);
                        continue;
                    }
                    String name = r.getName();
                    if(!name.startsWith("~$")) {
	                    FileProvider fp = (FileProvider) r.as(FileProvider.class);
	                    FileResource fr = ResourceUtils.asFileResource(fp);
	                    File fromDir = fr.getBaseDir();
	                    if(fromDir == null)
	                    	name = fr.getFile().getAbsolutePath();
	                    String[] mappedFiles = mapper.mapFileName(name);
		            	if(mappedFiles != null) {
		            		File src = new File(fromDir, name);
		            		File dst = new File(destDir == null ? fromDir : destDir, mappedFiles[0]);
		            		if(force || !dst.exists() || src.equals(dst) || (src.lastModified() - granularity > dst.lastModified())) {
		            			log(count++ + "/" + size + " Processing " + src + " to " + dst, Project.MSG_INFO);
		            			File dstDir = dst.getParentFile();
		            			if(!dstDir.exists())
		            				dstDir.mkdirs();
		            			runMacro(app, books, src, dst);
		            		} else
		            			skippedcount++;
		            	}
                    }
                }
			}
			
			if (skippedcount > 0)
				log(skippedcount + " file(s) skipped (no change found since last generation; set force=\"true\" to override).", Project.MSG_VERBOSE);
			
		} catch(Exception e) {
			if(e instanceof BuildException)
				throw (BuildException) e;
			else
				throw new BuildException(e);
			
		} finally {
			if(app != null) {
		    	try {
		    		exitApp(app);
					app.safeRelease();
				} catch(Exception e) {}
		    	app = null;
			}
			ComThread.Release();
			ComThread.quitMainSTA();
		}
    }
    
    protected void runMacro(ActiveXComponent app, Dispatch books, File src, File dst) throws Exception {
    	Dispatch book = null;
    	try {
	    	book = Dispatch.call(books, "Open", src.getPath(), new Variant(false)).toDispatch();
	    	Dispatch vbProj = Dispatch.get(book, "VBProject").toDispatch();
	    	Dispatch vbComps = Dispatch.get(vbProj, "VBComponents").toDispatch();
	    	Vector<Dispatch> createdComps = new Vector<Dispatch>();
	    	try {
		    	if(vbFiles != null) {
		    		for(File vbFile : vbFiles)
		    			createdComps.add(Dispatch.call(vbComps, "Import", vbFile.getAbsolutePath()).toDispatch());
		    	}
		    	Dispatch vbModule = Dispatch.call(vbComps, "Add", new Variant(1)).toDispatch();
		    	createdComps.add(vbModule);
		    	Dispatch codeModule = Dispatch.get(vbModule, "CodeModule").toDispatch();
		    	try {
		    		if(macroName != null) {
		    			Dispatch.call(codeModule, "AddFromString", "Function BibAntAutoMacro() As String\r\n" +
			    				"On Error GoTo BibAnt_ERROR_HANDLER\r\n" + 
			    				"\t" + (returnProperty != null ? "BibAntAutoMacro = " : "Call ") + macroName + (macroArgs != null ? "(" + macroArgs + ")": "") + "\r\n" +
			    				"\tExit Function\r\n" +
			    				"BibAnt_ERROR_HANDLER:\r\n" +
			    				"\tBibAntAutoMacro = \"@ERROR@Runtime Error:\" & vbCrLf & _\r\n" +
			    				"\t\tvbTab & Err.Description & \" (\" & Err.Number & \")\"\r\n" +
			    				"End Function");
		    		} else {
		    			Dispatch.call(codeModule, "AddFromString", "Function BibAntAutoMacro() As String\r\n" +
			    				"On Error GoTo BibAnt_ERROR_HANDLER\r\n" +
		    					"\tDim result As String\r\n" +
			    				code + "\r\n" +
				    			"\tBibAntAutoMacro = result\r\n" +
			    				"\tExit Function\r\n" +
			    				"BibAnt_ERROR_HANDLER:\r\n" +
			    				"\tBibAntAutoMacro = \"@ERROR@Runtime Error on VB code line \" & Erl & \":\" & vbCrLf & _\r\n" +
			    				"\t\tvbTab & Err.Description & \" (\" & Err.Number & \")\"\r\n" +
			    				"End Function");
		    		}
		    		String result = Dispatch.call(app, "Run", "BibAntAutoMacro").toString();
		    		if(result.startsWith("@ERROR@"))
		    			throw new BuildException(result.substring(7));
		    		else if(returnProperty != null)
		    			getProject().setProperty(returnProperty, result);
		    	} finally {
		    		codeModule.safeRelease();
		    	}
	    	} finally {
    			for(Dispatch importedComp : createdComps) {
    				Dispatch.call(vbComps, "Remove", importedComp);
    				importedComp.safeRelease();
    			}
	    		vbComps.safeRelease();
		    	vbProj.safeRelease();
	    	}
	    	Dispatch.call(book, "SaveAs", dst.getPath());
	    	if(preserveLastModified) {
				long lastmod = src.lastModified();
	        	FileUtils.getFileUtils().setFileLastModified(dst,lastmod);
			}
	    	
    	} catch(Exception e) {
			if(failonerror)
				throw e;
			else
                log("Warning: " + e.getMessage() == null ? e.toString() : e.getMessage(), Project.MSG_ERR);
		} finally {
			if(book != null) {
				Dispatch.call(book, "Close", new Variant(0));
		    	book.safeRelease();
			}
		}
    }
    
}
