/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.cli.InputHandler;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.GlobPatternMapper;

import com.leanpulse.bibant.Launcher;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class Xml2PdfTask extends Task {
	
	protected static final String PRODUCER = "LeanPulse BibAnt v" + Launcher.BIBANT_VER;
	
	private File foFile;
	private File xmlFile;
	private File xsltFile;
	private List<XsltParameter> xsltParams;
	private List<FileSet> filesets = new ArrayList<FileSet>();
	private File outFile;
	private File outDir;
	private File baseDir;
	private File userConfig;
	private boolean logFiles = true;
	private boolean force = false;
	private boolean relativebase = false;
	private boolean failonerror = true;
	
	/**
	 * Sets the filename for the userconfig.xml.
	 * @param userConfig Configuration to use
	 */
	public void setUserconfig(File userConfig) {
	    this.userConfig = userConfig;
	}
	
	/**
	 * Sets the input XSL-FO file.
	 * @param foFile input XSL-FO file
	 */
	public void setFofile(File foFile) {
	    this.foFile = foFile;
	}
	
	/**
	 * Sets the input XML file.
	 * @param xmlFile the input XML file.
	 */
	public void setXmlFile(File xmlFile) {
	    this.xmlFile = xmlFile;
	}
	
	/**
	 * Sets the input XSLT file.
	 * @param xsltFile the input XSLT file.
	 */
	public void setXsltFile(File xsltFile) {
	    this.xsltFile = xsltFile;
	}
	
	/**
	 * Adds a set of XSL-FO files (nested fileset attribute).
	 * @param set a fileset
	 */
	public void addFileset(FileSet set) {
	    filesets.add(set);
	}
	
	/**
     * Adds a parameter for the XSLT.
     *
     * @return new parameter created.
     */
	public XsltParameter createXsltparam() {
		if(xsltParams == null)
			xsltParams = new ArrayList<XsltParameter>();
		XsltParameter param = new XsltParameter();
		xsltParams.add(param);
    	return param;
    }
	
	/**
	 * Set whether to include files (external-graphics, instream-foreign-object)
	 * from a path relative to the .fo file (true) or the working directory (false, default)
	 * only useful for filesets
	 *
	 * @param relbase true if paths are relative to file.
	 */
	public void setRelativebase(boolean relbase) {
	    this.relativebase = relbase;
	}
	
	/**
	 * Set whether to check dependencies, or to always generate;
	 * optional, default is false.
	 *
	 * @param force true if always generate.
	 */
	public void setForce(boolean force) {
	    this.force = force;
	}
	
	/**
	 * Sets the output file.
	 * @param outFile File to output to
	 */
	public void setOutfile(File outFile) {
	    this.outFile = outFile;
	}
	
	/**
	 * Sets the output directory.
	 * @param outDir Directory to output to
	 */
	public void setOutdir(File outDir) {
	    this.outDir = outDir;
	}
	
	/**
	 * Set whether to fail when errors are encountered. If false, note errors
     * to the output but keep going. Default is true.
	 *
	 * @param failonerror true or false.
	 */
	public void setFailonerror(boolean failonerror) {
	    this.failonerror = failonerror;
	}
	
	/**
	 * Sets the base directory for single FO file (non-fileset) usage
	 * @param baseDir File to use as a working directory
	 */
	public void setBasedir(File baseDir) {
	    this.baseDir = baseDir;
	}
	
	/**
	 * Controls whether the filenames of the files that are processed are logged
	 * or not.
	 * @param logFiles True if the feature should be enabled
	 */
	public void setLogFiles(boolean logFiles) {
	    this.logFiles = logFiles;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void execute() throws BuildException {
        FOPTaskStarter starter = new FOPTaskStarter(this);
        starter.run();
    }
	
	public static class XsltParameter extends ProjectComponent {
		private String name;
		private String value;
		public void setName(String name) {
			this.name = name;
		}
		public String getName() throws BuildException {
			if(name == null)
    			throw new BuildException("Attribute 'name' has to be defined for the XSLT parameter.");
			return name;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public String getValue() throws BuildException {
			if(value == null)
    			throw new BuildException("Attribute 'value' has to be defined for the XSLT parameter.");
			return value;
		}
	}
	
	static class FOPTaskStarter {

		// configure fopFactory as desired
		private FopFactory fopFactory = FopFactory.newInstance();

		private Xml2PdfTask task;
		private String baseURL = null;
		
		FOPTaskStarter(Xml2PdfTask task) throws BuildException {
			this.task = task;
			if (task.userConfig != null) {
				try {
					fopFactory.setUserConfig(task.userConfig);
				} catch (Exception e) {
					throw new BuildException(e);
				}
			}
		}

		private File replaceExtension(File file, String expectedExt, String newExt) {
			String name = file.getName();
			if (name.toLowerCase().endsWith(expectedExt)) {
				name = name.substring(0, name.length() - expectedExt.length());
			}
			name = name.concat(newExt);
			return new File(file.getParentFile(), name);
		}

		/**
		 * {@inheritDoc}
		 */
		public void run() {
			// Set base directory
			if (task.baseDir != null) {
				try {
					this.baseURL = task.baseDir.toURI().toURL().toExternalForm();
				} catch (MalformedURLException mfue) {
					task.log("Error creating base URL from base directory", mfue, Project.MSG_ERR);
				}
			} else {
				try {
					if (task.foFile != null) {
						this.baseURL = task.foFile.getParentFile().toURI().toURL().toExternalForm();
					}
				} catch (MalformedURLException mfue) {
					task.log("Error creating base URL from XSL-FO input file", mfue, Project.MSG_ERR);
				}
			}

			task.log("Using base URL: " + baseURL, Project.MSG_DEBUG);

			// actioncount = # of fofiles actually processed through FOP
			int actioncount = 0;
			// skippedcount = # of fofiles which haven't changed (force = "false")
			int skippedcount = 0;

			// deal with single source file
			if (task.foFile!= null) {
				if (task.foFile.exists()) {
					File outf = task.outFile;
					if (outf == null) {
						throw new BuildException("outfile is required when fofile is used");
					}
					if (task.outDir != null) {
						outf = new File(task.outDir, outf.getName());
					}
					// Render if "force" flag is set OR
					// OR output file doesn't exist OR
					// output file is older than input file
					if (task.force
							|| !outf.exists()
							|| (task.foFile.lastModified() > outf.lastModified())) {
						render(task.foFile, outf, MimeConstants.MIME_PDF);
						actioncount++;
					} else {
						skippedcount++;
					}
				}
			} else if (task.xmlFile != null && task.xsltFile != null) {
				if (task.xmlFile.exists() && task.xsltFile.exists()) {
					File outf = task.outFile;
					if (outf == null) {
						throw new BuildException("outfile is required when fofile is used");
					}
					if (task.outDir != null) {
						outf = new File(task.outDir, outf.getName());
					}
					// Render if "force" flag is set OR output file doesn't exist OR
					// output file is older than input file
					if (task.force
							|| !outf.exists()
							|| (task.xmlFile.lastModified() > outf.lastModified()
							|| task.xsltFile.lastModified() > outf.lastModified())) {
						render(task.xmlFile, task.xsltFile, outf, MimeConstants.MIME_PDF);
						actioncount++;
					} else {
						skippedcount++;
					}
				}
			}

			GlobPatternMapper mapper = new GlobPatternMapper();

			String inputExtension = ".fo";
			File xsltFile = task.xsltFile;
			if (xsltFile != null) {
				inputExtension = ".xml";
			}
			mapper.setFrom("*" + inputExtension);
			mapper.setTo("*.pdf");

			// deal with the filesets
			for (int i = 0; i < task.filesets.size(); i++) {
				FileSet fs = task.filesets.get(i);
				DirectoryScanner ds = fs.getDirectoryScanner(task.getProject());
				String[] files = ds.getIncludedFiles();

				for (int j = 0; j < files.length; j++) {
					File f = new File(fs.getDir(task.getProject()), files[j]);

					File outf = null;
					if (task.outDir != null && files[j].endsWith(inputExtension)) {
						String[] sa = mapper.mapFileName(files[j]);
						outf = new File(task.outDir, sa[0]);
					} else {
						outf = replaceExtension(f, inputExtension, ".pdf");
						if (task.outDir != null) {
							outf = new File(task.outDir, outf.getName());
						}
					}
					File dir = outf.getParentFile();
					if (!dir.exists()) {
						dir.mkdirs();
					}
					try {
						if (task.relativebase) {
							this.baseURL = f.getParentFile().toURI().toURL().toExternalForm();
						}
						if (this.baseURL == null) {
							this.baseURL = fs.getDir(task.getProject()).toURI().toURL().toExternalForm();
						}

					} catch (Exception e) {
						task.log("Error setting base URL", Project.MSG_DEBUG);
					}

					// Render if "force" flag is set OR output file doesn't exist OR
					// output file is older than input file
					if (task.force || !outf.exists()
							|| (f.lastModified() > outf.lastModified())) {
						if (xsltFile != null) {
							render(f, xsltFile, outf, MimeConstants.MIME_PDF);
						} else {
							render(f, outf, MimeConstants.MIME_PDF);
						}
						actioncount++;
					} else {
						skippedcount++;
					}
				}
			}

			if (actioncount + skippedcount == 0) {
				task.log("No files processed. No files were selected by the filesets "
					+ "and no fofile was set.", Project.MSG_WARN);
			} else if (skippedcount > 0) {
				task.log(skippedcount + " input file(s) skipped (no change found"
					+ " since last generation; set force=\"true\" to override).", Project.MSG_VERBOSE);
			}
		}

		private void renderInputHandler(InputHandler inputHandler, File outFile, String outputFormat) throws Exception {
			OutputStream out = new java.io.FileOutputStream(outFile);
			out = new BufferedOutputStream(out);
			boolean success = false;
			try {
				FOUserAgent userAgent = fopFactory.newFOUserAgent();
				userAgent.setCreator(Xml2PdfTask.PRODUCER);
				userAgent.setProducer(Xml2PdfTask.PRODUCER);
				userAgent.setBaseURL(this.baseURL);
				inputHandler.renderTo(userAgent, outputFormat, out);
				success = true;
			} finally {
				try {
					out.close();
				} catch (IOException ioe) {}
				if (!success)
					outFile.delete();
			}
		}

		private void render(File foFile, File outFile, String outputFormat) {
			if(task.logFiles) {
				task.log("Rendering " + foFile + " to " + outFile, Project.MSG_INFO);
			}
			InputHandler inputHandler = new InputHandler(foFile);
			try {
				renderInputHandler(inputHandler, outFile, outputFormat);
			} catch (Exception ex) {
				String mess = "Error rendering fo file: " + foFile;
				if(task.failonerror) {
					throw new BuildException(mess, ex);
				} else {
					task.log(mess, ex, Project.MSG_ERR);
				}
			}
		}

		private void render(File xmlFile, File xsltFile, File outFile, String outputFormat) {
			Vector<String> xsltParams = null;
			if(task.xsltParams != null) {
				xsltParams = new Vector<String>(task.xsltParams.size()*2);
				for(int i=0; i<task.xsltParams.size(); i++) {
					XsltParameter param = task.xsltParams.get(i);
					xsltParams.add(param.getName());
					xsltParams.add(param.getValue());
				}
			}
			if(task.logFiles) {
				task.log("Rendering " + xmlFile + " to " + outFile + " (style: " + xsltFile + ")", Project.MSG_INFO);
			}
			InputHandler inputHandler = new InputHandler(xmlFile, xsltFile, xsltParams);
			try {
				renderInputHandler(inputHandler, outFile, outputFormat);
			} catch (Exception ex) {
				String mess = "Error rendering xml/xslt files: " + xmlFile + ", " + xsltFile;
				if(task.failonerror) {
					throw new BuildException(mess, ex);
				} else {
					task.log(mess, ex, Project.MSG_ERR);
				}
			}
		}

	}

}