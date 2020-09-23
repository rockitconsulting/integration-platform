package com.rockit.ip.healthmon;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Hashtable;

public class JksUtils {
	private KeyStore jks = null;
	private final String pwd;
	private final String jksPath;
	private Hashtable<String, X509Certificate> certs  = new Hashtable<>();

	public JksUtils(final String jksPath, final String pwd)	throws Exception {
		super();
		this.jksPath = jksPath;
		this.pwd = pwd;
		this.loadJKS();
	}

	
	public Hashtable<String, X509Certificate> getCerts()  {
		return certs;
	}

	
	private void loadJKS() throws Exception {
		this.jks = KeyStore.getInstance(KeyStore.getDefaultType());
		this.jks.load(new FileInputStream(
				new File(this.jksPath)), this.pwd.toCharArray());
		
		for (Enumeration<String> e = this.jks.aliases(); e
				.hasMoreElements();) {
			String alias = e.nextElement();
			Certificate cert = this.jks.getCertificate(alias);
			if (cert instanceof X509Certificate) {
				certs.put(alias, (X509Certificate) cert);
			}
		}
	}
}