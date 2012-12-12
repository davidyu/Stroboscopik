package com.cpsc434.stroboscopik;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.widget.Toast;

@TargetApi(11)
@SuppressLint("NewApi")
public class BTsetup extends Activity {
	SharedPreferences BTPrefs;
	String data_strings = "com.cpsc434.stroboscopik";
	public static String EXTRA_DEVICE_ADDRESS = "device_address";
	private ArrayList <String> devices_disc;
	private long time_stamp;
	private static final String TAG = "BTSetup";
    private static final boolean D = true;
    private int cluster_id_length;
    private int flag = 0;
    private long freq;
    private Handler mHandler = new Handler();
    
    
 // Member fields
    private BluetoothAdapter mAdapter;
    private int mState;
    
    // Constants that indicate the current connection state
    public static final int STATE_DISC = 0;       // Discoverability enabled
    public static final int STATE_NoDISC = 1;     // Discoverability disabled
    public static final int STATE_ClusterSetUp = 3;  // Cluster ID obtained
    public static final int STATE_Synchrony = 4;  // Cluster ID obtained
    public static final long DEF_FREQ = 10;
    
    //Intent codes
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_SETUP = 2;
    public static final int REQUEST_TIMESTAMP = 3;
    
   

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_btsetup);
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        BTPrefs = getSharedPreferences(data_strings,0);
        SharedPreferences.Editor editor = BTPrefs.edit();
    	String Adname = mAdapter.getName();
    	editor.putString("OrigAdapName",Adname);
    	editor.commit();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        if (!mAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }else {
        	ensureDiscoverable();
        	SetupCluster();
        }
       
    }
    
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
        if (mState!=STATE_DISC){
        	ensureDiscoverable();
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
        mHandler.removeCallbacks(mUpdateTimeTask);
        
        BTPrefs = getSharedPreferences(data_strings,0);
        String Adname = BTPrefs.getString("OrigAdapName","MyBT");
    	boolean b=mAdapter.setName(Adname);
    	if(b){
    		Intent discoverableintent = new
    		Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    		discoverableintent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
    		startActivity(discoverableintent);
    		mState=STATE_NoDISC;
    	}
    	
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
        mHandler.removeCallbacks(mUpdateTimeTask);
        
        BTPrefs = getSharedPreferences(data_strings,0);
        String Adname = BTPrefs.getString("OrigAdapName",null);
    	boolean b=mAdapter.setName(Adname);
    	if(b){
    		Intent discoverableintent = new
    		Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    		discoverableintent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
    		startActivity(discoverableintent);
    		mState=STATE_NoDISC;
    	}
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    private void SetupCluster() {
    	
    	Intent c_id_Intent = new Intent(this, Setup_list.class);
        startActivityForResult(c_id_Intent, REQUEST_SETUP);
    }
    
  private void formCluster() {
	  BTPrefs = getSharedPreferences(data_strings,0);
      String Adname = BTPrefs.getString("C_ID",null);
      cluster_id_length = (int) BTPrefs.getLong("C_ID_length",3);
      freq = BTPrefs.getLong("freq",DEF_FREQ);
  	  boolean b=mAdapter.setName(Adname);
  	  if(b&&Adname!=null){
      Intent c_id_Intent = new Intent(this, DeviceList.class);
      startActivityForResult(c_id_Intent, REQUEST_TIMESTAMP);
      }else{
    	  SetupCluster();
      }
    }
  
  private void Synchrony(Intent data) {
  	    String Adname = BTPrefs.getString("C_ID",null) + "System.currentTimeMillis()";
  	    boolean b=mAdapter.setName(Adname);
  	    devices_disc = data.getStringArrayListExtra (EXTRA_DEVICE_ADDRESS);
  	    if(b&&devices_disc.size()!=0){
  	    int i=devices_disc.size();
  	    long sum=0;
  	    for(int j=0;j<i;j++){
  	    	String adapName = devices_disc.get(j);
  	    	String time = adapName.substring(cluster_id_length,adapName.length());
  	    	sum = sum + Long.valueOf(time).longValue(); 
  	    	
  	    }
  	    time_stamp = sum;
  	    /*
  	     * Intent SyncIntent = new Intent(for sending to flashing algo);
                
           SyncIntent.putLong(avg_time, time_stamp);
           startActivity(intent);*/
  	    
  	    if(flag ==0){
  	    	mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, (int)5000/freq);
            flag = 1;
  	    }
  	     
  	  
  	    } else{
  	    	formCluster();
  	    }
  }
  
  private Runnable mUpdateTimeTask = new Runnable() {
      public void run() {
      	try {
      		formCluster();
      		flag=0;
			} catch (Exception e) {
			}
      	
          mHandler.postDelayed(this, 10000);
      }
  };
    
    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(discoverableIntent);
            mState=STATE_DISC;
        }
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        if(requestCode ==REQUEST_TIMESTAMP){
        	if (resultCode == Activity.RESULT_OK) {
        		mState=STATE_Synchrony;
                Synchrony(data);
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "User Backout, contact server");
                formCluster();
            }
        }
        if(requestCode ==REQUEST_SETUP){
        	if (resultCode == Activity.RESULT_OK) {
        		mState=STATE_ClusterSetUp;
                formCluster();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "cluster_ID not assigned, contact server");
                //function to contact server;
                formCluster();
            }
        }
        else if(requestCode ==REQUEST_ENABLE_BT){
            // When the request to enable Bluetooth returns
        	 
        	if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled,ensure discoverability
            	ensureDiscoverable();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
   
}
