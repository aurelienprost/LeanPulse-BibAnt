/*********************************************
 * Copyright (c) 2012 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant.pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.DecryptionMaterial;
import org.apache.pdfbox.pdmodel.encryption.PublicKeyDecryptionMaterial;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;

/**
 * Processor to decrypt an encrypted PDF document.
 * 
 * The PDF document can be protected by the standard security handler (password protection) as well
 * as by the public key security handler (X509 certificate protection).
 * 
 * @author <a href="mailto:a.prost@leanpulse.com">Aurélien PROST</a>
 */
public class Decrypt implements PdfProcessor {
	
	private String password;
    private File keyStore;
    private String alias;
    
	/**
	 * Create a new decrypt processor.
	 * 
	 * @param password
	 *            The password used to protect the document or, if a keystore is
	 *            defined, to extract the private key from there.
	 * @param keyStore
	 *            The keystore were the private key and the certificate are
	 * @param alias
	 *            The alias of the private key and the certificate. If the
	 *            keystore contains only 1 entry, this parameter can be left
	 *            null.
	 */
	public Decrypt(String password, File keyStore, String alias) {
		this.password = password;
		this.keyStore = keyStore;
		this.alias = alias;
	}

	/* (non-Javadoc)
	 * @see com.leanpulse.pdf.PdfProcessor#processDocument(org.apache.pdfbox.pdmodel.PDDocument)
	 */
	public void processDocument(PDDocument doc) throws IOException {
		if(doc.isEncrypted()) {
			try {
				DecryptionMaterial decryptionMaterial = null;
				if (keyStore != null) {
					KeyStore ks = KeyStore.getInstance("PKCS12");
					ks.load(new FileInputStream(keyStore), password.toCharArray());
					decryptionMaterial = new PublicKeyDecryptionMaterial(ks, alias, password);
				} else {
					decryptionMaterial = new StandardDecryptionMaterial(password);
				}
				doc.openProtection(decryptionMaterial);
			} catch(Exception e) {
				throw new IOException(e.getMessage());
			}
			doc.setAllSecurityToBeRemoved(true);
			/*AccessPermission ap = doc.getCurrentAccessPermission();
			if(!ap.isOwnerPermission())
				throw new IOException("You are only allowed to decrypt a document with the owner password.");*/
		} else {
			throw new IOException("The document is not encrypted.");
		}
	}

}
