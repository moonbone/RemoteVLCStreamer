package apps.moonbone.remotevlcstreamer;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AllTitlesView extends AlbumView
{
	private long m_artistID;
	
	AllTitlesView(Context context) {
		super(context);
		m_albumID = 0;
	}
	
	public long getArtistID() {
		return m_artistID;
	}
	public void setArtistID(long artistID) {
		m_artistID = artistID;
	}
	
	public void populateTitlesList()
	{
		((TextView)findViewById(R.id.albumName)).setBackgroundColor(getResources().getColor(R.color.chosen_album_background));
		((LinearLayout)findViewById(R.id.albumsTitles)).removeAllViews();
		
		Cursor albumsCursor;
		if (0 < m_artistID) 
		{
			albumsCursor = getContext().getContentResolver().query(Audio.Artists.Albums.getContentUri("external", m_artistID), new String[]{Audio.Albums.ALBUM, Audio.Albums._ID},null,null,null);
		}
		else
		{
			albumsCursor = getContext().getContentResolver().query(Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{Audio.Albums.ALBUM, Audio.Albums._ID},null,null,null);
		}
		StringBuilder ids = new StringBuilder();
		for (albumsCursor.moveToFirst();!albumsCursor.isAfterLast();albumsCursor.moveToNext())
		{
			ids.append(albumsCursor.getLong(albumsCursor.getColumnIndex(Audio.Albums._ID)));
			ids.append(',');
		}
		ids.deleteCharAt(ids.length()-1);
		Log.d("ALBUMS",ids.toString());
		Cursor titlesCursor = getContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				new String[]{Audio.Media.TITLE, Audio.Media._ID,Audio.Media.ALBUM_ID},
				Audio.Media.ALBUM_ID + " in ("+ ids.toString() +")",
				null,//new String[]{},
				albumsCursor.getCount() > 1 ? Audio.Media.TITLE : Audio.Media.TRACK
				);
		Log.d("ALBUMS",Long.toString(titlesCursor.getCount()));
		populateTitlesListImpl(titlesCursor);
		titlesCursor.close();
		albumsCursor.close();

		
	}
}