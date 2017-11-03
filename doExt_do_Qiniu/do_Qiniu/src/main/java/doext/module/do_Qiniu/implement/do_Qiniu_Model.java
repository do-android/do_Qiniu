package doext.module.do_Qiniu.implement;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.UrlSafeBase64;

import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.interfaces.DoISourceFS;
import core.object.DoEventCenter;
import core.object.DoSingletonModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import doext.module.do_Qiniu.define.do_Qiniu_IMethod;
import doext.module.do_Qiniu.app.do_Qiniu_App;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_Qiniu_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象；
 * 获取DoInvokeResult对象方式new DoInvokeResult(this.getUniqueKey());
 */
public class do_Qiniu_Model extends DoSingletonModule implements do_Qiniu_IMethod {

    public do_Qiniu_Model() throws Exception {
        super();
    }

    /**
     * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
     *
     * @_methodName 方法名称
     * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
     * @_scriptEngine 当前Page JS上下文环境对象
     * @_invokeResult 用于返回方法结果对象
     */
    @Override
    public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas,
                                    DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult)
            throws Exception {
        //...do something
        return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
    }

    /**
     * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用，
     * 可以根据_methodName调用相应的接口实现方法；
     *
     * @_methodName 方法名称
     * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
     * @_scriptEngine 当前page JS上下文环境
     * @_callbackFuncName 回调函数名
     * #如何执行异步方法回调？可以通过如下方法：
     * _scriptEngine.callback(_callbackFuncName, _invokeResult);
     * 参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
     * 获取DoInvokeResult对象方式new DoInvokeResult(this.getUniqueKey());
     */
    @Override
    public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas,
                                     DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
        if ("upload".equals(_methodName)) {
            this.upload(_dictParas, _scriptEngine, _callbackFuncName);
            return true;
        } else if ("download".equals(_methodName)) {
            this.download(_dictParas, _scriptEngine, _callbackFuncName);
            return true;
        }
        return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
    }


    /**
     * 从七牛云下载文件；
     *
     * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
     * @_scriptEngine 当前Page JS上下文环境对象
     * @_callbackFuncName 回调函数名
     */
    String downLoadPath = "";

    @Override
    public void download(JSONObject _dictParas, final DoIScriptEngine _scriptEngine, final String _callbackFuncName) throws JSONException {
        String _domainName = DoJsonHelper.getString(_dictParas, "domainName", ""); //外链域名
        String _fileName = DoJsonHelper.getString(_dictParas, "fileName", ""); //下载的文件名称
        String _path = DoJsonHelper.getString(_dictParas, "path", ""); //保存地址
        String _accessKey = DoJsonHelper.getString(_dictParas, "accessKey", ""); //用户凭证
        String _secretKey = DoJsonHelper.getString(_dictParas, "secretKey", ""); //签名密钥
        if (!isNetworkAvailable()) {
            DoServiceContainer.getLogEngine().writeInfo("Qiniu download", "下载失败," + "网络离线，没有可用网络！");
            return;
        }
        final DoInvokeResult _result = new DoInvokeResult(do_Qiniu_Model.this.getUniqueKey());
        try {
            if (TextUtils.isEmpty(_path)) {
                throw new Exception("path不能为空!");
            }
            if (TextUtils.isEmpty(_accessKey) || TextUtils.isEmpty(_secretKey)) {
                downLoadPath = getTempdownUrl(_domainName, _fileName);

            } else {
                downLoadPath = getDownUrl(_domainName, _fileName, _accessKey, _secretKey);
            }
            final String fileFullPath = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentPage().getCurrentApp(), _path);
            int beginIndex = fileFullPath.lastIndexOf(File.separator) + 1;
            String filePath = fileFullPath.substring(0, beginIndex);
            if (!DoIOHelper.existDirectory(filePath)) {
                DoIOHelper.createDirectory(filePath);
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(downLoadPath);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(5000);
                        conn.setRequestMethod("GET");
                        if (conn.getResponseCode() == 200) {
                            double contentLength = conn.getContentLength();
                            InputStream inputStream = conn.getInputStream();


                            int length;
                            long lengtsh = 0;
                            byte[] buffer = new byte[1024];
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            while ((length = inputStream.read(buffer)) != -1) {
                                bos.write(buffer, 0, length);
                                lengtsh += length;
                                fireProgress(contentLength, lengtsh / contentLength);
                            }
                            bos.close();
                            byte[] getData = bos.toByteArray();
                            DoIOHelper.createFile(fileFullPath);
                            FileOutputStream fos = new FileOutputStream(fileFullPath);
                            fos.write(getData);

                            if (fos != null) {
                                fos.close();
                            }
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            _result.setResultBoolean(true);
                            _scriptEngine.callback(_callbackFuncName, _result);
                        }
                    } catch (Exception ex) {
                        DoServiceContainer.getLogEngine().writeInfo("Qiniu download", "下载失败," + ex.getMessage());
                    }
                }
            }).start();

        } catch (Exception ex) {
            _result.setResultBoolean(false);
            DoServiceContainer.getLogEngine().writeInfo("Qiniu download", "下载失败," + ex.getMessage());
            _scriptEngine.callback(_callbackFuncName, _result);
        }
    }


    private String getDownUrl(String domainName, String fileName, String accessKey, String secretKey) {
        try {
            long _dataline = System.currentTimeMillis() / 1000 + 3600;
            String source = getTempdownUrl(domainName, fileName);
            source = source + "?e=" + _dataline;

            byte[] _sign = HmacSHA1Encrypt(source, secretKey);
            String _encodedSign = UrlSafeBase64.encodeToString(_sign);
            String _downloadPath = source + "&token=" + accessKey + ':' + _encodedSign;
            return _downloadPath;
        } catch (Exception ex) {
            return null;
        }
    }

    private String getTempdownUrl(String domainName, String fileName) throws Exception {
        if (DoIOHelper.getHttpUrlPath(domainName) == null) {
            return "http://" + domainName + "/" + URLEncoder.encode(fileName); //防止有些文件名为中文
        } else {
            return domainName + "/" + URLEncoder.encode(fileName);
        }
    }

    /**
     * 上传文件；
     *
     * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
     * @_scriptEngine 当前Page JS上下文环境对象
     * @_callbackFuncName 回调函数名
     */
    @Override
    public void upload(JSONObject _dictParas, final DoIScriptEngine _scriptEngine, final String _callbackFuncName) throws JSONException {
        String _filePath = DoJsonHelper.getString(_dictParas, "filePath", ""); //文件路径
        String _accessKey = DoJsonHelper.getString(_dictParas, "accessKey", ""); //用户凭证
        String _secretKey = DoJsonHelper.getString(_dictParas, "secretKey", ""); //签名密钥
        String _bucket = DoJsonHelper.getString(_dictParas, "bucket", ""); //七牛存储空间名称
        String _saveName = DoJsonHelper.getString(_dictParas, "saveName", ""); //上传后的文件名称(可不填)

        final DoInvokeResult _result = new DoInvokeResult(do_Qiniu_Model.this.getUniqueKey());
        try {
            if (TextUtils.isEmpty(_filePath)) {
                throw new Exception("path不能为空!");
            }
            if (!_filePath.startsWith(DoISourceFS.DATA_PREFIX)) {
                throw new Exception("path参数只支持 " + DoISourceFS.DATA_PREFIX + "打头!");
            }
            String _path = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentPage().getCurrentApp(), _filePath);
            if (TextUtils.isEmpty(_saveName) || _saveName.length() == 0) {
                _saveName = _path.substring(_path.lastIndexOf('/') + 1);
            }
            final File file = new File(_path);
            Configuration config = new Configuration.Builder().zone(Zone.httpAutoZone).build();
            UploadManager uploadManager = new UploadManager(config);
            String token = getToken(_accessKey, _secretKey, _bucket, _saveName);
            uploadManager.put(file, _saveName, token, new UpCompletionHandler() {
                public void complete(String key, ResponseInfo info, JSONObject res) {
                    if (info.isOK() == true) {
                        _result.setResultBoolean(true);
                        _scriptEngine.callback(_callbackFuncName, _result);
                    }
                }
            }, new UploadOptions(null, null, false,
                    new UpProgressHandler() {
                        public void progress(String key, double percent) {
                            fireProgress(file.length(), percent);
                        }

                    }, null));
        } catch (Exception ex) {
            _result.setResultBoolean(false);
            DoServiceContainer.getLogEngine().writeInfo("Qiniu upload", "上传失败," + ex.getMessage());
            _scriptEngine.callback(_callbackFuncName, _result);
        }
    }

    private void fireProgress(double fileSzie, double percent) {
        DoInvokeResult _invokeResult = new DoInvokeResult(getUniqueKey());
        JSONObject jsonNode = new JSONObject();
        DecimalFormat df = new DecimalFormat("######0.00");
        //Log.i("download", "fileSize:" + fileSzie / 1024f + "    percent:" + df.format(percent * 100));
        try {
            jsonNode.put("fileSize", fileSzie / 1024f);
            jsonNode.put("percent", df.format(percent * 100));//保留两位小数
            _invokeResult.setResultNode(jsonNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fireEvent("progress", _invokeResult);
    }

    private void fireEvent(String eventName, DoInvokeResult _invokeResult) {
        DoEventCenter eventCenter = getEventCenter();
        if (eventCenter != null) {
            eventCenter.fireEvent(eventName, _invokeResult);
        }
    }

    private static final String MAC_NAME = "HmacSHA1";
    private static final String ENCODING = "UTF-8";

    private String getToken(String accessKey, String secretKey, String bucket, String fileName) {
        try {
            JSONObject _json = new JSONObject();
            long _dataline = System.currentTimeMillis() / 1000 + 3600;
            _json.put("deadline", _dataline);// 有效时间为一个小时
            _json.put("scope", bucket + ":" + fileName); //如果只填存储空间名则不能替换 加上文件名称才允许修改
            String _encodedPutPolicy = UrlSafeBase64.encodeToString(_json
                    .toString().getBytes());
            byte[] _sign = HmacSHA1Encrypt(_encodedPutPolicy, secretKey);
            String _encodedSign = UrlSafeBase64.encodeToString(_sign);
            String _uploadToken = accessKey + ':' + _encodedSign + ':'
                    + _encodedPutPolicy;
            return _uploadToken;
        } catch (Exception ex) {
            return null;
        }
    }

    public static byte[] HmacSHA1Encrypt(String encryptText, String encryptKey)
            throws Exception {
        byte[] data = encryptKey.getBytes(ENCODING);
        // 根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
        SecretKey secretKey = new SecretKeySpec(data, MAC_NAME);
        // 生成一个指定 Mac 算法 的 Mac 对象
        Mac mac = Mac.getInstance(MAC_NAME);
        // 用给定密钥初始化 Mac 对象
        mac.init(secretKey);
        byte[] text = encryptText.getBytes(ENCODING);
        // 完成 Mac 操作
        return mac.doFinal(text);
    }

    private boolean isNetworkAvailable() {
        try {
            Context ctx = DoServiceContainer.getPageViewFactory().getAppContext();
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            return (info != null && info.isConnected());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}