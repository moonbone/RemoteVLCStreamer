package apps.moonbone.remotevlcstreamer;

import java.util.Iterator;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class OrderedTitleList extends LinearLayout {
	
	private LinearLayout m_ll;
	private ScrollView m_sv;
	
	public OrderedTitleList(Context context,Iterator<Long> iter)
	{
		super(context);
		m_ll = new LinearLayout(context);
		m_ll.setOrientation(LinearLayout.VERTICAL);
		
		m_sv = new ScrollView(context);
		
		m_sv.addView(m_ll);
		
		setOrientation(VERTICAL);

		
		
		View t = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.music_title_layout, this,false);
		TextView tv = (TextView)t.findViewById(R.id.title_name);
		tv.setText(getResources().getString(R.string.repopulate_playlist));
		
		final MainActivity activity = (MainActivity)context;
		
		t.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				activity.reloadPlaylist();
				
			}
		});
		
		addView(t);
		addView(m_sv);
		
		repopulateTitleList(iter);
		
	}
	
	public void repopulateTitleList(Iterator<Long> iter)
	{
		m_ll.removeAllViews();
		for(;iter.hasNext();)
		{
			Long currentTitleID = iter.next();
			m_ll.addView(getTitleViewByID(currentTitleID));
		}		
	}
	
	private View getTitleViewByID(Long id)
	{
		View view = new TitleView(getContext(), id, (MainActivity)getContext());
		
		((TextView)view.findViewById(R.id.title_name)).setText(getTitleNameByID(id));
		
		return view;
	}
	
	private String getTitleNameByID(long id)
	{
		Cursor c = getContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
												new String[]{MediaStore.Audio.Media.TITLE},
												MediaStore.Audio.Media._ID + "=?",
												new String[]{Long.toString(id)},
												null);
		String ret = null;
		
		if (c.getCount() > 0)
		{
			c.moveToFirst();
			ret = c.getString(0); 
		}
		else
		{
			ret = "";
		}
		
		c.close();
		return ret;
	}
}
