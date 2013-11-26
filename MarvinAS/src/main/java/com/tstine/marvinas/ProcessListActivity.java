package com.tstine.marvinas;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ProcessListActivity extends ActionBarActivity implements ActionBar.OnNavigationListener {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    private List<String> mData = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_list);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
            // Specify a SpinnerAdapter to populate the dropdown list.
            new ArrayAdapter<String>(
                actionBar.getThemedContext(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                new String[] {
                    getString(R.string.title_section1),
                }),
            this);
        new Thread( new Runnable() {
            public void run(){
                AmazonDynamoDBClient dbClient =
                    new AmazonDynamoDBClient(
                        new BasicAWSCredentials( Const.ACCESS_KEY, Const.SECRET_KEY ) );
                dbClient.setRegion( Region.getRegion(Regions.US_WEST_2) );
                List<String> tableNames = dbClient.listTables().getTableNames();

                Condition rangeKeyCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.GT.toString())
                    .withAttributeValueList(new AttributeValue().withN("0"));
                DynamoEntry entryKey = new DynamoEntry();
                entryKey.setUserId(Installation.getId());
                DynamoDBMapper mapper = new DynamoDBMapper(dbClient);
                 DynamoDBQueryExpression<DynamoEntry> expression = new
                     DynamoDBQueryExpression<DynamoEntry>()
                    .withHashKeyValues(entryKey)
                    .withRangeKeyCondition(Const.D_TIMESTAMP_ATTRIBUTE, rangeKeyCondition);

                List<DynamoEntry> list = mapper.query(DynamoEntry.class, expression);
            }
        }).start();

        //mData.add((Request) getIntent().getSerializableExtra(Const.REQUEST_EXTRA_KEY));
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getSupportActionBar().setSelectedNavigationItem(
                savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
            getSupportActionBar().getSelectedNavigationIndex());
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
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        mData = new ArrayList<String>(Arrays.asList(Const.imageThumbUrls));
        ListFragment frag = ProcessListFragment.newInstance(position+1, mData);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, frag )
            .commit();
        return true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class ProcessListFragment extends ListFragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private List<String> mData = null;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static ProcessListFragment newInstance(int sectionNumber, List<String> data) {
            ProcessListFragment fragment = new ProcessListFragment(data);
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public ProcessListFragment(List<String> data ) {
            this.mData = data;
        }

        public void onActivityCreated(Bundle savedInstanceState){
            super.onActivityCreated(savedInstanceState);
            setListAdapter(new MyAdapter(getActivity(), this.mData));
            this.setListShown(true);
            getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    if( scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING)
                        BitmapWorker.setPaused(true);
                    else
                        BitmapWorker.setPaused(false);
                }
                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                }
            });
        }

    }
    public static class MyAdapter extends BaseAdapter{
        private List<String> mData = null;
        private Context mCtx;
        private BitmapWorker mBitmapWorker=null;
        public MyAdapter( Context ctx, List<String> data ){
            super();
            mData = data;
            mCtx = ctx;
            mBitmapWorker = new BitmapWorker(mCtx);
            mBitmapWorker.setLoadingBitmap(R.drawable.loading_drawable);

        }
        @Override
        public int getCount(){return mData.size();}
        @Override
        public String getItem( int position ){return mData.get(position);}

        @Override
        public long getItemId(int position) {
            return 0;
        }
        @Override
        public View getView(int position, View convertView,
                            ViewGroup parent ){
            //if( convertView != null )
              //  return convertView;

            //Request request = getItem(position);

            String imgPath = getItem(position);
            LayoutInflater inflater =
                (LayoutInflater)mCtx.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
            View row = inflater.inflate(R.layout.request_row, null);
            ImageView imageView;
            imageView = (ImageView) row.findViewById( R.id.image);

            int containerHeight = (int)mCtx.getResources().getDimension(R.dimen
                .process_imageview_height);
            int containerWidth = (int) mCtx.getResources().getDimension(R.dimen
                .process_imageview_width);
            mBitmapWorker.setImageSize(containerWidth, containerHeight);
            mBitmapWorker.loadImage(getItem(position), imageView);

            TextView message = (TextView) row.findViewById( R.id.message );
            message.setText( "This was my message ");
            //message.setText( request.getMessage() );
            TextView time = (TextView) row.findViewById(R.id.date);
            //time.setText(request.getTimestamp("EEE MMM F yyyy h:m:s a"));
            time.setText(new SimpleDateFormat("EEE MMM F yyyy h:m:s a").format(new Date()));
            return row;
        }

    }
}
