/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.taskdefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.JDBCTask;

/**
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 *
 */
public class ExtractFileFromDbTask extends JDBCTask {
	
	private File destFile;
	private String sqlCommand;
	private boolean failifempty = false;
	
	public void setTofile(File destFile) {
		this.destFile = destFile;
	}
	
	public void setFailifempty(boolean failifempty) {
		this.failifempty = failifempty;
	}
	
	public void addText(String sql) {
		sqlCommand = sql.trim();
	}

    public void execute() throws BuildException {
    	if(sqlCommand == null)
    		throw new BuildException("The SQL command must be specified");
    	if(destFile == null)
    		throw new BuildException("The output file must be specified");
        Connection conn = getConnection();
        if(conn != null) {
	        Statement stmt = null;
	        try {
	            stmt = conn.createStatement();
	            ResultSet rs = stmt.executeQuery(sqlCommand);
	            ResultSetMetaData meta = rs.getMetaData();
	            if(meta.getColumnCount() != 1)
	            	throw new BuildException("The SQL command must return one single column");
	            int colType = meta.getColumnType(1);
	            if(colType != Types.LONGVARBINARY && colType != Types.BLOB)
	            	throw new BuildException("The SQL command must return a binary object");
	            if(rs.next()) {
	        		InputStream is = rs.getBinaryStream(1); 
	        		FileOutputStream fos = new FileOutputStream(destFile);
	        		try {
	            		int b = 0;  
	            		while((b = is.read()) != -1) {  
	            			fos.write(b);   
	            		}
	        		} finally {
	        			try {fos.close();} catch (IOException ignore) {}
	        		}
	            } else {
	            	if(failifempty)
	            		throw new BuildException("The SQL command didn't return any result.");
	            }
	        } catch (SQLException e) {
	        	throw new BuildException(e);
	        } catch (IOException e) {
	        	throw new BuildException(e);
			} finally {
	            if (stmt != null) {
	                try {stmt.close();} catch (SQLException ignore) {}
	            }
	            if (conn != null) {
	                try {conn.close();} catch (SQLException ignore) {}
	            }
	        }
        }
    }

}
