/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
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
import org.apache.tools.ant.util.ResourceUtils;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.DispatchEvents;
import com.jacob.com.Variant;
import com.leanpulse.bibant.utils.WinRegistry;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class Print2PdfTask extends Task {
	
	private static class PdfMapper implements FileNameMapper {
        public void setFrom(String s) {
        }
        public void setTo(String s) {
        }
        public String[] mapFileName(String fileName) {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0)
                return new String[] {fileName.substring(0, lastDot) + ".pdf"};
            else
                return new String[] {fileName + ".pdf"};
        }
    }
	
	private static class FlatPdfMapper implements FileNameMapper {
        public void setFrom(String s) {
        }
        public void setTo(String s) {
        }
        public String[] mapFileName(String fileName) {
        	String name = new File(fileName).getName();
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0)
                return new String[] {name.substring(0, lastDot) + ".pdf"};
            else
                return new String[] {name + ".pdf"};
        }
    }
	
	private static final String PDF_CREATOR_REGKEY = (System.getenv("ProgramFiles(x86)") == null) ?
			"SOFTWARE\\PDFCreator\\Program" : "SOFTWARE\\Wow6432Node\\PDFCreator\\Program";
	
	private static final int STATUS_READY = 0;
	private static final int STATUS_IN_PROGRESS = 1;
	private static final int STATUS_ERROR = 2;
	
	private int printStatus = STATUS_IN_PROGRESS;
	
	private File file = null;
	private File destFile = null;
	private File destDir = null;
	private Mapper mapperElement = null;
	private String title = null;
	private String titlefromfile = null;
	private String author = null;
	private String subject = null;
	private String keywords = null;
	
	private boolean force = false;
	private boolean preserveLastModified = false;
	private boolean flatten = false;
	private boolean failonerror = true;
	private long granularity = 0;
	
	private Vector<ResourceCollection> rcs = new Vector<ResourceCollection>();
	
	private ActiveXComponent pdfCreator = null;
	private DispatchEvents eventDispatch = null;
	private Variant defPrinter = null;
	
	public Print2PdfTask() {
		granularity = FileUtils.getFileUtils().getFileTimestampGranularity();
	}
	
	/**
     * Set a single source file to print.
     * @param file the file to print.
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
     * Set the output document title.
     * @param title the output document title.
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
	/**
	 * Set the output document title according the input file name.
	 * @param titlefromfile the output document title computed from the input file name.
	 */
	public void setTitlefromfile(String titlefromfile) {
		this.titlefromfile = titlefromfile;
	}

	/**
	 * Set the output document title.
	 * @param author the output document author.
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * Set the output document subject.
	 * @param subject the output document subject.
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * Set the output document keywords.
	 * @param keywords the output document keywords.
	 */
	public void setKeywords(String keywords) {
		this.keywords = keywords;
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
     * Set whether files printed from directory trees will be "flattened"
     * into a single directory.  If there are multiple files with
     * the same name in the source directory tree, only the first
     * file will be copied into the "flattened" directory, unless
     * the forceoverwrite attribute is true.
     * @param flatten if true flatten the destination directory. Default is false.
     */
    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
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
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper", getLocation());
        }
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
     * Ensure we have a consistent and legal set of attributes, and set
     * any internal flags necessary based on different combinations
     * of attributes.
     * @exception BuildException if an error occurs.
     */
    protected void validateAttributes() throws BuildException {
        if (file == null && rcs.size() == 0)
            throw new BuildException("Specify at least one source: a file or a resource collection.");
        if (destFile != null && destDir != null)
            throw new BuildException("Only one of tofile and todir may be set.");
        if (file != null && file.isDirectory())
            throw new BuildException("Use a resource collection to print several files in a directory.");
        if (destFile != null && rcs.size() > 0) {
            if (rcs.size() > 1) {
                throw new BuildException("Cannot concatenate multiple files into a single file.");
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
                        throw new BuildException("Cannot concatenate multiple files into a single file.");
                    }
                } else {
                    throw new BuildException("Cannot concatenate multiple files into a single file.");
                }
            }
        }
        if (destFile != null) {
            destDir = destFile.getParentFile();
        }
    }
    
    protected void initPrinting() throws BuildException {
    	if(!System.getProperty("os.name").contains("Windows"))
			throw new BuildException("This task only supports Windows operating systems !");
    	
		try {
			Logger.getLogger("java.util.prefs").setLevel(Level.SEVERE); // Avoid unwanted logs with some JVM versions
			if(WinRegistry.isAdmin()) {
				if(WinRegistry.readStringSubKeys(WinRegistry.HKEY_LOCAL_MACHINE, PDF_CREATOR_REGKEY) != null) {
					String updateInt = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE, PDF_CREATOR_REGKEY, "UpdateInterval");
					if(updateInt != null && !"0".equals(updateInt))
						WinRegistry.writeStringValue(WinRegistry.HKEY_LOCAL_MACHINE, PDF_CREATOR_REGKEY, "UpdateInterval", "0");
				}
			}
			if(WinRegistry.readStringSubKeys(WinRegistry.HKEY_CURRENT_USER, PDF_CREATOR_REGKEY) != null) {
				String updateInt = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER, PDF_CREATOR_REGKEY, "UpdateInterval");
				if(!"0".equals(updateInt))
					WinRegistry.writeStringValue(WinRegistry.HKEY_CURRENT_USER, PDF_CREATOR_REGKEY, "UpdateInterval", "0");
			}
		} catch(Exception e) {}
    	
		try {
			pdfCreator = new ActiveXComponent("PDFCreator.clsPDFCreator");
		} catch(Exception e) {
			throw new BuildException("PDFCreator doesn't seem to be correctly installed !", e);
		}
    }
    
    protected void configurePrinter() throws BuildException {
    	try {
	    	eventDispatch = new DispatchEvents(pdfCreator, new EventHandler());
			pdfCreator.invoke("cStart", new Variant[] {new Variant("/NoProcessingAtStartup"), new Variant(true)});
			pdfCreator.setProperty("cVisible", 0);
			Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"UseAutosave", 1}, new int[2]);
			Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"UseAutosaveDirectory", 1}, new int[2]);
			Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"AutosaveFormat", 0}, new int[2]);
			if(title != null)
				Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"StandardTitle", title}, new int[2]);
			if(author != null)
				Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"UseStandardAuthor", 1}, new int[2]);
				Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"StandardAuthor", author}, new int[2]);
			if(subject != null)
				Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"StandardSubject", subject}, new int[2]);
			if(keywords != null)
				Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"StandardKeywords", keywords}, new int[2]);
			defPrinter = pdfCreator.getProperty("cDefaultPrinter");
			pdfCreator.setProperty("cDefaultPrinter", "PDFCreator");
			Dispatch.call(pdfCreator, "cClearCache");
    	} catch(Exception e) {
			throw new BuildException("An error occured while configuring PDFCreator.", e);
		}
    }
    
    protected void launchPrint(File src) {
    	pdfCreator.invoke("cPrintFile", src.getPath());
    }
    
    protected void endPrinting() {
		if(pdfCreator != null) {
	    	try {
				if(eventDispatch != null) eventDispatch.safeRelease();
				if(defPrinter != null) pdfCreator.setProperty("cDefaultPrinter", defPrinter);
				pdfCreator.invoke("cClose");
				pdfCreator.safeRelease();
			} catch(Exception e) {}
	    	pdfCreator = null;
		}
    }
	
	public void execute() throws BuildException {
		try {
            validateAttributes();
        } catch (BuildException e) {
            if(failonerror) {
                throw e;
        	} else {
                log("Warning: " + e.getMessage(), Project.MSG_ERR);
                return;
            }
        }
		
		ComThread.InitMTA(true);
		try {
			initPrinting();
			
			configurePrinter();
			
			int skippedcount = 0;
			
			// deal with the single file
			if(file != null) {
	            if(file.exists()) {
	                if(destFile == null) {
	                	String name = file.getName();
	                	int lastDot = name.lastIndexOf('.');
	                	if (lastDot > 0)
	                		name = name.substring(0, lastDot) + ".pdf";
	                    else
	                    	name = name + ".pdf";
	                    destFile = new File(destDir == null ? file.getParentFile() : destDir, name);
	                }
	                if(force || !destFile.exists() || (file.lastModified() - granularity > destFile.lastModified())) {
	                	log("Printing " + file + " to " + destFile, Project.MSG_INFO);
	                	printFile(file, destFile);
	                } else
	                	skippedcount++;	
	            } else {
	                String message = "Warning: Could not find file " + file + " to print.";
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
	        else if (flatten)
	            mapper = new FlatPdfMapper();
	        else
	            mapper = new PdfMapper();
			
			for(int i = 0; i < rcs.size(); i++) {
				ResourceCollection rc = rcs.elementAt(i);
				if(!rc.isFilesystemOnly())
                    throw new BuildException("Only FileSystem resources are supported.");
				int size = rc.size();
				int count = 1;
				Iterator<?> resources = rc.iterator();
                while(resources.hasNext()) {
                    Resource r = (Resource) resources.next();
                    if (!r.isExists()) {
                        String message = "Warning: Could not find resource " + r.toLongString() + " to print.";
                        if (!failonerror)
                           log(message, Project.MSG_ERR);
                        else
                            throw new BuildException(message);
                        continue;
                    }
                    String name = r.getName();
                    FileProvider fp = (FileProvider) r.as(FileProvider.class);
                    FileResource fr = ResourceUtils.asFileResource(fp);
                    File fromDir = fr.getBaseDir();
                    if(fromDir == null)
                    	name = fr.getFile().getAbsolutePath();
                    String[] mappedFiles = mapper.mapFileName(name);
	            	if(mappedFiles != null) {
	            		File src = new File(fromDir, name);
	            		File dst = new File(destDir == null ? fromDir : destDir, mappedFiles[0]);
	            		if(force || !dst.exists() || (src.lastModified() - granularity > dst.lastModified())) {
	            			log(count++ + "/" + size + " Printing " + src + " to " + dst, Project.MSG_INFO);
	            			printFile(src, dst);
	            		} else
	            			skippedcount++;
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
			endPrinting();
			ComThread.Release();
	        ComThread.quitMainSTA();
		}
	}
	
	protected void printFile(File src, File dst) throws Exception {
		try {
			if(pdfCreator.invoke("cIsPrintable", new Variant[] { new Variant(src.getPath()) }).getBoolean()) {
				Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"AutosaveDirectory", dst.getParent()}, new int[2]);
				Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"AutosaveFilename", dst.getName()}, new int[2]);
				if(titlefromfile != null)
					Dispatch.invoke(pdfCreator, "cOption", Dispatch.Put, new Object[] {"StandardTitle", titlefromfile.replace("*", FilenameUtils.removeExtension(src.getName()))}, new int[2]);
				Dispatch.put(pdfCreator, "cPrinterStop", false);
				launchPrint(src);
				while(printStatus == STATUS_IN_PROGRESS && pdfCreator.getProperty("cProgramIsRunning").getBoolean()) {
					Thread.sleep(100);
				}
				if(printStatus == STATUS_READY) {
					if(preserveLastModified) {
						long lastmod = src.lastModified();
			        	FileUtils.getFileUtils().setFileLastModified(dst,lastmod);
					}
				} else { // an error occurred
					ActiveXComponent error = pdfCreator.getPropertyAsComponent("cError");
					Variant errorNum = error.getProperty("Number");
					Variant errorDes = error.getProperty("Description");
					int num = errorNum.getInt();
					String des = errorDes.getString();
					errorDes.safeRelease();
					errorNum.safeRelease();
					error.safeRelease();
					throw new BuildException("PDFCreator error [" + Integer.toString(num) + "]: " + des);
				}
			} else {
                throw new BuildException("The file " + src.getPath() + " can't be printed to PDF.");
			}
		} catch(Exception e) {
			if(failonerror)
				throw e;
			else
                log("Warning: " + e.getMessage() == null ? e.toString() : e.getMessage(), Project.MSG_ERR);
		} finally {
			printStatus = STATUS_IN_PROGRESS;
		}
	}
	
	public class EventHandler {
		public void eReady(Variant[] args) {
			printStatus = STATUS_READY;
		}
		public void eError(Variant[] args) {
			printStatus = STATUS_ERROR;
		}
	}

}
