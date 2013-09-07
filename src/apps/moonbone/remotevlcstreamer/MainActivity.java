package apps.moonbone.remotevlcstreamer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.TreeSet;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.EditText;

public class MainActivity extends FragmentActivity {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;
	
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	
	TreeSet<Long> m_chosenTitles;
	
	//Fragments:
	ChosenTitlesFragment m_chosenTab;
	VLCControlFragment m_vlcFragment;
	MusicTabFragment m_musicFragment;
	
	VlcPlayerInterface m_vlcInterface;
	
	
	HttpServerService m_httpServer;
	public boolean m_paused;
	String m_vlcServerIP;
	Thread m_ipScannerThread;
	
	PowerManager.WakeLock m_wl;
	
	public Object getVLCServerIP() {
		return m_vlcServerIP;
	}
	
	public boolean getChosenState(long id)
	{
		return m_chosenTitles.contains(Long.valueOf(id));
	}
	
	public void setChosenState(long id,boolean state)
	{
		if(state)
		{
			m_chosenTitles.add(Long.valueOf(id));
		}
		else
		{
			m_chosenTitles.remove(Long.valueOf(id));
		}
	}
	
	public TreeSet<Long> getChosenTitlesSet()
	{
		return m_chosenTitles;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	        Log.d("MENU", "MENU pressed");
	        getServerIPfromUser();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		m_chosenTitles = new TreeSet<Long>();
		
		startService(new Intent(this,HttpServerService.class));

		setContentView(R.layout.activity_main);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		
		getServerIPfromUser();
		
		PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
		m_wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VLCRemoteStreamer");
		m_wl.acquire();
		
	}
	
	@Override
	protected void onPause()
	{
		if(null != m_vlcInterface)
		{
			m_vlcInterface.stopThreads();
		}
		
		super.onPause();
		
	}
	
	@Override
	public void onResume()
	{
		
		super.onPause();
		
		if(null != m_vlcInterface)
		{
			try
			{
				m_vlcInterface.startThreads();
			}
			catch(Exception e)
			{
				//TODO: Handle specific exception instead.
			}
		}
	
		
	}
	
	@SuppressLint("Wakelock")
	@Override
	public void onDestroy()
	{
		stopService(new Intent(this,HttpServerService.class));
		
		m_wl.release();
		
		super.onDestroy();	
		
	}
	
	private void getServerIPfromUser() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("VLC IP");
		alert.setMessage("Choose server IP");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		final Handler textEditHandler = new Handler()
		{

			@Override
			public void handleMessage(Message msg) {
				if(0 == msg.what)
				{
					input.setText((String)msg.obj);
				}
				else if (1 == msg.what)
				{
					input.setText(String.format("Searching for server: %d/255",(Integer)msg.obj));
					
				}
			}
			
		};
		
		if(null != m_ipScannerThread)
		{
			m_ipScannerThread.interrupt();
		}
		
		m_ipScannerThread = new Thread() 
		{

			@Override
			public void run() {
				
				HttpParams httpParameters = new BasicHttpParams();
				// Set the timeout in milliseconds until a connection is established.
				// The default value is zero, that means the timeout is not used. 
				int timeoutConnection = 200;
				HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
				// Set the default socket timeout (SO_TIMEOUT) 
				// in milliseconds which is the timeout for waiting for data.
				int timeoutSocket = 200;
				HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
				
				DefaultHttpClient client = new DefaultHttpClient(httpParameters);
				
				
				WifiManager wim = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				
				//Only support IPv4 at the moment
				@SuppressWarnings("deprecation")
				String networkSubnet = Formatter.formatIpAddress( wim.getDhcpInfo().ipAddress & wim.getDhcpInfo().netmask);

				for (int host = 1; host < 255 ; ++host)
				{
					if(isInterrupted())
					{
						return;
					}
					try 
					{
						textEditHandler.sendMessage(textEditHandler.obtainMessage(1, Integer.valueOf(host) ));
						
						HttpResponse res = client.execute(new HttpGet(new URI(String.format("http://%s:8080/requests/status.xml",networkSubnet.replaceAll("\\.0$", "."+Integer.toString(host))))));
						res.getEntity().writeTo(new NullOutputStream());
						
						if(200 == res.getStatusLine().getStatusCode())
						{
							textEditHandler.sendMessage(textEditHandler.obtainMessage(0, networkSubnet.replaceAll("\\.0$", "."+Integer.toString(host)) ));
							return;
						}
						
					}
					catch (IOException e)
					{
						Log.d("IP_SCAN", "failed " + Integer.toString(host));
					}
					catch (URISyntaxException e)
					{
						Log.d("IP_SCAN","URI exception " + Integer.toString(host));
					}
				}
				
				textEditHandler.sendMessage(textEditHandler.obtainMessage(0, networkSubnet.replaceAll("\\.0$", ".")));
			}
			
		};
		
		m_ipScannerThread.start();
		
		alert.setView(input);

		alert.setPositiveButton(R.string.ip_choose_OK, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				
				String value = input.getText().toString();
				m_vlcServerIP = value;
				if(null != m_vlcInterface)
				{
					m_vlcInterface.stopThreads();
				}
				 
				m_vlcInterface = new VlcPlayerInterface(MainActivity.this);
				  
				m_vlcInterface.readPlaylist(getChosenTitlesSet());
				
				m_vlcInterface.findMaxVolume();
	  
	
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    m_ipScannerThread.interrupt();
		  }
		});

		alert.show();
		
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		
		mViewPager.setOnPageChangeListener(new OnPageChangeListener()
		{

			@Override
			public void onPageScrollStateChanged(int arg0) {

				
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {

				
			}

			@Override
			public void onPageSelected(int arg0) {
				if(null != m_chosenTab && arg0 == 1)
				{
					//m_vlcInterface.readPlaylist(getChosenTitlesSet());
					m_chosenTab.repopulateFragmentView();
				}
				if(null != m_vlcFragment && arg0 != 2)
				{
					m_vlcFragment.hideControls();
				}
				if(null != m_vlcFragment && arg0 == 2)
				{
					m_vlcFragment.setVlcPlayerInterface(m_vlcInterface);
					m_vlcFragment.showControls();					
				}
			}
			
		});
		return true;
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		
		
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0)
			{
				if(null == m_musicFragment)
				{
					m_musicFragment = new MusicTabFragment();
				}
				
				return m_musicFragment;
				
			}
			else if(position == 1)
			{
				if(null == m_chosenTab)
				{
					m_chosenTab = new ChosenTitlesFragment();
				}
				
				return m_chosenTab;
			}
			else if (position == 2)
			{
				if(null == m_vlcFragment)
				{
					m_vlcFragment = new VLCControlFragment();
				}
				
				return m_vlcFragment;
			}
			
			throw new IndexOutOfBoundsException("requested an invalid tab fragment. "+ Integer.toString(position));

		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}
		
		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.title_fragment_music_tab).toUpperCase(Locale.ENGLISH);
			case 1:
				return getString(R.string.title_fragment_chosen_titles_tab).toUpperCase(Locale.ENGLISH);
			case 2:
				return getString(R.string.title_fragment_vlc_control_tab).toUpperCase(Locale.ENGLISH);
			}
			return null;
		}
	}

	public void reloadPlaylist() {
		if(null != m_vlcInterface)
		{
			m_vlcInterface.populatePlaylist(getChosenTitlesSet());
		}
		
	}

	
	
}
