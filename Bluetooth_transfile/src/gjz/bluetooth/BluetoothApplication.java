package gjz.bluetooth;



import gjz.bluetooth.serchfile.AdapterManager;
import android.app.Application;

public class BluetoothApplication extends Application {
	/**
	 * Application实例
	 */
	private static BluetoothApplication application;
	
	/**
	 * 
	 */
	private AdapterManager mAdapterManager;
	
	@Override
	public void onCreate() {
		super.onCreate();
		if(null == application){
			application = this;
		}
		//mTouchObject = new TouchObject();
	}
	
	/**
	 * 获取Application实例
	 * @return
	 */
	public static BluetoothApplication getInstance(){
		return application;
	}

	public AdapterManager getAdapterManager() {
		if(mAdapterManager==null){
			mAdapterManager = new AdapterManager(getApplicationContext());
			return mAdapterManager;
		}else{
		return mAdapterManager;
		}
	}

	public void setAdapterManager(AdapterManager adapterManager) {
		this.mAdapterManager = adapterManager;
	}

	

}
