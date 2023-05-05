# 腾讯插件Shadow


## 接入

### 1.Project接入：

报错了也不管(因为有的错误，确实不影响结果，最终我们需要的是plugin-debug.zip(插件apk的产出物)和plugin-manager.apk(plugin-manager module的产出物))，一眼走到黑。

1. 导入
    1. 直接导入Shadow的[SDK](https://github.com/liaopen123/Shadow/tree/master/projects/sdk)到项目的根目录
    2. 直接导入Shadow的[buildScripts](https://github.com/liaopen123/Shadow/tree/master/buildScripts)到项目的根目录
    3. 在settings.gradle添加：
        
        ```groovy
        includeBuild 'sdk/coding'
        includeBuild 'sdk/core'
        includeBuild 'sdk/dynamic'
        ```
        
    4. 在project/build.gradle添加配置如下：
        
        ```groovy
        apply from: 'buildScripts/gradle/common.gradle'
        apply from: "buildScripts/gradle/maven.gradle"
        
        task clean(type: Delete) {//定义任务
            delete rootProject.buildDir
            dependsOn gradle.includedBuild('coding').task(':checks:clean')//添加依赖
            dependsOn gradle.includedBuild('coding').task(':lint:clean')
            dependsOn gradle.includedBuild('coding').task(':code-generator:clean')
            dependsOn gradle.includedBuild('core').task(':gradle-plugin:clean')
            dependsOn gradle.includedBuild('core').task(':common:clean')
            dependsOn gradle.includedBuild('core').task(':loader:clean')
            dependsOn gradle.includedBuild('core').task(':manager:clean')
            dependsOn gradle.includedBuild('core').task(':runtime:clean')
            dependsOn gradle.includedBuild('core').task(':activity-container:clean')
            dependsOn gradle.includedBuild('core').task(':transform:clean')
            dependsOn gradle.includedBuild('core').task(':transform-kit:clean')
            dependsOn gradle.includedBuild('dynamic').task(':dynamic-host:clean')
            dependsOn gradle.includedBuild('dynamic').task(':dynamic-loader:clean')
            dependsOn gradle.includedBuild('dynamic').task(':dynamic-loader-impl:clean')
            dependsOn gradle.includedBuild('dynamic').task(':dynamic-manager:clean')
        }
        ```
        

好了，build一下。

如果报错：


方案1：修改所有和`buildScripts` 相关的路径(麻烦)。

方案2：直接把`buildScripts` copy到所示的目录(方便)。

### 2. 创建constant module:

用来各个module间使用相同的变量,用到的module:plugin-host和plugin-manager。

```groovy
final public class Constant {
    public static final String KEY_PLUGIN_ZIP_PATH = "key_plugin_zip_path";
    public static final String KEY_ACTIVITY_CLASSNAME = "key_activity_classname";
    public static final String KEY_EXTRAS = "key_extras";
    public static final String KEY_PLUGIN_NAME = "key_plugin_name";
    public static final String PLUGIN_APP_NAME = "plugin-app";
    public static final String PLUGIN_OTHER_NAME = "plugin-other";
    public static final String KEY_PLUGIN_PART_KEY = "KEY_PLUGIN_PART_KEY";
    public static final String PART_KEY_PLUGIN_MAIN_APP = "sample-plugin-app";
    public static final String PART_KEY_PLUGIN_ANOTHER_APP = "sample-plugin-app2";
    public static final String PART_KEY_PLUGIN_BASE = "plugin-app";  //part-key  和 plugin-app  build.gradle中一致
    public static final int FROM_ID_NOOP = 1000;
    public static final long FROM_ID_START_ACTIVITY = 1002;//标识启动的是Activity
    public static final int FROM_ID_CALL_SERVICE = 1001;//标识启动的是Service
    public static final int FROM_ID_CLOSE = 1003;
    public static final int FROM_ID_LOAD_VIEW_TO_HOST = 1004;

}
```

### 3.宿主App接入：

(宿主App 就是大的APP，大APP接小APP)

1. 在**app/build.gradle**添加：
    
    ```groovy
    android {
    
        sourceSets {
            debug {
                assets.srcDir('build/generated/assets/plugin-manager/debug/')
                assets.srcDir('build/generated/assets/plugin-zip/debug/')
            }
            release {
                assets.srcDir('build/generated/assets/plugin-manager/release/')
                assets.srcDir('build/generated/assets/plugin-zip/release/')
            }
        }
    }
    
    dependencies {
      implementation project(path: ':constants')
        implementation 'com.tencent.shadow.core:common'//AndroidLogLoggerFactory
        implementation 'commons-io:commons-io:2.9.0'//sample-host从assets中复制插件用的
        implementation 'com.tencent.shadow.dynamic:dynamic-host'//腾讯插件框架shadow
    }
    ```
    
2. 添加代理 Activity 主题:
    
    在app/values/themes.xml声明：
    
    ```groovy
    <style name="PluginContainerActivity" parent="@android:style/Theme.NoTitleBar.Fullscreen">
            <item name="android:windowBackground">@android:color/transparent</item>
            <item name="android:windowContentOverlay">@null</item>
            <item name="android:windowNoTitle">true</item>
            <item name="android:windowIsTranslucent">true</item>
        </style>
    ```
    
3. ****清单文件注册代理Activity：****
    
    单文件中 预先 添加runtime lib的activity:
    
    **注意：这里声明的Activity的包名要和待会儿创建的plugin-runtime module包名保持一致 。Service：**`MainPluginProcessService`**路径和在宿主App中声明的保持一直即可。**
    
    ```groovy
    
             //android:multiprocess="true"//表示多进程(多插件的时候使用)
             //android:process=":plugin"//表示单进程(当个插件使用)
             //android:theme="@style/transparent_theme" //theme需要注册成透明
             //<!--以下这些类不打包在宿主app中，打包在plugin-runtime中，以便减少宿主方法数增量,Activity 路径需要和插件中的匹配，后面会说到-->
             
             <!--container 注册
             注意configChanges需要全注册
             theme需要注册成透明
    
             这些类不打包在host中，打包在runtime中，以便减少宿主方法数增量
             Activity 路径需要和插件中的匹配，后面会说到
             -->
            <activity
                android:name="com.lph.plugin_host.runtime.PluginDefaultProxyActivity"
                android:launchMode="standard"
                android:screenOrientation="portrait"
                android:configChanges="mcc|mnc|locale|touchscreen|keyboard|keyboardHidden|navigation|screenLayout|fontScale|uiMode|orientation|screenSize|smallestScreenSize|layoutDirection"
                android:hardwareAccelerated="true"
                android:theme="@style/PluginContainerActivity"
                android:process=":plugin" />
    
            <activity
                android:name="com.lph.plugin_host.runtime.PluginSingleInstance1ProxyActivity"
                android:launchMode="singleInstance"
                android:screenOrientation="portrait"
                android:configChanges="mcc|mnc|locale|touchscreen|keyboard|keyboardHidden|navigation|screenLayout|fontScale|uiMode|orientation|screenSize|smallestScreenSize|layoutDirection"
                android:hardwareAccelerated="true"
                android:theme="@style/PluginContainerActivity"
                android:process=":plugin" />
    
            <activity
                android:name="com.lph.plugin_host.runtime.PluginSingleTask1ProxyActivity"
                android:launchMode="singleTask"
                android:screenOrientation="portrait"
                android:configChanges="mcc|mnc|locale|touchscreen|keyboard|keyboardHidden|navigation|screenLayout|fontScale|uiMode|orientation|screenSize|smallestScreenSize|layoutDirection"
                android:hardwareAccelerated="true"
                android:theme="@style/PluginContainerActivity"
                android:process=":plugin" />
    
            <provider
                android:authorities="com.tencent.shadow.contentprovider.authority.dynamic"
                android:name="com.tencent.shadow.core.runtime.container.PluginContainerContentProvider" />
            <!--container 注册 end -->
    
            <service
                android:name=".plugin_manager.MainPluginProcessService"
                android:process=":plugin" />
    ```
    
4. ****在宿主中创建 PluginManager 管理工具：****

PluginManager 是用来装载插件，PluginManager 通过加载一个单独的apk来创建的。

下面会说怎么生成这个apk，先知道在宿主中怎么用。

4.1 创建``FixedPathPmUpdater``****文件升级器:****

```java

import com.tencent.shadow.dynamic.host.PluginManagerUpdater;

import java.io.File;
import java.util.concurrent.Future;

public class FixedPathPmUpdater implements PluginManagerUpdater {

    final private File apk;

    public FixedPathPmUpdater(File apk) {
        this.apk = apk;
    }

    /**
     * @return <code>true</code>表示之前更新过程中意外中断了
     */
    @Override
    public boolean wasUpdating() {
        return false;
    }
    /**
     * 更新
     *
     * @return 当前最新的PluginManager，可能是之前已经返回过的文件，但它是最新的了。
     */
    @Override
    public Future<File> update() {
        return null;
    }
    /**
     * 获取本地最新可用的
     *
     * @return <code>null</code>表示本地没有可用的
     */
    @Override
    public File getLatest() {
        return apk;
    }
    /**
     * 查询是否可用
     *
     * @param file PluginManagerUpdater返回的file
     * @return <code>true</code>表示可用，<code>false</code>表示不可用
     */
    @Override
    public Future<Boolean> isAvailable(final File file) {
        return null;
    }
}
```

4.2 ****创建插件进程服务:****

```java
import com.tencent.shadow.dynamic.host.PluginProcessService;
/**
 * 一个PluginProcessService（简称PPS）代表一个插件进程。插件进程由PPS启动触发启动。
 * 新建PPS子类允许一个宿主中有多个互不影响的插件进程。
 */
public class MainPluginProcessService extends PluginProcessService {
}
```

在清单文件注册：

```java
<service
            android:name=".MainPluginProcessService"
            android:process=":plugin" />
```

4.3 ****实现Log工具:****

```java
AndroidLoggerFactory.java
```

4.4 创建 `PluginHelper` ：

```java

import android.content.Context;
import android.os.Environment;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.lph.plugin_host.BuildConfig.DEBUG;
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
```

4.5 创建 Shadow 类:

```java
public class Shadow {
    public static PluginManager getPluginManager(File apk){
        final FixedPathPmUpdater fixedPathPmUpdater = new FixedPathPmUpdater(apk);
        File tempPm = fixedPathPmUpdater.getLatest();
        if (tempPm != null) {
            return new DynamicPluginManager(fixedPathPmUpdater);
        }
        return null;
    }
}
```

4.6 **在 Application 创建 PluginManager:**

```java

import android.app.ActivityManager;
import android.app.Application ;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.webkit.WebView;

import com.lph.plugin_host.plugin_manager.AndroidLoggerFactory;
import com.lph.plugin_host.plugin_manager.PluginHelper;
import com.lph.plugin_host.plugin_manager.Shadow;
import com.tencent.shadow.core.common.LoggerFactory;
import com.tencent.shadow.dynamic.host.DynamicRuntime;
import com.tencent.shadow.dynamic.host.PluginManager;
import java.io.File;
import java.util.concurrent.Future;
import static android.os.Process.myPid;

public class MyApplication extends Application {
    private static MyApplication sApp;
    private static PluginManager sPluginManager;//这个PluginManager对象在Manager升级前后是不变的。它内部持有具体实现，升级时更换具体实现

    @Override
    public void onCreate() {
        super.onCreate();
        sApp = this;
        detectNonSdkApiUsageOnAndroidP();
        setWebViewDataDirectorySuffix();
        LoggerFactory.setILoggerFactory(new AndroidLoggerFactory());

        if (isProcess(this, ":main_plugin")) {//TODO
            //在全动态架构中，Activity组件没有打包在宿主而是位于被动态加载的runtime，
            //为了防止插件crash后，系统自动恢复crash前的Activity组件，此时由于没有加载runtime而发生classNotFound异常，导致二次crash
            //因此这里恢复加载上一次的runtime
            DynamicRuntime.recoveryRuntime(this);
        }
        PluginHelper.getInstance().init(this);
    }

    private static void setWebViewDataDirectorySuffix() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        WebView.setDataDirectorySuffix(Application.getProcessName());
    }

    private static void detectNonSdkApiUsageOnAndroidP() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        boolean isRunningEspressoTest;
        try {
            Class.forName("androidx.test.espresso.Espresso");
            isRunningEspressoTest = true;
        } catch (Exception ignored) {
            isRunningEspressoTest = false;
        }
        if (isRunningEspressoTest) {
            return;
        }
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        builder.detectNonSdkApiUsage();
        StrictMode.setVmPolicy(builder.build());
    }

    public static MyApplication getApp() {
        return sApp;
    }

    public void loadPluginManager(File apk) {
        if (sPluginManager == null) {
            sPluginManager = Shadow.getPluginManager(apk);
        }
    }

    public PluginManager getPluginManager() {
        return sPluginManager;
    }

    private static boolean isProcess(Context context, String processName) {
        String currentProcName = "";
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == myPid()) {
                currentProcName = processInfo.processName;
                break;
            }
        }
        return currentProcName.endsWith(processName);
    }

```

1. 宿主APP 启动插件 Activity:
    
    这里需要注意的是:        Constant.KEY_ACTIVITY_CLASSNAME,
                    "com.lph.plugin_host.MainActivity"  插件 activity的完整包名
    
    布局文件：
    
    ```java
    <?xml version="1.0" encoding="utf-8"?>
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">
    
        <Button
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:onClick="start_plugin"
            android:text="启动插件" />
    
        <LinearLayout
            android:id="@+id/ll"
            android:layout_width="match_parent"
            android:layout_height="300dp"
            android:layout_marginTop="20dp"
            android:orientation="horizontal" />
    </LinearLayout>
    ```
    
    ```java
    package com.lph.plugin_host
    
    import android.os.Bundle
    import android.os.Handler
    import android.util.Log
    import android.view.View
    import android.widget.LinearLayout
    import androidx.appcompat.app.AppCompatActivity
    import com.lph.constants.Constant
    import com.lph.plugin_host.base.MyApplication
    import com.lph.plugin_host.plugin_manager.PluginHelper
    import com.tencent.shadow.dynamic.host.EnterCallback
    import org.jetbrains.annotations.Nullable
    
    class MainActivity : AppCompatActivity() {
        private var ll: LinearLayout? = null
        private val mHandler: Handler = Handler()
        override fun onCreate(@Nullable savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            ll = findViewById<LinearLayout>(R.id.ll)
        }
    
        fun start_plugin(view: View?) {
            PluginHelper.getInstance().singlePool.execute(Runnable {
                MyApplication.getApp().loadPluginManager(PluginHelper.getInstance().pluginManagerFile)
    
                /**
                 * @param context context
                 * @param formId  标识本次请求的来源位置，用于区分入口
                 * @param bundle  参数列表, 建议在参数列表加入自己的验证
                 * @param callback 用于从PluginManager实现中返回View
                 */
                val bundle = Bundle() //插件 zip，这几个参数也都可以不传，直接在 PluginManager 中硬编码
                bundle.putString(
                    Constant.KEY_PLUGIN_ZIP_PATH,
                    PluginHelper.getInstance().pluginZipFile.getAbsolutePath()
                )
                bundle.putString(
                    Constant.KEY_PLUGIN_NAME,
                    Constant.PLUGIN_APP_NAME
                ) // partKey 每个插件都有自己的 partKey 用来区分多个插件，如何配置在下面讲到
                bundle.putString(
                    Constant.KEY_ACTIVITY_CLASSNAME,
                    "com.lph.plugin_host.MainActivity"
                ) //要启动的插件的Activity页面
                bundle.putBundle(Constant.KEY_EXTRAS, Bundle()) // 要传入到插件里的参数
                MyApplication.getApp().getPluginManager().enter(
                    this@MainActivity,
                    Constant.FROM_ID_START_ACTIVITY,
                    bundle,
                    object : EnterCallback {
                        override fun onShowLoadingView(view: View) {
                            Log.e("PluginLoad", "onShowLoadingView")
                            loading(view)
                            //这里进行加载视图
                        }
    
                        override fun onCloseLoadingView() {
                            Log.e("PluginLoad", "onCloseLoadingView")
                        }
    
                        override fun onEnterComplete() {
                            // 启动成功
                            Log.e("PluginLoad", "onEnterComplete")
                        }
                    })
            })
        }
    
        private fun loading(view: View) {
            mHandler.post(Runnable {
                ll!!.removeAllViews()
                ll!!.addView(view)
            })
        }
    }
    ```
    

### 4. ****创建PluginManager 模块（用来生成插件管理apk包）****

   创建一个新 Module:plugin-manager    (app类型的module)。

1. 添加依赖：
    
    ```groovy
    dependencies {
    	  implementation project(path: ':constants')
        implementation 'com.tencent.shadow.dynamic:dynamic-manager'
        implementation 'com.tencent.shadow.core:manager'
        implementation 'com.tencent.shadow.dynamic:dynamic-loader'
        compileOnly 'com.tencent.shadow.core:common'
        compileOnly 'com.tencent.shadow.dynamic:dynamic-host'
    }
    ```
    
2. ****创建插件管理类FastPluginManager:**
    
    ```java
    /*
     * Tencent is pleased to support the open source community by making Tencent Shadow available.
     * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
     *
     * Licensed under the BSD 3-Clause License (the "License"); you may not use
     * this file except in compliance with the License. You may obtain a copy of
     * the License at
     *
     *     https://opensource.org/licenses/BSD-3-Clause
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     *
     */
    
    import android.content.Context;
    import android.os.RemoteException;
    import android.util.Pair;
    
    import com.tencent.shadow.core.common.Logger;
    import com.tencent.shadow.core.common.LoggerFactory;
    import com.tencent.shadow.core.manager.installplugin.InstalledPlugin;
    import com.tencent.shadow.core.manager.installplugin.InstalledType;
    import com.tencent.shadow.core.manager.installplugin.PluginConfig;
    import com.tencent.shadow.dynamic.host.FailedException;
    import com.tencent.shadow.dynamic.manager.PluginManagerThatUseDynamicLoader;
    
    import org.json.JSONException;
    
    import java.io.File;
    import java.io.IOException;
    import java.util.HashMap;
    import java.util.LinkedList;
    import java.util.List;
    import java.util.Map;
    import java.util.concurrent.Callable;
    import java.util.concurrent.ExecutionException;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    import java.util.concurrent.Future;
    import java.util.concurrent.TimeUnit;
    import java.util.concurrent.TimeoutException;
    
    public abstract class FastPluginManager extends PluginManagerThatUseDynamicLoader {
    
        private static final Logger mLogger = LoggerFactory.getLogger(FastPluginManager.class);
    
        private ExecutorService mFixedPool = Executors.newFixedThreadPool(4);
    
        public FastPluginManager(Context context) {
            super(context);
        }
    
        public InstalledPlugin installPlugin(String zip, String hash, boolean odex) throws IOException, JSONException, InterruptedException, ExecutionException {
            final PluginConfig pluginConfig = installPluginFromZip(new File(zip), hash);
            final String uuid = pluginConfig.UUID;
            List<Future> futures = new LinkedList<>();
            List<Future<Pair<String, String>>> extractSoFutures = new LinkedList<>();
            if (pluginConfig.runTime != null && pluginConfig.pluginLoader != null) {
                Future odexRuntime = mFixedPool.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        oDexPluginLoaderOrRunTime(uuid, InstalledType.TYPE_PLUGIN_RUNTIME,
                                pluginConfig.runTime.file);
                        return null;
                    }
                });
                futures.add(odexRuntime);
                Future odexLoader = mFixedPool.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        oDexPluginLoaderOrRunTime(uuid, InstalledType.TYPE_PLUGIN_LOADER,
                                pluginConfig.pluginLoader.file);
                        return null;
                    }
                });
                futures.add(odexLoader);
            }
            for (Map.Entry<String, PluginConfig.PluginFileInfo> plugin : pluginConfig.plugins.entrySet()) {
                final String partKey = plugin.getKey();
                final File apkFile = plugin.getValue().file;
                Future<Pair<String, String>> extractSo = mFixedPool.submit(() -> extractSo(uuid, partKey, apkFile));
                futures.add(extractSo);
                extractSoFutures.add(extractSo);
                if (odex) {
                    Future odexPlugin = mFixedPool.submit(new Callable() {
                        @Override
                        public Object call() throws Exception {
                            oDexPlugin(uuid, partKey, apkFile);
                            return null;
                        }
                    });
                    futures.add(odexPlugin);
                }
            }
    
            for (Future future : futures) {
                future.get();
            }
            Map<String, String> soDirMap = new HashMap<>();
            for (Future<Pair<String, String>> future : extractSoFutures) {
                Pair<String, String> pair = future.get();
                soDirMap.put(pair.first, pair.second);
            }
            onInstallCompleted(pluginConfig, soDirMap);
    
            return getInstalledPlugins(1).get(0);
        }
    
        protected void callApplicationOnCreate(String partKey) throws RemoteException {
            Map map = mPluginLoader.getLoadedPlugin();
            Boolean isCall = (Boolean) map.get(partKey);
            if (isCall == null || !isCall) {
                mPluginLoader.callApplicationOnCreate(partKey);
            }
        }
    
        private void loadPluginLoaderAndRuntime(String uuid, String partKey) throws RemoteException, TimeoutException, FailedException {
            if (mPpsController == null) {
                bindPluginProcessService(getPluginProcessServiceName(partKey));
                waitServiceConnected(10, TimeUnit.SECONDS);
            }
            loadRunTime(uuid);
            loadPluginLoader(uuid);
        }
    
        protected void loadPlugin(String uuid, String partKey) throws RemoteException, TimeoutException, FailedException {
            loadPluginLoaderAndRuntime(uuid, partKey);
            Map map = mPluginLoader.getLoadedPlugin();
            if (!map.containsKey(partKey)) {
                mPluginLoader.loadPlugin(partKey);
            }
        }
    
        protected abstract String getPluginProcessServiceName(String partKey);
    
    }
    ```
    
3. 创建业务实习类 `LphPluginManager`  ：
    
    需要注意的:getPluginProcessServiceName是在宿主app中声明的MainPluginProcessService类全路径包名
    
    ```java
    /*
     * Tencent is pleased to support the open source community by making Tencent Shadow available.
     * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
     *
     * Licensed under the BSD 3-Clause License (the "License"); you may not use
     * this file except in compliance with the License. You may obtain a copy of
     * the License at
     *
     *     https://opensource.org/licenses/BSD-3-Clause
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     *
     */
    
    import static com.lph.constants.Constant.PART_KEY_PLUGIN_BASE;
    
    import android.content.Context;
    import android.content.Intent;
    import android.os.Bundle;
    import android.os.RemoteException;
    import android.view.LayoutInflater;
    import android.view.View;
    
    import com.lph.constants.Constant;
    import com.tencent.shadow.core.manager.installplugin.InstalledPlugin;
    import com.tencent.shadow.dynamic.host.EnterCallback;
    
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    
    public class LphPluginManager extends FastPluginManager {
    
        private ExecutorService executorService = Executors.newSingleThreadExecutor();
    
        private Context mCurrentContext;
    
        public LphPluginManager(Context context) {
            super(context);
            mCurrentContext = context;
        }
    
        /**
         * @return PluginManager实现的别名，用于区分不同PluginManager实现的数据存储路径
         */
        @Override
        protected String getName() {
            return "test-dynamic-manager";
        }
    
        /**
         * @return 宿主中注册的PluginProcessService实现的类名
         */
        @Override
        protected String getPluginProcessServiceName(String partKey) {
            return "com.lph.plugin_host.plugin_manager.MainPluginProcessService";
        }
    
        @Override
        public void enter(final Context context, long fromId, Bundle bundle, final EnterCallback callback) {
            if (fromId == Constant.FROM_ID_NOOP) {
                //do nothing.
            } else if (fromId == Constant.FROM_ID_START_ACTIVITY) {
                onStartActivity(context, bundle, callback);
            } else if (fromId == Constant.FROM_ID_CLOSE) {
                close();
            } else if (fromId == Constant.FROM_ID_LOAD_VIEW_TO_HOST) {
                loadViewToHost(context, bundle);
            } else {
                throw new IllegalArgumentException("不认识的fromId==" + fromId);
            }
        }
    
        private void loadViewToHost(final Context context, Bundle bundle) {
            Intent pluginIntent = new Intent();
            pluginIntent.setClassName(
                    context.getPackageName(),
                    "com.tencent.shadow.sample.plugin.app.lib.usecases.service.HostAddPluginViewService"
            );
            pluginIntent.putExtras(bundle);
            try {
                mPluginLoader.startPluginService(pluginIntent);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    
        private void onStartActivity(final Context context, Bundle bundle, final EnterCallback callback) {
            final String pluginZipPath = bundle.getString(Constant.KEY_PLUGIN_ZIP_PATH);
            final String partKey = bundle.getString(Constant.KEY_PLUGIN_PART_KEY);
            final String className = bundle.getString(Constant.KEY_ACTIVITY_CLASSNAME);
            if (className == null) {
                throw new NullPointerException("className == null");
            }
            final Bundle extras = bundle.getBundle(Constant.KEY_EXTRAS);
    
            if (callback != null) {
                final View view = LayoutInflater.from(mCurrentContext).inflate(R.layout.activity_load_plugin, null);
                callback.onShowLoadingView(view);
            }
    
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        InstalledPlugin installedPlugin = installPlugin(pluginZipPath, null, true);
    
                        loadPlugin(installedPlugin.UUID, PART_KEY_PLUGIN_BASE);
    //                    loadPlugin(installedPlugin.UUID, PART_KEY_PLUGIN_MAIN_APP);
                        callApplicationOnCreate(PART_KEY_PLUGIN_BASE);
    //                    callApplicationOnCreate(PART_KEY_PLUGIN_MAIN_APP);
    
                        Intent pluginIntent = new Intent();
                        pluginIntent.setClassName(
                                context.getPackageName(),
                                className
                        );
                        if (extras != null) {
                            pluginIntent.replaceExtras(extras);
                        }
                        Intent intent = mPluginLoader.convertActivityIntent(pluginIntent);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mPluginLoader.startActivityInPluginProcess(intent);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if (callback != null) {
                        callback.onCloseLoadingView();
                    }
                }
            });
        }
    }
    ```
    
4. activity_load_plugin.xml:
    
    ```java
    <?xml version="1.0" encoding="utf-8"?>
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:orientation="vertical"
        android:background="#fcfcfc">
    
        <ProgressBar
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_gravity="center_horizontal" />
    
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal"
            android:text="正在启动插件" />
    
    </LinearLayout>
    ```
    
5. ****使用插件管理类****
    
    需要使用下面的路径和类名：
    
    - 两个类的路径一定是：com.tencent.shadow.dynamic.impl
    - 类名：ManagerFactoryImpl 和WhiteList
    
    ```java
    /*
     * Tencent is pleased to support the open source community by making Tencent Shadow available.
     * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
     *
     * Licensed under the BSD 3-Clause License (the "License"); you may not use
     * this file except in compliance with the License. You may obtain a copy of
     * the License at
     *
     *     https://opensource.org/licenses/BSD-3-Clause
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     *
     */
    
    package com.tencent.shadow.dynamic.impl;
    
    import android.content.Context;
    
    import com.lph.plugin_manager.LphPluginManager;
    import com.tencent.shadow.dynamic.host.ManagerFactory;
    import com.tencent.shadow.dynamic.host.PluginManagerImpl;
    
    /**
     * 此类包名及类名固定
     */
    public final class ManagerFactoryImpl implements ManagerFactory {
        @Override
        public PluginManagerImpl buildManager(Context context) {
            return new LphPluginManager(context);
        }
    }
    ```
    
    ```java
    /*
     * Tencent is pleased to support the open source community by making Tencent Shadow available.
     * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
     *
     * Licensed under the BSD 3-Clause License (the "License"); you may not use
     * this file except in compliance with the License. You may obtain a copy of
     * the License at
     *
     *     https://opensource.org/licenses/BSD-3-Clause
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     *
     */
    
    package com.tencent.shadow.dynamic.impl;
    
    /**
     * 此类包名及类名固定
     * classLoader的白名单
     * PluginManager可以加载宿主中位于白名单内的类
     */
    public interface WhiteList {
        String[] sWhiteList = new String[]
                {
                        "com.tencent.host.shadow",
                        "com.tencent.shadow.test.lib.constant",
                };
    }
    ```
    
6. 编译获取 manager apk：
    
    ```java
    ./gradlew assembleDebug
    ```
    
    ![Untitled](%E8%85%BE%E8%AE%AF%E6%8F%92%E4%BB%B6Shadow%204aac30e7c65647e18416267d890cd3d9/Untitled%203.png)
    

### 5. 创建 plugin-runtime module

     ****创建应用级模块 plugin-runtime****

1. 添加依赖
“plugin-runtime”应用模块主要放在宿主中注册的壳子，这个模块的 applicationd 可以随意。
    
    这个应用模块只需要下面的依赖，其他依赖都能删掉；
    清单文件也不需要任何配置；
    生成项目时自动创建的Activity 都可以删掉。
    
    ```groovy
    dependencies {
        implementation 'com.tencent.shadow.core:activity-container'
    }
    ```
    
2. 清单文件：
    
    ```groovy
    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.lph.plugin_host">
    
    </manifest>
    ```
    
3. 完成之前在宿主APP中声明的3个Activity的空实现：
    
    壳子路径包名要和宿主中注册的保持一致：
    
    `PluginDefaultProxyActivity`：
    
    ```java
    import com.tencent.shadow.core.runtime.container.PluginContainerActivity;
    public class PluginDefaultProxyActivity extends PluginContainerActivity {
    }
    ```
    
    `PluginSingleInstance1ProxyActivity`:
    
    ```java
    import com.tencent.shadow.core.runtime.container.PluginContainerActivity;
    public class PluginSingleInstance1ProxyActivity extends PluginContainerActivity {
    }
    ```
    
    `PluginSingleTask1ProxyActivity`:
    
    ```java
    public class PluginSingleTask1ProxyActivity extends PluginContainerActivity {
    }
    ```
    
    检查下宿主app/AndroidManifest.xml，配置的activity壳子路径是否一致。
    

### 6. 创建 plugin-****loader**** module:

****创建应用级模块 plugin-loader****

这个应用模块主要定义插件组件和壳子代理组件的配对关系。
PluginManager(插件管理器)在加载"插件"时，首先需要先加载"插件"中的runtime和loader，再通过loader的Binder（插件应该处于独立进程中避免native库冲突）操作loader进而加载业务App。
这个模块的applicationId 可以随意。

1. ****添加依赖****
    
    • 这个应用模块只需要下面的依赖，其他依赖都能删掉；
    
    • 清单文件也不需要任何配置；
    
    • 生成项目时自动创建的Activity 都可以删掉。
    
    ```groovy
    dependencies{
    implementation 'com.tencent.shadow.core:loader'
        implementation 'com.tencent.shadow.dynamic:dynamic-loader'
        implementation 'com.tencent.shadow.dynamic:dynamic-loader-impl'
        compileOnly 'com.tencent.shadow.core:runtime'
        compileOnly 'com.tencent.shadow.core:activity-container'
        compileOnly 'com.tencent.shadow.core:common'
        compileOnly 'com.tencent.shadow.dynamic:dynamic-host'//下面这行依赖是为了防止在proguard的时候找不到LoaderFactory接口
    }
    ```
    
2. **实现插件组件管理类**
    1. SampleComponentManager
        
        需要注意的:**runtime 模块中定义的壳子Activity, 路径类名保持一致，需要在宿主AndroidManifest.xml注册**
        
        ```groovy
        
        import android.content.ComponentName;
        import android.content.Context;
        
        import com.tencent.shadow.core.loader.infos.ContainerProviderInfo;
        import com.tencent.shadow.core.loader.managers.ComponentManager;
        
        public class SampleComponentManager extends ComponentManager {
        
            /**
             * runtime 模块中定义的壳子Activity, 路径类名保持一致，需要在宿主AndroidManifest.xml注册
             */
            private static final String DEFAULT_ACTIVITY = "com.lph.plugin_host.runtime.PluginDefaultProxyActivity";
            private static final String SINGLE_INSTANCE_ACTIVITY = "com.lph.plugin_host.runtime.PluginSingleInstance1ProxyActivity";
            private static final String SINGLE_TASK_ACTIVITY = "com.lph.plugin_host.runtime.PluginSingleTask1ProxyActivity";
        
            private Context context;
        
            public SampleComponentManager(Context context) {
                this.context = context;
            }
        
            /**
             * 配置插件Activity 到 壳子Activity的对应关系
             *
             * @param pluginActivity 插件Activity
             * @return 壳子Activity
             */
            @Override
            public ComponentName onBindContainerActivity(ComponentName pluginActivity) {
                switch (pluginActivity.getClassName()) {
                    /**
                     * 这里配置对应的对应关系, 启动不同启动模式的Acitvity
                     */
                }
                return new ComponentName(context, DEFAULT_ACTIVITY);
            }
        
            /**
             * 配置对应宿主中预注册的壳子contentProvider的信息
             */
            @Override
            public ContainerProviderInfo onBindContainerContentProvider(ComponentName pluginContentProvider) {
                return new ContainerProviderInfo(
                        "com.tencent.shadow.runtime.container.PluginContainerContentProvider",
                        "com.tencent.shadow.contentprovider.authority.dynamic");
            }
        
        }
        ```
        
    
    b. `SamplePluginLoader`:
    
    ```groovy
    
    import android.content.Context;
    
    import com.tencent.shadow.core.loader.ShadowPluginLoader;
    import com.tencent.shadow.core.loader.managers.ComponentManager;
    public class SamplePluginLoader extends ShadowPluginLoader {
    
        private final static String TAG = "shadow";
    
        private ComponentManager componentManager;
    
        public SamplePluginLoader(Context hostAppContext) {
            super(hostAppContext);
            componentManager = new SampleComponentManager(hostAppContext);
        }
    
        @Override
        public ComponentManager getComponentManager() {
            return componentManager;
        }
    }
    ```
    
3. **使用插件加载器**
    - 类名：CoreLoaderFactoryImpl
    - CoreLoaderFactoryImpl 的路径一定是：com.tencent.shadow.dynamic.loader.impl
    
    ```groovy
    package com.tencent.shadow.dynamic.loader.impl;
    
    import android.content.Context;
    
    import com.lph.plugin_loader.SamplePluginLoader;
    import com.tencent.shadow.core.loader.ShadowPluginLoader;
    
    import org.jetbrains.annotations.NotNull;
    
    /**
     * 这个类的包名类名是固定的。
     * <p>
     * 见com.tencent.shadow.dynamic.loader.impl.DynamicPluginLoader#CORE_LOADER_FACTORY_IMPL_NAME
     */
    public class CoreLoaderFactoryImpl implements CoreLoaderFactory {
    
        @NotNull
        @Override
        public ShadowPluginLoader build(@NotNull Context context) {
            return new SamplePluginLoader(context);
        }
    }
    ```
    

### 7. 创建插件模块**plugin-app：**

- 只要插件的包名和宿主的包名保持一致
1. 业务代码：

```groovy
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="hahahhahahahh    跑起来  冲啊"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

```groovy
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
```

1. 设置build.gradle
    
    ```groovy
    buildscript {//这个模块要放在plugins {} 之前
        repositories {
            google()
            jcenter()
        }
    
        dependencies {
            classpath 'com.tencent.shadow.core:runtime'
            classpath 'com.tencent.shadow.core:activity-container'
            classpath 'com.tencent.shadow.core:gradle-plugin'
            classpath 'org.javassist:javassist:3.22.0-GA'
        }
    }
    
    apply plugin: 'com.android.application'
    apply plugin: 'com.tencent.shadow.plugin'
    apply plugin: 'org.jetbrains.kotlin.android'
    
    android {
        namespace 'com.lph.plugin_host'
        compileSdk 33
    
        defaultConfig {
            applicationId "com.lph.plugin_host"
            minSdk 24
            targetSdk 33
            versionCode 1
            versionName "1.0"
    
            testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        }
    
        buildTypes {
            debug {
                minifyEnabled false
                proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            }
            release {
                minifyEnabled false
                proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            }
        }
        compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
        kotlinOptions {
            jvmTarget = '1.8'
        }
    
        aaptOptions {
            additionalParameters "--package-id", "0x7E", "--allow-reserved-package-id"
        }
    }
    
    dependencies {
    
        implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
        implementation 'androidx.core:core-ktx:1.2.0'
        implementation 'androidx.appcompat:appcompat:1.1.0'
        implementation 'com.google.android.material:material:1.6.0'
        implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
    
        //如果不以compileOnly方式依赖，会导致其他Transform或者Proguard找不到这些类
        compileOnly 'com.tencent.shadow.core:runtime'
    }
    
    shadow {
        transform {
    //   useHostContext = ['abc']
        }
    
        packagePlugin {
            pluginTypes {
                debug {
                    loaderApkConfig = new Tuple2('plugin-loader-debug.apk', ':plugin-loader:assembleDebug')
                    runtimeApkConfig = new Tuple2('plugin-runtime-debug.apk', ':plugin-runtime:assembleDebug')
                    pluginApks {
                        pluginApk1 {
                            businessName = 'plugin-app'
                            partKey = 'plugin-app'
                            buildTask = ':plugin-app:assembleDebug'
                            apkPath = 'plugin-app/build/outputs/apk/plugin/debug/plugin-app-plugin-debug.apk'
                        }
                    }
                }
    
                release {
                    loaderApkConfig = new Tuple2('plugin-loader-release.apk', ':plugin-loader:assembleRelease')
                    runtimeApkConfig = new Tuple2('plugin-runtime-release.apk', ':plugin-runtime:assembleRelease')
                    pluginApks {
                        pluginApk1 {
                            businessName = 'plugin-app'
                            partKey = 'plugin-app'
                            buildTask = ':plugin-app:assembleRelease'
                            apkPath = 'plugin-app/build/outputs/apk/release/plugin-app-release.apk'
                        }
                    }
                }
            }
    
            loaderApkProjectPath = 'plugin-loader'
            runtimeApkProjectPath = 'plugin-runtime'
    
            version = 1
            compactVersion = [1, 2, 3]
            uuidNickName = "1.0"
        }
    }
    ```
    
    执行：`./gradlew packageDebugPlugin`  即可得到plugin-debug.zip
    
    生成zip路径：
    
    ```
    build/plugin-debug.zip
    ```
    
    将 manager.apk 和 plugin-debug.zip 放到指定位置(demo中是host的assets目录下)即可享受。
    
    主要参考：
    
    [Shadow 插件化框架接入步骤——SDK版本_腾讯shadow接入_布拉格烈的博客-CSDN博客](https://blog.csdn.net/weixin_42150080/article/details/117474116)
    
    [Android Tencent Shadow 插件接入指南](https://www.jianshu.com/p/f00dc837227f)
