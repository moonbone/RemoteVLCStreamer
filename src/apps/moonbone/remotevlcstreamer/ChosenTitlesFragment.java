package apps.moonbone.remotevlcstreamer;

import java.util.Iterator;
import java.util.TreeSet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChosenTitlesFragment extends Fragment {
	
	private TreeSet<Long> m_chosenTitles;
	private OrderedTitleList m_view;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		m_chosenTitles = ((MainActivity)getActivity()).getChosenTitlesSet();
		
		m_view = new OrderedTitleList(container.getContext(),m_chosenTitles.iterator());
				
		return m_view;
	}
	
	public void repopulateFragmentView()
	{
		if(null == m_view)
		{
			return;
		}
		
		m_view.repopulateTitleList(m_chosenTitles.iterator());
		
		
	}
	
}
