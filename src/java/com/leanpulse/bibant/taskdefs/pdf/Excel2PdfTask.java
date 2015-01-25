/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComFailException;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.leanpulse.bibant.utils.WinRegistry;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class Excel2PdfTask extends Print2PdfTask {
	
	private static final int ALL = 0;
	private static final int ACTIVE = 1;
	private static final int SELECTED = 2;
	
	private static String pdfCreatorLocalizedName;
	
	protected static String getPDFCreatorLocalizedName(String activePrinter) {
		if(pdfCreatorLocalizedName == null) {
			try {
				String paramsString = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows NT\\CurrentVersion\\Devices", "PDFCreator");
				if(paramsString != null) {
					String[] params = paramsString.split(",");
					String port;
					if(params.length == 2)
						port = params[1];
					else
						port = "Ne00:";
					String[] activePrinterFields = activePrinter.split(" ");
					String localizedOn = activePrinterFields[activePrinterFields.length-2];
					pdfCreatorLocalizedName = "PDFCreator " + localizedOn + " " + port;
				} else
					throw new BuildException("Can't find the PDFCreator printer !");
			} catch (Exception e) {
				throw new BuildException("Unable to compute the PDFCreator printer name !", e);
			}
		}
		return pdfCreatorLocalizedName;
	}
	
	public static class OrientationAttribute extends EnumeratedAttribute {
		public static final int PORTRAIT = 0;
		public static final int LANDSCAPE = 1;
		
		public OrientationAttribute() {}

		public String[] getValues() {
			return new String[] {
				"portrait",
				"landscape"
			};
		}
	}
	
	private boolean visible = false;
	private int sheetsToPrint = ALL;
	private String[] sheetNames;
	private int orientation = OrientationAttribute.PORTRAIT;
	private int fitToPagesWide = -1;
	private int fitToPagesTall = -1;
	
	private ActiveXComponent excel = null;
	private Dispatch workbooks = null;
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public void setSheets(String sheets) {
		if("all".equals(sheets))
			sheetsToPrint = ALL;
		else if("active".equals(sheets))
			sheetsToPrint = ACTIVE;
		else {
			sheetsToPrint = SELECTED;
			sheetNames = sheets.split(",|\\s");
		}
	}
	
	public void setOrientation(OrientationAttribute orientationAttr) {
		this.orientation = orientationAttr.getIndex();
	}
	
	public void setFittopageswide(int pagesWide) {
		this.fitToPagesWide = pagesWide;
	}
	
	public void setFittopagestall(int pagesTall) {
		this.fitToPagesTall = pagesTall;
	}
	
	protected void initPrinting() throws BuildException {
		super.initPrinting();
		try {
			excel = new ActiveXComponent("Excel.Application");
			excel.setProperty("Visible", visible);
			excel.setProperty("IgnoreRemoteRequests", true);
			excel.setProperty("Interactive", false);
			workbooks = excel.getProperty("Workbooks").toDispatch();
		} catch(Exception e) {
			throw new BuildException("Excel doesn't seem to be correctly installed !", e);
		}
	}
	
	protected void launchPrint(File src) {
		Dispatch workbook = Dispatch.invoke(workbooks, "Open", Dispatch.Get, new Object[] {src.getPath()}, new int[1]).toDispatch();
		Variant xlDefPrinter = excel.getProperty("ActivePrinter");
		try {
			excel.setProperty("ActivePrinter", getPDFCreatorLocalizedName(xlDefPrinter.getString()));
			switch(sheetsToPrint) {
				case ALL:
					Dispatch sheets = Dispatch.get(workbook, "Sheets").toDispatch();
					try {
						for(int i=1; i <= Dispatch.get(sheets, "Count").getInt(); i++) {
							Dispatch sheet = Dispatch.invoke(sheets, "Item", Dispatch.Get, new Object[] {i}, new int[1]).toDispatch();
							try {
								setupPage(sheet);
							} finally {
								sheet.safeRelease();
							}
						}
					} finally {
						sheets.safeRelease();
					}
					Dispatch.call(workbook, "PrintOut");
					break;
				case ACTIVE:
					Dispatch activeSheet = Dispatch.get(excel, "ActiveSheet").toDispatch();
					try {
						setupPage(activeSheet);
						Dispatch.call(activeSheet, "PrintOut");
					} finally {
						activeSheet.safeRelease();
					}
					break;
				case SELECTED:
					try {
						sheets = Dispatch.invoke(workbook, "Sheets", Dispatch.Get, new Object[] {sheetNames}, new int[1]).toDispatch();
						try {
							for(int i=1; i <= Dispatch.get(sheets, "Count").getInt(); i++) {
								Dispatch sheet = Dispatch.invoke(sheets, "Item", Dispatch.Get, new Object[] {i}, new int[1]).toDispatch();
								try {
									setupPage(sheet);
								} finally {
									sheet.safeRelease();
								}
							}
							Dispatch.call(sheets, "PrintOut");
						} finally {
							sheets.safeRelease();
						}
					} catch(ComFailException e) {
						throw new BuildException("One of the selected Excel Sheet doesn't exist.", e);
					}
					break;
			}
		} finally {
			excel.setProperty("ActivePrinter", xlDefPrinter);
			Dispatch.call(workbook, "Close", new Variant(0));
			workbook.safeRelease();
		}
	}
	
	protected void setupPage(Dispatch sheet) {
		Dispatch pageSetup = Dispatch.get(sheet, "PageSetup").toDispatch();
		try {
			if(orientation == OrientationAttribute.LANDSCAPE)
				Dispatch.put(pageSetup, "Orientation", 2);
			else
				Dispatch.put(pageSetup, "Orientation", 1);
			if(fitToPagesWide > 0 || fitToPagesTall > 0) {
				Dispatch.put(pageSetup, "Zoom", false);
				Dispatch.put(pageSetup, "FitToPagesWide", fitToPagesWide > 0 ? fitToPagesWide : false);
				Dispatch.put(pageSetup, "FitToPagesTall", fitToPagesTall > 0 ? fitToPagesTall : false);
			}
		} finally {
			pageSetup.safeRelease();
		}
	}
	
	protected void endPrinting() {
		if(excel != null) {
			try {
				if(workbooks != null) workbooks.safeRelease();
				excel.setProperty("IgnoreRemoteRequests", false);
				excel.invoke("Quit");
				excel.safeRelease();
			} catch(Exception e) {}
			excel = null;
		}
		super.endPrinting();
	}

}
