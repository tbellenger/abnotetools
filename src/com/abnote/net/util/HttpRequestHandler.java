package com.abnote.net.util;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.util.Log;

public class HttpRequestHandler {
	private static final String TAG = "HttpRequestHandler";
	private static HttpRequestHandler _hrh;
	private static AndroidHttpClient client;
	private static BasicHttpContext mLocalContext = new BasicHttpContext();
	private static BasicCookieStore mCookieStore = new BasicCookieStore();

	private HttpRequestHandler() {
		if (client == null) {
			client = AndroidHttpClient.newInstance("Android");
			mLocalContext.setAttribute(ClientContext.COOKIE_STORE, mCookieStore);
			client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		}
	}
	
	public static HttpRequestHandler getInstance() {
		if (_hrh == null) {
			_hrh = new HttpRequestHandler();
		} 
		return _hrh;
	}
	
	public void finalize() {
		// don't leak;
		client.close();
	}
	
	
	public static boolean isNetworkAvailable(Context ctx) {
		// Requires NETWORK_STATE permissions
	    ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
	    // if no network is available networkInfo will be null
	    // otherwise check if we are connected
	    if (networkInfo != null && networkInfo.isConnected()) {
	        return true;
	    }
	    return false;
	}
	
	public CookieStore getCookieStore() {
		return mCookieStore;
	}

	public String sendRequest(ArrayList<NameValuePair> params, String url, String service) {
		String response = null;
		try {
			URI uri = new URI(url + "/" + service);
			Log.d(TAG, uri.toASCIIString());
			HttpPost post = new HttpPost(uri);
			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			for(NameValuePair b:params){
				Log.d(TAG, b.getName() + ": " + b.getValue());
			}
			HttpResponse httpResponse = client.execute(post, mLocalContext);
			if (HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode()) {
				HttpEntity entity = httpResponse.getEntity();
				if (entity != null) {
					response = EntityUtils.toString(entity, "UTF-8");
					if (response != null && response.trim().length() > 0) {
						Log.d(TAG, response);
					} else {
						Log.d(TAG, httpResponse.getStatusLine().getStatusCode() + "-" + httpResponse.getStatusLine().getReasonPhrase());
						Log.d(TAG, "No response received");
					}
				}
			} else {
				Log.d(TAG, httpResponse.getStatusLine().getStatusCode() + "-" + httpResponse.getStatusLine().getReasonPhrase());
			}
		} catch (ConnectException e) {
			e.printStackTrace();
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		} catch (ConnectTimeoutException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}
}
