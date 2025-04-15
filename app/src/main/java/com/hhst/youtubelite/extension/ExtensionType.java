package com.hhst.youtubelite.extension;


import lombok.Getter;

@Getter
public enum ExtensionType {

    DISPLAY_DISLIKES("display_dislikes.js"),
    HIDE_SHORTS("hide_shorts.css");

    private final String script;

    ExtensionType(String script) {
        this.script = script;
    }

}
