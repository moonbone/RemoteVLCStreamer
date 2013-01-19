package apps.moonbone.remotevlcstreamer;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.database.Cursor;
import android.net.http.AndroidHttpClient;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Xml;
import android.widget.MediaController;
import android.widget.SeekBar;
import apps.moonbone.remotevlcstreamer.VLCControlFragment.ControlStatus;


class VlcPlayerInterface implements MediaController.MediaPlayerControl
{
	private Context m_context;
	
	private HttpXMLGetter m_status,m_playlist;
	private AndroidHttpClient m_httpClient;
	private URI m_server;
	private SeekBar m_volumeBar;

	
	private boolean m_stopped;
	private boolean m_playing;
	private int m_position;
	private int m_length;
	private int m_volume;
	private String m_titleName;
	private int m_maxVolume;
	
	//playlist atts:
	private int m_current;
	private boolean m_shuffle;
	private boolean m_repeat;
	
	
	private class HttpBackgroundRequester extends AsyncTask<URI,Integer,Long>
	{
		

		@Override
		protected Long doInBackground(URI... uris) {
			if (uris.length == 0)
			{
				return Long.valueOf(0);
			}
			
			try
			{


				m_httpClient.execute(new HttpGet(uris[0])).getEntity().writeTo(new NullOutputStream());

				return Long.valueOf(1);
			}
			catch (Exception e)
			{
				Log.d("HttpClientTask",e.toString());
			}
			
			// TODO Auto-generated method stub
			return Long.valueOf(0);
		}


		
	}
	
	private class HttpXMLGetter extends Thread
	{
		private class XMLGetAttsHandler extends DefaultHandler
		{
			private Handler m_retValhandler;
			private StringBuffer m_buffer;
			
			public XMLGetAttsHandler(Handler retValHandler)
			{
				super();
				m_retValhandler = retValHandler;
				m_buffer = new StringBuffer(256);
				
			}
			
			
			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes attributes)
					throws SAXException {
				m_buffer.setLength(0);
				
				if("leaf".equals(localName))
				{
					if(null != attributes.getValue("current"))
					{
						try
						{
							m_current = Integer.valueOf(attributes.getValue("id"));
							m_titleName = attributes.getValue("name");
						}
						catch (NumberFormatException e)
						{
							m_current = 0;
							m_titleName = "";
						}
					}
					
					if( m_retValhandler != null )
					{
						m_retValhandler.sendMessage(m_retValhandler.obtainMessage(0, attributes.getValue("uri")));
					}
				}
				
			}

			@Override
			public void endElement(String uri, String localName,
					String qName) throws SAXException {
				try
				{
					m_retValhandler.sendMessage(m_retValhandler.obtainMessage(attNameToStatus(localName).ordinal(),m_buffer.toString().trim() ) );
				}
				catch (Exception e)
				{
					//TODO: handle exception somehow.
				}

			}

			public void characters(char[] buffer, int start, int length) {
				m_buffer.append(buffer, start, length);
			}
			

		}
		private Handler m_handler;
		private URI m_uriToGet;
		private int m_interval;
		AndroidHttpClient m_httpClient;
		private boolean m_stopNext;

		
		public HttpXMLGetter(Handler resultHandler, URI xmlToGet, AndroidHttpClient httpClient, int interval)
		{
			super();
			m_handler = resultHandler;
			m_uriToGet = xmlToGet;
			m_httpClient = httpClient;
			m_interval = interval;
			m_stopNext = false;
			
		}
		
		private ControlStatus attNameToStatus(String attName) throws Exception
		{
			if ( "state".equals(attName) )
			{
				return ControlStatus.IS_PLAYING;
			}
			else if ("length".equals(attName) )
			{
				return ControlStatus.TOTAL_LENGTH;
			}
			else if ("position".equals(attName) )
			{
				return ControlStatus.CURRENT_POSITION;
			}
			else if ("random".equals(attName) )
			{
				return ControlStatus.IS_RANDOM;
			}
			else if ("loop".equals(attName) )
			{
				return ControlStatus.IS_REPEAT;
			}
			else if ("volume".equals(attName) )
			{
				return ControlStatus.VOLUME;
			}
					
			throw new Exception("No such control");
			
		}
		
		public void run()
		{
			while( !isInterrupted() )
			{
				try
				{
					HttpResponse response =  m_httpClient.execute(new HttpGet(m_uriToGet));
					ByteArrayOutputStream buf = new ByteArrayOutputStream();
					response.getEntity().writeTo(buf);
					Xml.parse(buf.toString(), new XMLGetAttsHandler(m_handler));
				}
				catch (Exception e)
				{
					Log.d("XMLGetter",e.toString());
				}
				
				if(m_stopNext)
				{
					break;
				}
				
				try{sleep(m_interval);}catch(InterruptedException e){break;}
				
				
			}

			
		}
		
		public synchronized void interruptAfterNext()
		{
			m_stopNext = true;
		}
	}
	

	
	public VlcPlayerInterface(Context context)
	{
		try
		{
			m_context = context;
			m_httpClient = AndroidHttpClient.newInstance("VLC remote streamer V1.0 beta");
			
			
			m_server = new URI(String.format("http://%s:8080/", ((MainActivity)m_context).getVLCServerIP() ));
			
			Handler h = new Handler()
			{
				public void handleMessage(Message msg)
				{
					if( msg.what == ControlStatus.IS_PLAYING.ordinal())
					{
						m_playing = ((String)msg.obj).equals("playing");
						m_stopped = ((String)msg.obj).equals("stop");
					}
					else if( msg.what == ControlStatus.TOTAL_LENGTH.ordinal())
					{
						m_length = Integer.valueOf((String)msg.obj);
					}
					else if( msg.what == ControlStatus.CURRENT_POSITION.ordinal())
					{
						try
						{
							m_position = Integer.valueOf((String)msg.obj);
						}
						catch(NumberFormatException e)
						{
							m_position = (int)(100 * Float.valueOf((String)msg.obj));
						}
					}
					else if( msg.what == ControlStatus.IS_RANDOM.ordinal())
					{
						try
						{
							m_shuffle = 0 != Integer.valueOf((String)msg.obj);
						}
						catch(NumberFormatException e)
						{
							m_shuffle = Boolean.valueOf((String)msg.obj);
						}
					}
					else if( msg.what == ControlStatus.IS_REPEAT.ordinal())
					{
						try
						{
							m_repeat = 0 != Integer.valueOf((String)msg.obj);
						}
						catch(NumberFormatException e)
						{
							m_repeat = Boolean.valueOf((String)msg.obj);
						}
					}
					else if( msg.what == ControlStatus.VOLUME.ordinal())
					{
						int newVolume = Integer.valueOf((String)msg.obj);

						m_volume = newVolume;
						
						if(null != m_volumeBar)
						{
							m_volumeBar.setProgress(m_volume);
						}
					}
					else
					{
						return;
					}
				}
				
			};
			
			try
			{
				m_status = new HttpXMLGetter(h, m_server.resolve(new URI("requests/status.xml")), m_httpClient,500);
				
				m_status.start();
				
				m_playlist = new HttpXMLGetter(null, m_server.resolve(new URI("requests/playlist.xml")), m_httpClient,4000);
				
				m_playlist.start();
				
			
			
			} catch(Exception e){
				//TODO: handle the exception!
			}
			
		    
			
		}
		catch (Exception e)
		{
			Log.d("HttpClient",e.toString());
		}
	}
	
	
	public void stopThreads()
	{
		m_status.interrupt();
		m_playlist.interrupt();
	}
	
	
	public void setVolumeBarView(SeekBar volumeBar)
	{
		m_volumeBar = volumeBar;
		m_volumeBar.setMax(m_maxVolume);
		
		
	}
	
	public void findMaxVolume()
	{
		AsyncTask<Void,Void,Void> setMaxVolume = new AsyncTask<Void, Void, Void>()
		{

			@Override
			protected Void doInBackground(Void... params) {
				try
				{
					setVolume(12);
					while(12 != m_volume)
					{
						Thread.sleep(100);
						
					}
					
					setVolume(9999);
					while(12 == m_volume)
					{
						Thread.sleep(100);
						
					}
					m_maxVolume = m_volume;
					setVolume(m_volume/2);
				}
				catch(InterruptedException e)
				{
					m_maxVolume = 1024;
					setVolume(512);
				}
				
				return null;
			}
			
		};
				
		setMaxVolume.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void populatePlaylist(TreeSet<Long> chosenTitlesSet)
	{
		try
		{
			boolean first = true;
		
			//empty play list
			new HttpBackgroundRequester().execute((m_server.resolve(new URI("requests/status.xml?command=pl_empty"))));
			
			for (Iterator<Long> iter = chosenTitlesSet.iterator();iter.hasNext();)
			{
				new HttpBackgroundRequester().execute(m_server.resolve(new URI(String.format("requests/status.xml?command=in_%s&input=%s",first ? "play" : "enqueue",URLEncoder.encode(genTitlePath(iter.next()),"UTF-8")))));
				first = false;
			}
			
		}
		catch(URISyntaxException e)
		{
			Log.d("URI Exception",e.toString());
		}
		catch(UnsupportedEncodingException e )
		{
			Log.d("URI Exception",e.toString());
		}
			
	}
	
	public void readPlaylist(TreeSet<Long> chosenTitlesSet) 
	{
		try
		{
			final TreeSet<Long> localRefToTitleSet = chosenTitlesSet;
			
			//load current playlist:
			Handler h = new Handler()
			{
				public void handleMessage(Message msg)
				{
					String remotePath = URLDecoder.decode((String)msg.obj);
					String pathSuffix = remotePath.replaceFirst("^.+?:8080/", "%");
					Cursor c = m_context.getContentResolver().query(
							MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							new String[]{MediaStore.Audio.Media.DATA,MediaStore.Audio.Media._ID},
							MediaStore.Audio.Media.DATA + " like ?", 
							new String[]{pathSuffix},
							null);
					
					for(c.moveToFirst();!c.isAfterLast();c.moveToNext())
					{
						localRefToTitleSet.add(c.getLong(1));
					}
					c.close();
				}
			};
			
			HttpXMLGetter getter = new HttpXMLGetter(h, m_server.resolve(new URI("requests/playlist.xml")), m_httpClient, 1000);
			getter.start();
			getter.interruptAfterNext();
			try{getter.join();}catch(InterruptedException e){}
		}
		catch(URISyntaxException e)
		{
			Log.d("URI Exception",e.toString());
		}
	}

	private String genTitlePath(Long titleID)
	{
		Cursor c = m_context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
												new String[]{MediaStore.Audio.Media.DATA},
												MediaStore.Audio.Media._ID + "=?",
												new String[]{titleID.toString()},
												null);
		c.moveToFirst();
		
		WifiManager wim = (WifiManager) m_context.getSystemService(Context.WIFI_SERVICE);
		
		//Only support IPv4
		@SuppressWarnings("deprecation")
		String deviceIP = Formatter.formatIpAddress( wim.getConnectionInfo().getIpAddress());
		
		String retVal = c.getString(0).replaceFirst(".+?/",String.format("http://%s:8080/",deviceIP)).replaceAll("\'", "%27");
		
		c.close();
		
		return retVal;
	}

	public String getCurrentPlayingTitleName()
	{
		try
		{
			String niceLookingStr = (URLDecoder.decode(m_titleName));
			return niceLookingStr.substring(niceLookingStr.lastIndexOf("/")+1,niceLookingStr.lastIndexOf("."));
		}
		catch(NullPointerException e)
		{
			return "";
		}
		catch(StringIndexOutOfBoundsException e)
		{
			return "";
		}

	}
	
	public int getBufferPercentage()
	{ 
		return m_position; 
		
	} 
		
	public int getCurrentPosition()
	{ 
		return (m_position * m_length) / 100;		    
	} 
	 
	public int getDuration()
	{ 
	   return m_length; 
	} 
	 
	public boolean isPlaying() 
	{
		return m_playing;
	}
	
	public void setShuffle(boolean state)
	{
		try
		{
			if(m_shuffle != state)
			{
				new HttpBackgroundRequester().execute((m_server.resolve(new URI("requests/status.xml?command=pl_random"))));
			}
		}
		catch(URISyntaxException e)
		{
			Log.d("URI Exception",e.toString());
		}
	}
	
	public void setRepeat(boolean state)
	{
		try
		{
			if(m_repeat != state)
			{
				new HttpBackgroundRequester().execute((m_server.resolve(new URI("requests/status.xml?command=pl_loop"))));
			}
		}
		catch(URISyntaxException e)
		{
			Log.d("URI Exception",e.toString());
		}
	}
	
	public void setVolume(int volume)
	{
		try
		{
			new HttpBackgroundRequester().execute((m_server.resolve(new URI(String.format("requests/status.xml?command=volume&val=%d",volume)))));
		}
		catch(URISyntaxException e)
		{
			Log.d("URI Exception",e.toString());
		}
		
	}
	
	public void pause()
	{
		try
		{
			new HttpBackgroundRequester().execute((m_server.resolve(new URI("requests/status.xml?command=pl_pause&id=0"))));
		}
		catch(URISyntaxException e)
		{
			Log.d("URI Exception",e.toString());
		}

	} 
	 
	public void seekTo(int pos) 
	{
		try
		{
			new HttpBackgroundRequester().execute((m_server.resolve(new URI(String.format("requests/status.xml?command=seek&val=%d",pos)))));
		}
		catch(URISyntaxException e)
		{
			Log.d("URI Exception",e.toString());
		}
	} 
	 
	public void start() 
	{ 
		try
		{
			if(m_stopped)
			{
				new HttpBackgroundRequester().execute((m_server.resolve(new URI(String.format("requests/status.xml?command=pl_pause&id=%d",m_current)))));
			}
			else
			{
				new HttpBackgroundRequester().execute((m_server.resolve(new URI("requests/status.xml?command=pl_pause&id=0"))));
			}
		}
		catch(URISyntaxException e)
		{
			Log.d("URI Exception",e.toString());
		}
	}
	
	public void next()
	{
		try
		{
			new HttpBackgroundRequester().execute((m_server.resolve(new URI("requests/status.xml?command=pl_next"))));	
		}
		catch(URISyntaxException e)
		{
			Log.d("URI Exception",e.toString());
		}
	}
	
	public void prev()
	{
		try
		{
			new HttpBackgroundRequester().execute((m_server.resolve(new URI("requests/status.xml?command=pl_previous"))));	
		}
		catch(URISyntaxException e)
		{
			Log.d("URI Exception",e.toString());
		}
	}

	@Override
	public boolean canPause()
	{
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canSeekBackward() 
	{

		return true;
	}

@Override
	public boolean canSeekForward() 
	{

		return true;
	}




}
