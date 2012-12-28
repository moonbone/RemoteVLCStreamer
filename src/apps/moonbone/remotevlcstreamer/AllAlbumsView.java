package apps.moonbone.remotevlcstreamer;
import android.content.Context;
import android.provider.MediaStore.Audio;
import android.widget.TextView;

public class AllAlbumsView extends ArtistView
{
	public AllAlbumsView(Context context,MusicTabFragment mtf) {
		super(context,mtf);
		((TextView)findViewById(R.id.artistName)).setText(R.string.all_albums);
		m_artistID = 0;
		m_albumsURI = Audio.Albums.EXTERNAL_CONTENT_URI;
		
		
	}

	
}