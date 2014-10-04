package com.waisblut.soccerscoreboard.views;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.waisblut.soccerscoreboard.Logger;
import com.waisblut.soccerscoreboard.R;
import com.waisblut.soccerscoreboard.services.NotificationService;

public class Main_wear
        extends Activity
        implements View.OnClickListener
{

    //region Variables...
    private GoogleApiClient mGoogleApiClient;
    private RelativeLayout mRlA, mRlB;
    private TextView mTxtNameA, mTxtNameB;
    private TextView mTxtScoreA, mTxtScoreB;
    private int mCounterA = 0, mCounterB = 0;
    private SharedPreferences mSp;
    private ResponseReceiver mReceiver;
    //endregion


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.rect_activity_main_wear);
        getWindow().setBackgroundDrawable(null);

        mTxtNameA = (TextView) findViewById(R.id.txtTeam_A);
        mTxtNameB = (TextView) findViewById(R.id.txtTeam_B);
        mTxtScoreA = (TextView) findViewById(R.id.txtScore_A);
        mTxtScoreB = (TextView) findViewById(R.id.txtScore_B);
        mRlA = (RelativeLayout) findViewById(R.id.lay_A);
        mRlB = (RelativeLayout) findViewById(R.id.lay_B);
        Button btnUndoA = (Button) findViewById(R.id.btnUndo_A);
        Button btnUndoB = (Button) findViewById(R.id.btnUndo_B);
        Button btnReset = (Button) findViewById(R.id.btnReset);

        //region LongClick....
        View.OnLongClickListener myLongClick = new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                switch (v.getId())
                {
                case R.id.btnUndo_A:
                    mCounterA--;
                    mCounterA = changeScore(mCounterA, 'A');
                    break;

                case R.id.btnUndo_B:
                    mCounterB--;
                    mCounterB = changeScore(mCounterB, 'B');
                    break;

                case R.id.btnReset:
                    resetCounters();
                    break;
                }

                return true;
            }
        };
        //endregion

        mRlA.setOnClickListener(this);
        mRlB.setOnClickListener(this);
        mTxtScoreA.setOnClickListener(this);
        mTxtScoreB.setOnClickListener(this);
        btnUndoA.setOnClickListener(this);
        btnUndoB.setOnClickListener(this);
        btnReset.setOnClickListener(this);

        btnUndoA.setOnLongClickListener(myLongClick);
        btnUndoB.setOnLongClickListener(myLongClick);
        btnReset.setOnLongClickListener(myLongClick);

        mTxtScoreA.setText(String.valueOf(mCounterA));
        mTxtScoreB.setText(String.valueOf(mCounterB));

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
        {
            @Override
            public void onConnected(Bundle connectionHint)
            {
                Logger.log('d', "WEAR: onConnected: " + connectionHint);
            }

            @Override
            public void onConnectionSuspended(int cause)
            {
                Logger.log('d', "WEAR: onConnectionSuspended: " + cause);
            }
        }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener()
        {
            @Override
            public void onConnectionFailed(ConnectionResult result)
            {
                Logger.log('d', "WEAR: onConnectionFailed: " + result);
            }
        }).addApi(Wearable.API).build();

        registerThisReceiver();

        setInitialSettings();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        unregisterThisReceiver();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
        case R.id.txtScore_A:
        case R.id.lay_A:
            mCounterA++;
            changeScore(mCounterA, 'A');
            break;

        case R.id.txtScore_B:
        case R.id.lay_B:
            mCounterB++;
            changeScore(mCounterB, 'B');
            break;

        case R.id.btnUndo_A:
        case R.id.btnUndo_B:
            Toast.makeText(this,
                           getResources().getString(R.string.long_press_undo),
                           Toast.LENGTH_SHORT).show();
            break;

        case R.id.btnReset:
            Toast.makeText(this,
                           getResources().getString(R.string.long_press_reset),
                           Toast.LENGTH_SHORT).show();
            sendNotification();
            break;
        }
    }

    private void setInitialSettings()
    {
        setPreferences();

        mCounterA = getIntent().getIntExtra(Logger.TEAM_A_SCORE, 0);
        mCounterB = getIntent().getIntExtra(Logger.TEAM_B_SCORE, 0);
        changeScore(mCounterA, 'A');
        changeScore(mCounterB, 'B');


        mTxtNameA.setText(mSp.getString(Logger.TEAM_A_NAME,
                                        getResources().getString(R.string.team_A)));
        mTxtNameB.setText(mSp.getString(Logger.TEAM_B_NAME,
                                        getResources().getString(R.string.team_B)));

        setBackground(mRlA, mSp.getInt(Logger.TEAM_A_COLOR,
                                       R.drawable.background_team_divider_red));
        setBackground(mRlB, mSp.getInt(Logger.TEAM_B_COLOR,
                                       R.drawable.background_team_divider_blue));
    }

    private void sendNotification()
    {
        if (mGoogleApiClient.isConnected())
        {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(Logger.NOTIFICATION_PATH);
            // Make sure the data item is unique. Usually, this will not be required, as the payload
            // (in this case the title and the content of the notification) will be different for almost all
            // situations. However, in this example, the text and the content are always the same, so we need
            // to disambiguate the data item by adding a field that contains teh current time in milliseconds.
            dataMapRequest.getDataMap().putDouble(Logger.NOTIFICATION_TIMESTAMP,
                                                  System.currentTimeMillis());
            dataMapRequest.getDataMap().putString(Logger.NOTIFICATION_TITLE, "This is the title");
            dataMapRequest.getDataMap().putString(Logger.NOTIFICATION_CONTENT,
                                                  "This is a notification with some text.");

            dataMapRequest.getDataMap().putInt(Logger.TEAM_A_SCORE, mCounterA);
            dataMapRequest.getDataMap().putInt(Logger.TEAM_B_SCORE, mCounterB);

            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);
            Logger.log('d', "WEAR: SENDING NOTIFICATION.....");
        }
        else
        {
            Logger.log('e', "WEAR: No connection to wearable available!");
        }
    }

    @SuppressWarnings("deprecation")
    private void setBackground(View v, int color)
    {
        if (Build.VERSION.SDK_INT >= 16)
        {
            v.setBackground(getResources().getDrawable(color));
        }
        else
        {
            v.setBackgroundDrawable(getResources().getDrawable(color));
        }

        if (v.getId() == mRlA.getId())
        {
            mSp.edit().putInt(Logger.TEAM_A_COLOR, color).apply();
        }
        else if (v.getId() == mRlB.getId())
        {
            mSp.edit().putInt(Logger.TEAM_B_COLOR, color).apply();
        }
    }

    private int changeScore(int value, char team)
    {
        value = (value < 0) ? 0 : value;

        if (team == 'A')
        {
            mTxtScoreA.setText(String.valueOf(value));
            mSp.edit().putInt(Logger.TEAM_A_SCORE, value).apply();
        }
        else if (team == 'B')
        {
            mTxtScoreB.setText(String.valueOf(value));
            mSp.edit().putInt(Logger.TEAM_B_SCORE, value).apply();
        }

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(50);

        return value;
    }

    private void resetCounters()
    {
        mCounterA = 0;
        mCounterB = 0;

        changeScore(mCounterA, 'A');
        changeScore(mCounterB, 'B');
    }

    private void setPreferences()
    {
        mSp = getPreferences(Context.MODE_PRIVATE);
    }

    private void registerThisReceiver()
    {
        mReceiver = new ResponseReceiver(this);
        IntentFilter filter = new IntentFilter(NotificationService.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        registerReceiver(mReceiver, filter);
    }

    private void unregisterThisReceiver()
    {
        if (mReceiver != null) unregisterReceiver(mReceiver);
    }

    public static class ResponseReceiver
            extends BroadcastReceiver
    {
        private Main_wear myActivity;

        @SuppressWarnings("UnusedDeclaration")
        public ResponseReceiver()
        {

        }

        public ResponseReceiver(Main_wear activity)
        {
            this.myActivity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Logger.log('d', "WEAR: ResponseReceiver....");
            if (intent.getExtras() != null)
            {
                myActivity.mCounterA = intent.getIntExtra(Logger.TEAM_A_SCORE, -1);
                myActivity.mCounterB = intent.getIntExtra(Logger.TEAM_B_SCORE, -1);

                myActivity.changeScore(myActivity.mCounterA, 'A');
                myActivity.changeScore(myActivity.mCounterB, 'B');
            }
        }
    }
}
