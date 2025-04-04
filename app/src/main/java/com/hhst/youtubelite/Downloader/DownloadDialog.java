package com.hhst.youtubelite.Downloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.hhst.youtubelite.MainActivity;
import com.hhst.youtubelite.R;
import com.yausername.youtubedl_android.mapper.VideoFormat;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class DownloadDialog {
    private final Context context;

    private final String url;
    private final ExecutorService executor;
    private DownloadDetails details;
    private final CountDownLatch latch;
    private View dialogView;
    private AlertDialog qualityDialog;

    private ProgressBar progressBar;
    private ProgressBar progressBar2;

    private AtomicReference<VideoFormat> selectedQuality;
    private AtomicBoolean isVideoSelected;
    private Button buttonVideo;

    private final int themeColor;

    public DownloadDialog(String url, Context context) {
        this.url = url;
        this.context = context;
        // get theme color
        TypedValue value = new TypedValue();
        context.getTheme()
                .resolveAttribute(com.google.android.material.R.attr.colorPrimary,
                        value, true);
        themeColor = value.data;
        executor = Executors.newCachedThreadPool();
        latch = new CountDownLatch(1);
        executor.execute(() -> {
            try {
                details = Downloader.info(url);
                if (progressBar != null)
                    new Handler(Looper.getMainLooper()).post(() -> progressBar.setVisibility(View.GONE));
                if (progressBar2 != null && qualityDialog != null)
                    new Handler(Looper.getMainLooper()).post(() -> {
                        progressBar2.setVisibility(View.GONE);
                        qualityDialog.dismiss();
                        showVideoQualityDialog();
                    });
                latch.countDown();
            } catch (Throwable e) {
                // avoid some unnecessary toast
                if (e instanceof InterruptedException) return;
                Log.e("failed to load video detail", Log.getStackTraceString(e));
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, context.getString(R.string.failed_to_load_video_details) + Log.getStackTraceString(e), Toast.LENGTH_SHORT).show());

            }
        });
    }

    public void show() {

        dialogView = View.inflate(context, R.layout.download_dialog, null);
        progressBar = dialogView.findViewById(R.id.loadingBar);
        if (progressBar != null && details == null) progressBar.setVisibility(View.VISIBLE);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.download))
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialog.setOnDismissListener(dialogInterface -> executor.shutdownNow());

        ImageView imageView = dialogView.findViewById(R.id.download_image);
        EditText editText = dialogView.findViewById(R.id.download_edit_text);
        buttonVideo = dialogView.findViewById(R.id.button_video);
        Button buttonThumbnail = dialogView.findViewById(R.id.button_thumbnail);
        Button buttonAudio = dialogView.findViewById(R.id.button_audio);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);
        Button buttonDownload = dialogView.findViewById(R.id.button_download);

        // load image
        loadImage(imageView);

        // load default video name
        loadVideoName(editText);

        // state
        isVideoSelected = new AtomicBoolean(false);
        AtomicBoolean isThumbnailSelected = new AtomicBoolean(false);
        AtomicBoolean isAudioSelected = new AtomicBoolean(false);
        selectedQuality = new AtomicReference<>(null);

        // set button default background color
        buttonVideo.setBackgroundColor(context.getColor(android.R.color.darker_gray));
        buttonThumbnail.setBackgroundColor(context.getColor(android.R.color.darker_gray));
        buttonAudio.setBackgroundColor(context.getColor(android.R.color.darker_gray));


        // on video button clicked
        buttonVideo.setOnClickListener(v -> showVideoQualityDialog());

        // on thumbnail button clicked
        buttonThumbnail.setOnClickListener(v -> {
            isThumbnailSelected.set(!isThumbnailSelected.get());
            buttonThumbnail.setSelected(isThumbnailSelected.get());
            if (isThumbnailSelected.get()) {
                buttonThumbnail.setBackgroundColor(themeColor);
            } else {
                buttonThumbnail.setBackgroundColor(context.getColor(android.R.color.darker_gray));
            }
        });

        // on audio-only button clicked
        buttonAudio.setOnClickListener(v -> {
            isAudioSelected.set(!isAudioSelected.get());
            buttonAudio.setSelected(isAudioSelected.get());
            if (isAudioSelected.get()) {
                buttonAudio.setBackgroundColor(themeColor);
            } else {
                buttonAudio.setBackgroundColor(context.getColor(android.R.color.darker_gray));
            }
        });

        // on download button clicked
        buttonDownload.setOnClickListener(v -> {
            // fixed in live page
            if (details == null) {
                dialog.dismiss();
                return;
            }

            if (!isVideoSelected.get() && !isThumbnailSelected.get() && !isAudioSelected.get()) {
                dialogView.post(() -> Toast.makeText(
                        context,
                        R.string.select_something_first,
                        Toast.LENGTH_SHORT
                ).show());
                return;
            }

            String fileName = editText.getText().toString().trim();
            String thumbnail = isThumbnailSelected.get() ? details.getThumbnail(): null;
            ((MainActivity)context).downloadService.initiateDownload(new DownloadTask(
                    url,
                    fileName,
                    thumbnail,
                    selectedQuality.get(),
                    isAudioSelected.get(),
                    DownloaderState.RUNNING
            ));
            dialog.dismiss();
        });


        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        // show dialog
        dialog.show();
    }
    private void loadImage(ImageView imageView) {
        executor.execute(() -> {
            try {
                latch.await();
                URL url = new URL(details.getThumbnail());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();

                Bitmap bitmap = BitmapFactory.decodeStream(input);

                dialogView.post(() -> imageView.setImageBitmap(bitmap));

                connection.disconnect();
            } catch (IOException | InterruptedException e) {
                Log.e("When fetch thumbnail", Log.getStackTraceString(e));
                dialogView.post(() ->
                        Toast.makeText(context,
                                context.getString(R.string.failed_to_load_image) + e,
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void loadVideoName(EditText editText) {
        executor.execute(() -> {
            try {
                latch.await();
                String title = details.getTitle();
                String author = details.getAuthor();
                String video_default_name = String.format("%s-%s", title, author);
                dialogView.post(() -> editText.setText(video_default_name));
            } catch (InterruptedException e) {
                Log.e("When load video title and author", Log.getStackTraceString(e));
            }
        });
    }

    private void showVideoQualityDialog() {
        View dialogView = View.inflate(context, R.layout.quality_selector, null);
        progressBar2 = dialogView.findViewById(R.id.loadingBar2);
        if (progressBar2 != null && details == null) progressBar2.setVisibility(View.VISIBLE);
        qualityDialog = new MaterialAlertDialogBuilder(context)
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
        // avoid trigger radioGroup.setOnCheckedChangeListener when initiate the radio button check state
        executor.execute(() -> {
            try {
                latch.await();
                AtomicLong audioSize = new AtomicLong();
                details.getFormats().forEach(it -> {
                    String ext = it.getExt();
                    if ("m4a".equals(ext)) {
                        audioSize.updateAndGet(current -> Math.max(it.getFileSize(), current));
                    }
                    List<String> bad_formats = List.of("233", "234", "616");
                    String format_note = it.getFormatNote();
                    if ("mp4".equals(ext)
                            && !bad_formats.contains(it.getFormatId())
                            && format_note != null
                    ) {
                        // avoid duplicate labels
                        if (!quality_labels.contains(it.getFormatNote())) {
                            quality_labels.add(it.getFormatNote());
                            CheckBox choice = new CheckBox(context);
                            choice.setText(String.format("%s (%s)", it.getFormatNote(), formatSize(audioSize.get() + it.getFileSize())));
                            choice.setLayoutParams(
                                    new RadioGroup.LayoutParams(
                                            RadioGroup.LayoutParams.MATCH_PARENT,
                                            RadioGroup.LayoutParams.WRAP_CONTENT
                                    )
                            );
                            choice.setOnCheckedChangeListener((v, isChecked) -> {
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
        confirmButton.setOnClickListener(v -> {
            if (checked_box.get() == null) {
                selectedQuality.set(null);
                isVideoSelected.set(false);
                buttonVideo.setBackgroundColor(context.getColor(android.R.color.darker_gray));
            } else {
                selectedQuality.set(selected_format.get());
                isVideoSelected.set(true);
                buttonVideo.setBackgroundColor(themeColor);
            }
            qualityDialog.dismiss();
        });

        qualityDialog.show();
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


}
