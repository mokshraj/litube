package com.hhst.youtubelite.extension;

import android.content.Context;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hhst.youtubelite.R;
import java.util.List;

public class ExtensionDialog {

  private final Context context;
  private final ExtensionManager manager;

  public ExtensionDialog(Context context) {
    this.context = context;
    manager = new ExtensionManager();
  }

  /** Display extension overview */
  public void show() {
    // Create the base dialog builder
    MaterialAlertDialogBuilder builder =
        new MaterialAlertDialogBuilder(context)
            .setTitle(R.string.extension)
            .setCancelable(true)
            // Set menu items (but delay setting the click listener)
            .setItems(
                new CharSequence[] {
                  context.getString(R.string.general),
                  context.getString(R.string.player),
                  context.getString(R.string.video),
                  context.getString(R.string.ads),
                  context.getString(R.string.downloader),
                  context.getString(R.string.other)
                },
                null // Click listener will be set later
                )
            // Add "Close" button
            .setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss());

    // Create the dialog
    AlertDialog dialog = builder.create();

    // When the dialog is shown, set item click listener
    dialog.setOnShowListener(
        dlg -> {
          ListView listView = dialog.getListView();
          listView.setOnItemClickListener(
              (parent, view, position, id) -> {
                // Open the detailed dialog for the selected category
                switch (position) {
                  case 0: // General
                    show(
                        List.of(
                            new Extension(
                                ExtensionType.DISPLAY_DISLIKES,
                                manager.isEnable(ExtensionType.DISPLAY_DISLIKES),
                                R.string.display_dislikes),
                            new Extension(
                                ExtensionType.HIDE_SHORTS,
                                manager.isEnable(ExtensionType.HIDE_SHORTS),
                                R.string.hide_shorts),
                            new Extension(
                                ExtensionType.LIVE_CHAT,
                                manager.isEnable(ExtensionType.LIVE_CHAT),
                                R.string.enable_live_chat)),
                        R.string.general);
                    break;
                  case 1: // Player
                    show(
                        List.of(
                            new Extension(
                                ExtensionType.SKIP_SPONSORS,
                                manager.isEnable(ExtensionType.SKIP_SPONSORS),
                                R.string.skip_sponsors)),
                        R.string.player);
                    break;
                  case 2: // Video
                    show(
                        List.of(
                            new Extension(
                                ExtensionType.H264IFY,
                                manager.isEnable(ExtensionType.H264IFY),
                                R.string.h264ify)),
                        R.string.video);
                    break;
                  case 3: // Ads
                    show(List.of(), R.string.ads);
                    break;
                  case 4: // Downloader
                    show(List.of(), R.string.downloader);
                    break;
                  case 5: // Other
                    show(
                        List.of(
                            new Extension(
                                ExtensionType.CPU_TAMER,
                                manager.isEnable(ExtensionType.CPU_TAMER),
                                R.string.cpu_tamer_experimental)),
                        R.string.other);
                    break;
                }
              });
        });

    // Show the dialog
    dialog.show();
  }

  /**
   * Display a detailed list of extensions
   *
   * @param extensions Extensions to display
   */
  public void show(List<Extension> extensions, int title) {
    // Prepare display texts and checked states for each extension
    CharSequence[] items = new CharSequence[extensions.size()];
    boolean[] checked = new boolean[extensions.size()];
    for (int i = 0; i < extensions.size(); ++i) {
      Extension ext = extensions.get(i);
      items[i] = context.getString(ext.getDescription());
      checked[i] = ext.getEnabled();
    }

    // Create the dialog instance first
    AlertDialog dialog =
        new MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setCancelable(true)
            // Use multi-choice list to toggle extensions
            .setMultiChoiceItems(
                items,
                checked,
                (dlg, which, isChecked) -> extensions.get(which).setEnabled(isChecked))
            // Set positive button with no click listener yet
            .setPositiveButton(R.string.confirm, null)
            // Negative button simply dismisses the dialog
            .setNegativeButton(R.string.cancel, (dlg, which) -> dlg.dismiss())
            .create();

    // After the dialog is shown, set the actual click behavior for the "Confirm" button
    dialog.setOnShowListener(
        dlg ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      boolean isChanged = false;

                      // Apply changes and detect if any setting was modified
                      for (Extension ext : extensions) {
                        if (!manager.isEnable(ext.getType()).equals(ext.getEnabled()))
                          isChanged = true;
                        manager.enableExtension(ext.getType(), ext.getEnabled());
                      }

                      if (isChanged) {
                        // Show restart dialog without dismissing this one
                        RestartDialog.show(context);
                      } else {
                        // If nothing changed, dismiss normally
                        dialog.dismiss();
                      }
                    }));

    // Show the final dialog
    dialog.show();
  }
}
