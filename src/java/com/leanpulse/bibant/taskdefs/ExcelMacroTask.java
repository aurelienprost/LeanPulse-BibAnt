/*********************************************
 * Copyright (c) 2013 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs;

import org.apache.tools.ant.BuildException;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class ExcelMacroTask extends MacroTask {

	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.MacroTask#startApp()
	 */
	protected ActiveXComponent startApp() throws BuildException {
		try {
			ActiveXComponent excel = new ActiveXComponent("Excel.Application");
			excel.setProperty("IgnoreRemoteRequests", true);
			excel.setProperty("Interactive", interactive);
			return excel;
		} catch(Exception e) {
			throw new BuildException("MS Excel doesn't seem to be correctly installed !", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.MacroTask#getBooks(com.jacob.activeX.ActiveXComponent)
	 */
	protected Dispatch getBooks(ActiveXComponent app) {
		return app.getProperty("Workbooks").toDispatch(); 
	}
	
	@Override
	protected void exitApp(ActiveXComponent app) {
		app.setProperty("IgnoreRemoteRequests", false);
		super.exitApp(app);
    }

}
