package com.tstine.marvinas;
import java.io.Serializable;

public class Request implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final String id;
	private final String imagePath;
  private final String message;
	private final String installId;
	private final String timestamp;

	public Request( final String imagePath, final String installId,
									final String timestamp, final String message ){
		this.imagePath = imagePath;
		this.installId = installId;
		this.timestamp = timestamp;
		this.id = installId + "_" + Installation.randomGen.nextInt(100)+ "_" + timestamp;
    this.message = message;
	}

  public String getMessage(){return this.message;}
  public String getInstallId(){return this.installId;}
	public String getId(){return this.id;}
	public String getImagePath(){return this.imagePath;}
  public String getTimestamp(){return this.timestamp;}
}