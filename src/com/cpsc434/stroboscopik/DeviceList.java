package com.cpsc434.stroboscopik;

import java.util.ArrayList;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;


/**
 * It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
@SuppressLint("NewApi")
public class DeviceList extends Activity {
	
	SharedPreferences BTPrefs;
	String data_strings = "com.example.bluetoothcom";
	// Debugging
    private static final String TAG = "DeviceList";
    private static final boolean D = true;
    
 // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayList<String> discovered_devices = new ArrayList<String>();
    private String cluster_id;
    private int c_id_length;
    private long startedAt;
    
 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        startedAt = System.currentTimeMillis();
        if ((System.currentTimeMillis() - startedAt) > 10*1000) {
            outData();
            finish();
          }
        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);
        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

                doDiscovery();
            
                // Register for broadcasts when a device is discovered
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                this.registerReceiver(mReceiver, filter);
                // Register for broadcasts when discovery has finished
                filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                this.registerReceiver(mReceiver, filter);
                BTPrefs = getSharedPreferences(data_strings,0);
                cluster_id = BTPrefs.getString("C_ID","C01");
                c_id_length = (int) BTPrefs.getLong("C_ID_length",3);
               
        }
    
    
        public void outData() {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();
            
            if (discovered_devices.size() != 0) {
            	
            // Create the result Intent and include the timestamp info
            	Intent intent = new Intent();

                // for some reason, I remember a posting saying it's best to create a new
                // object to pass.  I have no idea why..
                ArrayList <String> addyExtras = new ArrayList <String>();

                for (int i = 0; i < discovered_devices.size(); i++)
                    addyExtras.add (discovered_devices.get(i));

                intent.putStringArrayListExtra (EXTRA_DEVICE_ADDRESS, addyExtras);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);}
            else{
            	setResult(Activity.RESULT_CANCELED);
            }
        }
    
    

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }
    
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery() - deviceList");

        
        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }
    
 // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                	String dev_name = device.getName();
                    if(dev_name.substring(0, c_id_length) == cluster_id){
                	discovered_devices.add(device.getName());}
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                outData();
            }
        }
    };

}