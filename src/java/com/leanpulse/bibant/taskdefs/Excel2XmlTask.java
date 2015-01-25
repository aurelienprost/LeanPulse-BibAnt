/*********************************************
 * Copyright (c) 2013 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
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
import com.jacob.com.Variant;
import com.sun.org.apache.xerces.internal.util.XMLChar;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
@SuppressWarnings("restriction")
public class Excel2XmlTask extends Task {
	
	public class XmlFilterReader extends FilterReader {	
		public XmlFilterReader(Reader in) {
			super(in);
		}
		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			int read = super.read(cbuf, off, len);
			if (read == -1)
				return -1;
			int pos = off;
			for (int readPos = off; readPos < off + read; readPos++) {
				if (cbuf[readPos] == '&' && (readPos + 3) < (off + read) && cbuf[readPos+1] == '#') {
					StringBuilder builder = new StringBuilder();
					readPos += 2;
					while(readPos < off + read && cbuf[readPos] != ';') {
						builder.append(cbuf[readPos]);
						readPos++;
					}
					String codeStr = builder.toString();
					int charCode = -1;
					if(codeStr.startsWith("x"))
						charCode = Integer.parseInt(codeStr.substring(1), 16);
					else
						charCode = Integer.parseInt(codeStr);
					if (XMLChar.isValid(charCode))
						cbuf[pos++] = (char) charCode;
				} else {
					if(XMLChar.isValid(cbuf[readPos]))
						cbuf[pos++] = cbuf[readPos];
				}
			}
			return pos - off;
		}
	}
	
	private static class XmlMapper implements FileNameMapper {
        public void setFrom(String s) {
        }
        public void setTo(String s) {
        }
        public String[] mapFileName(String fileName) {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0)
                return new String[] {fileName.substring(0, lastDot) + ".xml"};
            else
                return new String[] {fileName + ".xml"};
        }
    }
	
	private File file = null;
	private File destFile = null;
	private File destDir = null;
	private Mapper mapperElement = null;
	
	private boolean force = false;
	private boolean filter = false;
	private boolean failonerror = true;
	private long granularity = 0;
	
	private Vector<ResourceCollection> rcs = new Vector<ResourceCollection>();
	
	public Excel2XmlTask() {
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
    
    public void setFilter(boolean filter) {
        this.filter = filter;
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
     * Set whether to fail when errors are encountered. If false, note errors
     * to the output but keep going. Default is true.
     * @param failonerror true or false.
     */
    public void setFailonerror(boolean failonerror) {
        this.failonerror = failonerror;
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
		ActiveXComponent excel = null;
		try {
			excel = new ActiveXComponent("Excel.Application");
			excel.setProperty("IgnoreRemoteRequests", true);
			excel.setProperty("Interactive", false);
		} catch(Exception e) {
			ComThread.Release();
			ComThread.quitMainSTA();
			throw new BuildException("Excel doesn't seem to be correctly installed !", e);
		}
		
		int skippedcount = 0;
		
		try {
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
	                	log("Converting " + file + " to " + destFile, Project.MSG_INFO);
	                	convertFile(excel, file, destFile);
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
	        else
	            mapper = new XmlMapper();
			
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
	            			log(count++ + "/" + size + " Converting " + src + " to " + dst, Project.MSG_INFO);
	            			convertFile(excel, src, dst);
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
			try {
				excel.setProperty("IgnoreRemoteRequests", false);
				excel.invoke("Quit");
				excel.safeRelease();
			} catch(Exception e) {}
			ComThread.Release();
	        ComThread.quitMainSTA();
		}
		
    }
    
    private void convertFile(ActiveXComponent excel, File src, File dst) throws Exception {
		try {
			Dispatch workbooks = excel.getProperty("Workbooks").toDispatch();
			Dispatch workbook = Dispatch.invoke(workbooks, "Open", Dispatch.Get, new Object[] {src.getPath()}, new int[1]).toDispatch();
			Dispatch.call(workbook, "SaveAs", new Object[] {dst.getPath(), 46});
			Dispatch.call(workbook, "Close", new Variant(false));
		} catch(Exception e) {
			if(failonerror)
				throw e;
			else
                log("Warning: " + e.getMessage() == null ? e.toString() : e.getMessage(), Project.MSG_ERR);
		}
		
		if(filter) {
			File tmpFile = new File(dst.getPath() + ".filtered");
			Reader r = null;
	        Writer w = null;
	        try {
	            r = new XmlFilterReader(new InputStreamReader(new FileInputStream(dst), "ISO-8859-1"));
	            w = new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8");
	            IOUtils.copyLarge(r, w);
	            w.flush();
	        } catch (Exception e) {
	        	if(failonerror)
					throw e;
				else
	                log("Warning: " + e.getMessage() == null ? e.toString() : e.getMessage(), Project.MSG_ERR);
	        } finally {
	            IOUtils.closeQuietly(r);
	            IOUtils.closeQuietly(w);
	        }
	        dst.delete();
	        tmpFile.renameTo(dst);
		}
    }

}
