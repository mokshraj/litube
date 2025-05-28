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

  /** Filter the enabled script names from given script list.
   * @param scripts The script list need to be filtered.
   * @return The filtered script list.
   */
  public static List<String> filter(List<String> scripts) {
    MMKV mmkv = MMKV.defaultMMKV();
    return scripts.stream()
        .filter(
            script -> {
              ExtensionType type =
                  ExtensionType.getExtension(FilenameUtils.removeExtension(script));
              return type == null || mmkv.getBoolean(type.getName(), type.getDefaultEnable());
            })
        .collect(Collectors.toList());
  }

  public void enableExtension(ExtensionType type, Boolean enable) {
    mmkv.putBoolean(type.getName(), enable);
  }

  public Boolean isEnable(ExtensionType type) {
    return mmkv.getBoolean(type.getName(), type.getDefaultEnable());
  }
}
