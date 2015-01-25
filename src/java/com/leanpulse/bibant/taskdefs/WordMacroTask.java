/*********************************************
 * Copyright (c) 2013 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.DeweyDecimal;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 * 
 */
public class WordMacroTask extends MacroTask {
	
	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.MacroTask#startApp()
	 */
	@Override
	protected ActiveXComponent startApp() throws BuildException {
		try {
			ActiveXComponent word = null;
			ActiveXComponent tmpWord = new ActiveXComponent("Word.Application");
			if(new DeweyDecimal(tmpWord.getProperty("Version").getString()).isLessThanOrEqual(new DeweyDecimal("11.0"))) {
				// Workaround for bug : http://support.microsoft.com/?kbid=188546
				word = new ActiveXComponent("Word.Application");
				tmpWord.invoke("Quit");
				tmpWord.safeRelease();
			} else {
				word = tmpWord;
			}
			return word;
		} catch(Exception e) {
			throw new BuildException("MS Word doesn't seem to be correctly installed !", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.MacroTask#getBooks(com.jacob.activeX.ActiveXComponent)
	 */
	@Override
	protected Dispatch getBooks(ActiveXComponent app) {
		return app.getProperty("Documents").toDispatch(); 
	}
    
}
