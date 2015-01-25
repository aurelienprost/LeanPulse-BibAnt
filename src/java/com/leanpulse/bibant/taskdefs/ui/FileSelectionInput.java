/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.tools.ant.BuildException;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class FileSelectionInput extends TextFieldInput {
	
	private int mode = JFileChooser.DIRECTORIES_ONLY;
	private boolean allownotexist = false;
	private String filterDescription = null;
	private SuffixFileFilter fileFilter = null;
	protected JButton button;
	
	public void setMode(String mode) {
		if("directory".equals(mode))
			this.mode = JFileChooser.DIRECTORIES_ONLY;
		else if("file".equals(mode))
			this.mode = JFileChooser.FILES_ONLY;
		else
			throw new BuildException("The mode attribute must be set to either \"file\" or \"directory\".");
	}
	
	public void setAllownotexist(boolean allownotexist) {
		this.allownotexist = allownotexist;
	}
	
	public void setFilterdescription(String filterDescription) {
		this.filterDescription = filterDescription;
	}
	
	public void setFiltersuffixes(String filterSuffixes) {
		this.fileFilter = new SuffixFileFilter(filterSuffixes.split(":"));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.TextFieldInput#initComponents(java.lang.String)
	 */
	@Override
	protected JComponent[] initComponents(String value) throws BuildException {
		super.initComponents(value);
		if(fileFilter != null && filterDescription == null)
			throw new BuildException("If a file filter is defined, a description must be provided with the \"filterdescription\" attribute.");
		button = new JButton("...");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(textField.getText());
				fc.setFileSelectionMode(mode);
				if(fileFilter != null) {
					fc.setFileFilter(new FileFilter() {
						@Override
						public String getDescription() {
							return filterDescription;
						}
						@Override
						public boolean accept(File f) {
							if(mode == JFileChooser.FILES_ONLY && f.isDirectory())
								return true;
							return fileFilter.accept(f);
						}
					});
				}
				int returnVal = fc.showOpenDialog(textField);
				if (returnVal == JFileChooser.APPROVE_OPTION)
					textField.setText(fc.getSelectedFile().getPath());
			}
		});
		return new JComponent[] {label, textField, button};
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.leanpulse.bibant.taskdefs.ui.TextFieldInput#getValue()
	 */
	@Override
	protected String getValue() throws IllegalArgumentException {
		String value = super.getValue();
		if(value == null || value.length() == 0)
			return value;
		File file = new File(value);
		if(!allownotexist) {
			if(!file.exists())
				throw new IllegalArgumentException("The " + (mode == JFileChooser.FILES_ONLY ? "file" : "directory") + " must exist.");
			switch(mode) {
				case JFileChooser.FILES_ONLY:
					if(file.isDirectory())
						throw new IllegalArgumentException("The path must point to a file.");
					break;
				default:
					if(file.isFile())
						throw new IllegalArgumentException("The path must point to a directory.");
					break;
			}
		}
		return value;
	}

}
