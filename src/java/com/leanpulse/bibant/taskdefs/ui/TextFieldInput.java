/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.ui;

import javax.swing.JComponent;
import javax.swing.JTextField;

import org.apache.tools.ant.BuildException;


/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class TextFieldInput extends LabelInput {
	
	protected String defval;
	protected String tooltip;
	protected boolean allowempty = false;
	protected JTextField textField;
	
	public void setDefault(String defval) {
		this.defval = defval;
	}
	
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}
	
	public void setAllowempty(boolean allowempty) {
		this.allowempty = allowempty;
	}

	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.LabelInput#initComponents(java.lang.String)
	 */
	@Override
	protected JComponent[] initComponents(String value) throws BuildException {
		super.initComponents(value);
		textField = new JTextField(value == null ? defval : value, 50);
		textField.setToolTipText(tooltip);
		return new JComponent[] {label, textField};
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.LabelInput#getValue()
	 */
	@Override
	protected String getValue() throws IllegalArgumentException {
		String value = textField.getText();
		if(!allowempty && value.length() == 0)
			throw new IllegalArgumentException("This field can't be empty.");
		return value;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.LabelInput#select()
	 */
	@Override
	protected void select() {
		textField.requestFocus();
		textField.selectAll();
	}


}
