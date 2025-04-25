package com.hhst.youtubelite.extension;

import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum ExtensionType {
  DISPLAY_DISLIKES("display_dislikes", true),
  HIDE_SHORTS("hide_shorts", false),
  H264IFY("h264ify", false),
  LIVE_CHAT("live_chat", false);

  private static final Map<String, ExtensionType> EXT_MAP =
      Arrays.stream(ExtensionType.values())
          .collect(Collectors.toMap(ExtensionType::getScript, Function.identity()));
  private final String script;
  private final Boolean defaultEnable;

  ExtensionType(String script, Boolean defaultEnable) {
    this.script = script;
    this.defaultEnable = defaultEnable;
  }

  @Nullable
  public static ExtensionType getExtension(String script) {
    return EXT_MAP.get(script);
  }
}
