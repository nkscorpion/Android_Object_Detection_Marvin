package com.tstine.marvinas;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

import static com.tstine.marvinas.Const.*;

public class AddToDynamoService extends IntentService{
	public AddToDynamoService(){
		super("AddToDynamoService");
	}
	
	@Override
	protected void onHandleIntent( Intent intent ){
		Request request = (Request )intent.getSerializableExtra( REQUEST_EXTRA_KEY );
      if( request == null )
          return;
      AmazonDynamoDBClient ddbClient =
        new AmazonDynamoDBClient(
            new BasicAWSCredentials ( ACCESS_KEY, SECRET_KEY ) );

		ddbClient.setRegion( Region.getRegion( Regions.US_WEST_2 ) );
		
		//HashMap<String, AttributeValue> map = createAttribMap(request);
		List<String> tables = ddbClient.listTables().getTableNames();
    if( tables.indexOf( MARVIN_DYNAMO_TABLE) == -1 ){
      throw new RuntimeException("The table does not exist for Dynamo DB");
      //createTable();
     }

      DynamoEntry entry = new DynamoEntry();
      entry.setUserId(Installation.getId());
      entry.setTimestamp(Long.parseLong(request.getTimestamp()));
      entry.setDetectionResults(Const.NO_DETECTION_RESULTS);
      entry.setImageFile( request.getImageName());
      entry.setStatus(Const.UNPROCESSED_STATUS);
      entry.setUserInputMessage(request.getMessage());
      entry.setUserResponse(Const.NO_USER_RESPONSE);
      DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);
      mapper.save(entry);

      //PutItemRequest pir = new PutItemRequest( MARVIN_DYNAMO_TABLE, map);
      //ddbClient.putItem( pir );
  }

  private HashMap<String, AttributeValue> createAttribMap(Request request){
    HashMap<String, AttributeValue> map =
        new HashMap<String, AttributeValue>();
      map.put( D_USER_ID_ATTRIBUTE,
          new AttributeValue().withS( Installation.getId() ));

      map.put(D_TIMESTAMP_ATTRIBUTE,
          new AttributeValue().withN(request.getTimestamp()));
    map.put( D_DETECTION_RESULT_ATTRIBUTE,
        new AttributeValue().withS( NO_DETECTION_RESULTS));
    map.put( D_STATUS_ATTRIBUTE,
        new AttributeValue().withS( UNPROCESSED_STATUS ) );
    map.put( D_USER_RESPONSE_ATTRIBUTE,
        new AttributeValue().withS( NO_USER_RESPONSE ) );
    map.put( D_IMAGE_NAME_ATTRIBUTE,
        new AttributeValue().withS( request.getImageName() ) );
    map.put( D_PERCENT_COMPLETE_ATTRIBUTE,
        new AttributeValue().withN("0") );
    map.put( D_USER_MESSAGE_ATTRIBUTE,
        new AttributeValue().withS( request.getMessage() ) );
    return map;
  }
  private Collection<AttributeDefinition> createAttribList(){
    Collection<AttributeDefinition> attributes =
        new ArrayList<AttributeDefinition>();
    attributes.add(new AttributeDefinition(D_ID_ATTRIBUTE, ScalarAttributeType.S));
    attributes.add(new AttributeDefinition(D_TIMESTAMP_ATTRIBUTE, ScalarAttributeType.N));
    attributes.add(new AttributeDefinition(D_USER_ID_ATTRIBUTE, ScalarAttributeType.S));
    attributes.add(new AttributeDefinition(D_DETECTION_RESULT_ATTRIBUTE, ScalarAttributeType.S));
    attributes.add(new AttributeDefinition(D_STATUS_ATTRIBUTE, ScalarAttributeType.S));
    attributes.add(new AttributeDefinition(D_USER_RESPONSE_ATTRIBUTE, ScalarAttributeType.S));
    attributes.add(new AttributeDefinition(D_IMAGE_NAME_ATTRIBUTE, ScalarAttributeType.S));
    attributes.add(new AttributeDefinition(D_PERCENT_COMPLETE_ATTRIBUTE, ScalarAttributeType.N));
    attributes.add(new AttributeDefinition(D_USER_MESSAGE_ATTRIBUTE, ScalarAttributeType.S));
    return attributes;
  }

  //TODO implement this create table request, currently throws an error
  //I'm not sure if it's appropriate to have an application create a table anyway
  private void createTable(AmazonDynamoDBClient ddbClient){
      Collection<AttributeDefinition> attributes = createAttribList();
      ArrayList<KeySchemaElement> keys = new ArrayList<KeySchemaElement>();
      keys.add(new KeySchemaElement(D_ID_ATTRIBUTE, KeyType.HASH));
      keys.add( new KeySchemaElement(D_TIMESTAMP_ATTRIBUTE, KeyType.RANGE));
      Collection<LocalSecondaryIndex> indexes =
          new ArrayList<LocalSecondaryIndex>();
      ArrayList<KeySchemaElement> statusIdxKey = new ArrayList<KeySchemaElement>();
      ArrayList<KeySchemaElement> userIdxKey = new ArrayList<KeySchemaElement>();
      statusIdxKey.add( new KeySchemaElement( D_ID_ATTRIBUTE, KeyType.HASH));
      statusIdxKey.add( new KeySchemaElement( D_STATUS_ATTRIBUTE, KeyType.RANGE));
      //statusIdxKey.add( new KeySchemaElement( D_TIMESTAMP_ATTRIBUTE, KeyType.RANGE));
      userIdxKey.add( new KeySchemaElement( D_ID_ATTRIBUTE, KeyType.HASH));
      userIdxKey.add( new KeySchemaElement( D_USER_RESPONSE_ATTRIBUTE, KeyType.RANGE));
      //userIdxKey.add( new KeySchemaElement( D_TIMESTAMP_ATTRIBUTE, KeyType.RANGE));
      indexes.add(new LocalSecondaryIndex()
          .withIndexName("Status-index")
          .withKeySchema(statusIdxKey)
          .withProjection(
              new Projection()
                  .withProjectionType(ProjectionType.KEYS_ONLY)
          )
      );
      indexes.add( new LocalSecondaryIndex()
          .withIndexName("UserResponse-index")
          .withKeySchema(userIdxKey)
          .withProjection(
              new Projection()
                  .withProjectionType(ProjectionType.KEYS_ONLY)
          )
      );

      ddbClient.createTable(new CreateTableRequest()
          .withAttributeDefinitions(attributes)
          .withKeySchema(keys)
          .withProvisionedThroughput(new ProvisionedThroughput(10L, 5L))
          .withTableName(MARVIN_DYNAMO_TABLE)
      );
  }

}