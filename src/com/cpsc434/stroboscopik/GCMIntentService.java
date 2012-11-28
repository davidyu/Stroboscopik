package com.cpsc434.stroboscopik;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {

	//TAG for Local Log calls
	private static final String TAG = "GCMIntentService";
	
	//Public constructor for initializing the Intent Service
	public GCMIntentService() {
		super(Constants.APP_SENDER_ID);
	}
	
	protected void onRegistered(Context context, String registrationId) {
		SharedPreferences settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
		String regId = GCMRegistrar.getRegistrationId(getApplicationContext());
		String oldId = settings.getString(Constants.APP_GCM_REGID_KEY, regId);
		String cluster = settings.getString(Constants.APP_GCM_CLUSTER_KEY, Constants.APP_NO_CLUSTER);
		
		//Perform Registration and Unregistration
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(Constants.APP_REG_URL);
		
		try {
			//Add Data to the Request object
			List<NameValuePair> data = new ArrayList<NameValuePair>();
			data.add(new BasicNameValuePair(Constants.APP_DATABASE_ID, oldId));
			data.add(new BasicNameValuePair(Constants.APP_REG_ID, regId));
			data.add(new BasicNameValuePair(Constants.APP_CLUSTER_ID, cluster));
			httppost.setEntity(new UrlEncodedFormEntity(data));
			
			//Execute the request
			HttpResponse response = httpclient.execute(httppost);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new IOException(response.getStatusLine().getReasonPhrase());
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		
		//Commit new settings to memory
		SharedPreferences.Editor ed = settings.edit();
		ed.putString(Constants.APP_GCM_REGID_KEY, regId);
		ed.commit();
	}
	
	protected void onError(Context context, String errorId) {
		//TODO: Error of server not available already dealt with. Need to deal with other errors
	}
	
	protected void onUnregistered(Context context, String errorId) {
		SharedPreferences settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
		String oldId = settings.getString(Constants.APP_GCM_REGID_KEY, "");

		//Notify the server and dissociate from the cluster
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(Constants.APP_UNREG_URL);
		
		try {
			//Add Data to the Request object
			List<NameValuePair> data = new ArrayList<NameValuePair>();
			data.add(new BasicNameValuePair(Constants.APP_DATABASE_ID, oldId));
			httppost.setEntity(new UrlEncodedFormEntity(data));
			
			//Execute the request
			HttpResponse response = httpclient.execute(httppost);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new IOException(response.getStatusLine().getReasonPhrase());
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		
		SharedPreferences.Editor ed = settings.edit();
		ed.putString(Constants.APP_GCM_REGID_KEY, "");
		ed.commit();
	}
	
	protected void onMessage(Context context, Intent intent) {
		//TODO: Server has just sent a message to the cluster. Deal with it
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Binding not allowed");
	}
}
