package com.lph.constants;

/**
 * SimpleDes:
 * Creator: Sindi
 * Date: 2021-06-21
 * UseDes:
 */
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
