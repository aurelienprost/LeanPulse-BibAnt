/*********************************************
 * Copyright (c) 2013 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.ui;

import javax.swing.JComponent;
import javax.swing.JRadioButton;

import org.apache.tools.ant.BuildException;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class RadioButtonInput extends InputElement {
	
	private String text;
	private boolean defval = false;
	protected JRadioButton radioButton;
	
	public void setText(String text) {
		this.text = text;
	}
	
	public void addText(String text) {
		this.text = text.trim();
	}
	
	public void setDefault(boolean defval) {
		this.defval = defval;
	}
	
	protected JComponent[] initComponents(String value) throws BuildException {
		if(text == null)
			throw new BuildException("A text must be specified for the checkbox.");
		radioButton = new JRadioButton(text);
		radioButton.setSelected(value == null ? defval : Boolean.parseBoolean(value));
		return new JComponent[] {radioButton};
	}
	
	JRadioButton getRadioButton() {
		return radioButton;
	}
	
	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.InputElement#getValue()
	 */
	@Override
	protected String getValue() throws IllegalArgumentException {
		return Boolean.toString(radioButton.isSelected());
	}
	
	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.InputElement#select()
	 */
	@Override
	protected void select() {
		// NOP
	}

}
