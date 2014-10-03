package com.waisblut.soccerscoreboard.services;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.waisblut.soccerscoreboard.Logger;
import com.waisblut.soccerscoreboard.views.Main_wear;

import java.util.List;

import static com.google.android.gms.wearable.PutDataRequest.WEAR_URI_SCHEME;

public class NotificationService
        extends WearableListenerService
{

    public static final String ACTION_RESP = "com.waisblut.soccerscoreboard.intent.action.MESSAGE_PROCESSED";
    //private int notificationId = 001;
    private Intent mBroadcastIntent;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mBroadcastIntent = new Intent();
        mBroadcastIntent.setAction(ACTION_RESP);
        mBroadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (null != intent)
        {
            String action = intent.getAction();
            if (Logger.ACTION_DISMISS.equals(action))
            {
                dismissNotification();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents)
    {
        for (DataEvent dataEvent : dataEvents)
        {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED)
            {
                if (Logger.NOTIFICATION_PATH.equals(dataEvent.getDataItem().getUri().getPath()))
                {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());
                    //                    String title = dataMapItem.getDataMap().getString(Logger.NOTIFICATION_TITLE);
                    //                    String content = dataMapItem.getDataMap()
                    //                                                .getString(Logger.NOTIFICATION_CONTENT);

                    int scoreA = dataMapItem.getDataMap().getInt(Logger.TEAM_A_SCORE);
                    int scoreB = dataMapItem.getDataMap().getInt(Logger.TEAM_B_SCORE);

                    if (isActivityOnTop())
                    {
                        sendMessage(scoreA, scoreB);
                    }
                    else
                    {
                        openApplication(scoreA, scoreB);
                    }

                    Logger.log('d', "RECEIVING NOTIFICATION.....");
                }
            }
        }
    }

    private boolean isActivityOnTop()
    {
        List<ActivityManager.RunningTaskInfo> lst = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE))
                .getRunningTasks(1);

        return lst.get(0).topActivity.getPackageName().equalsIgnoreCase(getPackageName());
    }

    /*
    Serve para abrir a aplicação através do Smartphone
     */
    private void openApplication(int scoreA, int scoreB)
    {
        Logger.log('d', "Opening Application");

        Intent viewIntent = new Intent(this, Main_wear.class);
        Bundle extras = new Bundle();

        extras.putInt(Logger.TEAM_A_SCORE, scoreA);
        extras.putInt(Logger.TEAM_B_SCORE, scoreB);

        viewIntent.putExtras(extras);
        viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(viewIntent);
    }

    private void sendMessage(int scoreA, int scoreB)
    {
        mBroadcastIntent.putExtra(Logger.TEAM_A_SCORE, scoreA);
        mBroadcastIntent.putExtra(Logger.TEAM_B_SCORE, scoreB);
        mBroadcastIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendBroadcast(mBroadcastIntent);
    }

    private void dismissNotification()
    {
        new DismissNotificationCommand(this).execute();
    }


    private class DismissNotificationCommand
            implements GoogleApiClient.ConnectionCallbacks,
                       ResultCallback<DataApi.DeleteDataItemsResult>,
                       GoogleApiClient.OnConnectionFailedListener
    {

        private static final String TAG = "DismissNotification";

        private final GoogleApiClient mGoogleApiClient;

        public DismissNotificationCommand(Context context)
        {
            mGoogleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API)
                                                                   .addConnectionCallbacks(this)
                                                                   .addOnConnectionFailedListener(
                                                                           this)
                                                                   .build();
        }

        public void execute()
        {
            mGoogleApiClient.connect();
        }

        @Override
        public void onConnected(Bundle bundle)
        {
            final Uri dataItemUri = new Uri.Builder().scheme(WEAR_URI_SCHEME)
                                                     .path(Logger.NOTIFICATION_PATH)
                                                     .build();
            if (Log.isLoggable(TAG, Log.DEBUG))
            {
                Log.d(TAG, "Deleting Uri: " + dataItemUri.toString());
            }
            Wearable.DataApi.deleteDataItems(mGoogleApiClient, dataItemUri).setResultCallback(this);
        }

        @Override
        public void onConnectionSuspended(int i)
        {
            Log.d(TAG, "onConnectionSuspended");
        }

        @Override
        public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult)
        {
            if (!deleteDataItemsResult.getStatus().isSuccess())
            {
                Log.e(TAG, "dismissWearableNotification(): failed to delete DataItem");
            }
            mGoogleApiClient.disconnect();
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult)
        {
            Log.d(TAG, "onConnectionFailed");
        }
    }
}
