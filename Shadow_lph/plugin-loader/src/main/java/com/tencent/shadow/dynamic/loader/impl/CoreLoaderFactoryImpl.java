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
