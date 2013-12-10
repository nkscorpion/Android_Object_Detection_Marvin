package com.tstine.marvinas.fragment;

import android.content.Context;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.tstine.marvinas.aws.AWSWorker;
import com.tstine.marvinas.bimap.BitmapWorker;
import com.tstine.marvinas.util.Const;
import com.tstine.marvinas.aws.DynamoEntry;
import com.tstine.marvinas.util.Installation;
import com.tstine.marvinas.util.Log;
import com.tstine.marvinas.R;
import com.tstine.marvinas.aws.Request;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by taylor on 12/4/13.
 */
public class ProcessListFragment extends ListFragment implements AdapterView
    .OnItemClickListener, AdapterView.OnItemLongClickListener, AbsListView.OnScrollListener
{
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static MyAdapter mAdapter;
    private static AWSWorker.DynamoQueryTask mQueryTask;
    private static ProcessListFragment sInstance;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    public static ProcessListFragment getInstance(){
        if(sInstance == null){
            sInstance = new ProcessListFragment();
        }
        return sInstance;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Bundle args = getArguments();
        Request request = (Request) args.getSerializable(Const.REQUEST_EXTRA_KEY);
        if( mAdapter == null)
            mAdapter = new MyAdapter(this);
        if( request != null){
            DynamoEntry entry = new DynamoEntry();
            entry.setUserId(Installation.getId());
            entry.setTimestamp(Long.parseLong(request.getTimestamp()));
            entry.setDetectionResults(Const.NO_DETECTION_RESULTS);
            entry.setImageUrl( request.getImagePath());
            entry.setImageFile( request.getImageName());
            entry.setStatus(Const.UNPROCESSED_STATUS);
            entry.setUserInputMessage(request.getMessage());
            entry.setUserResponse(Const.NO_USER_RESPONSE);
            mAdapter.addItem(entry);
        }
        mQueryTask = AWSWorker.queryDynamo(request,this);

        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
        getListView().setOnScrollListener(this);
        registerForContextMenu(getListView());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id){
        DynamoEntry entry = mAdapter.getItem(position);
        if( !entry.getDetectionResults().equals(Const.NO_DETECTION_RESULTS)){
            //&& entry.getUserResponse().equals(Const.NO_USER_RESPONSE) ){
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            DetectionResultsFragment newFragment = new DetectionResultsFragment(entry);
            transaction.add(R.id.container, newFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   int position, long id ){

        return false;
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.process_list_context_menu, menu);
    }

    /*@Override
    public boolean onContexItemSelected(MenuItem item){
        switch( item.getItemId()){
            case R.id.delete_item:
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }*/

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
        if( mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }
    public void setAdapterData(List<DynamoEntry> newEntries){
        if( mAdapter != null){
            mAdapter.setData(newEntries);
            setListAdapter(mAdapter);
        }
    }

    public static void saveState(){
        ProcessListFragment instance = sInstance;
        if(instance != null){
            instance.getAdapter().saveState();
        }
    }

    public MyAdapter getAdapter(){
        return mAdapter;
    }


    public AWSWorker.DynamoQueryTask getQueryTask(){return mQueryTask;}

    /**
     * Adapter for the list view of detection results.
     */
    public static class MyAdapter extends BaseAdapter{
        private List<DynamoEntry> mData = null;
        private static BitmapWorker sBitmapWorker =null;
        private LinkedList<DynamoEntry> mJustAdded = null;
        private ProcessListFragment mListFragment = null;
        private Context mCtx;

        public MyAdapter(ProcessListFragment parent){
            this(null, parent);
        }
        public MyAdapter( List<DynamoEntry> data,ProcessListFragment parent){
            mJustAdded = new LinkedList<DynamoEntry>();
            mData = data;
            mListFragment = parent;
            mCtx = mListFragment.getActivity();
            if( sBitmapWorker == null ){
                sBitmapWorker = new BitmapWorker(mCtx);
                sBitmapWorker.setLoadingBitmap(R.drawable.loading_drawable);
                sBitmapWorker.addMemoryCache(.15f, mListFragment.getFragmentManager());
            }
        }

        public void setData(List<DynamoEntry> entries){mData = entries;}
        public void addItem(DynamoEntry entry){
            mJustAdded.addFirst(entry);
        }

        public void saveState(){
            new AWSWorker.AddToDynamoTask().execute(mJustAdded.toArray(new DynamoEntry[mJustAdded.size()]));
            new AWSWorker.DynamoUpdateTask().execute(mData.toArray(new DynamoEntry[mJustAdded.size()]));
            mJustAdded.clear();
        }
        public boolean isEmpty(){return mData == null;}

        /**
         * Gets the number of items in the data set.  If the mJustAdded value is not null, then this
         * size will be one plus the size of the data set
         * @return
         */
        @Override
        public int getCount(){
            return mData.size() + mJustAdded.size();
        }


        /**
         * returns an item from the data set to use.  If the list asks for the first item,
         * the mJustAdded item is returned (since it's not in the data set).  Otherwise
         * the position is decremented, and that corresponding item from the data set is returned
         * @param position position of the view to get
         * @return an item from the data set for that view
         */
        @Override
        public DynamoEntry getItem( int position ){
            DynamoEntry entry = null;
            if(position < mJustAdded.size()){
                entry = mJustAdded.get(position);
                entry.setImageUrl(AWSWorker.getPresignedUrl(entry.getImageFile()));
                return entry;
            }
            else{
                position -= mJustAdded.size();
                if(mData != null){
                    entry = mData.get(position);
                    entry.setImageUrl(AWSWorker.getPresignedUrl(entry.getImageFile()));
                }
                return entry;
            }
        }


        /**
         * There is no need for this method, but it must be overridden
         * @param position
         * @return
         */
        @Override
        public long getItemId(int position) {
            return 0;
        }

        /**
         * Returns the view for each row of the layout
         * @param position position in the list of the item
         * @param convertView the recycled view, if it was already inflated
         * @param parent the viewgroup the view will be added to
         * @return the view to be added to the list view
         */
        @Override
        public View getView(int position, View convertView,
                            ViewGroup parent ){
            View row = convertView;
            ViewHolder viewHolder;
            if( row == null ){
                LayoutInflater inflater =
                    (LayoutInflater)mCtx.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.request_row, null);
                viewHolder = new ViewHolder(row, getItem(position),
                    (int)mCtx.getResources().getDimension(R.dimen.process_imageview_width),
                    (int)mCtx.getResources().getDimension(R.dimen.process_imageview_height),
                    mListFragment );
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

    /**
     * ViewHolder class to implement the viewholder design pattern
     * This class holds a reference to all of a views widgets.  This reduces the needs to call
     * findViewById which improves speed when rendering the list
     * see: http://developer.android.com/training/improving-layouts/smooth-scrolling.html
     */
    public static class ViewHolder{
        @InjectView(R.id.image) ImageView imageView;
        int imageWidth;
        int imageHeight;
        @InjectView(R.id.image_container)RelativeLayout imageContainer;
        @InjectView(R.id.progress_bar)ProgressBar progressBar;
        @InjectView(R.id.message) TextView messageTextView;
        @InjectView(R.id.date) TextView timeTextView;
        DynamoEntry entry;
        ProcessListFragment processListFragment;
        public ViewHolder(View view, DynamoEntry entry, int width, int height, ProcessListFragment processListFragment){
            ButterKnife.inject(this, view);
            this.entry = entry;
            entry.setViewHolder(this);
            this.imageWidth = width;
            this.imageHeight=height;
            this.processListFragment = processListFragment;
        }

        public ProcessListFragment getFragment(){return processListFragment;}
    }
}