package com.tstine.marvinas;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ProcessListActivity extends ActionBarActivity implements ActionBar.OnNavigationListener {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    private PaginatedQueryList<DynamoEntry> mQueryResults = null;
    private DynamoEntry mJustAdded = null;


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


        Intent intent = getIntent();
        Request request = (Request)intent.getSerializableExtra(Const.REQUEST_EXTRA_KEY);
        mQueryResults = AWSWorker.queryDynamo(request);

        mJustAdded = new DynamoEntry();
        mJustAdded.setImageUrl(request.getImagePath());
        mJustAdded.setUserInputMessage(request.getMessage());
        mJustAdded.setTimestamp(Long.parseLong(request.getTimestamp()));
        mJustAdded.setImageFile(request.getImageName());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onStart(){
        super.onStart();
        if( !getActionBar().isShowing())
            getActionBar().show();
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
        ListFragment frag = ProcessListFragment.newInstance(position+1, mQueryResults, mJustAdded);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, frag)
            .commit();
        return true;
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class ProcessListFragment extends ListFragment implements AdapterView
        .OnItemClickListener, AdapterView.OnItemLongClickListener, AbsListView.OnScrollListener
    {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private PaginatedQueryList<DynamoEntry> mData = null;
        private DynamoEntry mJustAdded = null;
        private BaseAdapter mAdapter;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static ProcessListFragment newInstance(int sectionNumber, PaginatedQueryList<DynamoEntry> data, DynamoEntry justAdded
                                                      ) {
            ProcessListFragment fragment = new ProcessListFragment(data, justAdded);
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public ProcessListFragment(PaginatedQueryList<DynamoEntry> data, DynamoEntry justAdded ) {
            this.mData = data;
            this.mJustAdded = justAdded;
        }


        public void onActivityCreated(Bundle savedInstanceState){
            super.onActivityCreated(savedInstanceState);
            mAdapter = new MyAdapter(getActivity(), this.mData, this.mJustAdded, this);
            setListAdapter( mAdapter );
            this.setListShown(true);
            getListView().setOnItemClickListener(this);
            getListView().setOnItemLongClickListener(this);
            getListView().setOnScrollListener(this);
        }

        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id){
            ViewHolder viewHolder = (ViewHolder)view.getTag();
            if(position>1){
                viewHolder.entry.setImageUrl(AWSWorker.getPresignedUrl(viewHolder.entry.getImageFile()));
            }
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            DetectionResultsFragment newFragment = new DetectionResultsFragment(viewHolder.entry);
            transaction.replace(R.id.container, newFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }

        public boolean onItemLongClick(AdapterView<?> parent, View view,
                                    int position, long id ){
            return false;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if( scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING){
                BitmapWorker.setPaused(true);
            }
            else
                BitmapWorker.setPaused(false);
        }
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        }

        public void notifyDatasetChanged(){
            mAdapter.notifyDataSetChanged();
        }

    }
    public static class MyAdapter extends BaseAdapter{
        private PaginatedQueryList<DynamoEntry> mData = null;
        private Context mCtx;
        private static BitmapWorker sBitmapWorker =null;
        private DynamoEntry mJustAdded = null;
        private ProcessListActivity.ProcessListFragment mListFragment = null;

        public MyAdapter( Context ctx, PaginatedQueryList<DynamoEntry> data, DynamoEntry justAdded,
                          ProcessListFragment parent){
            super();
            mData = data;
            mJustAdded = justAdded;
            mCtx = ctx;
            mListFragment = parent;
            if( sBitmapWorker == null ){
                sBitmapWorker = new BitmapWorker(mCtx);
                sBitmapWorker.setLoadingBitmap(R.drawable.loading_drawable);
                final int maxMemoryKB = (int)(Runtime.getRuntime().maxMemory() / 1024);
                final int totalMemoryKB = (int)(Runtime.getRuntime().totalMemory() /1024);
                final int freeMemoryKB = (int)(Runtime.getRuntime().freeMemory() / 1024);
                final int memoryCacheSizeKB = (maxMemoryKB- totalMemoryKB)/ 8;
                Log.d("cache size: " + maxMemoryKB + ", " +totalMemoryKB +", " + freeMemoryKB + ", " + memoryCacheSizeKB );
                sBitmapWorker.addMemoryCache(memoryCacheSizeKB);
            }
        }
        @Override
        public int getCount(){return 1;}//return mData.size();}
        @Override
        public DynamoEntry getItem( int position ){
            DynamoEntry value = null;
            if(position == 0 ){
                value=mJustAdded;
            }
            else if(false){
                DynamoEntry entry = mData.get(position--);
                entry.setImageUrl(AWSWorker.getPresignedUrl(entry.getImageFile()));
                value= entry;
            }
            return value;
        }


        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView,
                            ViewGroup parent ){

            View row = convertView;
            ViewHolder viewHolder;
            if( row == null ){
                LayoutInflater inflater =
                    (LayoutInflater)mCtx.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.request_row, null);
                viewHolder = new ViewHolder(row, getItem(position), (int)mCtx.getResources().getDimension(R.dimen.process_imageview_width), (int)mCtx.getResources().getDimension(R.dimen.process_imageview_height), mListFragment);
            }else{
                viewHolder = (ViewHolder) row.getTag();
                viewHolder.entry =getItem(position);
            }
            sBitmapWorker.loadImage()
                .withDataLocation(viewHolder.entry.getImageUrl())
                .withImageView(viewHolder.imageView)
                .withIdentifier(viewHolder.entry.getImageFile())
                .withSampling(viewHolder.imageWidth, viewHolder.imageHeight)
                .start();

            if( viewHolder.entry.getDetectionResults().equals(Const.NO_DETECTION_RESULTS) ){
                viewHolder.imageContainer.setBackgroundColor(mCtx.getResources().getColor(R.color.light_purple));
                viewHolder.progressBar.setVisibility(View.VISIBLE);
                viewHolder.entry.addQueuePollerTask();
            }else{
                viewHolder.progressBar.setVisibility(View.GONE);
                if( viewHolder.entry.getUserResponse().equals(Const.NO_USER_RESPONSE)){
                    viewHolder.imageContainer.setBackgroundColor(mCtx.getResources().getColor(R.color.light_green));
                }else{
                    viewHolder.imageContainer.setBackgroundColor(mCtx.getResources().getColor(R.color.light_grey));
                }

            }
            viewHolder.messageTextView.setText( viewHolder.entry.getUserInputMessage() );
            Date date = new SimpleDateFormat(Request.MESSAGE_DATE_FORMAT)
                .parse(viewHolder.entry.getTimestamp().toString(), new ParsePosition(0));
            viewHolder.timeTextView.setText(new SimpleDateFormat("EEE MMM F yyyy h:m:s a").format
                (date));
            row.setTag(viewHolder);
            return row;
        }

    }

    public static class ViewHolder{
        @InjectView(R.id.image) ImageView imageView;
        int imageWidth;
        int imageHeight;
        @InjectView(R.id.image_container)RelativeLayout imageContainer;
        @InjectView(R.id.progress_bar) ProgressBar progressBar;
        @InjectView(R.id.message) TextView messageTextView;
        @InjectView(R.id.date) TextView timeTextView;
        DynamoEntry entry;
        ProcessListFragment processListFragment;
        public ViewHolder(View view, DynamoEntry entry, int width, int height, ProcessListFragment processListFragment){
            ButterKnife.inject(this, view);
            this.entry = entry;
            entry.viewHolder = this;
            this.imageWidth = width;
            this.imageHeight=height;
            this.processListFragment = processListFragment;
        }
    }

}
