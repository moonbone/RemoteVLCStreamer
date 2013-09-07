package apps.moonbone.remotevlcstreamer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.TextView;
import android.widget.VerticalSeekBar;

public class VLCControlFragment extends Fragment {
	
	private VlcPlayerInterface m_mpInterface;
	private PermanentMediaController m_pmc;
	private Handler m_handler;
	private View m_view;
	private TextViewRefresher m_textRefresher;
	
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
	
	public void setVlcPlayerInterface(VlcPlayerInterface vlcInterface)
	{
		m_mpInterface = vlcInterface;
		
		m_pmc.setMediaPlayer(m_mpInterface); 
		
		m_mpInterface.setVolumeBarView((VerticalSeekBar)m_view.findViewById(R.id.volumeBar));				
		
		m_handler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				((TextView)m_view.findViewById(R.id.currentTitleNameText)).setText((String)msg.obj);
			}
		};
		
		//populatePlaylist();
		
		m_textRefresher = new TextViewRefresher();
		m_textRefresher.start();
	}

	@Override
	public void onDestroyView() {
		
		if (null != m_textRefresher)
		{
			m_textRefresher.interrupt();
		}
		
		super.onDestroyView();
		
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		
		m_pmc = new PermanentMediaController(getActivity());
		
		
		
		m_pmc.setEnabled(true);
		
		m_pmc.setPrevNextListeners(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						m_mpInterface.next();						
					}
				}
				, 
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						m_mpInterface.prev();						
					}
				}
		);
		
		super.onCreateView(inflater, container, savedInstanceState);
		
		m_view = (RelativeLayout)inflater.inflate(R.layout.fragment_vlc_control_tab, container,false);
		

		m_pmc.setAnchorView(m_view);
		
		((Switch)m_view.findViewById(R.id.shuffleSwitch)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				m_mpInterface.setShuffle(isChecked);
			}
		});
		
		((Switch)m_view.findViewById(R.id.repeatSwitch)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				m_mpInterface.setRepeat(isChecked);
			}
		});
		

		((VerticalSeekBar)m_view.findViewById(R.id.volumeBar)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
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
		
				
		
		
		
		return m_view;
	}
	
	public void showControls()
	{
		m_pmc.show(1);
	}
	
	public void hideControls() {
		
		m_pmc.realHide();
		
	}
	
	public void populatePlaylist()
	{
		m_mpInterface.populatePlaylist(((MainActivity)getActivity()).getChosenTitlesSet());
		
	}
	
	private class TextViewRefresher extends Thread
	{
		@Override
		public void run() {
			super.run();
			
			while (!isInterrupted())
			{
				m_handler.sendMessage(m_handler.obtainMessage(0, m_mpInterface.getCurrentPlayingTitleName()));
				try{sleep(1000);}catch(InterruptedException e){return;}
			}
		}
		
	}
	
	private class PermanentMediaController extends MediaController
	{
		private boolean m_isHidden;
		public PermanentMediaController(Context context) {
			super(context,false);

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
