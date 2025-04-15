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
        manager = new ExtensionManager(context);
    }

    public void show() {
        List<Extension> extensions = List.of(
                new Extension(ExtensionType.DISPLAY_DISLIKES,
                        manager.isEnable(ExtensionType.DISPLAY_DISLIKES),
                        context.getString(R.string.display_dislikes)),
                new Extension(ExtensionType.HIDE_SHORTS,
                        manager.isEnable(ExtensionType.HIDE_SHORTS),
                        context.getString(R.string.hide_shorts))
        );

        CharSequence[] items = extensions.stream()
                .map(extension -> extension.description)
                .toArray(CharSequence[]::new);

        boolean[] checked = new boolean[extensions.size()];
        for (int i = 0; i < extensions.size(); ++i) {
            checked[i] = extensions.get(i).enable;
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.extension)
                .setCancelable(true)
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) ->
                        extensions.get(which).enable = isChecked)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    for (Extension ext : extensions) {
                        manager.enableExtension(ext.type, ext.enable);
                    }
                    RestartDialog.show(context);
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
