package apps.moonbone.remotevlcstreamer;

import java.util.TreeSet;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MusicTabFragment extends Fragment 
{
	private TreeSet<Long> m_chosen;
	
	public void onActivityCreated(Bundle b)
	{
		super.onActivityCreated(b);
		getView().invalidate();
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		m_chosen = new TreeSet<Long>();
		
		View view = inflater.inflate(R.layout.fragment_music_tab,container,false);
		
		Cursor artistsCursor = getActivity().getContentResolver().query(Audio.Artists.EXTERNAL_CONTENT_URI, new String[]{Audio.Artists.ARTIST,Audio.Artists._ID}, null, null,Audio.Artists.ARTIST);
		
		SimpleCursorAdapter adapter = new ArtistsSimpleCursorAdapter(view.getContext(), R.layout.music_artist_list_item_layout, artistsCursor, new String[]{Audio.Artists.ARTIST},new int[]{R.id.artistName},0 );
		
		ListView lView = (ListView)view.findViewById(R.id.artistsLists);
		lView.addHeaderView(new AllAlbumsView(view.getContext()));
		lView.setAdapter(adapter);
		
		
		return view;
	}
	public class ArtistsSimpleCursorAdapter extends SimpleCursorAdapter
	{
		private TreeSet<Long> m_expanded;

		public ArtistsSimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to,int flags)
		{
			super(context, layout, c, from, to,flags);
			m_expanded = new TreeSet<Long>();
		}
		
		//	@Override
		public View getView(int position , View convertView , ViewGroup parent)
		{
			
			if (!getCursor().moveToPosition(position)) {
	            throw new IllegalStateException("couldn't move cursor to position " + position);
	        }
			ArtistView v;
	        if (convertView == null)
	        {
	        	Log.d("View","creating new");
	            v = (ArtistView)newView(parent.getContext(), getCursor(), parent);
				v.setArtistID(getCursor().getLong(1));
	        } 
	        else 
	        {
	        	Log.d("View","converting existing.");
	        	v = (ArtistView)convertView;
	        	v.terminateThread();
	        	Long aID = Long.valueOf(v.getArtistID());
	        	if (v.getExpandedState())
	        	{
	        		m_expanded.add(aID);
	        	}
	        	else
	        	{
	        		m_expanded.remove(aID);
	        	}
	        	
	            v.setArtistID(getCursor().getLong(1));
	            
	            v.setExpandedState(m_expanded.contains(Long.valueOf(getCursor().getLong(1))));
	        }
	        bindView(v, parent.getContext(), getCursor());
	        v.refreshState();
	      
	        return v;
		    
		}
		
		public View newView(Context context, Cursor c, ViewGroup vg)
		{
			ArtistView av = new ArtistView(context);
			return av;
		}
		
		
	}
	
	public class AllAlbumsView extends ArtistView
	{
		public AllAlbumsView(Context context) {
			super(context);
			((TextView)findViewById(R.id.artistName)).setText(R.string.all_albums);
			m_artistID = 0;
			m_albumsURI = Audio.Albums.EXTERNAL_CONTENT_URI;
			
			
		}

		
	}
	
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
		        m_thread = new AlbumArtsBackgroundLoader(m_handler, m_albumsURI, numberOfArtsToShow,getActivity().getContentResolver() , m_row);
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
			
			Cursor albumsCursor = getActivity().getContentResolver().query(m_albumsURI, new String[]{Audio.Albums.ALBUM, Audio.Albums._ID},null,null,Audio.Albums.ALBUM);
			
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
				if (0 < ArtistView.this.m_artistID) 
				{
					albumsCursor = getActivity().getContentResolver().query(Audio.Artists.Albums.getContentUri("external", m_artistID), new String[]{Audio.Albums.ALBUM, Audio.Albums._ID},null,null,null);
				}
				else
				{
					albumsCursor = getActivity().getContentResolver().query(Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{Audio.Albums.ALBUM, Audio.Albums._ID},null,null,null);
				}
				StringBuilder ids = new StringBuilder();
				for (albumsCursor.moveToFirst();!albumsCursor.isAfterLast();albumsCursor.moveToNext())
				{
					ids.append(albumsCursor.getLong(albumsCursor.getColumnIndex(Audio.Albums._ID)));
					ids.append(',');
				}
				ids.deleteCharAt(ids.length()-1);
				Log.d("ALBUMS",ids.toString());
				Cursor titlesCursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
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
		
		public class AlbumView extends LinearLayout
		{
			protected long m_albumID;
			protected boolean m_expanded;
			
			AlbumView(Context context)
			{
				super(context);
				m_expanded = false;	
				
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
				Cursor c = getActivity().getContentResolver().query(Audio.Albums.EXTERNAL_CONTENT_URI,new String[]{Audio.Albums.ALBUM_ART},Audio.Albums._ID + " =?",new String[]{ Long.toString(m_albumID)},null);
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

					View titleView = LayoutInflater.from(getContext()).inflate(R.layout.music_title_layout,albumLayout,false);
					((TextView)titleView.findViewById(R.id.title_name)).setText(cursor.getString(0));
					titleView.setOnClickListener(new OnClickListener()
					{
						
						@Override
						public void onClick(View v) {
							Toast.makeText(getContext(),"bla", Toast.LENGTH_SHORT).show();	
						}
					});
					
					
					albumLayout.addView(titleView);
				}
			}
			
			public void populateTitlesList()
			{
				((TextView)findViewById(R.id.albumName)).setBackgroundColor(getResources().getColor(R.color.chosen_album_background));
				((LinearLayout)findViewById(R.id.albumsTitles)).removeAllViews();

				Cursor titlesCursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
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
	}
	
}

