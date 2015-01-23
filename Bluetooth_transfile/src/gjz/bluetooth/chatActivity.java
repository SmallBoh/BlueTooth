package gjz.bluetooth;

import gjz.bluetooth.Bluetooth.ServerOrCilent;
import gjz.bluetooth.serchfile.SelectFileActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.transition.ChangeBounds;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class chatActivity extends Activity implements OnItemClickListener ,OnClickListener{
    /** Called when the activity is first created. */
	
	private ListView mListView;
	private ArrayList<deviceListItem>list;
	private Button sendButton;
	private Button disconnectButton;
	deviceListAdapter mAdapter;
	Context mContext;
	
	/* һЩ��������������������� */
	public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
	public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
	public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
	public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";
	
	
	public static boolean READ_FILE_NAME=true;
	
	public static boolean SEND_FILE=false;
	
	
	public static boolean SEND_FILE_NAME = true;
	
	public static boolean READ_TO_LOCALSTO = false;
	
	private BluetoothServerSocket mserverSocket = null;
	private ServerThread startServerThread = null;
	private clientThread clientConnectThread = null;
	private BluetoothSocket socket = null;
	private BluetoothDevice device = null;
	private readThread mreadThread = null;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.chat);
        mContext = this;
        init();
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		 if(requestCode == 11){
			//����Ϊ "ѡ���ļ�"
			try {
				sendFileName = data.getStringExtra("filepath");
			} catch (Exception e) {
				
			}
		}
	}
    
	private void init() {		   
		list = new ArrayList<deviceListItem>();
		mAdapter = new deviceListAdapter(this, list);
		mListView = (ListView) findViewById(R.id.list);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setFastScrollEnabled(true);
		Button selectFile = (Button) findViewById(R.id.MessageText);
		selectFile.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				/**
				 * ��ѡ���ļ��Ĵ��ڣ�
				 */
				Intent intent = new Intent(getApplicationContext(), SelectFileActivity.class);
				startActivityForResult(intent,11);
			}
		});
		sendButton= (Button)findViewById(R.id.btn_msg_send);
		sendButton.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						if(sendFileName==null){
							Toast.makeText(mContext, "δѡ���ļ�", Toast.LENGTH_SHORT).show();;
						}else{
							sendMessageHandle(sendFileName);	
						}
					}
				}
				);
		
		disconnectButton= (Button)findViewById(R.id.btn_disconnect);
		disconnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
		        if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) 
				{
		        	shutdownClient();
				}
				else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE) 
				{
					shutdownServer();
				}
				Bluetooth.isOpen = false;
				Bluetooth.serviceOrCilent=ServerOrCilent.NONE;
				Toast.makeText(mContext, "�ѶϿ����ӣ�", Toast.LENGTH_SHORT).show();
			}
		});		
	}    

    private Handler LinkDetectedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	if(msg.what==1)
        	{
        		list.add(new deviceListItem((String)msg.obj, true));
        	}
        	else
        	{
        		list.add(new deviceListItem((String)msg.obj, false));
        	}
			mAdapter.notifyDataSetChanged();
			mListView.setSelection(list.size() - 1);
        }
    };
	private String sendFileName;
	private OutputStream os;
	private String filepath;    
    
    @Override
    public synchronized void onPause() {
        super.onPause();
    }
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(Bluetooth.isOpen)
        {
        	Toast.makeText(mContext, "�����Ѿ��򿪣�����ͨ�š����Ҫ�ٽ������ӣ����ȶϿ���", Toast.LENGTH_SHORT).show();
        	return;
        }
        if(Bluetooth.serviceOrCilent==ServerOrCilent.CILENT)
        {
			String address = Bluetooth.BlueToothAddress;
			if(!address.equals("null"))
			{
				device = mBluetoothAdapter.getRemoteDevice(address);	
				clientConnectThread = new clientThread();
				clientConnectThread.start();
				Bluetooth.isOpen = true;
			}
			else
			{
				Toast.makeText(mContext, "address is null !", Toast.LENGTH_SHORT).show();
			}
        }
        else if(Bluetooth.serviceOrCilent==ServerOrCilent.SERVICE)
        {        	      	
        	startServerThread = new ServerThread();
        	startServerThread.start();
        	Bluetooth.isOpen = true;
        }
    }
	//�����ͻ���
	private class clientThread extends Thread { 		
		public void run() {
			try {
				//����һ��Socket���ӣ�ֻ��Ҫ��������ע��ʱ��UUID��
				// socket = device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
				socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
				//����
				Message msg2 = new Message();
				msg2.obj = "���Ժ��������ӷ�����:"+Bluetooth.BlueToothAddress;
				msg2.what = 0;
				LinkDetectedHandler.sendMessage(msg2);
				
				socket.connect();
				
				Message msg = new Message();
				msg.obj = "�Ѿ������Ϸ���ˣ����Է�����Ϣ��";
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg);
				//������������
				mreadThread = new readThread();
				mreadThread.start();
			} 
			catch (IOException e) 
			{
				Log.e("connect", "", e);
				Message msg = new Message();
				msg.obj = "���ӷ�����쳣���Ͽ�����������һ�ԡ�";
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg);
			} 
		}
	};

	//����������
	private class ServerThread extends Thread { 
		public void run() {
					
			try {
				/* ����һ������������ 
				 * �����ֱ𣺷��������ơ�UUID*/	
				mserverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM,
						UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));		
				
				Log.d("server", "wait cilent connect...");
				
				Message msg = new Message();
				msg.obj = "���Ժ����ڵȴ��ͻ��˵�����...";
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg);
				
				/* ���ܿͻ��˵��������� */
				socket = mserverSocket.accept();
				Log.d("server", "accept success !");
				
				Message msg2 = new Message();
				String info = "�ͻ����Ѿ������ϣ����Է�����Ϣ��";
				msg2.obj = info;
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg2);
				//������������
				mreadThread = new readThread();
				mreadThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
	/* ֹͣ������ */
	private void shutdownServer() {
		new Thread() {
			public void run() {
				if(startServerThread != null)
				{
					startServerThread.interrupt();
					startServerThread = null;
				}
				if(mreadThread != null)
				{
					mreadThread.interrupt();
					mreadThread = null;
				}				
				try {					
					if(socket != null)
					{
						socket.close();
						socket = null;
					}
					if (mserverSocket != null)
					{
						mserverSocket.close();/* �رշ����� */
						mserverSocket = null;
					}
				} catch (IOException e) {
					Log.e("server", "mserverSocket.close()", e);
				}
			};
		}.start();
	}
	/* ֹͣ�ͻ������� */
	private void shutdownClient() {
		new Thread() {
			public void run() {
				if(clientConnectThread!=null)
				{
					clientConnectThread.interrupt();
					clientConnectThread= null;
				}
				if(mreadThread != null)
				{
					mreadThread.interrupt();
					mreadThread = null;
				}
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					socket = null;
				}
			};
		}.start();
	}
	//��������
	private void sendMessageHandle(String msg) 
	{		
		if (socket == null) 
		{
			Toast.makeText(mContext, "û������", Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			os = socket.getOutputStream(); 
			String msg1 = "����"+msg;
			os.write(msg1.getBytes());
			os.flush();
			list.add(new deviceListItem(msg, false));
			mAdapter.notifyDataSetChanged();
			mListView.setSelection(list.size() - 1);
			filepath = msg;
			File file = new File(filepath);
			 FileInputStream is = new FileInputStream(file);   
		        // �趨��ȡ���ֽ���   
		        int n;   
		        byte buffer[] = new byte[1024];  
		        // ��ȡ������   
		        while ((n = is.read(buffer))>0) {   
		        	os.write(buffer,0,n);
			}
		        //os.flush();
		       Message msgs = new Message();
	    		msgs.obj = "�������";
	    		msgs.what = 1;
	    		LinkDetectedHandler.sendMessage(msgs);
			/**
			 * �����ļ�
			 */
			 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//��ȡ����
    private class readThread extends Thread { 
        private OutputStream fos;
		//private File file;
		private String s;
		private File file;

		public void run() {
        	
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream mmInStream = null;
            
            try {
				mmInStream = socket.getInputStream();
			} catch (IOException e1) {
				e1.printStackTrace();
			}	
            while (true) {//����������ļ����ƣ�
                try {
                    if( (bytes = mmInStream.read(buffer)) > 0)
                    {
                    	if(bytes<buffer.length){
                    		byte[] buf_data = new byte[bytes];
    				    	for(int i=0; i<bytes; i++)
    				    	{
    				    		buf_data[i] = buffer[i];
    				    	}
    						s = new String(buf_data);
    						String[] split = s.split("/");
    						if(split.length>2&&s.contains("����")){
	    						String name = split[split.length-1];
	    						Message msg = new Message();
	    						msg.obj = "��Ҫ���䣺"+name;
	    						msg.what = 1;
	    						LinkDetectedHandler.sendMessage(msg);
	    			    		String path =Environment.getExternalStorageDirectory()+"/transfile/";
	    			    		file = new File(path);
	    			    		if(!file.exists()){
	    			    			file.mkdir();
	    			    		}
	    			    		file = new File(path+name);
	    			    		if(!file.exists()){
	    			    			file.createNewFile();
	    			    		}
	    			    		if(file.exists()){
	    			    		fos = new FileOutputStream(file);
	    			    		}
    						}else{
    								fos.write(buffer,0,bytes);
    						}
                    	}else{
                				fos.write(buffer,0,bytes);
                    	}
                	}
                } catch (IOException e) {
                	try {
						mmInStream.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                    break;
                }
            }
            
		}
    }
   
    	
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) 
		{
        	shutdownClient();
		}
		else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE) 
		{
			shutdownServer();
		}
        Bluetooth.isOpen = false;
		Bluetooth.serviceOrCilent = ServerOrCilent.NONE;
    }
	public class SiriListItem {
		String message;
		boolean isSiri;

		public SiriListItem(String msg, boolean siri) {
			message = msg;
			isSiri = siri;
		}
	}
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
	}	
	public class deviceListItem {
		String message;
		boolean isSiri;

		public deviceListItem(String msg, boolean siri) {
			message = msg;
			isSiri = siri;
		}
	}
}