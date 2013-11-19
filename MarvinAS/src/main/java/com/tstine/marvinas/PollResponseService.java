package com.tstine.marvinas;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import android.app.Service;
import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import android.util.Log;
import static com.tstine.marvinas.Const.*;

public class PollResponseService extends Service{
	private final IBinder mBinder = new LocalBinder();
	public static AtomicInteger mNumRequests= new AtomicInteger();
	private PollQueue mPollQueueTask = new PollQueue();

	@Override
	public IBinder onBind( Intent intent ){
		Log.d(TAG, "Service Bound!");
		incrementQueueRequests();
		return mBinder;
	}

	public void incrementQueueRequests(){
		if( mPollQueueTask.getStatus() != AsyncTask.Status.RUNNING ){
			mPollQueueTask = new PollQueue();
			mPollQueueTask.execute();
		}

	}
	public void decrementQueueRequests(){
		mNumRequests.getAndDecrement();
	}

	public class LocalBinder extends Binder{
		PollResponseService getService(){
			return PollResponseService.this;
		}
	}

	private class PollQueue extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground( Void...v ){
			AmazonSQSClient sqsClient = new AmazonSQSClient(
				new BasicAWSCredentials( ACCESS_KEY, SECRET_KEY ) );
			
			sqsClient.setRegion( Region.getRegion( Regions.US_WEST_2) );
			List<String> queues = sqsClient.listQueues(
				new ListQueuesRequest( Installation.getId()) ).getQueueUrls();
			
			String queueUrl;
			if( queues.isEmpty() ){
				queueUrl = sqsClient.createQueue(
					new CreateQueueRequest( Installation.getId() ) ).getQueueUrl();
			}else{
				queueUrl = queues.get(0).toString();
			}
			ReceiveMessageRequest request = new ReceiveMessageRequest( queueUrl )
				.withVisibilityTimeout(5).withWaitTimeSeconds(20);
			while( mNumRequests.get() > 0 ){
				List<Message> queueMessage = sqsClient.receiveMessage( request  ).getMessages();
				if( queueMessage.size() > 0 ){
					decrementQueueRequests();
					//do something with the message here
					sqsClient.deleteMessage( new DeleteMessageRequest( queueUrl,
																														 queueMessage.get(0).getReceiptHandle() ) );
					Log.d( TAG, "Got answer: " + queueMessage.get(0).getBody() );
				}
				Log.d(TAG, "Queue searched: " + mNumRequests +
							" Message size: " + queueMessage.size());
			}
			Log.d(TAG, "Queue polled!");
			return null;
		}
	};
	
}