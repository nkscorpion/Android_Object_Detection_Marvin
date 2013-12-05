package com.tstine.marvinas.util;
import java.util.UUID;
import java.util.Date;
import java.util.Random;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.Context;

public class Installation{
	private static String sId = null;
	private static final String INSTALLATION = "com.tstine.marvinas_INSTALLATION";
	private static Context mContext = null;
	public static Random randomGen = new Random();
	

	public synchronized static String getId(){
		if( mContext == null ) throw new RuntimeException("Null context in install");
		if( sId == null ){
			File installation = new File( mContext.getFilesDir(), INSTALLATION );
			try{
				if( !installation.exists() )
					writeInstallationFile( installation );
				sId = readInstallationFile( installation );
			}catch (Exception e){
				throw new RuntimeException( e );
			}
		}
		return sId;
	}

	private static String readInstallationFile( File installation ) throws IOException{
		RandomAccessFile f = new RandomAccessFile( installation, "r" );
		byte[] bytes = new byte[(int) f.length()];
		f.readFully(bytes);
		f.close();
		return new String(bytes);
	}
	
	private static void writeInstallationFile( File installation) throws IOException{
		FileOutputStream out = new FileOutputStream( installation );
		String id = UUID.randomUUID().toString();
		out.write( id.getBytes() );
		out.close();
	}
	
	public static void setContext( Context ctx ){mContext = ctx;}

	public static  Date getTimestamp(){
      return new Date();
	}

}