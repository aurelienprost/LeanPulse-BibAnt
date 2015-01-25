/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs.pdf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

import com.leanpulse.bibant.pdf.PdfProcessor;
import com.leanpulse.bibant.pdf.ReplaceRegExp;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class ReplaceRegExpTask extends PdfProcessingTask {
	
	private String match = null;
	private String replace = null;
	private int countperpage = -1;
	private int startpage = -1;
	private int endpage = Integer.MAX_VALUE;
	private PDFont subFont = PDType1Font.TIMES_ROMAN;
	
	private List<ReplaceEntry> repEntries;
    
    /**
     * The regular expression pattern to match in the file(s);
     * required if no nested &lt;entry&gt; is used
     * @param match the match attribute.
     */
    public void setMatch(String match) {
        if(repEntries != null)
            throw new BuildException("You cannot supply replace attributes "
            		+ "and nested replace entries at the same time.");
    	this.match = match;
    }
    
    /**
     * The substitution string to place in the file(s) in place
     * of the regular expression.
     * Required if no nested &lt;entry&gt; is used
     * @param replace the replace attribute
     */
    public void setReplace(String replace) {
    	if(repEntries != null)
            throw new BuildException("You cannot supply replace attributes "
            		+ "and nested replace entries at the same time.");
    	this.replace = replace;
    }
    
    public void setCountperpage(int num) {
		countperpage = num;
	}
	
	public void setStartpage(int num) {
		startpage = num;
	}
	
	public void setEndpage(int num) {
		endpage = num;
	}
	
	public void setSubfont(String name) {
		if(name.equals("Times-Roman"))
			subFont = PDType1Font.TIMES_ROMAN;
		else if(name.equals("Times-Bold"))
			subFont = PDType1Font.TIMES_BOLD;
		else if(name.equals("Times-Italic"))
			subFont = PDType1Font.TIMES_ITALIC;
		else if(name.equals("Times-BoldItalic"))
			subFont = PDType1Font.TIMES_BOLD_ITALIC;
		else if(name.equals("Helvetica"))
			subFont = PDType1Font.HELVETICA;
		else if(name.equals("Helvetica-Bold"))
			subFont = PDType1Font.HELVETICA_BOLD;
		else if(name.equals("Helvetica-Oblique"))
			subFont = PDType1Font.HELVETICA_OBLIQUE;
		else if(name.equals("Helvetica-BoldOblique"))
			subFont = PDType1Font.HELVETICA_BOLD_OBLIQUE;
		else if(name.equals("Courier"))
			subFont = PDType1Font.COURIER;
		else if(name.equals("Courier-Bold"))
			subFont = PDType1Font.COURIER_BOLD;
		else if(name.equals("Courier-Oblique"))
			subFont = PDType1Font.COURIER_OBLIQUE;
		else if(name.equals("Courier-BoldOblique"))
			subFont = PDType1Font.COURIER_BOLD_OBLIQUE;
		else if(name.equals("Symbol"))
			subFont = PDType1Font.SYMBOL;
		else if(name.equals("ZapfDingbats"))
			subFont = PDType1Font.ZAPF_DINGBATS;
		else
			throw new BuildException(name + "isn't a valid Base 14 font.");
	}
	
	/**
     * Adds a replace entry.
     *
     * @return new entry created.
     */
	public ReplaceEntry createRepentry() {
		if(match != null)
            throw new BuildException("You cannot supply replace attributes "
            		+ "and nested replace entries at the same time.");
		if(repEntries == null)
			repEntries = new ArrayList<ReplaceEntry>();
		ReplaceEntry entry = new ReplaceEntry();
		repEntries.add(entry);
    	return entry;
    }
	
	
    public PdfProcessor getProcessor() throws BuildException {
    	if(match == null && repEntries == null)
    		throw new BuildException("No expression to match.");
    	if(match != null && replace == null)
    		throw new BuildException("Nothing to replace expression with.");
    	
    	ReplaceRegExp rep = new ReplaceRegExp(subFont);
    	if(match != null) {
    		rep.addReplace(match, replace, countperpage, startpage, endpage);
    	} else {
    		for(Iterator<ReplaceEntry> i = repEntries.iterator(); i.hasNext(); ) {
    			ReplaceEntry entry = i.next();
    			rep.addReplace(entry.getMatch(), entry.getReplace(),
    					entry.getCountperpage() > 0 ? entry.getCountperpage() : countperpage,
    					entry.getStartpage() > -1 ? entry.getStartpage() : startpage,
    					entry.getEndpage() < Integer.MAX_VALUE ? entry.getEndpage() : endpage);
    		}
    	}
    	return rep;
    }
    
    
    public static class ReplaceEntry extends ProjectComponent {
    	private String match;
    	private String replace;
    	private int countperpage = -1;
    	private int startpage = -1;
    	private int endpage = Integer.MAX_VALUE;
    	
    	public void setMatch(String string) {
    		match = string;
    	}
    	
    	public void setReplace(String string) {
    		replace = string;
    	}
    	
    	public void setCountperpage(int num) {
    		countperpage = num;
    	}
    	
    	public void setStartpage(int num) {
    		startpage = num;
    	}
    	
    	public void setEndpage(int num) {
    		endpage = num;
    	}
    	
    	public String getMatch() throws BuildException {
    		if(match == null)
    			throw new BuildException("Attribute 'match' has to be defined.");
    		return match;
    	}
    	
    	public String getReplace() throws BuildException {
    		if(replace == null)
    			throw new BuildException("Attribute 'match' has to be defined.");
    		return replace;
    	}
    	
    	public int getCountperpage() {
    		return countperpage;
    	}
    	
    	public int getStartpage() {
    		return startpage;
    	}
    	
    	public int getEndpage() {
    		return endpage;
    	}

    }

}
