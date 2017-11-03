package dotest.module.do_Qiniu.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import doext.module.do_Qiniu.implement.do_Qiniu_Model;
import dotest.module.do_Qiniu.R;
import dotest.module.do_Qiniu.debug.DoService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import core.DoServiceContainer;
import core.object.DoInvokeResult;
import core.object.DoUIModule;


/**
 * webview组件测试样例
 */
public class WebViewSampleTestActivty extends DoTestActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initModuleModel() throws Exception {
        this.model = new do_Qiniu_Model();
    }

    @Override
    protected void initUIView() throws Exception {
//		Do_WebView_View view = new Do_WebView_View(this);
//		((DoUIModule) this.model).setCurrentUIModuleView(view);
//		((DoUIModule) this.model).setCurrentPage(currentPage);
//		view.loadView((DoUIModule) this.model);
//		LinearLayout uiview = (LinearLayout) findViewById(R.id.uiview);
//		uiview.addView(view);
    }

    @Override
    public void doTestProperties(View view) {
        DoService.setPropertyValue(this.model, "url", "https://www.baidu.com");
    }

    @Override
    protected void doTestSyncMethod() {
        Map<String, String> _paras_back = new HashMap<String, String>();
        DoService.syncMethod(this.model, "back", _paras_back);
    }

    @Override
    protected void doTestAsyncMethod() {
        Map<String, String> _paras_loadString = new HashMap<String, String>();
//        _paras_loadString.put("filePath", "data://image/1.jpg");
        _paras_loadString.put("filePath", "data://file/2.pdf");
        _paras_loadString.put("accessKey", "_Q19fnkkSPWUW_zeiu_Cft6GYnZDfa5sMpFcmlut");
        _paras_loadString.put("secretKey", "Q-IaOH92Gcw_njSFV3eJuExvxjeMyt_f7zNj67oZ");
        _paras_loadString.put("bucket", "fengzispace");
        _paras_loadString.put("saveName", "");

        DoService.asyncMethod(this.model, "upload", _paras_loadString, new DoService.EventCallBack() {
            @Override
            public void eventCallBack(String _data) {// 回调函数
                DoServiceContainer.getLogEngine().writeDebug("异步方法回调：" + _data);
            }
        });
    }

    @Override
    protected void onEvent() {
        // 系统事件订阅
        DoService.subscribeEvent(this.model, "loaded", new DoService.EventCallBack() {
            @Override
            public void eventCallBack(String _data) {
                DoServiceContainer.getLogEngine().writeDebug("系统事件回调：name = loaded, data = " + _data);
                Toast.makeText(WebViewSampleTestActivty.this, "系统事件回调：loaded", Toast.LENGTH_LONG).show();
            }
        });
        // 自定义事件订阅
        DoService.subscribeEvent(this.model, "_messageName", new DoService.EventCallBack() {
            @Override
            public void eventCallBack(String _data) {
                DoServiceContainer.getLogEngine().writeDebug("自定义事件回调：name = _messageName, data = " + _data);
                Toast.makeText(WebViewSampleTestActivty.this, "自定义事件回调：_messageName", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void doTestFireEvent(View view) throws UnsupportedEncodingException {
        Map<String, String> _paras_loadString = new HashMap<String, String>();

        //_paras_loadString.put("domainName", "olcuiapt7.bkt.clouddn.com");
        _paras_loadString.put("domainName", "oj8so80jf.bkt.clouddn.com");
        _paras_loadString.put("fileName", "1.mp4");
        //_paras_loadString.put("fileName", URLEncoder.encode("4.zip"));
        _paras_loadString.put("path", "data://qiniu/download/2.mp4");
//        _paras_loadString.put("accessKey", "_Q19fnkkSPWUW_zeiu_Cft6GYnZDfa5sMpFcmlut");
//        _paras_loadString.put("secretKey", "Q-IaOH92Gcw_njSFV3eJuExvxjeMyt_f7zNj67oZ");


        DoService.asyncMethod(this.model, "download", _paras_loadString, new DoService.EventCallBack() {
            @Override
            public void eventCallBack(String _data) {// 回调函数
                DoServiceContainer.getLogEngine().writeDebug("异步方法回调：" + _data);
            }
        });
    }

}
