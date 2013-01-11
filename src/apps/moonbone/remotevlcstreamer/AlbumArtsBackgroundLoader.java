package apps.moonbone.remotevlcstreamer;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

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
    		
    		Log.d("Thread","Started.");
    		sleep(350);
    		
        	Cursor albumsCursor = m_resolver.query(m_albumsURI, new String[]{Audio.Albums.ALBUM_ART},null,null,null);
			albumsCursor.moveToFirst();
			int childNumber = 1;
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

				Message message = m_UIHandler.obtainMessage(childNumber, new Object[]{m_context,iv});
	            m_UIHandler.sendMessage(message);
	            
				
				albumsCursor.moveToNext();
				++childNumber;
				--m_spaceLeft;
			}
			albumsCursor.close();
    	}
    	catch(InterruptedException e)
    	{
    		Log.d("Thread","Interrupted: "+ e.toString());
    		return;
    	}
    	Log.d("Thread","Finished.");
   }
}

