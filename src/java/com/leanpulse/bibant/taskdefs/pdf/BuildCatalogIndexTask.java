/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.DeweyDecimal;

import com.google.code.jdde.client.ClientConversation;
import com.google.code.jdde.client.DdeClient;
import com.google.code.jdde.client.event.AsyncTransactionListener;
import com.google.code.jdde.event.AsyncTransactionEvent;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class BuildCatalogIndexTask extends Task {
	
	private File index;
	private int startDelay = 5;
	private String logerror = ".+\\WErr.+";
	private boolean keepLog = false;
	private Object lock = new Object();
	
	public void setIndex(File index) {
		this.index = index;
	}
	
	public void setStartdelay(int startDelay) {
		if(startDelay < 1)
			throw new BuildException("The start delay must be superior or equal to 1 second.");
		this.startDelay = startDelay;
	}
	
	public void setLogerrormatch(String logerror) {
		this.logerror = logerror;
	}
	
	public void setKeeplog(boolean keepLog) {
		this.keepLog = keepLog;
	}
	
	public void execute() throws BuildException {
		if(index == null)
			throw new BuildException("The catalog index file must be specified.");
		
		if(!System.getProperty("os.name").contains("Windows"))
			throw new BuildException("This task only supports Windows operating systems !");
		
		String acrobatCmd = AcrobatVersion.getCommand();
		DeweyDecimal acroVer = AcrobatVersion.getVersion();
		
		File indexDir = new File(index.getAbsolutePath().substring(0,index.getAbsolutePath().length()-4));
		File logFile = new File(indexDir.getPath() + ".log");
		
		log("Building catalog index " + index, Project.MSG_INFO);
		
		if(acroVer.isGreaterThanOrEqual(new DeweyDecimal("6.0"))) {
			File bpdxFile = null;
			FileWriter fw = null;
			try {
				bpdxFile = File.createTempFile("index", ".bpdx");
				fw = new FileWriter(bpdxFile);
				fw.write(index.getPath() + " /rebuild");
				fw.close();
			} catch (IOException e) {
				throw new BuildException("An error occured while creating the BPDX file.", e);
			} finally {
				if(fw != null)
					try {
						fw.close();
					} catch (IOException e) {}
			}
			Process process = null;
			try {
				process = Runtime.getRuntime().exec(new String[] {acrobatCmd, "/h", bpdxFile.getPath()});
				int time = 0;
				while(!logFile.exists() && time < startDelay) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
					time++;
				}
				if(logFile.exists()) {
					FileChannel channel = null;
					while(true) {
						try {
							if(channel == null)
								channel = new RandomAccessFile(logFile, "rw").getChannel();
							FileLock lock = channel.tryLock();
							if(lock != null) {
								lock.release();
								channel.close();
								break;
							}
						} catch(Exception e) {}
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {}
					}
				}
			} catch (Exception e) {
				throw new BuildException("An error occured while starting Adobe Acrobat.", e);
			} finally {
				if(process != null)
					process.destroy();
				bpdxFile.delete();
			}
		} else {
			Process process = null;
			ClientConversation conv = null;
			try {
				process = Runtime.getRuntime().exec(new String[] {acrobatCmd, "/h"});
				
				DdeClient client = new DdeClient();
				int time = 0;
				Exception connEx = null;
				while(conv == null && time < startDelay) {
					try {
						conv = client.connect("Acrocat", "Control");
					} catch(Exception e) {
						connEx = e;
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
					time++;
				}
				if(conv == null)
					throw connEx;
				conv.executeAsync("[FileBuild(\"" + index.getAbsolutePath() + "\")]", new AsyncTransactionListener() {
					public void onError(AsyncTransactionEvent e) {
						synchronized(lock) {
							lock.notify();
						}
					}
					public void onSuccess(AsyncTransactionEvent e) {
						synchronized(lock) {
							lock.notify();
						}
					}
				});
				synchronized(lock) {
					try {
						lock.wait();
					} catch (InterruptedException e) {}
				}
			} catch (Exception e) {
				throw new BuildException("An error occured while executing Adobe Acrobat.", e);
			} finally {
				if(conv != null)
					conv.disconnect();
				if(process != null)
					process.destroy();
			}
		}
		
		if(!indexDir.exists())
			throw new BuildException("The index build failed!\nSee Acrobat's log file for more details.");
		
		try {
			RandomAccessFile in = new RandomAccessFile(logFile,"r");
			try {
				long filePos = logFile.length()-1;
				int readByte = 0;
				while(filePos > 0 && readByte != 0xA) {
					filePos--;
					in.seek(filePos);
					readByte = in.readByte();
				}
				String lastLine = in.readLine();
				if(lastLine == null || lastLine.matches(logerror)) {
					throw new BuildException("The index build failed!\nSee Acrobat's log file for more details.");
				}
			} finally {
				in.close();
			}
		} catch(IOException e) {
			System.out.println(e);
			throw new BuildException("An error occured while reading the Acrobat's log file.", e);
		}
		
		if(!keepLog)
			logFile.delete();
	}

}
