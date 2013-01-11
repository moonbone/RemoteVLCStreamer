package apps.moonbone.remotevlcstreamer;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.VerticalSeekBar;

public class VLCControlFragment extends Fragment {
	
	private VlcPlayerInterface m_mpInterface;
	private PermanentMediaController m_pmc;
	
	enum ControlStatus 
	{
		IS_PLAYING,
		TOTAL_LENGTH,
		CURRENT_POSITION,
		IS_RANDOM,
		IS_REPEAT,
		VOLUME,
	}
	public VLCControlFragment()
	{
		
	}
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = (RelativeLayout)inflater.inflate(R.layout.fragment_vlc_control_tab, container,false);
		m_pmc = new PermanentMediaController(container.getContext());
		
		m_mpInterface = new VlcPlayerInterface(getActivity());
		m_pmc.setMediaPlayer(m_mpInterface); 
		m_pmc.setEnabled(true);

		m_pmc.setAnchorView(view);
				
		((Switch)view.findViewById(R.id.shuffleSwitch)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				m_mpInterface.setShuffle(isChecked);
			}
		});
		
		((Switch)view.findViewById(R.id.repeatSwitch)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				m_mpInterface.setRepeat(isChecked);
			}
		});
		
		((VerticalSeekBar)view.findViewById(R.id.volumeBar)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			private int _progressToSet;
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				m_mpInterface.setVolume(_progressToSet);
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				//Does nothing.
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				_progressToSet = progress;
			}
		});
		
		m_mpInterface.setVolumeBarView((VerticalSeekBar)view.findViewById(R.id.volumeBar));				
		populatePlaylist();
		
		return view;
	}
	
	public void showControls()
	{
		m_pmc.show(1);
	}
	
	public void hideControls() {
		
		m_pmc.realHide();
		
	}
	
	private void populatePlaylist()
	{
		m_mpInterface.populatePlaylist(((MainActivity)getActivity()).getChosenTitlesSet());
		
	}
	
	private class PermanentMediaController extends MediaController
	{
		private boolean m_isHidden;
		public PermanentMediaController(Context context) {
			super(context);

		}
		
		public void hide()
		{
			if(!m_isHidden)
			{
				show();
			}
		}
		public void show(int i)
		{
			super.show(i);
			m_isHidden = false;
		}
		public void realHide()
		{
			super.hide();
			m_isHidden = true;
		}
		
	}
	
	
	    
}
