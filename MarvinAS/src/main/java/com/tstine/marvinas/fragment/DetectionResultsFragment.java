package com.tstine.marvinas.fragment;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

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
    private static BitmapWorker sBitmapWorker;
    public DetectionResultsFragment(DynamoEntry entry){
        mEntry = entry;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @InjectView(R.id.user_image) ImageView userImage;
    @InjectView(R.id.result_image) ImageView resultImage;
    @InjectView(R.id.correct) Button correctButton;
    @InjectView(R.id.incorrect) Button incorrectButton;
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

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        sBitmapWorker.loadImage()
            .withDataLocation(mEntry.getImageUrl())
            .withImageView(userImage)
            .withIdentifier(mEntry.getImageFile()+"L")
            .withSampling(width/2, height/2)
            .start();
        String[] detectionResults=null;
        if( mEntry.getDetectionResults() != null){
            detectionResults = mEntry.getDetectionResults().split(" ");
        }
        String imageUrl;
        if( detectionResults.length > 0){
            imageUrl = AWSWorker.getPresignedUrl(detectionResults[0] + ".jpg", Const.RECOGNIZABLE_OBJECTS_BUCKET);
            sBitmapWorker.loadImage()
                .withDataLocation(imageUrl)
                .withImageView(resultImage)
                .withIdentifier(detectionResults[0])
                .withSampling(width/2, height/2)
                .start();
        }

        if(!mEntry.getUserResponse().equals(Const.NO_USER_RESPONSE)){
            disableButtons();
            if(mEntry.getUserResponse().equals(Const.CORRECT_RESPONSE)){
                setPressedDrawable(correctButton);
            }else{
                setPressedDrawable(incorrectButton);
            }
        }
        return layout;
    }
    @OnClick(R.id.correct)
    public void onCorrectClick(){
        mEntry.setUserResponse(Const.CORRECT_RESPONSE);
        //if(Const.SEND_TO_AWS)
            //AWSWorker.saveDynamoEntry(mEntry);
        Toast.makeText(getActivity(), "Correct", 3000).show();
        mEntry.getViewHolder().getFragment().notifyDatasetChanged();
        disableButtons();
        setPressedDrawable(correctButton);
    }
    @OnClick(R.id.incorrect)
    public void onIncorrectClick(){
        mEntry.setUserResponse(Const.INCORRECT_RESPONSE);
        //if(Const.SEND_TO_AWS)
            //AWSWorker.saveDynamoEntry(mEntry);
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
}
