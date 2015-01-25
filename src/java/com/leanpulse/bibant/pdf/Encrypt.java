/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.PublicKeyProtectionPolicy;
import org.apache.pdfbox.pdmodel.encryption.PublicKeyRecipient;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

/**
 * Processor to encrypt a PDF document.
 * 
 * The PDF document can be protected by the standard security handler (password protection) as well
 * as by the public key security handler (X509 certificate protection).
 * 
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 */
public class Encrypt implements PdfProcessor {

    private String ownerPassword;
    private String userPassword;
    private File certFile;
    private int keyLength;
    private AccessPermission ap;
    
	/**
	 * Create a new encrypt processor.
	 * 
	 * @param ownerPassword
	 *            the owner's password (for the standard security handler)
	 * @param userPassword
	 *            the user's password (for the standard security handler)
	 * @param ap
	 *            the access permissions to the document
	 * @param certFile
	 *            the X509 certificate file (for the public key security
	 *            handler)
	 * @param keyLength
	 *            the length in (bits) of the secret key that will be used to
	 *            encrypt document data
	 */
    public Encrypt(String ownerPassword, String userPassword, AccessPermission ap, File certFile, int keyLength) {
    	this.ownerPassword = ownerPassword;
    	this.userPassword = userPassword;
    	this.ap = ap;
    	this.certFile = certFile;
    	this.keyLength = keyLength;
    }
    

	/* (non-Javadoc)
	 * @see com.leanpulse.pdf.PdfProcessor#processDocument(org.apache.pdfbox.pdmodel.PDDocument)
	 */
	public void processDocument(PDDocument doc) throws IOException {
		if(!doc.isEncrypted()) {
			try {
				if(certFile != null) {
					PublicKeyProtectionPolicy ppp = new PublicKeyProtectionPolicy();
					PublicKeyRecipient recip = new PublicKeyRecipient();
					recip.setPermission(ap);
	
					CertificateFactory cf = CertificateFactory.getInstance("X.509");
					InputStream inStream = new FileInputStream(certFile.getAbsolutePath());
					X509Certificate certificate = (X509Certificate) cf.generateCertificate(inStream);
					inStream.close();
	
					recip.setX509(certificate);
	
					ppp.addRecipient(recip);
	
					ppp.setEncryptionKeyLength(keyLength);
	
					doc.protect(ppp);
				} else {
					StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
					spp.setEncryptionKeyLength(keyLength);
					doc.protect(spp);
				}
			} catch(Exception e) {
				throw new IOException(e.getMessage());
			}
		} else {
			throw new IOException("The document is already encrypted.");
		}
	}

}
