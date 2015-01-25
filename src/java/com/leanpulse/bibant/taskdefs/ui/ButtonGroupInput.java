/*********************************************
 * Copyright (c) 2013 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.ui;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.tools.ant.BuildException;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class ButtonGroupInput extends InputElement {
	
	private int defval = 0;
	private List<RadioButtonInput> elements = new ArrayList<RadioButtonInput>();
	
	public void setDefault(int defval) {
		this.defval = defval;
	}
	
	public void addConfigured(RadioButtonInput radioButton) {
		elements.add(radioButton);
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.InputElement#initComponents(java.lang.String)
	 */
	@Override
	protected JComponent[] initComponents(String value) throws BuildException {
		int selectedIdx = value != null ? Integer.valueOf(value) : defval;
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(elements.size(), 1));
		ButtonGroup group = new ButtonGroup();
		for(int i=0; i<elements.size(); i++) {
			RadioButtonInput element = elements.get(i);
			element.initComponents(i == selectedIdx ? "true" : "false");
			panel.add(element.radioButton);
			group.add(element.radioButton);
		}
		return new JComponent[] {panel};
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.InputElement#getValue()
	 */
	@Override
	protected String getValue() throws IllegalArgumentException {
		for(int i=0; i<elements.size(); i++) {
			if(elements.get(i).radioButton.isSelected())
				return Integer.toString(i);
		}
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
