package com.hhst.youtubelite.webview;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hhst.youtubelite.MainActivity;
import com.hhst.youtubelite.downloader.DownloadDialog;
import com.hhst.youtubelite.extension.ExtensionDialog;
import com.hhst.youtubelite.extension.ExtensionManager;

public class JavascriptInterface {
    private final Context context;

    public JavascriptInterface(Context context){
        this.context = context;
    }

    @android.webkit.JavascriptInterface
    public void test(String url){
        Log.d("JavascriptInterface-test", url);
    }

    @android.webkit.JavascriptInterface
    public void finishRefresh(){
        ((MainActivity)context).swipeRefreshLayout.setRefreshing(false);
    }

    @android.webkit.JavascriptInterface
    public void setRefreshLayoutEnabled(boolean enabled){
        ((MainActivity)context).swipeRefreshLayout.setEnabled(enabled);
    }


    @android.webkit.JavascriptInterface
    public void download(String video_id){
        new Handler(Looper.getMainLooper()).post(
                () -> new DownloadDialog(video_id, context).show()
        );
    }

    @android.webkit.JavascriptInterface
    public void extension(){
        new Handler(Looper.getMainLooper()).post(
                () -> new ExtensionDialog(context).show()
        );
    }

    @android.webkit.JavascriptInterface
    public void showPlayback(String url){
        ((MainActivity)context).playbackService.showNotification(url);
    }

    @android.webkit.JavascriptInterface
    public void hidePlayback(){
        ((MainActivity)context).playbackService.hideNotification();
    }

    @android.webkit.JavascriptInterface
    public void updatePlayback(long pos, float playbackSpeed, boolean isPlaying){
        ((MainActivity)context).playbackService.updateProgress(pos, playbackSpeed, isPlaying);
    }

}
