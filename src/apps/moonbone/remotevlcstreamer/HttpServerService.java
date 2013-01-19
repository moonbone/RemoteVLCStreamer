package apps.moonbone.remotevlcstreamer;

import java.io.File;

import org.jibble.simplewebserver.SimpleWebServer;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

public class HttpServerService extends Service {

	SimpleWebServer m_httpServer;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try
		{
			if(null == m_httpServer)
			{
				Cursor c = getApplicationContext().getContentResolver().query(
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						new String[]{MediaStore.Audio.Media.DATA},
						null,
						null,
						null);
				
				c.moveToFirst();
				String sdPath = c.getString(0).split("/")[1];
				c.close();
				m_httpServer = new SimpleWebServer(new File(String.format("/%s/",sdPath)), 8080);
			}
		}
		catch (/*IO*/Exception e)
		{
			Log.d("HTTP Service","Exception Caught!");
		}
		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		
		m_httpServer.interrupt();
		
		super.onDestroy();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
	}

	

}
