package doext.module.do_Qiniu.app;
import android.content.Context;
import core.interfaces.DoIAppDelegate;

/**
 * APP启动的时候会执行onCreate方法；
 *
 */
public class do_Qiniu_App implements DoIAppDelegate {

	private static do_Qiniu_App instance;
	
	private do_Qiniu_App(){
		
	}
	
	public static do_Qiniu_App getInstance() {
		if(instance == null){
			instance = new do_Qiniu_App();
		}
		return instance;
	}
	
	@Override
	public void onCreate(Context context) {
		// ...do something
	}
	
	@Override
	public String getTypeID() {
		return "do_Qiniu";
	}
}
