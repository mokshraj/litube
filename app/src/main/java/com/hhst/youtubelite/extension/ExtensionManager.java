package com.hhst.youtubelite.extension;

import com.tencent.mmkv.MMKV;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;

public class ExtensionManager {

  private final MMKV mmkv;

  public ExtensionManager() {
    mmkv = MMKV.defaultMMKV();
  }

  public static List<String> filter(List<String> scripts) {
    MMKV mmkv = MMKV.defaultMMKV();
    return scripts.stream()
        .filter(
            script -> {
              ExtensionType type =
                  ExtensionType.getExtension(FilenameUtils.removeExtension(script));
              return type == null || mmkv.getBoolean(type.getScript(), type.getDefaultEnable());
            })
        .collect(Collectors.toList());
  }

  public void enableExtension(ExtensionType type, Boolean enable) {
    mmkv.putBoolean(type.getScript(), enable);
  }

  public Boolean isEnable(ExtensionType type) {
    return mmkv.getBoolean(type.getScript(), type.getDefaultEnable());
  }
}
