package apps.moonbone.remotevlcstreamer;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AlbumView extends LinearLayout
{
	protected long m_albumID;
	protected boolean m_expanded;
	protected MusicTabFragment m_mtf;
	
	AlbumView(Context context,MusicTabFragment mtf)
	{
		super(context);
		m_expanded = false;
		m_mtf = mtf;
		
		((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.music_artist_album_list_item_layout, this,true);
		
		setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v) {
				flipState();
			}
		});

	}

	public long getAlbumID() {
		return m_albumID;
	}

	public void setAlbumID(long albumID) {
		m_albumID = albumID;
		Cursor c = getContext().getContentResolver().query(Audio.Albums.EXTERNAL_CONTENT_URI,new String[]{Audio.Albums.ALBUM_ART},Audio.Albums._ID + " =?",new String[]{ Long.toString(m_albumID)},null);
		if(c.getCount() > 0 )
		{
			c.moveToFirst();
								
			((ImageView)findViewById(R.id.albumArt)).setImageDrawable(Drawable.createFromPath(c.getString(0)));
			
			c.close();
		}
	}

	public boolean isExpanded() {
		return m_expanded;
	}

	public void setExpanded(boolean expanded) {
		m_expanded = expanded;
	}
	
	public void flipState()
	{
		m_expanded = m_expanded ? false : true;
		
		refreshState();
	}
	
	public void refreshState()
	{
		if (m_expanded)
		{
			populateTitlesList();
		}
		else
		{
			hideTitlesList();
		}
		invalidate();
	}
	protected void populateTitlesListImpl(Cursor cursor)
	{
		LinearLayout albumLayout = (LinearLayout)findViewById(R.id.albumsTitles);
		
		for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext())
		{

			View titleView = new TitleView(getContext(), cursor.getLong(1), (MainActivity)m_mtf.getActivity());//LayoutInflater.from(getContext()).inflate(R.layout.music_title_layout,albumLayout,false);
			((TextView)titleView.findViewById(R.id.title_name)).setText(cursor.getString(0));
			
			albumLayout.addView(titleView);
		}
	}
	
	public void populateTitlesList()
	{
		((TextView)findViewById(R.id.albumName)).setBackgroundColor(getResources().getColor(R.color.chosen_album_background));
		((LinearLayout)findViewById(R.id.albumsTitles)).removeAllViews();

		Cursor titlesCursor = getContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				new String[]{Audio.Media.TITLE, Audio.Media._ID,Audio.Media.ALBUM_ID},
				Audio.Media.ALBUM_ID + " =?",
				new String[]{Long.toString(m_albumID)},
				Audio.Media.TRACK
				);
		
		populateTitlesListImpl(titlesCursor);
							
		titlesCursor.close();
	}
	
	public void hideTitlesList()
	{
		((TextView)findViewById(R.id.albumName)).setBackgroundColor(getResources().getColor(R.color.unchosen_album_background));
		((LinearLayout)findViewById(R.id.albumsTitles)).removeAllViews();
	}
	
	
	
}

