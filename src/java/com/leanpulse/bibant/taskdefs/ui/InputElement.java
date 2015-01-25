/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.ui;

import javax.swing.JComponent;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public abstract class InputElement extends ProjectComponent {
	
	protected String property;
	
	public void setProperty(String property) {
		this.property = property;
	}
	
	public String getProperty() {
		return property;
	}
	
	protected abstract JComponent[] initComponents(String value) throws BuildException;
	
	protected abstract String getValue() throws IllegalArgumentException;
	
	protected abstract void select();
	
}
