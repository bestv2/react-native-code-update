package wang.wind.rn.codeupdate;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.client.params.ClientPNames;

/**
 * Created by wangduo on 2016/12/16.
 */

public class RCTUpdateManager extends ReactContextBaseJavaModule {
    private static final String TAG = "RCTUpdateManager";
    private Thread downLoadThread;
    private ProgressDialog progressDialog;
    private Callback callback;
    private Context mContext;
    private VersionUpdate update;
    private final PackageInfo pInfo;

    private Handler mHandler = null;
    /**
     * {
     * lastVersion: 0,
     * currentVersion: 1.1.1
     * }
     */


    public static final String JS_BUNDLE_LOCAL_FILE = "index.android.bundle";

    private static String APPID = "undefined";
    private static String APPNAME = "undefined";
    private static String CHECK_HOST = "/";
    private static String FILE_BASE_PATH = Environment.getExternalStorageDirectory().toString() + File.separator + APPNAME;
    private static String LAST_JS_BUNDLE_LOCAL_PATH = FILE_BASE_PATH + File.separator + "js_bundle";
    private static String JS_BUNDLE_LOCAL_PATH = FILE_BASE_PATH + File.separator + ".js_bundle";
    private static String APK_SAVED_LOCAL_PATH = FILE_BASE_PATH + File.separator + "download_apk";

    private static final String REACT_APPLICATION_CLASS_NAME = "com.facebook.react.ReactApplication";
    private static final String REACT_NATIVE_HOST_CLASS_NAME = "com.facebook.react.ReactNativeHost";

    private static final String JS_BUNDLE_VERSION = "JS_BUNDLE_VERSION";
    private static final String JS_BUNDLE_VERSION_CODE = "JS_BUNDLE_VERSION_CODE";
    private static final String JS_BUNDLE_PATH = "JS_BUNDLE_PATH";
    private static final String UPDATED_APP_VERSION_CODE = "UPDATED_APP_VERSION";
    private static String PREF_NAME = "creativelocker.pref";
    private static boolean sIsAtLeastGB;

    private static AsyncHttpClient mClient;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            sIsAtLeastGB = true;
        }
    }

    /**
     * 读取application 节点  meta-data 信息
     */
    private static String readMetaDataFromApplication(Application application) {
        try {
            ApplicationInfo appInfo = application.getPackageManager()
                    .getApplicationInfo(application.getPackageName(),
                            PackageManager.GET_META_DATA);
            String updateURI = appInfo.metaData.getString("react.native.update.uri");
            return updateURI;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void init(String appName, String appId, String checkHost, Application application, AsyncHttpClient client) {
        APPID = appId;
        APPNAME = appName;
        CHECK_HOST = checkHost;
        FILE_BASE_PATH = Environment.getExternalStorageDirectory().toString() + File.separator + APPNAME;
        LAST_JS_BUNDLE_LOCAL_PATH = FILE_BASE_PATH + File.separator + "js_bundle";
        JS_BUNDLE_LOCAL_PATH = FILE_BASE_PATH + File.separator + ".js_bundle";
        APK_SAVED_LOCAL_PATH = FILE_BASE_PATH + File.separator + "download_apk";
        mClient = client;

    }

    public RCTUpdateManager(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext.getApplicationContext();
        pInfo = getPackageInfo(mContext);
    }

    private Runnable mdownApkRunnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("=========run1");
            if (!haveNew()) {
                return;
            }
            System.out.println("=========run2");
            File saveFile = null;
            String dirPath = null;
            String saveFileName = null;
//            System.out.println("update.getUpdateType():"+update.getUpdateType()+","+(update.getUpdateType()==1));
            if (update.getUpdateType() == 1) {

                dirPath = APK_SAVED_LOCAL_PATH;
                saveFileName = dirPath + File.separator + update.getVersionCode() + ".apk";

            } else {
//                dirPath =JS_BUNDLE_LOCAL_PATH+File.separator+update.getJsBundleVersionCode();
                dirPath = JS_BUNDLE_LOCAL_PATH;
                if (update.getDownloadUrl().endsWith(".zip")) {
                    saveFileName = dirPath + File.separator + "update.zip";
                } else {
                    saveFileName = dirPath + File.separator + JS_BUNDLE_LOCAL_FILE;
                }


            }
//            System.out.println("saveFileName:"+saveFileName);
            File file = new File(dirPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            saveFile = new File(saveFileName);
            try {
                downloadUpdateFile(update.getDownloadUrl(), saveFile);
            } catch (Exception e) {
                mHandler.sendEmptyMessage(0);
                e.printStackTrace();
            }

        }
    };

    // 取消下载按钮的监听器类
    class CancelDownloadListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // 点击“确定”按钮取消对话框
            dialog.cancel();
            if (downLoadThread != null && !downLoadThread.isInterrupted()) {
                System.out.println("interrupt");
                downLoadThread.interrupt();
            }
        }
    }

    private boolean haveNew() {
        return update != null && update.getSuccess() && update.getUpdateType() > 0;
    }

    private void startUpdate() {
        try {
            if (progressDialog == null) {
//            progressDialog = new YProgressDialog(mContext,"下载中...");
                progressDialog = DialogHelp.getProgressDialog(getCurrentActivity(), "下载中...");
                CharSequence title = "取消下载";
                progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, title, new CancelDownloadListener());

            }
            progressDialog.show();
            downLoadThread = new Thread(mdownApkRunnable);
            mHandler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    // TODO Auto-generated method stub
                    super.handleMessage(msg);
                    AlertDialog.Builder dialog;
                    switch (msg.what) {
                        case 0:
                            //下载失败
                            progressDialog.hide();
                            if (callback != null) {
                                callback.invoke();
                                callback = null;
                            }
                        case 1:
                            int rate = msg.arg1;
                            if (rate < 100) {
                                progressDialog.setProgress(rate);
                            } else {
                                // 下载完毕后变换通知形式
                                progressDialog.hide();
                            }
//                    mNotificationManager.notify(NOTIFY_ID, mNotification);
                            break;
                        case 2:
                            if (callback != null) {
                                callback.invoke("true");
                                callback = null;
                            }
                            break;
                        //js下载完毕
                        // 取消通知
                        case 3:
                            restartReact(getReactApplicationContext());
//                    mNotificationManager.cancel(NOTIFY_ID);
                            break;

                    }
                }
            };
            downLoadThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getName() {
        return "UpdateManager";
    }

    @ReactMethod
    public void checkUpdate(final Callback cb) {
        //隐藏标题栏

        final Context context = getReactApplicationContext();
        Log.d(TAG, "hasInternet:" + hasInternet());
        if (hasInternet()) {

            String version = pInfo.versionName;
            int versionCode = pInfo.versionCode;

            int updatedAppVersionCode = getUpdatedAppVersionCode(mContext);
            if (updatedAppVersionCode != versionCode) {
                setJsBundlePath(null, mContext);
                setJsBundleVersionCode(0, mContext);
                setUpdatedAppVersionCode(versionCode, mContext);
            }
            context.getSystemService(Context.DOWNLOAD_SERVICE);
            AsyncHttpResponseHandler mCheckUpdateHandle = new AsyncHttpResponseHandler() {

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    cb.invoke();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        String jsonString = new String(responseBody, "UTF-8");
                        WritableMap params = null;
                        JSONObject mainObject = new JSONObject(jsonString);
                        if (mainObject != null) {
                            BundleJSONConverter bjc = new BundleJSONConverter();
                            Bundle bundle = bjc.convertToBundle(mainObject);
                            params = Arguments.fromBundle(bundle);
                        }
                        cb.invoke(params);
                    } catch (Exception e) {
                        e.printStackTrace();
                        cb.invoke();

                    }
                }
            };
            checkUpdate(pInfo, mCheckUpdateHandle);
        } else {
//            MainApplication.showToast(R.string.tip_network_error);
            AlertDialog.Builder dialog = DialogHelp.getMessageDialog(getCurrentActivity(), mContext.getString(R.string.tip_network_error));
//            dialog.setTitle();
            dialog.show();

        }

    }

    @ReactMethod
    public void doUpdate(ReadableMap options, final Callback cb) {
        update = new VersionUpdate(options);
        if (update.getUpdateType() == 1) {
            AlertDialog.Builder dialog;
            String changeLog = update.getChangeLog();
            System.out.println(changeLog);
            String toastMessage = (update.getChangeLog() == null ? "" : (changeLog));
            dialog = DialogHelp.getConfirmDialog(getCurrentActivity(), toastMessage, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
//                            System.out.println("1231232131");
                    callback = cb;
//                    System.out.println("====="+update.getSuccess()+","+update.getUpdateType());
                    startUpdate();
                }
            });
            dialog.setTitle("发现新版本是否下载?");
            dialog.show();
        } else {
            callback = cb;
            System.out.println("=====" + update.getSuccess() + "," + update.getUpdateType());
            startUpdate();
        }


    }

    private void checkUpdate(PackageInfo packageInfo, AsyncHttpResponseHandler mCheckUpdateHandle) {
        int bundleVersionCode = getJsBundleVersionCode(mContext);
        Log.d(TAG, "checkUpdate");
        mClient.get(CHECK_HOST + "/app/checkUpdate?platform=android&app_id=" + APPID +
                "&app_version_code=" + packageInfo.versionCode +
                "&js_version_code=" + bundleVersionCode, mCheckUpdateHandle);
    }

    public long downloadUpdateFile(String downloadUrl, File saveFile)
            throws Exception {

        int downloadCount = 0;
        int currentSize = 0;
        long totalSize = 0;
        int updateTotalSize = 0;

        HttpURLConnection httpConnection = null;
        InputStream is = null;
        FileOutputStream fos = null;

        try {
            URL url = new URL(downloadUrl);
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection
                    .setRequestProperty("User-Agent", "PacificHttpClient");
            if (currentSize > 0) {
                httpConnection.setRequestProperty("RANGE", "bytes="
                        + currentSize + "-");
            }
            httpConnection.setConnectTimeout(10000);
            httpConnection.setReadTimeout(20000);
            updateTotalSize = httpConnection.getContentLength();
            if (httpConnection.getResponseCode() == 404) {
                throw new Exception("fail!");
            }
            is = httpConnection.getInputStream();
            fos = new FileOutputStream(saveFile, false);
            byte buffer[] = new byte[1024];
            int readsize = 0;
            while (!downLoadThread.isInterrupted() && (readsize = is.read(buffer)) > 0) {
                fos.write(buffer, 0, readsize);
                totalSize += readsize;
                // 为了防止频繁的通知导致应用吃紧，百分比增加10才通知一次
                if ((downloadCount == 0)
                        || (int) (totalSize * 100 / updateTotalSize) - 5 >= downloadCount) {
                    downloadCount += 5;
                    System.out.println(downloadCount + "," + update.getUpdateType());
                    if (update.getUpdateType() == 1 || update.getUpdateType() == 2) {
                        // 更新进度
                        Message msg = mHandler.obtainMessage();
                        msg.what = 1;
                        msg.arg1 = downloadCount;
                        mHandler.sendMessage(msg);

                    }
                }
            }
            if (downLoadThread.isInterrupted()) {
                is.close();
                fos.close();
                saveFile.delete();
                if (callback != null) {
                    callback.invoke();
                    callback = null;
                }
                return 0l;
            }
            if (downLoadThread != null && !downLoadThread.isInterrupted()) {
                downLoadThread.interrupt();
            }

            if (update.getUpdateType() == 2) {
                if (saveFile.getAbsolutePath().endsWith(".zip")) {
                    ZipUtils.unZipFile(saveFile.getAbsolutePath(), JS_BUNDLE_LOCAL_PATH);
                }
                setJsBundlePath(JS_BUNDLE_LOCAL_PATH + File.separator + JS_BUNDLE_LOCAL_FILE, mContext);
                setJsBundleVersionCode(update.getJsBundleVersionCode(), mContext);
                setUpdatedAppVersionCode(pInfo.versionCode, mContext);
                mHandler.sendEmptyMessage(2);
            } else {
                File apkfile = saveFile;
                if (apkfile.exists()) {
                    setJsBundlePath(null, mContext);
                    setUpdatedAppVersionCode(0, mContext);
                    setJsBundleVersionCode(0, mContext);
                    installAPK(getReactApplicationContext(), apkfile);
                }

            }
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
            if (is != null) {
                is.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
        return totalSize;
    }

    @ReactMethod
    public void askForReload() {
        final AlertDialog.Builder dialog = DialogHelp.getConfirmDialog(getCurrentActivity(), "自动更新已经完成是否重新启动应用?", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mHandler.sendEmptyMessage(3);
            }
        });
        dialog.setTitle("自动更新完成");
        dialog.show();
    }


    private void restartReact(ReactContext context) {
        if (context == null) {
            restart(context);
            return;
        }
        loadBundle(context.getCurrentActivity());
    }

    public static void restart(Context context) {
        Intent i = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(i);
    }

    // Use reflection to find and set the appropriate fields on ReactInstanceManager. See #556 for a proposal for a less brittle way
    // to approach this.
    private void setJSBundle(ReactInstanceManager instanceManager, String latestJSBundleFile) throws NoSuchFieldException, IllegalAccessException {
        try {
            Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
            Class<?> jsBundleLoaderClass = Class.forName("com.facebook.react.cxxbridge.JSBundleLoader");
            Method createFileLoaderMethod = null;

            Method[] methods = jsBundleLoaderClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName() == "createFileLoader") {
                    createFileLoaderMethod = method;
                    break;
                }
            }

            if (createFileLoaderMethod == null) {
                throw new NoSuchMethodException("Could not find a recognized 'createFileLoader' method");
            }

            int numParameters = createFileLoaderMethod.getGenericParameterTypes().length;
            Object latestJSBundleLoader;

            if (numParameters == 1) {
                // RN >= v0.34
                latestJSBundleLoader = createFileLoaderMethod.invoke(jsBundleLoaderClass, latestJSBundleFile);
            } else if (numParameters == 2) {
                // RN >= v0.31 && RN < v0.34
                latestJSBundleLoader = createFileLoaderMethod.invoke(jsBundleLoaderClass, getReactApplicationContext(), latestJSBundleFile);
            } else {
                throw new NoSuchMethodException("Could not find a recognized 'createFileLoader' method");
            }

            bundleLoaderField.setAccessible(true);
            bundleLoaderField.set(instanceManager, latestJSBundleLoader);
        } catch (Exception e) {
            // RN < v0.31
            Field jsBundleField = instanceManager.getClass().getDeclaredField("mJSBundleFile");
            jsBundleField.setAccessible(true);
            jsBundleField.set(instanceManager, latestJSBundleFile);
        }
    }

    private void loadBundleLegacy(final Activity currentActivity) {
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentActivity.recreate();
            }
        });
    }

    private void loadBundle(final Activity currentActivity) {

        if (currentActivity == null) {
            // The currentActivity can be null if it is backgrounded / destroyed, so we simply
            // no-op to prevent any null pointer exceptions.
            return;
        } else if (!ReactActivity.class.isInstance(currentActivity)) {
            // Our preferred reload logic relies on the user's Activity inheriting
            // from the core ReactActivity class, so if it doesn't, we fallback
            // early to our legacy behavior.
            loadBundleLegacy(currentActivity);
        } else {
            try {
                ReactActivity reactActivity = (ReactActivity) currentActivity;
                ReactInstanceManager instanceManager;

                // #1) Get the ReactInstanceManager instance, which is what includes the
                //     logic to reload the current React context.
                try {
                    // In RN 0.29, the "mReactInstanceManager" field yields a null value, so we try
                    // to get the instance manager via the ReactNativeHost, which only exists in 0.29.
                    Method getApplicationMethod = ReactActivity.class.getMethod("getApplication");
                    Object reactApplication = getApplicationMethod.invoke(reactActivity);
                    Class<?> reactApplicationClass = tryGetClass(REACT_APPLICATION_CLASS_NAME);
                    Method getReactNativeHostMethod = reactApplicationClass.getMethod("getReactNativeHost");
                    Object reactNativeHost = getReactNativeHostMethod.invoke(reactApplication);
                    Class<?> reactNativeHostClass = tryGetClass(REACT_NATIVE_HOST_CLASS_NAME);
                    Method getReactInstanceManagerMethod = reactNativeHostClass.getMethod("getReactInstanceManager");
                    instanceManager = (ReactInstanceManager) getReactInstanceManagerMethod.invoke(reactNativeHost);
                } catch (Exception e) {
                    // The React Native version might be older than 0.29, so we try to get the
                    // instance manager via the "mReactInstanceManager" field.
                    Field instanceManagerField = ReactActivity.class.getDeclaredField("mReactInstanceManager");
                    instanceManagerField.setAccessible(true);
                    instanceManager = (ReactInstanceManager) instanceManagerField.get(reactActivity);
                }

                // #3) Get the context creation method and fire it on the UI thread (which RN enforces)
                final Method recreateMethod = instanceManager.getClass().getMethod("recreateReactContextInBackground");

                final ReactInstanceManager finalizedInstanceManager = instanceManager;
                if (getJsBundlePath(mContext) != null) {
                    setJSBundle(finalizedInstanceManager, getJsBundlePath(mContext));

                }

                reactActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            recreateMethod.invoke(finalizedInstanceManager);
                        } catch (Exception e) {
                            // The recreation method threw an unknown exception
                            // so just simply fallback to restarting the Activity
                            loadBundleLegacy(currentActivity);
                        }
                    }
                });
            } catch (Exception e) {
                // Our reflection logic failed somewhere
                // so fall back to restarting the Activity
                loadBundleLegacy(currentActivity);
            }
        }
    }

    private Class tryGetClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private boolean hasInternet() {
        boolean flag;
        if (((ConnectivityManager) getReactApplicationContext().getSystemService(
                "connectivity")).getActiveNetworkInfo() != null)
            flag = true;
        else
            flag = false;
        return flag;
    }

    private void installAPK(Context context, File file) {
        if (file == null || !file.exists())
            return;
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file),
                "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static SharedPreferences getPreferences(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME,
                Context.MODE_MULTI_PROCESS);
        return pre;
    }

    public static String getJsBundlePath(Context context) {
        return getPreferences(context).getString(
                JS_BUNDLE_PATH, null);
    }

    public static void setJsBundlePath(String bundlePath, Context context) {
        Log.d(TAG, "====================setJsBundlePath:" + bundlePath);
        if (bundlePath == null) {
            Log.d(TAG, "====================delete old file begin");
            try {
                File temp = new File(JS_BUNDLE_LOCAL_PATH);
                if (temp.exists() && temp.isDirectory()) {
                    deleteDir(temp);
                }
                File temp2 = new File(LAST_JS_BUNDLE_LOCAL_PATH);
                if (temp2.exists() && temp2.isDirectory()) {
                    deleteDir(temp2);
                }
            } catch (Exception e) {
                Log.d(TAG, "delete old file failed" + e.getMessage());
            }
            Log.d(TAG, "====================delete old file end");
        }
        set(JS_BUNDLE_PATH, bundlePath, context);
    }


    public static int getJsBundleVersionCode(Context context) {
        return getPreferences(context).getInt(
                JS_BUNDLE_VERSION_CODE, 0);
    }

    public static void setJsBundleVersionCode(int bundleVersionCode, Context context) {
        set(
                JS_BUNDLE_VERSION_CODE, bundleVersionCode, context);
    }

    public static int getUpdatedAppVersionCode(Context context) {
        return getPreferences(context).getInt(
                UPDATED_APP_VERSION_CODE, 0);
    }

    public static void setUpdatedAppVersionCode(int updatedAppVersionCode, Context context) {
        set(
                UPDATED_APP_VERSION_CODE, updatedAppVersionCode, context);
    }

    /**
     * 获取App安装包信息
     *
     * @return
     */
    public static PackageInfo getPackageInfo(Context context) {
        PackageInfo info = null;
        try {
            info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace(System.err);
        }
        if (info == null)
            info = new PackageInfo();
        return info;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void apply(SharedPreferences.Editor editor) {
        if (sIsAtLeastGB) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    public static void set(String key, int value, Context context) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt(key, value);
        apply(editor);
    }

    public static void set(String key, boolean value, Context context) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean(key, value);
        apply(editor);
    }

    public static void set(String key, String value, Context context) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(key, value);
        apply(editor);
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

}