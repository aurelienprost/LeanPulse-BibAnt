/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.ui;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.apache.tools.ant.BuildException;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class LabelInput extends InputElement {

	private String text;
	private String iconKey;
	protected JLabel label;
	
	public void setText(String text) {
		this.text = text;
	}
	
	public void addText(String text) {
		this.text = text.trim();
	}
	
	public void setIcon(String icon) {
		if("question".equals(icon)) {
			iconKey = "OptionPane.questionIcon";
		} else if("information".equals(icon)) {
			iconKey = "OptionPane.informationIcon";
		} else if("warning".equals(icon)) {
			iconKey = "OptionPane.warningIcon";
		} else if("error".equals(icon)) {
			iconKey = "OptionPane.errorIcon";
		} else {
			throw new BuildException("Unsupported icon !");
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.InputElement#initComponents(java.lang.String)
	 */
	@Override
	protected JComponent[] initComponents(String value) throws BuildException {
		if(text == null)
			throw new BuildException("A text must be specified for the label.");
		Icon icon = null;
		if(iconKey != null)
			icon = UIManager.getIcon(iconKey);
		label = new JLabel(text, icon, JLabel.LEADING);
		return new JComponent[] {label};
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.InputElement#getValue()
	 */
	@Override
	protected String getValue() throws IllegalArgumentException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.InputElement#select()
	 */
	@Override
	protected void select() {
		// NOP
	}

}
