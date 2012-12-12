package com.cpsc434.stroboscopik;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.cpsc434.stroboscopik.util.SystemUiHider;
import com.google.android.gcm.GCMRegistrar;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class StrobeActivity extends Activity {
  //TAG for Log messages in this class
  private static final String TAG = "StrobeActivity";

  /**
   * Whether or not the system UI should be auto-hidden after
   * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
   */
  private static final boolean AUTO_HIDE = true;

  /**
   * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
   * user interaction before hiding the system UI.
   */
  private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

  /**
   * If set, will toggle the system UI visibility upon interaction. Otherwise,
   * will show the system UI visibility upon interaction.
   */
  private static final boolean TOGGLE_ON_CLICK = true;

  /**
   * The flags to pass to {@link SystemUiHider#getInstance}.
   */
  private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

  /**
   * The instance of the {@link SystemUiHider} for this activity.
   */
  private static double flashPeriod = 150.0; //the period of time when the screen is "white" or in strobing state
  private static int restPeriod = 300; //the period of time when the screen is "black" or in resting state

  //define resting color
  private static int restR = 0;
  private static int restG = 0;
  private static int restB = 0;

  //define flashing color
  private static int flashR = 255;
  private static int flashG = 0;
  private static int flashB = 146;
  
  private static enum State {
    IDLE,
    ORPHAN,
    SUBNODE,
    SUPERNODE,
    IN_TRANSITION
  };
  
  private static enum Transition {
    TO_SUPERNODE,
    TO_SUBNODE,
    SEARCHING_FOR_SUPERNODE,
    NONE
  };
  
  private static State state = State.IDLE;
  private static Transition trans = Transition.NONE;

  private SystemUiHider mSystemUiHider;
  private Handler mHandler = new Handler(); //for dummy flashing
  private SharedPreferences settings;

  public long flashTimeStamp = 0;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Check for GCM Registration. Register if not registered
    GCMRegistrar.checkDevice(this);
    GCMRegistrar.checkManifest(this);
    settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
    String regId = GCMRegistrar.getRegistrationId(this);
    if (regId == "") {
      GCMRegistrar.register(this, Constants.APP_SENDER_ID);
    } else {
      Log.v(TAG, "Already Registered with ID: "+regId);
    }
    // End GCM Registration

    setContentView(R.layout.activity_strobe);

    final View controlsView = findViewById(R.id.fullscreen_content_controls);
    final View contentView = findViewById(R.id.fullscreen_content);

    //startFlashing();
    mSystemUiHider = SystemUiHider.getInstance(this, contentView,
        HIDER_FLAGS);
    mSystemUiHider.setup();
    mSystemUiHider
    .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
      int mControlsHeight;
      int mShortAnimTime;

      @Override
      @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
      public void onVisibilityChange(boolean visible) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
          if (mControlsHeight == 0) {
            mControlsHeight = controlsView.getHeight();
          }
          if (mShortAnimTime == 0) {
            mShortAnimTime = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
          }
          controlsView
          .animate()
          .translationY(visible ? 0 : mControlsHeight)
          .setDuration(mShortAnimTime);
        } else {
          controlsView.setVisibility(visible ? View.VISIBLE
              : View.GONE);
        }

        if (visible && AUTO_HIDE) {
          delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }
      }
    });

    contentView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (TOGGLE_ON_CLICK) {
          mSystemUiHider.toggle();
        } else {
          mSystemUiHider.show();
        }
      }
    });

    findViewById(R.id.dummy_button).setOnTouchListener(
        mDelayHideTouchListener);

    findViewById(R.id.dummy_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //searchForSupernode();
        initDevice();
      }
    });
  }
 
  private void initDevice() {
    String uid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    long seed = new BigInteger(uid, 16).longValue() + System.currentTimeMillis();
    Random r = new Random(seed);
    int wait = 3000 + (int)(r.nextDouble() * 1300);
    mHandler.postDelayed(coerceBecomeSubnode, wait);
  }
  
  private Runnable onBecomeSupernode = new Runnable(){
    public void run() {
      Toast message = Toast.makeText(getApplicationContext(), "Success! You are now a supernode!", Toast.LENGTH_SHORT);
      message.show();
      
      startFlashing();
    }
  };
  
  private Runnable coerceBecomeSubnode = new Runnable(){
    public void run() {
      Toast message = Toast.makeText(getApplicationContext(), "Success! You are now connected to a cluster!", Toast.LENGTH_SHORT);
      message.show();
      
      startFlashing();
    }
  };
  
  private void searchForSupernode() {
    String uid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    long seed = new BigInteger(uid, 16).longValue() + System.currentTimeMillis();
    Random r = new Random(seed);

    double flip = r.nextDouble();
    int wait = 0;
    
    state = State.IN_TRANSITION;
    trans = Transition.SEARCHING_FOR_SUPERNODE;

    if (flip < 0.7) {
      wait = (int) (r.nextDouble() * Constants.APP_STARTUP_LONG_WAIT);
      Log.d("waitForSupernode", "waiting " + Integer.toString(wait) + " milliseconds.");
      mHandler.postDelayed(evolveIntoSupernode, wait);
    } else {
      wait = (int) (r.nextDouble() * Constants.APP_STARTUP_SHORT_WAIT);
      Log.d("waitForSupernode", "promoted; will become supernode soon.");
      mHandler.postDelayed(evolveIntoSupernode, wait);
    }
  }

  private Runnable evolveIntoSupernode = new Runnable() {
    public void run() {
      if (!(state == State.IN_TRANSITION && trans == Transition.SEARCHING_FOR_SUPERNODE) ) return;
  
      String regId = settings.getString(Constants.APP_GCM_REGID_KEY, ""); //if "" then BAD
      if (regId == "") {
        Log.e("evolveIntoSupernode", "regId null");
        return;
      }
  
      List<NameValuePair> data = new ArrayList<NameValuePair>();
      data.add(new BasicNameValuePair(Constants.APP_DATABASE_ID, regId));
  
      HTTPRequestParams[] params = new HTTPRequestParams[1];
      params[0] = new HTTPRequestParams(Constants.APP_SUPER_URL, data,
          "Success! You are now a supernode.",
          "Failure: cannot contact servers");
  
      PostHTTPTask p = new PostHTTPTask();
      
      trans = Transition.TO_SUPERNODE;
      
      p.execute(params);
    }
  };

  private void startFlashing() {
    mHandler.postDelayed(mFlashTask, 1000);
  }

  private Runnable mFlashTask = new Runnable() { //this is terrible
    public void run() {
      updatePeriods();

      final View contentView = findViewById(R.id.fullscreen_content);
      contentView.setBackgroundColor(Color.argb(255, flashR, flashG, flashB));
      flashTimeStamp = System.currentTimeMillis();

      mHandler.postDelayed(mFadeTask, 10); //ideally these are constants defined elsewhere
      mHandler.postDelayed(this, restPeriod);
    }
  };

  private void updatePeriods() {
    int freq = settings.getInt(Constants.APP_GCM_FREQUENCY_KEY, Constants.APP_DEFAULT_FREQ);
    restPeriod = 1000/freq;

    if ( flashPeriod > restPeriod ) {
      flashPeriod = restPeriod - 100;
    } else {
      flashPeriod = Constants.APP_DEFAULT_FADE;
    }
    //Log.d("UpdatePeriods", "new frequency: " + freq);
  }


  private void onBecomeSubnode() {
    String regId = settings.getString(Constants.APP_GCM_REGID_KEY, ""); //if "" then bad
    if (regId == "") {
      Log.e("onBecomeSubnode", "regId null");
      return;
    }

    String cluster = settings.getString(Constants.APP_CLUSTER_KEY, Constants.APP_NO_CLUSTER);

    //Perform Registration and Unregistration
    List<NameValuePair> data = new ArrayList<NameValuePair>();
    data.add(new BasicNameValuePair(Constants.APP_DATABASE_ID, regId));
    data.add(new BasicNameValuePair(Constants.APP_REG_ID, regId));
    data.add(new BasicNameValuePair(Constants.APP_CLUSTER_ID, "0"));

    HTTPRequestParams[] params = new HTTPRequestParams[1];
    params[0] = new HTTPRequestParams(Constants.APP_REG_URL, data,
        "Success! You are now connected to a cluster.",
        "Failure: cannot contact servers");

    PostHTTPTask p = new PostHTTPTask();
    p.execute(params);
  }
 
  //do any post results cleanup if necessary; also retry if necessary
  private void postResultsCleanup()
  {
    switch (trans) {
    case NONE:
      break;
    case SEARCHING_FOR_SUPERNODE:
      break;
    case TO_SUBNODE:
      state = State.SUBNODE;
      trans = Transition.NONE;
      break;
    case TO_SUPERNODE:
      state = State.SUPERNODE;
      trans = Transition.NONE;
      break;
    default:
      break;
    }
  }

  private class PostHTTPTask extends AsyncTask<HTTPRequestParams, Integer, String> {

    protected void onPostExecute(String result) {
      Toast message = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT);
      message.show();
      postResultsCleanup();
    } // end of onPostExecute

    @Override
    protected String doInBackground(HTTPRequestParams... params) {
      HTTPRequestParams p = params[0];

      String url = p.url;
      List<NameValuePair> data = p.data;

      String success = p.success;
      String failure = p.failure;

      Log.d("PostHTTPTask", "Gathering HTTP POST message");
      
      //debug
      Log.d("PostHTTPTaskData", "what I'm sending follows:");
      Iterator<NameValuePair> i = data.iterator();
      while (i.hasNext()) {
        NameValuePair pair = i.next();
        Log.d("PostHTTPTaskData", pair.getName().toString() + ":" + pair.getValue());
      }
      Log.d("PostHTTPTaskData", "end POST data");
      
      HttpClient httpclient = new DefaultHttpClient();
      HttpPost httppost = new HttpPost(url);

      try {
        Log.d("PostHTTPTask", "posting to " + url + " ...");
        httppost.setEntity(new UrlEncodedFormEntity(data));
        HttpResponse response = httpclient.execute(httppost);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
          Log.e("postHTTPRequest", "HTTP Response NOT OK " + Integer.toString(response.getStatusLine().getStatusCode()) +": " + response.getStatusLine().getReasonPhrase());
          return failure;
        }

      } catch (ClientProtocolException e) {
        Log.e(TAG, e.getMessage());
        return failure;
      } catch (IOException e) {
        Log.e(TAG, e.getMessage());
        return failure;
      }

      return success;
    }
  }


  private Runnable mFadeTask = new Runnable() { //make this faster
    public void run() {
      final View contentView = findViewById(R.id.fullscreen_content);
      long elapsed = System.currentTimeMillis() - flashTimeStamp;

      //interpolate
      double frac = (double) elapsed/flashPeriod;
      int r = (int) ((1 - frac) * flashR) + (int) (frac * restR);
      int g = (int) ((1 - frac) * flashG) + (int) (frac * restG);
      int b = (int) ((1 - frac) * flashB) + (int) (frac * restB);

      //System.out.println(r + ", " + g + ", " + b);

      //normalize
      r = r < 0 ? 0 : r;
      g = g < 0 ? 0 : g;
      b = b < 0 ? 0 : b;

      contentView.setBackgroundColor(Color.argb(255, r, g, b));

      if (r != restR || g != restG || b != restB ) {
        mHandler.postDelayed(this, 5);
      }
    }
  };


  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    delayedHide(100);
  }

  /**
   * Touch listener to use for in-layout UI controls to delay hiding the
   * system UI. This is to prevent the jarring behavior of controls going away
   * while interacting with activity UI.
   */
  View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
      if (AUTO_HIDE) {
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
      }
      return false;
    }
  };

  Handler mHideHandler = new Handler();
  Runnable mHideRunnable = new Runnable() {
    @Override
    public void run() {
      mSystemUiHider.hide();
    }
  };

  /**
   * Schedules a call to hide() in [delay] milliseconds, canceling any
   * previously scheduled calls.
   */
  private void delayedHide(int delayMillis) {
    mHideHandler.removeCallbacks(mHideRunnable);
    mHideHandler.postDelayed(mHideRunnable, delayMillis);
  }
}
