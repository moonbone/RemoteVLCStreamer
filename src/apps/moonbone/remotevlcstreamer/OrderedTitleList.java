package apps.moonbone.remotevlcstreamer;

import java.util.Iterator;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OrderedTitleList extends LinearLayout {
	
	public OrderedTitleList(Context context,Iterator<Long> iter)
	{
		super(context);
		setOrientation(VERTICAL);
		
		repopulateTitleList(iter);
		
	}
	
	public void repopulateTitleList(Iterator<Long> iter)
	{
		removeAllViews();
		for(;iter.hasNext();)
		{
			Long currentTitleID = iter.next();
			addView(getTitleViewByID(currentTitleID));
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
