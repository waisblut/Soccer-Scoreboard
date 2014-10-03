package com.waisblut.soccerscoreboard.views;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import com.waisblut.soccerscoreboard.R;


public class Main
        extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null)
        {
            FragmentMain fragmentMain = new FragmentMain();

            getFragmentManager().beginTransaction().add(R.id.container, fragmentMain).commit();
        }

        this.getWindow().setBackgroundDrawable(null);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //    @Override
    //    public boolean onOptionsItemSelected(MenuItem item)
    //    {
    //        // Handle action bar item clicks here. The action bar will
    //        // automatically handle clicks on the Home/Up button, so long
    //        // as you specify a parent activity in AndroidManifest.xml.
    //        int id = item.getItemId();
    //        if (id == R.id.action_settings)
    //        {
    //            return true;
    //        }
    //        return super.onOptionsItemSelected(item);
    //    }
}
