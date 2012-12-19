package apps.moonbone.remotevlcstreamer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ArtistView extends LinearLayout
	{
		protected boolean m_expanded;
		protected Uri m_albumsURI;
		protected long m_artistID;
		protected Thread m_thread;
		protected LinearLayout m_row;
		protected Handler m_handler;

		public ArtistView(Context context) {
			super(context);

			m_expanded = false;

			((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.music_artist_list_item_layout, this,true);
			
			setOnClickListener(new OnClickListener() 
			{
				
				@Override
				public void onClick(View v) {
					Log.d("Artist ID",Long.toString(m_artistID));
					flipState();					
				}
			});

		}
		public void setArtistID(long artistId)
		{
			m_albumsURI = MediaStore.Audio.Artists.Albums.getContentUri("external", artistId);
			m_artistID = artistId;
		}
		public long getArtistID()
		{
			return m_artistID;
		}
		
		public void setExpandedState(boolean newState)
		{
			m_expanded = newState;
		}
		public boolean getExpandedState()
		{
			return m_expanded;
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
				populateAlbumsList();
			}
			else
			{
				hideAlbumsList();
			}
			
			try
			{
				showAlbumsArts();
			}
			catch(Exception e)
			{
				LinearLayout row = (LinearLayout)findViewById(R.id.artistAlbumArts);
				row.removeViews(1, row.getChildCount()-1);
			}
		}
		public void terminateThread()
		{
			if(null != m_thread)
			{
				m_thread.interrupt();
			}
			if(null != m_handler)
			{
				m_handler.removeMessages(0);
			}
			
		}
		protected void showAlbumsArts()
		{
			m_row = (LinearLayout)findViewById(R.id.artistAlbumArts);
			m_row.removeViews(1, m_row.getChildCount()-1);
			
			Log.d("showAlbums",Long.toString(m_artistID) + " " + ((TextView)m_row.getChildAt(0)).getText().toString());
			
			long availableSize = m_row.getMeasuredWidth() - (long)((TextView)m_row.getChildAt(0)).getPaint().measureText(((TextView)m_row.getChildAt(0)).getText().toString()) - 120;
			int numberOfArtsToShow = (int)(availableSize / m_row.getMeasuredHeight());
			Log.d("numberOfArtsToShow",Integer.toString(numberOfArtsToShow));
			if(0 < numberOfArtsToShow)
			{
				m_handler = new Handler() {
		            @Override
		            public void handleMessage(Message message)
		            {
		            	Log.d("what",Long.toString(message.what));

		            	LinearLayout _row = (LinearLayout)((Object[])message.obj)[0];
		            	ImageView _iv = (ImageView)((Object[])message.obj)[1];
		           		_row.addView(_iv);
		       
		            }
		           
		        };
		       		        
		        Log.d("BLA","Creating Thread.");
		        m_thread = new AlbumArtsBackgroundLoader(m_handler, m_albumsURI, numberOfArtsToShow,getContext().getContentResolver() , m_row);
		        Log.d("BLA","Starting Thread.");
		        m_thread.start();
		        //try{m_thread.join();}catch (InterruptedException e){}
			}
		}

		public void addAllTitlesView()
		{
			LinearLayout artistLayout = (LinearLayout)findViewById(R.id.artistsAlbums);
			AllTitlesView aav = new AllTitlesView(getContext());
			aav.setArtistID(m_artistID);
			((TextView)aav.findViewById(R.id.albumName)).setText(R.string.all_titles);
			
			artistLayout.addView(aav);
		}
		

		public void populateAlbumsList()
		{
			((TextView)findViewById(R.id.artistName)).setBackgroundColor(getResources().getColor(R.color.chosen_artist_background));
			((LinearLayout)findViewById(R.id.artistsAlbums)).removeAllViews();
			
			addAllTitlesView();
			
			Cursor albumsCursor = getContext().getContentResolver().query(m_albumsURI, new String[]{Audio.Albums.ALBUM, Audio.Albums._ID},null,null,Audio.Albums.ALBUM);
			
			LinearLayout artistLayout = (LinearLayout)findViewById(R.id.artistsAlbums);
			
			for(albumsCursor.moveToFirst();!albumsCursor.isAfterLast();albumsCursor.moveToNext())
			{
				View albumView = new AlbumView(getContext());//LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1,artistLayout,false);
				((TextView)albumView.findViewById(R.id.albumName)).setText(albumsCursor.getString(0));
				((AlbumView)albumView).setAlbumID(albumsCursor.getLong(1));
				
				artistLayout.addView(albumView);
			}
								
			albumsCursor.close();
		}
		
		public void hideAlbumsList()
		{
			((TextView)findViewById(R.id.artistName)).setBackgroundColor(getResources().getColor(R.color.unchosen_artist_background));
			((LinearLayout)findViewById(R.id.artistsAlbums)).removeAllViews();
		}
		
		
		
		
	}