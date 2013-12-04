package com.tstine.marvinas;
import java.util.List;

import android.util.Log;
import android.app.IntentService;
import android.content.Intent;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

import org.apache.commons.codec.binary.Base64;

import static com.tstine.marvinas.Const.*;

public class AddToQueueService extends IntentService{
	public AddToQueueService(){
		super("AddToQueueService");
	}

	@Override
	protected void onHandleIntent( Intent intent ){
		Request request = (Request) intent.getSerializableExtra(
			REQUEST_EXTRA_KEY );
		AmazonSQSClient sqsClient = new AmazonSQSClient(
			new BasicAWSCredentials( ACCESS_KEY, SECRET_KEY ) );
		Region region = Region.getRegion(Regions.US_WEST_2); 
		sqsClient.setRegion(region);

		List<String> queues = sqsClient.listQueues(new ListQueuesRequest( WORK_QUEUE_NAME)).getQueueUrls();
    String queueUrl = null;
    if( queues.isEmpty() ){
      CreateQueueResult res = sqsClient.createQueue(
          new CreateQueueRequest( WORK_QUEUE_NAME));
      queueUrl = res.getQueueUrl();
      Log.w( TAG, "Warning: no queue existed, so I created one");
    }else{
      queueUrl = queues.get(0).toString();
    }
    if( queueUrl == null ){
      Log.e(TAG, "Could not create a valid queue");
      throw new RuntimeException("Error, I could not create a valid queue");
    }
      Base64 base64 = new Base64();
      String message = request.getImageName() + "\n" + request.getId() + "\n" + request.getMessage();
      String encodedMessage = new String(base64.encode(message.getBytes()));

		String messageId = sqsClient.sendMessage( new SendMessageRequest( queueUrl, encodedMessage)).getMessageId();
	}
}