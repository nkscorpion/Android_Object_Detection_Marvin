package com.tstine.marvinas;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
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
    private String UserResponse;

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
        return UserResponse;
    }

    public void setUserResponse(String userResponse) {
        UserResponse = userResponse;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(getUserId());
        sb.append(" | ");
        sb.append(getTimestamp());
        sb.append(" | ");
        sb.append(getDetectionResults());
        sb.append(" | ");
        sb.append(getImageFile());
        sb.append(" | ");
        sb.append(getStatus());
        sb.append(" | ");
        sb.append(getUserInputMessage());
        sb.append(" | ");
        sb.append(getUserResponse());
        return sb.toString();
    }

}
