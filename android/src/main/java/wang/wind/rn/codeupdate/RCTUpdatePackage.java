package wang.wind.rn.codeupdate;

import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wangduo on 2016/12/16.
 */

public class RCTUpdatePackage implements ReactPackage {

    private static ReactInstanceManager mReactInstanceManager;

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
//        return null;
        List<NativeModule> list = new ArrayList<NativeModule>();
        list.add(new RCTUpdateManager(reactContext));
        return list;
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
    public static void setReactInstanceManager(ReactInstanceManager reactInstanceManager) {
        mReactInstanceManager = reactInstanceManager;
    }
    static ReactInstanceManager getReactInstanceManager() {
        return mReactInstanceManager;
    }

}
