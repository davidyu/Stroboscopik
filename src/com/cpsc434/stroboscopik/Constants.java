package com.cpsc434.stroboscopik;

public class Constants {
	
	//Sender ID of Project. Required when initially registering with GCM
	static final String APP_SENDER_ID = "457186895027";
	
	//URL(s) of Server for GCM
	static final String APP_REG_URL = 	"http://mholkesvik.webfactional.com/register/";
	static final String APP_UNREG_URL = "http://mholkesvik.webfactional.com/unregister/";
	
	//Fields for Server Requests
	static final String APP_DATABASE_ID = 	"database_id";
	static final String APP_REG_ID = 		"reg_id";
	static final String APP_CLUSTER_ID = 	"cluster_id";
	
	//SharedPreferences that store the App's settings
	static final String APP_SETTINGS =  		"com.cpsc434.stroboscopik.settings";
	static final String APP_GCM_REGID_KEY = 	"GCM_REGID";
	static final String APP_GCM_CLUSTER_KEY = 	"GCM_CLUSTER";

	//Other important Constants
	static final String APP_NO_CLUSTER =  "NONE";
	static final String APP_NEW_CLUSTER = "NEW";
}