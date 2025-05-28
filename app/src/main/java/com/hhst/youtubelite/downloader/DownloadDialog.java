package com.hhst.youtubelite.downloader;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hhst.youtubelite.FullScreenImageActivity;
import com.hhst.youtubelite.MainActivity;
import com.hhst.youtubelite.R;
import com.squareup.picasso.Picasso;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class DownloadDialog {
  private final Context context;

  private final String url;
  private final ExecutorService executor;
  private final CountDownLatch detailsLatch;
  private final CountDownLatch formatsLatch;
  private DownloadDetails details;
  private List<VideoFormat> formats;
  private View dialogView;

  public DownloadDialog(String url, String detailsData, Context context) {
    this.url = url;
    this.context = context;
    executor = Executors.newCachedThreadPool();
    detailsLatch = new CountDownLatch(1);
    formatsLatch = new CountDownLatch(1);
    executor.submit(
        () -> {
          try {
            // try to get details from cache
            details = Downloader.infoWithCache(url, detailsData);
            detailsLatch.countDown();
          } catch (Throwable e) {
            // avoid some unnecessary toast
            if (e instanceof InterruptedException) return;
            Log.e(
                context.getString(R.string.failed_to_load_video_details),
                Log.getStackTraceString(e));
            new Handler(Looper.getMainLooper())
                .post(
                    () ->
                        Toast.makeText(
                                context, R.string.failed_to_load_video_details, Toast.LENGTH_SHORT)
                            .show());
          }
        });
    executor.submit(
        () -> {
          try {
            // try to get formats from cache
            formats = Downloader.fetchFormats(url);
            formatsLatch.countDown();
          } catch (Throwable e) {
            // avoid some unnecessary toast
            if (e instanceof InterruptedException) return;
            Log.e(
                context.getString(R.string.failed_to_load_video_formats),
                Log.getStackTraceString(e));
            new Handler(Looper.getMainLooper())
                .post(
                    () ->
                        Toast.makeText(
                                context, R.string.failed_to_load_video_formats, Toast.LENGTH_SHORT)
                            .show());
          }
        });
  }

  public static String formatSize(long length) {
    if (length < 0) {
      return "Invalid size";
    }

    if (length == 0) {
      return "0";
    }

    int unitIndex = 0;

    String[] UNITS = {"B", "KB", "MB", "GB", "TB"};
    double size = length;

    while (size >= 1024 && unitIndex < UNITS.length - 1) {
      size /= 1024;
      unitIndex++;
    }

    return String.format(Locale.US, "%.1f %s", size, UNITS[unitIndex]);
  }

  public void show() {

    dialogView = View.inflate(context, R.layout.download_dialog, null);
    ProgressBar progressBar = dialogView.findViewById(R.id.loadingBar);
    if (progressBar != null && details == null) progressBar.setVisibility(View.VISIBLE);

    AlertDialog dialog =
        new MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.download))
            .setView(dialogView)
            .setCancelable(true)
            .create();

    dialog.setOnDismissListener(dialogInterface -> executor.shutdownNow());

    ImageView imageView = dialogView.findViewById(R.id.download_image);
    EditText editText = dialogView.findViewById(R.id.download_edit_text);
    Button videoButton = dialogView.findViewById(R.id.button_video);
    Button thumbnailButton = dialogView.findViewById(R.id.button_thumbnail);
    final Button audioButton = dialogView.findViewById(R.id.button_audio);
    final Button cancelButton = dialogView.findViewById(R.id.button_cancel);
    final Button downloadButton = dialogView.findViewById(R.id.button_download);

    executor.submit(
        () -> {
          try {
            detailsLatch.await();
            if (progressBar != null && progressBar.getVisibility() == View.VISIBLE)
              dialogView.post(() -> progressBar.setVisibility(View.GONE));
            // load image
            loadImage(imageView);
            // load default video name
            loadVideoName(editText);
          } catch (InterruptedException ignored) {
          }
        });

    // state
    final AtomicBoolean isVideoSelected = new AtomicBoolean(false);
    final AtomicBoolean isThumbnailSelected = new AtomicBoolean(false);
    final AtomicBoolean isAudioSelected = new AtomicBoolean(false);
    final AtomicReference<VideoFormat> selectedQuality = new AtomicReference<>(null);

    // set button default background color
    videoButton.setBackgroundColor(context.getColor(android.R.color.darker_gray));
    thumbnailButton.setBackgroundColor(context.getColor(android.R.color.darker_gray));
    audioButton.setBackgroundColor(context.getColor(android.R.color.darker_gray));

    // get theme color
    TypedValue value = new TypedValue();
    context
        .getTheme()
        .resolveAttribute(com.google.android.material.R.attr.colorPrimary, value, true);
    final int themeColor = value.data;

    // on video button clicked
    videoButton.setOnClickListener(
        v -> showVideoQualityDialog(selectedQuality, isVideoSelected, videoButton, themeColor));

    // on thumbnail button clicked
    thumbnailButton.setOnClickListener(
        v -> {
          if (details == null) {
            return;
          }
          isThumbnailSelected.set(!isThumbnailSelected.get());
          thumbnailButton.setSelected(isThumbnailSelected.get());
          if (isThumbnailSelected.get()) {
            thumbnailButton.setBackgroundColor(themeColor);
          } else {
            thumbnailButton.setBackgroundColor(context.getColor(android.R.color.darker_gray));
          }
        });

    // on audio-only button clicked
    audioButton.setOnClickListener(
        v -> {
          if (details == null) {
            return;
          }
          isAudioSelected.set(!isAudioSelected.get());
          audioButton.setSelected(isAudioSelected.get());
          if (isAudioSelected.get()) {
            audioButton.setBackgroundColor(themeColor);
          } else {
            audioButton.setBackgroundColor(context.getColor(android.R.color.darker_gray));
          }
        });

    // on download button clicked
    downloadButton.setOnClickListener(
        v -> {
          // fixed in live page
          if (details == null) {
            dialog.dismiss();
            return;
          }

          if (!isVideoSelected.get() && !isThumbnailSelected.get() && !isAudioSelected.get()) {
            dialogView.post(
                () ->
                    Toast.makeText(context, R.string.select_something_first, Toast.LENGTH_SHORT)
                        .show());
            return;
          }

          String fileName = sanitizeFileName(editText.getText().toString().trim());
          String thumbnail = isThumbnailSelected.get() ? details.getThumbnail() : null;
          // check permissions
          ((MainActivity) context).requestPermissions();
          ((MainActivity) context)
              .downloadService.initiateDownload(
                  new DownloadTask(
                      url,
                      fileName,
                      thumbnail,
                      selectedQuality.get(),
                      isAudioSelected.get(),
                      DownloaderState.RUNNING,
                      null,
                      null,
                      null));
          dialog.dismiss();
        });

    cancelButton.setOnClickListener(v -> dialog.dismiss());

    // show dialog
    dialog.show();
  }

  private void loadImage(ImageView imageView) {
    try {
      new Handler(Looper.getMainLooper())
          .post(
              () -> {
                // use picasso to load and cache thumbnail
                Picasso.get().load(details.getThumbnail()).into(imageView);
                // on image clicked
                imageView.setOnClickListener(
                    view ->
                        executor.submit(
                            () -> {
                              Intent intent = new Intent(context, FullScreenImageActivity.class);
                              intent.putExtra("thumbnail", details.getThumbnail());
                              intent.putExtra(
                                  "filename",
                                  sanitizeFileName(
                                      String.format(
                                              "%s-%s", details.getTitle(), details.getAuthor())
                                          .trim()));
                              context.startActivity(intent);
                            }));
              });
    } catch (Exception e) {
      Log.e(context.getString(R.string.failed_to_load_image), Log.getStackTraceString(e));
      dialogView.post(
          () -> Toast.makeText(context, R.string.failed_to_load_image, Toast.LENGTH_SHORT).show());
    }
  }

  private void loadVideoName(EditText editText) {
    executor.submit(
        () -> {
          String title = details.getTitle();
          String author = details.getAuthor();
          String video_default_name = String.format("%s-%s", title, author);
          dialogView.post(() -> editText.setText(video_default_name));
        });
  }

  private void showVideoQualityDialog(
      AtomicReference<VideoFormat> selectedQuality,
      AtomicBoolean isVideoSelected,
      Button videoButton,
      int themeColor) {
    View dialogView = View.inflate(context, R.layout.quality_selector, null);
    ProgressBar progressBar = dialogView.findViewById(R.id.loadingBar2);
    if (progressBar != null && formats == null) progressBar.setVisibility(View.VISIBLE);
    AlertDialog qualityDialog =
        new MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.video_quality))
            .setView(dialogView)
            .create();

    LinearLayout quality_selector = dialogView.findViewById(R.id.quality_container);
    Button cancelButton = dialogView.findViewById(R.id.button_cancel);
    Button confirmButton = dialogView.findViewById(R.id.button_confirm);

    // create radio button dynamically
    // get video quality labels
    List<String> quality_labels = new ArrayList<>();
    // checked checkbox view, for mutex checkbox
    AtomicReference<CheckBox> checked_box = new AtomicReference<>();
    AtomicReference<VideoFormat> selected_format = new AtomicReference<>();
    // avoid trigger radioGroup.setOnCheckedChangeListener when initiate the radio button check
    // state
    executor.submit(
        () -> {
          try {
            formatsLatch.await();
            if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
              dialogView.post(() -> progressBar.setVisibility(View.GONE));
              qualityDialog.dismiss();
              dialogView.post(
                  () ->
                      showVideoQualityDialog(
                          selectedQuality, isVideoSelected, videoButton, themeColor));
            }
            AtomicLong audioSize = new AtomicLong();
            formats.forEach(
                it -> {
                  String ext = it.getExt();
                  if ("m4a".equals(ext)) {
                    audioSize.updateAndGet(current -> Math.max(it.getFileSize(), current));
                  }
                  List<String> bad_formats = List.of("233", "234", "616");
                  String format_note = it.getFormatNote();
                  if ("mp4".equals(ext)
                      && !bad_formats.contains(it.getFormatId())
                      && format_note != null) {
                    // avoid duplicate labels
                    if (!quality_labels.contains(it.getFormatNote())) {
                      quality_labels.add(it.getFormatNote());
                      CheckBox choice = new CheckBox(context);
                      choice.setText(
                          String.format(
                              "%s (%s)",
                              it.getFormatNote(), formatSize(audioSize.get() + it.getFileSize())));
                      choice.setLayoutParams(
                          new RadioGroup.LayoutParams(
                              RadioGroup.LayoutParams.MATCH_PARENT,
                              RadioGroup.LayoutParams.WRAP_CONTENT));
                      choice.setOnCheckedChangeListener(
                          (v, isChecked) -> {
                            if (isChecked) {
                              if (checked_box.get() != null) {
                                checked_box.get().setChecked(false);
                              }
                              selected_format.set(it);
                              checked_box.set((CheckBox) v);
                            } else {
                              selected_format.set(null);
                              checked_box.set(null);
                            }
                          });
                      quality_selector.addView(choice);
                      if (selectedQuality.get() != null && selectedQuality.get().equals(it)) {
                        choice.setChecked(true);
                      }
                    }
                  }
                });
          } catch (Exception e) {
            Log.e("When show VideoQualityDialog", Log.getStackTraceString(e));
          }
        });

    cancelButton.setOnClickListener(v -> qualityDialog.dismiss());
    confirmButton.setOnClickListener(
        v -> {
          if (checked_box.get() == null) {
            selectedQuality.set(null);
            isVideoSelected.set(false);
            videoButton.setBackgroundColor(context.getColor(android.R.color.darker_gray));
          } else {
            selectedQuality.set(selected_format.get());
            isVideoSelected.set(true);
            videoButton.setBackgroundColor(themeColor);
          }
          qualityDialog.dismiss();
        });

    qualityDialog.show();
  }

  private String sanitizeFileName(String fileName) {
    Pattern INVALID_FILENAME_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");
    String cleanFilename = INVALID_FILENAME_PATTERN.matcher(fileName).replaceAll("_");
    // avoid too long filename
    return cleanFilename.length() > 60 ? cleanFilename.substring(0, 60) + "..." : cleanFilename;
  }
}
