package wang.wind.rn.codeupdate;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;

import java.lang.reflect.Field;

/**
 * Created by wangduo on 16/3/7.
 */
public class VersionUpdate {
    private Boolean success;
    private String errorMsg;
    private int updateType=0; // 0无更新 1 apk更新  2 jsbundle 更新
    private String downloadUrl;
    private String versionName;
    private int versionCode;
    private String jsBundleVersion;
    private int jsBundleVersionCode;
    private String changeLog;
    private Boolean slight;

    public Boolean getSlight() {
        return slight;
    }

    public void setSlight(Boolean slight) {
        this.slight = slight;
    }

    public VersionUpdate(ReadableMap options){
//        this.setChangeLog(options.getString("changeLog"));
//        this.setDownloadUrl(options.getString("downloadUrl"));
//        this.setSuccess(options.getBoolean("success"));
//        this.setUpdateType(options.getInt("updateType"));
//        this.setVersionCode(options.getInt("versionCode"));
//        this.setVersionName(options.getString("versionName"));

        ReadableMapKeySetIterator iterator = options.keySetIterator();
        while (iterator.hasNextKey()) {
            Field field = null;
            try {
                String key = iterator.nextKey();
                field =VersionUpdate.class.getDeclaredField(key);
                Class<?> type = field.getType();
                System.out.println(key+":"+type);
                if(type == String.class){
                    field.set(this,options.getString(key));
                }else if(type == Boolean.class) {
                    field.set(this,options.getBoolean(key));
                }else if(type == int.class){
                    field.set(this,options.getInt(key));
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
        if(options.hasKey("jsBundleVersionCode")){
            this.setJsBundleVersionCode(options.getInt("jsBundleVersionCode"));
        }
    }
    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public int getUpdateType() {
        return updateType;
    }

    public void setUpdateType(int updateType) {
        this.updateType = updateType;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getJsBundleVersion() {
        return jsBundleVersion;
    }

    public void setJsBundleVersion(String jsBundleVersion) {
        this.jsBundleVersion = jsBundleVersion;
    }

    public int getJsBundleVersionCode() {
        return jsBundleVersionCode;
    }

    public void setJsBundleVersionCode(int jsBundleVersionCode) {
        this.jsBundleVersionCode = jsBundleVersionCode;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }
}
