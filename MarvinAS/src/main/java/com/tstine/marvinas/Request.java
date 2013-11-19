package com.tstine.marvinas;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Request implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final String id;
	private final String imagePath;
  private final String message;
	private final String installId;
	private final Date timestamp;
    private final String imageName;

	public Request( final String imagePath, final String imageName,
                  final String installId,
									final Date timestamp, final String message ){
		this.imagePath = imagePath;
      this.imageName = imageName;
		this.installId = installId;
		this.timestamp = timestamp;
		this.id = installId + "_" + Installation.randomGen.nextInt(100)+ "_" + timestamp;
    this.message = message;
	}

  public String getMessage(){return this.message;}
  public String getInstallId(){return this.installId;}
	public String getId(){return this.id;}
	public String getImagePath(){return this.imagePath;}
    public String getImageName(){return this.imageName;}
    public String getTimestamp(){return getTimestamp("yyyyMMddHHmmssSSS");}
  public String getTimestamp(String format){
      return new SimpleDateFormat(format).format(
          this.timestamp);
  }

}