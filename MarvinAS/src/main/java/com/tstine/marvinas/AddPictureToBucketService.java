package com.tstine.marvinas;
import java.util.List;
import java.io.File;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.auth.BasicAWSCredentials;
import android.content.Intent;
import android.app.IntentService;
import static com.tstine.marvinas.Const.*;

public class AddPictureToBucketService extends IntentService{
	public AddPictureToBucketService(){
		super("AddPictureToBucketService");
	}

	@Override
	protected void onHandleIntent( Intent intent ){
		AmazonS3Client s3Client =
			new AmazonS3Client( new BasicAWSCredentials( ACCESS_KEY,
																									 SECRET_KEY ) );
		File picFile = new File( intent.getData().getPath() );
		List<Bucket> buckets = s3Client.listBuckets();
		Bucket bucket = null;
		for( Bucket b : buckets ){
			if( b.getName() == MARVINS_BUCKET_NAME )
 				bucket = b;
		}
		if( bucket == null ){
			bucket = s3Client.createBucket( MARVINS_BUCKET_NAME );
		}
		try{
		PutObjectRequest por = new PutObjectRequest( bucket.getName(),
																								 picFile.getName(),
																								 picFile );
		s3Client.putObject(por);
		} catch( Exception exception ) {
			throw new RuntimeException("Error putting object");
		}
	}

}