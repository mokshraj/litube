package com.hhst.youtubelite;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hhst.youtubelite.downloader.DownloadService;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;


public class FullScreenImageActivity extends AppCompatActivity {

    private String url;
    private String filename;
    private File file;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        PhotoView photoView = findViewById(R.id.photoView);
        // destroy this activity when click image or button
        photoView.setOnClickListener(view -> finish());
        photoView.setOnLongClickListener(view -> {
                    AlertDialog menu = new MaterialAlertDialogBuilder(this)
                            .setCancelable(true)
                            .setItems(new CharSequence[]{getString(R.string.save), getString(R.string.share)},
                                    ((dialog, which) -> onContextMenuClicked(which)))
                            .create();
                    Window window = menu.getWindow();
                    if (window != null)
                        window.setLayout(600, WindowManager.LayoutParams.WRAP_CONTENT);
                    menu.show();
                    return true;
                }
        );

        findViewById(R.id.btnClose).setOnClickListener(view -> finish());
        url = getIntent().getStringExtra("thumbnail");
        filename = getIntent().getStringExtra("filename");
        try {
            // set screen orientation and image in image_view
            Picasso.get().load(url).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    if (bitmap.getWidth() > bitmap.getHeight())
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    photoView.setImageBitmap(bitmap);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    throw new RuntimeException(e);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });
        } catch (Exception e) {
            Log.e(getString(R.string.failed_to_load_image), Log.getStackTraceString(e));
            Toast.makeText(this, getString(R.string.failed_to_load_image), Toast.LENGTH_SHORT)
                    .show();
        }
    }


    public void onContextMenuClicked(int index) {
        switch (index) {
            case 0:
                Intent saveIntent = new Intent(this, DownloadService.class);
                saveIntent.setAction("DOWNLOAD_THUMBNAIL");
                saveIntent.putExtra("thumbnail", url);
                saveIntent.putExtra("filename", filename);
                startService(saveIntent);
                return;
            case 1:
                File file = new File(getCacheDir(), filename + ".jpg");
                // download thumbnail to local cache directory and send it
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        // download thumbnail
                        FileUtils.copyURLToFile(new URL(url), file);
                        this.file = file;
                        // build uri
                        Uri uri = FileProvider.getUriForFile(
                                this, getPackageName() + ".provider", file);
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("image/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_thumbnail)));
                    } catch (IOException e) {
                        Log.e(getString(R.string.failed_to_download_thumbnail), Log.getStackTraceString(e));
                        Toast.makeText(this, getString(R.string.failed_to_download_thumbnail),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        }

    }

    @Override
    public void finish() {
        // clean cached image
        FileUtils.deleteQuietly(file);
        super.finish();
    }

}
