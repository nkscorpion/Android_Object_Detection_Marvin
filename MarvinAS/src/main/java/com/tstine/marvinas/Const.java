package com.tstine.marvinas;

public class Const{
	public static final String TAG="Marvin";
	
	public static final String ACTIVE_DB = "DYNAMO";

	public static final String MARVINS_BUCKET_NAME = "marvinsbucket"; //good

  //TODO set this up with a TVM through aws anonomyous identity
	public static final String ACCESS_KEY = "AKIAI4GLOTYYDK5FWC4A";
	public static final String SECRET_KEY = "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";

	public static final String UNPROCESSED_STATUS = "unprocessed";
	public static final String NO_USER_RESPONSE = "unanswered";
	public static final String NO_DETECTION_RESULTS = "awaiting results";

	public static final String MARVINS_DOMAIN = "Marvin";
	public static final String ID_ATTRIBUTE = "id";
	public static final String IMAGE_NAME_ATTRIBUTE = "imageName";
	public static final String IP_ADDRESS_ATTRIBUTE = "ipAddress";
	public static final String TIMESTAMP_ATTRIBUTE = "timestamp";
	public static final String STATUS_ATTRIBUTE = "status";
	public static final String USER_RESPONSE_ATTRIBUTE = "userResponse";
	public static final String DETECTION_RESULTS_ATTRIBUTE = "detectionResults";

	public static final String MARVIN_DYNAMO_TABLE = "MarvinsTable";
	public static final String D_ID_ATTRIBUTE = "Id";
	public static final String D_TIMESTAMP_ATTRIBUTE = "Timestamp";
	public static final String D_USER_ID_ATTRIBUTE = "UserId";
	public static final String D_DETECTION_RESULT_ATTRIBUTE = "DetectionResults";
	public static final String D_STATUS_ATTRIBUTE = "Status";
	public static final String D_USER_RESPONSE_ATTRIBUTE = "UserResponse";
	public static final String D_IMAGE_NAME_ATTRIBUTE = "ImageFile";
	public static final String D_PERCENT_COMPLETE_ATTRIBUTE = "PercentComplete";
  public static final String D_USER_MESSAGE_ATTRIBUTE = "UserInputMessage";

	public static final String REQUEST_EXTRA_KEY = "REQUEST"; //good
	public static final String USER_MESSAGE_KEY = "MESSAGE";

	public static final String WORK_QUEUE_NAME = "MarvinToDo";

  public static final String USER_MESSAGE_EXTRA = "user_message";
    public static final boolean SEND_TO_AWS = true;

}