package apps.moonbone.remotevlcstreamer;

import java.util.TreeSet;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class MusicTabFragment extends Fragment 
{
	protected TreeSet<Long> m_chosen;
	

	
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		m_chosen = ((MainActivity)getActivity()).getChosenTitlesSet();
		
		View view = inflater.inflate(R.layout.fragment_music_tab,container,false);
		
		Cursor artistsCursor = getActivity().getContentResolver().query(Audio.Artists.EXTERNAL_CONTENT_URI, new String[]{Audio.Artists.ARTIST,Audio.Artists._ID}, null, null,Audio.Artists.ARTIST);
		
		SimpleCursorAdapter adapter = new ArtistsSimpleCursorAdapter(view.getContext(), R.layout.music_artist_list_item_layout, artistsCursor, new String[]{Audio.Artists.ARTIST},new int[]{R.id.artistName},0,this );
		
		ListView lView = (ListView)view.findViewById(R.id.artistsLists);
		lView.addHeaderView(new AllAlbumsView(view.getContext(),this));
		lView.setAdapter(adapter);
		
		
		return view;
	}
	public class ArtistsSimpleCursorAdapter extends SimpleCursorAdapter
	{
		private TreeSet<Long> m_expanded;
		private MusicTabFragment m_mtf; 

		public ArtistsSimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to,int flags, MusicTabFragment mtf)
		{
			super(context, layout, c, from, to,flags);
			m_expanded = new TreeSet<Long>();
			m_mtf = mtf;
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
			ArtistView av = new ArtistView(context,m_mtf);
			return av;
		}
		
		
	}
	
	
	

	
}

