package com.hhst.youtubelite.Downloader;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.yausername.youtubedl_android.mapper.VideoFormat;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DownloadDetails implements Parcelable {
    private String id;
    private String title;
    private String author;
    private String description;
    private String thumbnail;
    private List<VideoFormat> formats;

    protected DownloadDetails(Parcel in) {
        id = in.readString();
        title = in.readString();
        author = in.readString();
        description = in.readString();
        thumbnail = in.readString();
    }

    public static final Creator<DownloadDetails> CREATOR = new Creator<DownloadDetails>() {
        @Override
        public DownloadDetails createFromParcel(Parcel in) {
            return new DownloadDetails(in);
        }

        @Override
        public DownloadDetails[] newArray(int size) {
            return new DownloadDetails[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(author);
        dest.writeString(description);
        dest.writeString(thumbnail);
    }

}
