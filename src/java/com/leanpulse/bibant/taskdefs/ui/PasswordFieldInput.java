/*********************************************
 * Copyright (c) 2013 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.ui;

import javax.swing.JComponent;
import javax.swing.JPasswordField;

import org.apache.tools.ant.BuildException;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class PasswordFieldInput extends LabelInput {
	
	protected String tooltip;
	protected boolean allowempty = false;
	protected JPasswordField passwordField;
	
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
		passwordField = new JPasswordField(50);
		passwordField.setToolTipText(tooltip);
		return new JComponent[] {label, passwordField};
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.LabelInput#getValue()
	 */
	@Override
	protected String getValue() throws IllegalArgumentException {
		char[] password = passwordField.getPassword();
		if(!allowempty && password.length == 0)
			throw new IllegalArgumentException("This field can't be empty.");
		return new String(password);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.LabelInput#select()
	 */
	@Override
	protected void select() {
		passwordField.requestFocus();
		passwordField.selectAll();
	}

}
