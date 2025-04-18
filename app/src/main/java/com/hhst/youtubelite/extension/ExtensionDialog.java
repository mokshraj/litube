package com.hhst.youtubelite.extension;


import android.content.Context;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hhst.youtubelite.R;

import java.util.List;

import lombok.Data;

public class ExtensionDialog {

    private final Context context;
    private final ExtensionManager manager;
    public ExtensionDialog(Context context) {
        this.context = context;
        manager = new ExtensionManager();
    }

    public void show() {
        List<Extension> extensions = List.of(
                new Extension(ExtensionType.DISPLAY_DISLIKES,
                        manager.isEnable(ExtensionType.DISPLAY_DISLIKES),
                        context.getString(R.string.display_dislikes)),
                new Extension(ExtensionType.HIDE_SHORTS,
                        manager.isEnable(ExtensionType.HIDE_SHORTS),
                        context.getString(R.string.hide_shorts)),
                new Extension(ExtensionType.H264IFY,
                        manager.isEnable(ExtensionType.H264IFY),
                        context.getString(R.string.h264ify))
        );

        CharSequence[] items = new CharSequence[extensions.size()];
        boolean[] checked = new boolean[extensions.size()];
        for (int i = 0; i < extensions.size(); ++i) {
            Extension ext = extensions.get(i);
            items[i] = ext.description;
            checked[i] = ext.enable;
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.extension)
                .setCancelable(true)
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) ->
                        extensions.get(which).enable = isChecked)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    boolean isChanged = false;
                    for (Extension ext : extensions) {
                        if (!manager.isEnable(ext.type).equals(ext.enable))
                            isChanged = true;
                        manager.enableExtension(ext.type, ext.enable);
                    }
                    if (isChanged) RestartDialog.show(context);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) ->
                        dialog.dismiss())
                .show();
    }



    @Data
    private static class Extension {
        ExtensionType type;
        Boolean enable;
        String description;
        Extension (ExtensionType type, Boolean enable, String description) {
            this.type = type;
            this.enable = enable;
            this.description = description;
        }
    }

}
