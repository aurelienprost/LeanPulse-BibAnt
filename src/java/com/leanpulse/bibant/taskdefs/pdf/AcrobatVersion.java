/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.util.DeweyDecimal;

import com.leanpulse.bibant.utils.WinRegistry;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class AcrobatVersion extends Task implements Condition {
	
	private static final String ACROBAT_REGKEY = (System.getenv("ProgramFiles(x86)") == null) ?
			"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\Acrobat.exe" :
			"SOFTWARE\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\App Paths\\Acrobat.exe";
	
	private static final Pattern ACROBAT_VER_PATTERN = Pattern.compile("Acrobat (\\d+\\.\\d+)");
	
	private static String acroCmd = null;
	private static DeweyDecimal acroVer = null;
	
	static String getCommand() throws BuildException {
		if(acroCmd != null)
			return acroCmd;
		else {
			try {
				acroCmd = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE, ACROBAT_REGKEY, "");
			} catch(Exception e) {}
			if(acroCmd == null)
				throw new BuildException("Adobe Acrobat doesn't seem to be correctly installed !");
			else
				return acroCmd;
		}
	}
	
	static DeweyDecimal getVersion() throws BuildException {
		if(acroVer != null)
			return acroVer;
		else {
			Matcher matcher = ACROBAT_VER_PATTERN.matcher(getCommand());
			if(matcher.find()) {
				acroVer = new DeweyDecimal(matcher.group(1));
				return acroVer;
			} else
				throw new BuildException("Can't determine the Adobe Acrobat version.");
		}
    }
	
	private String atLeast = null;
    private String exactly = null;
    private String propertyname = null;

    /**
     * Set the atleast attribute.
     * This is of the form major.minor.
     * For example 10.0.
     * @param atLeast the version to check against.
     */
    public void setAtLeast(String atLeast) {
        this.atLeast = atLeast;
    }

    /**
     * Set the exactly attribute.
     * This is of the form major.minor.
     * For example 10.0.
     * @param exactly the version to check against.
     */
    public void setExactly(String exactly) {
        this.exactly = exactly;
    }

    /**
     * Set the name of the property to hold the Acrobat version.
     * @param propertyname the name of the property.
     */
    public void setProperty(String propertyname) {
        this.propertyname = propertyname;
    }
    
    /**
     * Run as a task.
     * @throws BuildException if an error occurs.
     */
    public void execute() throws BuildException {
        if (propertyname == null) {
            throw new BuildException("'property' must be set.");
        }
        if (atLeast != null || exactly != null) {
            // If condition values are set, evaluate the condition
            if (eval()) {
                getProject().setNewProperty(propertyname, getVersion().toString());
            }
        } else {
            // Raw task
            getProject().setNewProperty(propertyname, getVersion().toString());
        }
    }
    
    /**
     * Evalute the condition.
     * @return true if the condition is true.
     * @throws BuildException if an error occurs.
     */
    public boolean eval() throws BuildException {
        validate();
        DeweyDecimal actual = getVersion();
        if (null != atLeast) {
            return actual.isGreaterThanOrEqual(new DeweyDecimal(atLeast));
        }
        if (null != exactly) {
            return actual.isEqual(new DeweyDecimal(exactly));
        }
        //default
        return false;
    }
    
    private void validate() throws BuildException {
        if (atLeast != null && exactly != null) {
            throw new BuildException("Only one of atleast or exactly may be set.");
        }
        if (null == atLeast && null == exactly) {
            throw new BuildException("One of atleast or exactly must be set.");
        }
        if (atLeast != null) {
            try {
                new DeweyDecimal(atLeast);
            } catch (NumberFormatException e) {
                throw new BuildException(
                    "The 'atleast' attribute is not a Dewey Decimal eg 10.0 : "
                    + atLeast);
            }
        } else {
            try {
                new DeweyDecimal(exactly);
            } catch (NumberFormatException e) {
                throw new BuildException(
                    "The 'exactly' attribute is not a Dewey Decimal eg 10.0 : "
                    + exactly);
            }
        }
    }

}
