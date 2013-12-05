package com.tstine.marvinas.aws;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.tstine.marvinas.util.Const;
import com.tstine.marvinas.fragment.ProcessListFragment;
import com.tstine.marvinas.util.AsyncTask;
import com.tstine.marvinas.util.Installation;

/**
 * Created by taylor on 11/21/13.
 */
@DynamoDBTable(tableName = Const.MARVIN_DYNAMO_TABLE)
public class DynamoEntry {

    private String userId;
    private Long timestamp;
    private String detectionResults;
    private String imageFile;
    private String status;
    private String userInputMessage;
    private String userResponse;
    private String imageUrl;



    private AWSWorker.ResultQueuePollerTask queuePollerTask;
    private ProcessListFragment.ViewHolder viewHolder;


    public DynamoEntry(){
        userId = Installation.getId();
        timestamp=0L;
        detectionResults=Const.NO_DETECTION_RESULTS;
        imageFile="";
        status=Const.UNPROCESSED_STATUS;
        userInputMessage = "";
        userResponse =Const.NO_USER_RESPONSE;
    }
    @DynamoDBHashKey(attributeName=Const.D_USER_ID_ATTRIBUTE)
    public String getUserId() {
        return userId;
    }


    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDBRangeKey(attributeName=Const.D_TIMESTAMP_ATTRIBUTE)
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @DynamoDBAttribute(attributeName=Const.D_DETECTION_RESULT_ATTRIBUTE)
    public String getDetectionResults() {
        return detectionResults;
    }

    public void setDetectionResults(String detectionResults) {
        this.detectionResults = detectionResults;
    }

    @DynamoDBAttribute(attributeName=Const.D_IMAGE_NAME_ATTRIBUTE)
    public String getImageFile() {
        return imageFile;
    }

    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
    }

    @DynamoDBAttribute(attributeName=Const.D_STATUS_ATTRIBUTE)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @DynamoDBAttribute(attributeName=Const.D_USER_MESSAGE_ATTRIBUTE)
    public String getUserInputMessage() {
        return userInputMessage;
    }

    public void setUserInputMessage(String userInputMessage) {
        this.userInputMessage = userInputMessage;
    }

    @DynamoDBAttribute(attributeName=Const.D_USER_RESPONSE_ATTRIBUTE)
    public String getUserResponse() {
        return userResponse;
    }

    public void setUserResponse(String userResponse) {
        this.userResponse = userResponse;
    }


    @DynamoDBIgnore
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @DynamoDBIgnore
    public AWSWorker.ResultQueuePollerTask getQueuePollerTask(){return queuePollerTask;}

    public void addQueuePollerTask() {
        if( this.queuePollerTask == null || this.queuePollerTask.getStatus().equals(AsyncTask.Status.FINISHED)){
            this.queuePollerTask = new AWSWorker.ResultQueuePollerTask(this);
            this.queuePollerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @DynamoDBIgnore
    public ProcessListFragment.ViewHolder getViewHolder() {
        return viewHolder;
    }
    @DynamoDBIgnore
    public void setViewHolder(ProcessListFragment.ViewHolder viewHolder) {
        this.viewHolder = viewHolder;
    }


}
