/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.DeweyDecimal;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class Word2PdfTask extends Print2PdfTask {
	
	public static class ChangesAttribute extends EnumeratedAttribute {
		
		public static final int NOP = 0;
		public static final int ACCEPT = 1;
		public static final int REJECT = 2;
		
		public ChangesAttribute() {}

		/* (non-Javadoc)
		 * @see org.apache.tools.ant.types.EnumeratedAttribute#getValues()
		 */
		@Override
		public String[] getValues() {
			return new String[] {
				"nop",
				"accept",
				"reject"
			};
		}

	}
	
	private boolean visible = false;
	private boolean rmcomments = false;
	private int changes = ChangesAttribute.NOP;
	private boolean bkgprinting = true;
	private boolean striphidbkm = false;
	
	private ActiveXComponent word = null;
	private Dispatch docs = null;
	private Variant wordDefPrinter = null;
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public void setRmcomments(boolean rmcomments) {
		this.rmcomments = rmcomments;
	}
	 
	public void setChanges(ChangesAttribute changesAttr) {
		this.changes = changesAttr.getIndex();
	}
	
	public void setBkgprinting(boolean bkgprinting) {
		this.bkgprinting = bkgprinting;
	}
	
	public void setStriphiddenbookmarks(boolean striphidbkm) {
		this.striphidbkm = striphidbkm;
	}
	
	protected void initPrinting() throws BuildException {
		super.initPrinting();
		try {
			ActiveXComponent tmpWord = new ActiveXComponent("Word.Application");
			if(new DeweyDecimal(tmpWord.getProperty("Version").getString()).isLessThanOrEqual(new DeweyDecimal("11.0"))) {
				// Workaround for bug : http://support.microsoft.com/?kbid=188546
				word = new ActiveXComponent("Word.Application");
				tmpWord.invoke("Quit");
				tmpWord.safeRelease();
			} else {
				word = tmpWord;
			}
			word.setProperty("Visible", visible);
			docs = word.getProperty("Documents").toDispatch();
		} catch(Exception e) {
			throw new BuildException("MS Word doesn't seem to be correctly installed !", e);
		}
	}
	
	protected void configurePrinter() throws BuildException {
		super.configurePrinter();
		try {
			wordDefPrinter = word.getProperty("ActivePrinter");
			word.setProperty("ActivePrinter", "PDFCreator");
		} catch(Exception e) {
			throw new BuildException("An error occured while configuring Word.", e);
		}
	}
	
	protected void launchPrint(File src) {
		Dispatch doc = Dispatch.call(docs, "Open", src.getPath(), new Variant(false)).toDispatch();
		try {
			if(rmcomments) {
				Dispatch comments = Dispatch.get(doc, "Comments").toDispatch();
				if(Dispatch.get(comments, "Count").getInt() > 0)
					Dispatch.call(doc, "DeleteAllComments");
				comments.safeRelease();
			}
			switch(changes) {
				case ChangesAttribute.ACCEPT:
					Dispatch.call(doc, "AcceptAllRevisions");
					break;
				case ChangesAttribute.REJECT:
					Dispatch.call(doc, "RejectAllRevisions");
					break;
			}
			if(striphidbkm) {
				Dispatch bookmarks = Dispatch.get(doc, "Bookmarks").toDispatch();
				Dispatch.put(bookmarks, "ShowHidden", new Variant(true));
				boolean hiddenBkmDeleted = false;
				for(int i = Dispatch.get(bookmarks, "Count").getInt(); i > 0; i--) {
					Dispatch bookmark = Dispatch.call(bookmarks, "Item", new Variant(i)).toDispatch();
					if(Dispatch.get(bookmark, "Name").getString().startsWith("_Toc")) {
						Dispatch.call(bookmark, "Delete");
						hiddenBkmDeleted = true;
					}
					bookmark.safeRelease();
					Dispatch.call(doc, "UndoClear");
				}
				Dispatch.put(bookmarks, "ShowHidden", new Variant(false));
				bookmarks.safeRelease();
				if(hiddenBkmDeleted) {
					Dispatch tocs = Dispatch.get(doc, "TablesOfContents").toDispatch();
					for(int i = 1; i <= Dispatch.get(tocs, "Count").getInt(); i++) {
						Dispatch toc = Dispatch.call(tocs, "Item", new Variant(i)).toDispatch();
						Dispatch.call(toc, "Update");
						toc.safeRelease();
					}
					tocs.safeRelease();
				}
			}	
			Dispatch.call(doc, "PrintOut", new Variant(bkgprinting));
		} finally {
			Dispatch.call(doc, "Close", new Variant(0));
			doc.safeRelease();
		}
	}
	
	protected void endPrinting() {
		if(word != null) {
			try {
				if(docs != null) docs.safeRelease();
				if(wordDefPrinter != null) word.setProperty("ActivePrinter", wordDefPrinter);
				word.invoke("Quit");
				word.safeRelease();
			} catch(Exception e) {}
			word = null;
		}
		super.endPrinting();
	}

}
