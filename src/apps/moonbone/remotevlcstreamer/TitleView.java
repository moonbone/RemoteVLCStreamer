package apps.moonbone.remotevlcstreamer;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class TitleView extends LinearLayout 
{
	protected long m_titleID;
	protected MainActivity m_ma;

	
	public TitleView(Context context,long id,MainActivity mtf)
	{
		super(context);
		m_titleID = id;
		m_ma = mtf;
		
		((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.music_title_layout, this,true);
		
		refreshView();
		
		setOnClickListener(new OnClickListener() 
		{
			
			@Override
			public void onClick(View v) {
				Log.d("Title ID",Long.toString(m_titleID));
				flipState();					
			}
		});
	}
	
	protected void flipState()
	{
		if(getChosenState())
		{
			m_ma.setChosenState(m_titleID,false);
		}
		else
		{
			m_ma.setChosenState(m_titleID,true);
		}
		refreshView();
	}
	
	protected void refreshView()
	{
		if (getChosenState())
		{
			setBackgroundColor(getResources().getColor(R.color.chosen_title_background));
		}
		else
		{
			setBackgroundColor(getResources().getColor(R.color.unchosen_title_background));
		}
	}
	
	protected boolean getChosenState()
	{
		return m_ma.getChosenState(m_titleID);
	}

}
