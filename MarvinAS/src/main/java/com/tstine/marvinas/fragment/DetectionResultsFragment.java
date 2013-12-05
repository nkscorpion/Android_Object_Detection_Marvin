package com.tstine.marvinas.fragment;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tstine.marvinas.aws.AWSWorker;
import com.tstine.marvinas.bimap.BitmapWorker;
import com.tstine.marvinas.util.Const;
import com.tstine.marvinas.aws.DynamoEntry;
import com.tstine.marvinas.R;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by taylor on 12/2/13.
 */
public class DetectionResultsFragment extends Fragment {
    //TODO:Allow the image views to be clickable and implement a transition animation to bring up
    // the view
    //TODO:implement a view pager to swipe through the results
    //TODO:upload images to aws bucket for detection objects
    //TODO:implement a global bitmap cache
    //TODO:fix bitmap cache so that it doesn't give an out of memory error
    //TODO: for some reaons mEntry is not pointint to the correct object because it's not updated
    private DynamoEntry mEntry;
    private SlidePagerAdapter mPagerAdapter;
    private static BitmapWorker sBitmapWorker;
    public DetectionResultsFragment(DynamoEntry entry){
        mEntry = entry;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @InjectView(R.id.user_image) ImageView userImage;
    @Optional @InjectView(R.id.result_image) ImageView resultImage;
    @InjectView(R.id.correct) Button correctButton;
    @InjectView(R.id.incorrect) Button incorrectButton;
    @InjectView(R.id.result_pager) ViewPager mPager;
    @InjectView(R.id.indicator) CirclePageIndicator mPageIndicator;
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().getActionBar().hide();
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.detection_results, null);
        ButterKnife.inject(this,layout);
        if( sBitmapWorker == null ){
            sBitmapWorker = new BitmapWorker(getActivity(), userImage.getWidth(),
                userImage.getHeight());
            sBitmapWorker.addMemoryCache(.15f, getFragmentManager());
            sBitmapWorker.setLoadingBitmap(R.drawable.loading_drawable);
        }
        sBitmapWorker.loadImage()
            .withDataLocation(mEntry.getImageUrl())
            .withImageView(userImage)
            .withIdentifier(mEntry.getImageFile()+"L")
            .start();
        String[] detectionResults=null;
        if( mEntry.getDetectionResults() != null){
            detectionResults = mEntry.getDetectionResults().split(" ");
        }
        mPagerAdapter = new SlidePagerAdapter(getFragmentManager(), Arrays.asList(detectionResults));
        mPager.setAdapter(mPagerAdapter);

        if(!mEntry.getUserResponse().equals(Const.NO_USER_RESPONSE)){
            disableButtons();
            if(mEntry.getUserResponse().equals(Const.CORRECT_RESPONSE)){
                setPressedDrawable(correctButton);
            }else{
                setPressedDrawable(incorrectButton);
            }
        }
        mPageIndicator.setViewPager(mPager);
        return layout;
    }
    @OnClick(R.id.correct)
    public void onCorrectClick(){
        mEntry.setUserResponse(Const.CORRECT_RESPONSE);
        Toast.makeText(getActivity(), "Correct", 3000).show();
        mEntry.getViewHolder().getFragment().notifyDatasetChanged();
        disableButtons();
        setPressedDrawable(correctButton);
    }
    @OnClick(R.id.incorrect)
    public void onIncorrectClick(){
        mEntry.setUserResponse(Const.INCORRECT_RESPONSE);
        Toast.makeText(getActivity(), "Incorrect", 3000).show();
        mEntry.getViewHolder().getFragment().notifyDatasetChanged();
        disableButtons();
        setPressedDrawable(incorrectButton);
    }
    public void disableButtons(){
        correctButton.setClickable(false);
        incorrectButton.setClickable(false);
    }

    public void setPressedDrawable(Button button){
        button.setBackgroundColor(getResources().getColor(R.color.light_grey));
    }

    public static class SlidePagerAdapter extends FragmentPagerAdapter{
        private List<String> mDetectionResults;
        public SlidePagerAdapter(FragmentManager fm, List<String> detectionResults){
            super(fm);
            mDetectionResults = detectionResults;
        }
        @Override
        public Fragment getItem(int position){
            String imageUrl = AWSWorker.getPresignedUrl(mDetectionResults.get(position) + ".jpg", Const.RECOGNIZABLE_OBJECTS_BUCKET);
            return ImagePageFragment.newInstance(position, imageUrl, mDetectionResults.get(position));
        }
        public int getCount(){
            return mDetectionResults.size();
        }
    }

    public static class ImagePageFragment extends Fragment{
        String mUrl = null;
        String mResult = null;

        public static ImagePageFragment newInstance(int position, String url, String result){
            ImagePageFragment frag = new ImagePageFragment(url, result);
            Bundle args = new Bundle();
            args.putInt("position", position);
            frag.setArguments(args);
            return frag;
        }
        public ImagePageFragment(String url, String result){
            mUrl = url;
            mResult = result;
        }
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
        }
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
            ImageView imageView= (ImageView) inflater.inflate(R.layout.image_page_fragment, container, false);
            sBitmapWorker.loadImage()
                .withDataLocation(mUrl)
                .withImageView(imageView)
                .withIdentifier(mResult)
                .start();
            return imageView;
        }

    }
}
