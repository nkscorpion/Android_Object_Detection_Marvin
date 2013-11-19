package com.tstine.marvinas;
import java.util.ArrayList;
import java.util.List;

import android.app.IntentService;
import android.content.Intent;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

import static com.tstine.marvinas.Const.*;

public class AddToSimpleDBService extends IntentService{
	public AddToSimpleDBService(){
		super( "AddRequestService" );
	}

	@Override
	protected void onHandleIntent( Intent intent ){
		Request request = (Request) intent.getSerializableExtra( REQUEST_EXTRA_KEY );
		AmazonSimpleDBClient sdbClient = new AmazonSimpleDBClient( new BasicAWSCredentials( ACCESS_KEY, SECRET_KEY ));

		ReplaceableAttribute idAttribute =
			new ReplaceableAttribute( ID_ATTRIBUTE,
																request.getId(),
																true );
		ReplaceableAttribute imageNameAttribute = 
			new ReplaceableAttribute( IMAGE_NAME_ATTRIBUTE,
																request.getImagePath(),
																true);
		ReplaceableAttribute ipAddressAttribute =
			new ReplaceableAttribute( IP_ADDRESS_ATTRIBUTE,
																request.getInstallId(),
																true );
		ReplaceableAttribute timestampAttribute =
			new ReplaceableAttribute( TIMESTAMP_ATTRIBUTE,
																request.getTimestamp(),
																true);
		ReplaceableAttribute statusAttribute =
			new ReplaceableAttribute( STATUS_ATTRIBUTE,
																UNPROCESSED_STATUS,
																true );
		ReplaceableAttribute userResponseAttribute =
			new ReplaceableAttribute( USER_RESPONSE_ATTRIBUTE,
																NO_USER_RESPONSE,
																true);
		ReplaceableAttribute detectionAttribute =
			new ReplaceableAttribute( DETECTION_RESULTS_ATTRIBUTE,
																NO_DETECTION_RESULTS,
																true );
		List<ReplaceableAttribute> attribs = new ArrayList<ReplaceableAttribute>(7);
		attribs.add( idAttribute );
		attribs.add( imageNameAttribute );
		attribs.add( ipAddressAttribute );
		attribs.add( timestampAttribute );
		attribs.add( statusAttribute );
		attribs.add( userResponseAttribute );
		attribs.add( detectionAttribute );
		
		String marvinsDomain = sdbClient.listDomains().withDomainNames( MARVINS_DOMAIN ).toString();
		
		if( marvinsDomain.equals("") ){
			throw new RuntimeException("Marvins database doesn't exist!");
		}

		PutAttributesRequest par = new PutAttributesRequest( MARVINS_DOMAIN, request.getId(), attribs );
		try{
			sdbClient.putAttributes( par );
		}catch( Exception e ){
			throw new RuntimeException("Error adding to database: " + e.getMessage() );
		}

		stopSelf();

		/*GetAttributesRequest gar = new GetAttributesRequest( MARVINS_DOMAIN,
																												 request.getId() );
		GetAttributesResult response = sdbClient.getAttributes( gar );

		String selectRec = "select * from " + MARVINS_DOMAIN + 
			" where " + ID_ATTRIBUTE + " = '" + request.getId() +"'";
		String selectAll = "select * from " + MARVINS_DOMAIN;
		SelectResult selectRes=sdbClient.select( new SelectRequest( selectRec, true));
		//while( selectRes.getItems().size() == 0 )
		selectRes = sdbClient.select( new SelectRequest( selectRec, true ));

		SelectResult allRes = sdbClient.select( new SelectRequest( selectAll ));
		Log.d(ADD_REQUEST_TAG, "select result: " + selectRes.toString());
		Log.d(ADD_REQUEST_TAG, "db size: " + allRes.getItems().size());
		Log.d(ADD_REQUEST_TAG, "request id: " + request.getId() );
		//Log.d(ADD_REQUEST_TAG, "all: " + allRes.toString() );*/
	}
}