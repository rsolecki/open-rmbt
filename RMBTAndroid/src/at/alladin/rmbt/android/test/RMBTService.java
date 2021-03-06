/*******************************************************************************
 * Copyright 2013 alladin-IT OG
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.alladin.rmbt.android.test;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import at.alladin.openrmbt.android.R;
import at.alladin.rmbt.android.main.RMBTMainActivity;
import at.alladin.rmbt.android.test.RMBTTask.EndTaskListener;
import at.alladin.rmbt.android.util.ConfigHelper;
import at.alladin.rmbt.client.helper.IntermediateResult;
import at.alladin.rmbt.client.helper.NdtStatus;

public class RMBTService extends Service implements EndTaskListener
{
    
    private RMBTTask testTask;
    // private InformationCollector fullInfo;
    
    private Handler handler;
    
    private static final String TAG = "RMBTService";
    
    private static WifiManager wifiManager;
    private static WifiLock wifiLock;
    private static WakeLock wakeLock;
    
    private static long DEADMAN_TIME = 120 * 1000;
    private final Runnable deadman = new Runnable()
    {
        @Override
        public void run()
        {
            stopTest();
        }
    };
    
    // private BroadcastReceiver mNetworkStateIntentReceiver;
    
    // This is the object that receives interactions from clients. See
    // RemoteService for a more complete example.
    private final IBinder localRMBTBinder = new RMBTBinder();
    private boolean completed = false;
    
    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class RMBTBinder extends Binder
    {
        public RMBTService getService()
        {
            // Return this instance of RMBTService so clients can call public
            // methods
            return RMBTService.this;
        }
    }
    
    @Override
    public void onCreate()
    {
        Log.d(TAG, "created");
        super.onCreate();
        
        handler = new Handler();
        // initialise the locks
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "RMBTWifiLock");
        wakeLock = ((PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE)).newWakeLock(
                PowerManager.FULL_WAKE_LOCK, "RMBTWakeLock");
        
        // mNetworkStateIntentReceiver = new BroadcastReceiver() {
        // @Override
        // public void onReceive(Context context, Intent intent) {
        // if
        // (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
        // {
        //
        // final boolean connected = !
        // intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY,
        // false);
        // if (! connected)
        // stopTest();
        // }
        // }
        // };
        // final IntentFilter networkStateChangedFilter = new IntentFilter();
        // networkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        //
        // registerReceiver(mNetworkStateIntentReceiver,
        // networkStateChangedFilter);
    }
    
    @Override
    public void onDestroy()
    {
        Log.d(TAG, "destroyed");
        super.onDestroy();
        removeNotification();
        unlock(true, true);
        if (testTask != null)
        {
            testTask.cancel();
            testTask = null;
        }
        
        handler.removeCallbacks(addNotificationRunnable);
        handler.removeCallbacks(unlockRunnable);
        handler.removeCallbacks(deadman);
        
        // unregisterReceiver(mNetworkStateIntentReceiver);
    }
    
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId)
    {
        if (testTask != null && testTask.isRunning())
            testTask.cancel();
        
        completed = false;
        
        // lock wifi + power
        lock();
        testTask = new RMBTTask(getApplicationContext());
        
        testTask.setEndTaskListener(this);
        testTask.execute(handler);
        Log.d(TAG, "RMBTTest started");
        
        if (!ConfigHelper.isRepeatTest(getApplicationContext()))
            handler.postDelayed(deadman, DEADMAN_TIME);
        
        return START_NOT_STICKY;
    }
    
    public void stopTest()
    {
        
        if (testTask != null)
        {
            Log.d(TAG, "RMBTTest stopped");
            testTask.cancel();
            taskEnded();
        }
    }
    
    public boolean isTestRunning()
    {
        return testTask != null && testTask.isRunning();
    }
    
    private void addNotificationIfTestRunning()
    {
        if (isTestRunning())
        {
            final Resources res = getResources();
            final Notification notification = new Notification(R.drawable.stat_icon_test,
                    res.getText(R.string.test_notification_ticker), System.currentTimeMillis());
            final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(
                    getApplicationContext(), RMBTMainActivity.class), 0);
            notification.setLatestEventInfo(getApplicationContext(), res.getText(R.string.test_notification_title),
                    res.getText(R.string.test_notification_text), contentIntent);
            startForeground(1, notification);
        }
    }
    
    private void removeNotification()
    {
        handler.removeCallbacks(addNotificationRunnable);
        stopForeground(true);
    }
    
    @Override
    public IBinder onBind(final Intent intent)
    {
        return localRMBTBinder;
    }
    
    private final Runnable addNotificationRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            addNotificationIfTestRunning();
        }
    };
    
    @Override
    public boolean onUnbind(final Intent intent)
    {
        handler.postDelayed(addNotificationRunnable, 200);
        return true;
    }
    
    @Override
    public void onRebind(final Intent intent)
    {
        removeNotification();
    }
    
    public IntermediateResult getIntermediateResult(final IntermediateResult result)
    {
        if (testTask != null)
            return testTask.getIntermediateResult(result);
        else
            return null;
    }
    
    public boolean isConnectionError()
    {
        if (testTask != null)
            return testTask.isConnectionError();
        else
            return false;
    }
    
    public Integer getSignal()
    {
        if (testTask != null)
            return testTask.getSignal();
        else
            return null;
    }
    
    public String getOperatorName()
    {
        if (testTask != null)
            return testTask.getOperatorName();
        else
            return null;
    }
    
    public int getNetworkType()
    {
        if (testTask != null)
            return testTask.getNetworkType();
        else
            return 0;
    }
    
    public Location getLocation()
    {
        if (testTask != null)
            return testTask.getLocation();
        else
            return null;
    }
    
    public String getServerName()
    {
        if (testTask != null)
            return testTask.getServerName();
        else
            return null;
    }
    
    // protected Status getStatus()
    // {
    // if (testTask != null)
    // return testTask.getStatus();
    // else
    // return null;
    // }
    
    public String getIP()
    {
        if (testTask != null)
            return testTask.getIP();
        else
            return null;
    }
    
    public String getTestUuid()
    {
        if (testTask != null)
            return testTask.getTestUuid();
        else
            return null;
    }
    
    public float getNDTProgress()
    {
        if (testTask != null)
            return testTask.getNDTProgress();
        else
            return 0;
    }
    
    public NdtStatus getNdtStatus()
    {
        if (testTask != null)
            return testTask.getNdtStatus();
        else
            return null;
    }
    
    public void lock()
    {
        try
        {
            handler.removeCallbacks(unlockRunnable);
            if (!wakeLock.isHeld())
                wakeLock.acquire();
            if (!wifiLock.isHeld())
                wifiLock.acquire();
            Log.d(TAG, "Lock");
        }
        catch (final Exception e)
        {
            Log.e(TAG, "Error getting Lock: " + e.getMessage());
        }
    }
    
    public static void unlock(final boolean wake, final boolean wifi)
    {
        if (wake && wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
        if (wifi && wifiLock != null && wifiLock.isHeld())
            wifiLock.release();
        Log.d(TAG, "Unlock");
    }
    
    private final Runnable unlockRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            unlock(true, false);
            stopSelf();
        };
    };
    
    @Override
    public void taskEnded()
    {
        unlock(false, true);
        handler.postDelayed(unlockRunnable, 15000);
        removeNotification();
        handler.removeCallbacks(deadman);
        completed = true;
    }
    
    public boolean isCompleted()
    {
        return completed;
    }
    
    public void letNDTStart()
    {
        if (testTask != null)
            testTask.letNDTStart();
    }
}
