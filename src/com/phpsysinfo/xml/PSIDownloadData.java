package com.phpsysinfo.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.phpsysinfo.activity.PSIActivity;

public class PSIDownloadData
extends AsyncTask<String, Void, Void>
{
	private PSIErrorCode errorCode = PSIErrorCode.NO_ERROR;
	private PSIActivity activity;
	private PSIHostData psiObject;
	private String address = "";

	public PSIDownloadData(PSIActivity psiaa) {
		super();
		this.activity = psiaa;
	}

	@Override
	protected Void doInBackground(String... strs) {
		address = strs[0];
		String user = strs[1];
		String password = strs[2];

		SAXParser parser = null;
		InputStream input = null;
		try {
			input = getUrl(address,user,password);
		}
		catch (Exception e) {
			Log.d("PSIAndroid", "BAD_URL", e);
			errorCode = PSIErrorCode.BAD_URL;
			return null;
		}

		try {
			parser = SAXParserFactory.newInstance().newSAXParser();
		}
		catch (Exception e) {
			Log.d("PSIAndroid", "XML_PARSER_CREATE", e);
			errorCode = PSIErrorCode.XML_PARSER_CREATE;
			return null;
		}

		DefaultHandler handler = new PSIXmlParse();
		try {
			if(input == null) {
				Log.d("PSIAndroid", "CANNOT_GET_XML");
				errorCode = PSIErrorCode.CANNOT_GET_XML;
				return null;
			}
			else {
				parser.parse(input, handler);
				psiObject = ((PSIXmlParse) handler).getData();
			}
		}
		catch (Exception e) {
			Log.d("PSIAndroid", "XML_PARSER_ERROR", e);
			errorCode = PSIErrorCode.XML_PARSER_ERROR;
			return null;
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (this.errorCode.equals(PSIErrorCode.NO_ERROR)) {
			this.activity.displayInfo(psiObject);
		}
		else {
			this.activity.displayError(address, errorCode);
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.activity.loadingStart();
	}

	private static InputStream getUrl(String url, String user, String password)
			throws MalformedURLException, IOException
			{

		int code = 0;
		URL u = new URL(url); 
		HttpURLConnection huc = null;

		//check if https url
		if (u.getProtocol().toLowerCase().equals("https")) {
			trustAllHosts();
			huc = (HttpsURLConnection) u.openConnection();
			((HttpsURLConnection)huc).setHostnameVerifier(DO_NOT_VERIFY);
		}
		else {
			huc = (HttpURLConnection) u.openConnection(); 
		}

		//if a username is set
		if(!user.equals("")) {
			huc.setRequestMethod("GET");
			huc.setRequestProperty("Authorization", 
					"Basic " + Base64.encodeToString((user+":"+password).getBytes(), Base64.NO_WRAP));
		}

		huc.connect();
		code = huc.getResponseCode();

		if (code == 200)
			return huc.getInputStream();
		else
			return null;
			}



	// always verify the host - don't check for certificate
	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};


	/**
	 * Trust every server - don't check for any certificate
	 */
	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
			.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
