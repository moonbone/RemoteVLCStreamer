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
	
	
	

	
}

