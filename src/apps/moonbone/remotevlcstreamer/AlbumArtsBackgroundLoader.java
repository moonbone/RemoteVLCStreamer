package apps.moonbone.remotevlcstreamer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.support.v4.app.Fragment;

public class AlbumArtsBackgroundLoader extends Thread {

	private Handler m_UIHandler;
	private Uri m_albumsURI;
	private int m_spaceLeft;
	private ContentResolver m_resolver;
	private LinearLayout m_context;
	
	public AlbumArtsBackgroundLoader(Handler UIHandler,Uri albumsUri,int maxNumberOfAlbumArts,ContentResolver resolver,LinearLayout context) {
		super();
		m_UIHandler = UIHandler;
		m_albumsURI = albumsUri;
		m_spaceLeft = maxNumberOfAlbumArts;		
		m_resolver = resolver;
		m_context = context;
	}
	
	public void run() {
    	try
    	{
    		
    		Log.d("thread","Started.");
    		sleep(350);
    		
        	Cursor albumsCursor = m_resolver.query(m_albumsURI, new String[]{Audio.Albums.ALBUM_ART},null,null,null);
			albumsCursor.moveToFirst();
			while (0 < m_spaceLeft && !albumsCursor.isAfterLast())
			{
				if (isInterrupted())
				{
					albumsCursor.close();
					return;
				}
				ImageView iv = new ImageView(m_context.getContext());
				iv.setAdjustViewBounds(true);
				iv.setImageDrawable(Drawable.createFromPath(albumsCursor.getString(0)));
				//fetchDrawableOnThread(albumsCursor.getString(0), iv);
				Message message = m_UIHandler.obtainMessage(0, new Object[]{m_context,iv});
	            m_UIHandler.sendMessage(message);
				//row.addView(iv);
				
				albumsCursor.moveToNext();
				--m_spaceLeft;
			}
			albumsCursor.close();
    	}
    	catch(InterruptedException e)
    	{
    		Log.d("thread","Interrupted: "+ e.toString());
    		return;
    	}
    	Log.d("thread","Finished.");
   }
}

