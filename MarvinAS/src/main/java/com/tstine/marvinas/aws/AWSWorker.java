package com.tstine.marvinas.aws;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.tstine.marvinas.fragment.ProcessListFragment;
import com.tstine.marvinas.util.AsyncTask;
import com.tstine.marvinas.util.Const;
import com.tstine.marvinas.util.Installation;
import com.tstine.marvinas.util.Log;

import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Taylor on 12/3/13.
 * This class process most of the AWS back end work
 */
public class AWSWorker {
    private static final AmazonS3Client s3Client;
    private static final AmazonDynamoDBClient dbClient;
    private static final AmazonSQSClient sqsClient;
    private static List<SoftReference<DynamoEntry>> sQueuedEntries;
    private static DynamoDBMapper mMapper;
    private static List<DynamoEntry> sQueryResults;
    private static boolean sDynamoUpdated;
static{
    dbClient =
        new AmazonDynamoDBClient(
            new BasicAWSCredentials( Const.ACCESS_KEY, Const.SECRET_KEY ) );
    dbClient.setRegion( Region.getRegion(Regions.US_WEST_2) );
    sqsClient = new AmazonSQSClient(
        new BasicAWSCredentials( Const.ACCESS_KEY, Const.SECRET_KEY ) );
    sqsClient.setRegion(Region.getRegion(Regions.US_WEST_2));
    s3Client = new AmazonS3Client(
        new BasicAWSCredentials( Const.ACCESS_KEY, Const.SECRET_KEY));
    mMapper = new DynamoDBMapper(dbClient);
    sQueuedEntries = Collections.synchronizedList(new ArrayList<SoftReference<DynamoEntry>>());
    sQueryResults = new ArrayList<DynamoEntry>();
    sDynamoUpdated = false;
}

    public static String getPresignedUrl(String imageName, String bucketName){
        //you have 30 seconds to download it
        Date expirationDate = new Date( new Date().getTime() + 30000);
        String url = s3Client.generatePresignedUrl(bucketName,
            imageName,
            expirationDate,
            HttpMethod.GET ).toString();
        return url;
    }

    public static String getPresignedUrl(String imageName){
        return getPresignedUrl(imageName, Const.MARVINS_BUCKET_NAME);
    }

    public static DynamoQueryTask queryDynamo(Request request, ProcessListFragment processListFragment){
        Condition rangeKeyCondition;
        if( request == null ){
            rangeKeyCondition= new Condition()
                .withComparisonOperator(ComparisonOperator.GT.toString())
                .withAttributeValueList(new AttributeValue().withN("0"));
        }else{
        rangeKeyCondition= new Condition()
            .withComparisonOperator(ComparisonOperator.LT.toString())
            .withAttributeValueList(new AttributeValue().withN(request.getTimestamp()));
        }
        DynamoEntry entryKey = new DynamoEntry();
        entryKey.setUserId(Installation.getId());

        DynamoDBQueryExpression<DynamoEntry> expression = new
            DynamoDBQueryExpression<DynamoEntry>()
            .withHashKeyValues(entryKey)
            .withRangeKeyCondition(Const.D_TIMESTAMP_ATTRIBUTE, rangeKeyCondition)
            .withScanIndexForward(false);

        DynamoQueryTask task = processListFragment.getQueryTask();
        if(task == null || task.getStatus() == AsyncTask.Status.FINISHED){
            task = new DynamoQueryTask(processListFragment);
            task.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, expression);
        }
        else{
            processListFragment.setAdapterData(sQueryResults);
        }

        return task;
        //TODO:app stops here and never continues
        //task.execute( expression );
    }

    public static class ResultQueuePollerTask extends AsyncTask<Void, Void, List<Message>> {
        public WeakReference<DynamoEntry> mEntryReference;
        public ResultQueuePollerTask(DynamoEntry entry){
            mEntryReference = new WeakReference<DynamoEntry>(entry);
            synchronized (sQueuedEntries){
                sQueuedEntries.add(new SoftReference<DynamoEntry>(entry));
            }
        }
        @Override
        protected List<Message> doInBackground(Void... params) {
            List<String> queues = new ArrayList<String>();
            String queueUrl;
            List<Message> queueMessages = new ArrayList<Message>();
            while(queues.isEmpty() && !isCancelled()){
                queues = sqsClient.listQueues(new ListQueuesRequest(Installation.getId())).getQueueUrls();
                Log.d("Looking for queue " + queues.size());
                try{
                    Thread.sleep(Const.POLL_SLEEP_TIME);
                }catch(InterruptedException e){}
            }

            if(!queues.isEmpty() && !isCancelled()){
                queueUrl = queues.get(0).toString();
                ReceiveMessageRequest request = new ReceiveMessageRequest( queueUrl )
                    .withVisibilityTimeout(10)
                    .withWaitTimeSeconds(20)
                    .withMaxNumberOfMessages(10);
                while( queueMessages.size() == 0 && !isCancelled()){
                    queueMessages = sqsClient.receiveMessage(request).getMessages();
                    Log.d("entry looking for messages");
                }
            }
            return queueMessages;
        }

        @Override
        protected void onPostExecute(List<Message> queueMessages) {
            synchronized(sQueuedEntries){
                for( final Message queueMessage : queueMessages){
                    String queueBody = new String(Base64.decodeBase64(queueMessage.getBody().getBytes()));
                    String[] lines = queueBody.split(System.getProperty("line.separator"));
                    for(SoftReference<DynamoEntry> entrySoftReference : sQueuedEntries){
                        DynamoEntry entry = entrySoftReference.get();
                        if( entry != null){
                            String requestId = entry.getUserId() + "_" + entry.getTimestamp();
                            if(lines[0].equals(requestId)){
                                entry.setDetectionResults(lines[3]);
                                entry.setStatus(Const.PROCESSED_STATUS);
                                entry.getViewHolder().getFragment().notifyDatasetChanged();
                                entry.getQueuePollerTask().cancel(true);
                                //if(Const.SEND_TO_AWS)
                                //AWSWorker.saveDynamoEntry(entry);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        List<String> queues = sqsClient.listQueues(new ListQueuesRequest(Installation.getId())).getQueueUrls();
                                        sqsClient.deleteMessage(new DeleteMessageRequest(queues.get(0).toString(), queueMessage.getReceiptHandle()));
                                    }
                                }).start();
                                break;
                            }

                        }
                    }

                }
            }
        }

        private DynamoEntry getAttachedEntry(){
            final DynamoEntry entry = mEntryReference.get();
            final ResultQueuePollerTask resultQueuePollerTask = entry.getQueuePollerTask();
            if( this.equals(resultQueuePollerTask)){
                return entry;
            }
            return null;
        }
    }

    public static void saveDynamoEntry(DynamoEntry e){
        sDynamoUpdated = true;
        new DynamoUpdateTask().execute(e);
    }

    public static void uploadToAWS( Request request ){
        sDynamoUpdated = true;
        new AddToBucketTask().execute(request);
        new AddToQueueTask().execute(request);
    }

    public static class DynamoQueryTask extends AsyncTask<DynamoDBQueryExpression<DynamoEntry>, Void, PaginatedQueryList<DynamoEntry>>{
        public ProcessListFragment mProcessListFragment;
        public DynamoQueryTask(ProcessListFragment processListFragment){
            mProcessListFragment = processListFragment;
        }
        @Override
        public PaginatedQueryList<DynamoEntry> doInBackground(DynamoDBQueryExpression<DynamoEntry>... params){
            DynamoDBQueryExpression<DynamoEntry> expression = params[0];
            PaginatedQueryList<DynamoEntry>queryResults = null;
            synchronized (mMapper){
                queryResults= mMapper.query(DynamoEntry.class, expression);
            }
            return queryResults;
        }

        @Override
        protected void onPostExecute(PaginatedQueryList<DynamoEntry> dynamoEntries) {
            mProcessListFragment.setAdapterData(dynamoEntries);
            sQueryResults = dynamoEntries;
            sDynamoUpdated = false;
        }
    }

    public static class DynamoUpdateTask extends AsyncTask<DynamoEntry, Void, Void>{
        @Override
        public Void doInBackground(DynamoEntry... params){
            if( params.length > 0){
                synchronized (mMapper){
                    mMapper.batchSave(params[0]);
                }
            }
            return null;
        }
    }

    public static class AddToBucketTask extends AsyncTask<Request, Void, Void>{
        public Void doInBackground(Request... params){
            Request request = params[0];
            File picFile = new File( request.getImagePath());
            List<Bucket> buckets = s3Client.listBuckets();
            Bucket bucket = null;
            for( Bucket b : buckets ){
                if( b.getName() == Const.MARVINS_BUCKET_NAME )
                    bucket = b;
            }
            if( bucket == null ){
                bucket = s3Client.createBucket( Const.MARVINS_BUCKET_NAME );
            }
            try{
                PutObjectRequest por = new PutObjectRequest( bucket.getName(),
                    picFile.getName(),
                    picFile );
                s3Client.putObject(por);
            } catch( Exception exception ) {}
            return null;
        }

    }

    public static class AddToDynamoTask extends AsyncTask<DynamoEntry, Void, Void>{
        public Void doInBackground(DynamoEntry... params){
            if(params.length > 0){
                synchronized(mMapper){
                    mMapper.batchSave(params);
                }
            }
            return null;
        }
    }

    public static class AddToQueueTask extends AsyncTask<Request, Void, Void>{
        public Request mRequest;
            public Void doInBackground(Request... params){
            mRequest= params[0];
            List<String> queues = sqsClient.listQueues(new ListQueuesRequest( Const.WORK_QUEUE_NAME)).getQueueUrls();
            String queueUrl = queues.get(0).toString();
            Base64 base64 = new Base64();
            String message = mRequest.getImageName() + "\n" + mRequest.getId() + "\n" + mRequest.getMessage();
            String encodedMessage = new String(base64.encode(message.getBytes()));
            String messageId = sqsClient.sendMessage( new SendMessageRequest( queueUrl, encodedMessage)).getMessageId();
            return null;
        }
    }
}
