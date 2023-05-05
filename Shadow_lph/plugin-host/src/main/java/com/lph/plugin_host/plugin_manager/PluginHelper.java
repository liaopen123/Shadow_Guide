package com.lph.plugin_host.plugin_manager;

import static com.lph.plugin_host.BuildConfig.DEBUG;

import android.content.Context;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class PluginHelper {
    public final static String sPluginManagerName = "plugin-manager.apk";//动态加载的插件管理apk

    /**
     * 动态加载的插件包，里面包含以下几个部分，插件apk，插件框架apk（loader apk和runtime apk）, apk信息配置关系json文件
     */
    public final static String sPluginZip = DEBUG ? "plugin-debug.zip" : "plugin-release.zip";
    public File pluginManagerFile;
    public File pluginZipFile;
    public ExecutorService singlePool = Executors.newSingleThreadExecutor();
    private Context mContext;

    private static PluginHelper sInstance = new PluginHelper();
    public static PluginHelper getInstance() {
        return sInstance;
    }

    private PluginHelper() {
    }

    public void init(Context context) {
        pluginManagerFile = new File(context.getFilesDir(), sPluginManagerName);
        pluginZipFile = new File(context.getFilesDir(), sPluginZip);
        mContext = context.getApplicationContext();
        singlePool.execute(() -> preparePlugin());
    }

    private void preparePlugin() {
        try {
            InputStream is = mContext.getAssets().open(sPluginManagerName);
            FileUtils.copyInputStreamToFile(is, pluginManagerFile);
            InputStream zip = mContext.getAssets().open(sPluginZip);
            FileUtils.copyInputStreamToFile(zip, pluginZipFile);
        } catch (IOException e) {
            throw new RuntimeException("从assets中复制apk出错", e);
        }
    }
}
