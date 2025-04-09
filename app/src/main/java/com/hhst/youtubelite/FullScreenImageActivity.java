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

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hhst.youtubelite.downloader.DownloadService;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;


public class FullScreenImageActivity extends AppCompatActivity {

    private String url;
    private String filename;

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
                    if (window != null) {
                        window.setLayout(800, WindowManager.LayoutParams.WRAP_CONTENT);
                    }
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
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
                startActivity(Intent.createChooser(shareIntent, "Share Image"));
        }

    }
}
