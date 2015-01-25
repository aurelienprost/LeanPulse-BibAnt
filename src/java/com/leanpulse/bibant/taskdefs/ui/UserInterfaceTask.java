/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.ui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class UserInterfaceTask extends Task {
	
	protected JFrame frame;
	protected String title;
	protected File file;
	protected List<InputElement> elements = new ArrayList<InputElement>();
	
	protected JLabel errorLabel;
	protected Properties props;
	
	private Object lock = new Object();
	private boolean aborted;
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setFile(File file) {
		this.file = file;
	}
	
	public void addConfigured(InputElement element) {
		elements.add(element);
	}
	
	public void execute () throws BuildException {
		if(elements.size() == 0)
			throw new BuildException("The user interface must specify at least one input element.");
		
		props = new Properties();
		if(file != null && file.exists()) {
			InputStream in = null;
	        try {
	            in = new FileInputStream(file);
	            props.load(in);
	        } catch (IOException e) {
	        	log("An error occured while loading the properties file " + file, e, Project.MSG_DEBUG);
	        } finally {
	            FileUtils.close(in);
	        }
		}
		
		frame = new JFrame(title == null ? "LeanPulse BibAnt" : "LeanPulse BibAnt - " + title);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				abort();
			}
		});
		
		try {
			BufferedImage image = ImageIO.read(UserInterfaceTask.class.getResource("/images/icon.png"));
			frame.setIconImage(image);
		} catch(IOException e) {}
		
		JComponent content = (JComponent) frame.getContentPane();
		content.setLayout(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints cons = new GridBagConstraints();
		cons.fill = GridBagConstraints.HORIZONTAL;
		cons.insets = new Insets(5,5,5,5);
		cons.weightx = 1;
		for(int i=0; i<elements.size(); i++) {
			InputElement el = elements.get(i);
			String propName = el.getProperty();
			String propValue = null;
			if(propName != null)
				propValue = props.getProperty(propName);
			JComponent[] components = el.initComponents(propValue);
			
			cons.gridy = i;
			switch(components.length) {
				case 1:
					cons.gridwidth = 3;
					cons.gridx = 0;
					content.add(components[0],cons);
					break;
				case 2:
					cons.weightx = 0;
					cons.gridwidth = 1;
					cons.gridx = 0;
					content.add(components[0],cons);
					cons.weightx = 1;
					cons.gridwidth = 2;
					cons.gridx = 1;
					content.add(components[1],cons);
					break;
				case 3:
					cons.weightx = 0;
					cons.gridwidth = 1;
					cons.gridx = 0;
					content.add(components[0],cons);
					cons.weightx = 1;
					cons.gridx = 1;
					content.add(components[1],cons);
					cons.weightx = 0;
					cons.gridx = 2;
					content.add(components[2],cons);
					break;
			}
		}
		
		errorLabel = new JLabel((String)null, JLabel.CENTER);
		errorLabel.setForeground(Color.RED);
		cons.gridwidth = 3;
		cons.gridx = 0;
		cons.gridy++;
		content.add(errorLabel, cons);
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridBagLayout());
		JButton continueButton = new JButton("Continue");
		continueButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				validate();
			}
		});
		JButton abortButton = new JButton("Abort");
		abortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				abort();
			}
		});
		buttonsPanel.add(continueButton);
		buttonsPanel.add(abortButton);
		cons.gridy++;
		content.add(buttonsPanel, cons);
		
		aborted = false;
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		log("Waiting for user input - " + title);
		
		synchronized(lock) {
			while (frame.isVisible())
				try {
					lock.wait();
				} catch (InterruptedException e) {}
		}
		
		if(aborted)
			throw new BuildException("Execution aborted by the user.");
	}
	
	private void abort() {
		aborted = true;
		frame.setVisible(false);
		synchronized(lock) {
			lock.notify();
		}
	}
	
	private void validate() {
		Project project = getProject();
		for(int i=0; i<elements.size(); i++) {
			InputElement el = elements.get(i);
			String propName = el.getProperty();
			if(propName != null) {
				try {
					String propValue = el.getValue();
					if(propValue != null) {
						project.setProperty(propName, propValue);
						if(!(el instanceof PasswordFieldInput))
							props.setProperty(propName, propValue);
					}
				} catch(IllegalArgumentException e) {
					errorLabel.setText(e.getMessage());
					el.select();
					return;
				}
			}
		}
		if(file != null && props.size() > 0) {
			try {
				props.store(new FileOutputStream(file), "Build properties automatically saved by the UserInterface task");
			} catch (IOException e) {
				log("An error occured while storing the properties file " + file, e, Project.MSG_DEBUG);
			}
		}
		frame.setVisible(false);
		synchronized(lock) {
			lock.notify();
		}
	}

}
