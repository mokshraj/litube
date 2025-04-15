package com.hhst.youtubelite.extension;

import android.content.Context;

import com.tencent.mmkv.MMKV;

import java.util.List;
import java.util.stream.Collectors;

public class ExtensionManager {

    private final MMKV mmkv;

    public ExtensionManager(Context context) {
        MMKV.initialize(context);
        mmkv = MMKV.defaultMMKV();
    }

    public void enableExtension(ExtensionType type, Boolean enable) {
        mmkv.putBoolean(type.getScript(), enable);
    }

    public Boolean isEnable(ExtensionType type) {
        return mmkv.getBoolean(type.getScript(), true);
    }

    public static List<String> filter(Context context, List<String> scripts) {
        MMKV.initialize(context);
        MMKV mmkv = MMKV.defaultMMKV();
        return scripts.stream().filter(script -> mmkv.getBoolean(script, true)).collect(Collectors.toList());
    }


}
