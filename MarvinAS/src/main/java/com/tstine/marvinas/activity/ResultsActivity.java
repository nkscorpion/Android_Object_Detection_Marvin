package com.tstine.marvinas.activity;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.tstine.marvinas.bimap.BitmapWorker;
import com.tstine.marvinas.util.Const;
import com.tstine.marvinas.fragment.ProcessListFragment;
import com.tstine.marvinas.R;
import com.tstine.marvinas.aws.Request;
import com.tstine.marvinas.util.Log;

public class ResultsActivity extends ActionBarActivity{

    private boolean mSaveState = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_list);
        getSupportActionBar().hide();
        if(!CameraActivity.testConnection(this)){
            finish();
            Toast.makeText(this, "Sorry no connectivity", Toast.LENGTH_SHORT);
        }
        ListFragment frag = ProcessListFragment.getInstance();
        Request request = (Request)getIntent().getSerializableExtra(Const.REQUEST_EXTRA_KEY);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Const.REQUEST_EXTRA_KEY, request );
        frag.setArguments(bundle);
        if(request == null ){
            mSaveState = false;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, frag)
            .commit();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.process_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id ){
            case R.id.action_settings:
                return true;
            case R.id.action_pause:
                BitmapWorker.setPaused(!BitmapWorker.getPaused());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop(){
        super.onStop();
        if(mSaveState){
            ProcessListFragment.getInstance().saveState();
            Log.d("State saved");
        }
    }

}
