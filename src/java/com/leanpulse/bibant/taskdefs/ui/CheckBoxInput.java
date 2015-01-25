/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.ui;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import org.apache.tools.ant.BuildException;


/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class CheckBoxInput extends InputElement {
	
	private String text;
	private boolean defval = false;
	protected JCheckBox checkBox;
	
	public void setText(String text) {
		this.text = text;
	}
	
	public void addText(String text) {
		this.text = text.trim();
	}
	
	public void setDefault(boolean defval) {
		this.defval = defval;
	}

	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.InputElement#initComponents(java.lang.String)
	 */
	@Override
	protected JComponent[] initComponents(String value) throws BuildException {
		if(text == null)
			throw new BuildException("A text must be specified for the checkbox.");
		checkBox = new JCheckBox(text);
		checkBox.setSelected(value == null ? defval : Boolean.parseBoolean(value));
		return new JComponent[] {checkBox};
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.InputElement#getValue()
	 */
	@Override
	protected String getValue() throws IllegalArgumentException {
		return Boolean.toString(checkBox.isSelected());
	}
	
	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.InputElement#select()
	 */
	@Override
	protected void select() {
		// NOP
	}
	
}
