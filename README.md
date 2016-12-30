# react-native-code-update

##更新请求地址为： 
{code.update.url}/app/checkUpdate?platform=ios&app_id={yourAppId}&app_version_code={currentAppVersionCode}&js_version_code={currentJSVersionCode}
返回内容：res
{
    success: 1;
    updateType: 0;
} success:标示请求成功， 
updateType: 
0表示没有更新，
1表示为android平台时检测到新的apk安装包, res.versionName版本名称，res.versionCode:版本好，res.downloadUrl 下载连接，res.changeLog 更新日志
2表示有js包更新，res.jsBundleVersion js版本名称, res.jsBundleVersionCode js版本号，res.downloadUrl,res.changeLog
##ios:
info.plist中增加：
code.update.app_id（string） yourAppId
code.update.url（string） http://xxxxx
```Object-c
		jsCodeLocation = [RCTUpdateManager bundleURL];
		RCTRootView *rootView = [[RCTRootView alloc] initWithBundleURL:jsCodeLocation														moduleName:@"XXX"													initialProperties:nil
										launchOptions:launchOptions];
```

##android
在Application.java
```java
    @Override
    public void onCreate() {
        super.onCreate();
        ...
        ...
        AsyncHttpClient client = new AsyncHttpClient();//http请求用
        RCTUpdateManager.init("{yourAppName(会在存储中创建目录保存更新文件)}","appId","更新url（参考ios配置）",this,client);
    }
    
    private final ReactNativeHost mReactNativeHost = new ReactNativeHost(this) {
        ...
        ...
        @Override
        protected List<ReactPackage> getPackages() {
            //mAliPushPackage = new AliPushPackage(); // <------ Initialize the Package
            return Arrays.<ReactPackage>asList(
           new MainReactPackage(),
                    ...
                    new RCTUpdatePackage()
            );
        }
     }

```

##js
```js
  UpdateManager.checkUpdate(function (jsonObject) {
            if (jsonObject && jsonObject.updateType) {
                UpdateManager.doUpdate(jsonObject, function (success) {
                    if (success == "true") {
                        // Platform.OS==='android' && jsonObject.updateType == 2 && UpdateManager.askForReload();
                        if(jsonObject.updateType == 2){
                            if(Platform.OS==='android'){
                                UpdateManager.askForReload();
                            }else {
                                Alert.alert(
                                    '提示',
                                    '有数据更新是否刷新?',
                                    [
                                        {text: '刷新', onPress: () => {
                                            UpdateManager.askForReload();
                                        }},
                                        {text: '取消', onPress: () => {}, style: 'cancel'},
                                    ]
                                )
                            }
                        }
                    }else {
                        Alert.alert("提示,下载失败,请稍候再试");
                    }
                });
            }
        });
```
