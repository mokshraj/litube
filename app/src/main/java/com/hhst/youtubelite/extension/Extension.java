package com.hhst.youtubelite.extension;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Extension {
    private ExtensionType type;
    private Boolean enabled;
    private int description;

}
