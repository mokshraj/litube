litube
============

litube is a lightweight Android WebView wrapper for YouTube, offering many additional features such as ads blocking, background play, video download and playback progress memory.

## Features

- ~~**Lightweight, the installation package is only a few MBs in size.**~~(Due to YouTube restrictions, the original download method has been deprecated. To restore this feature, I integrated `yt-dlp` and `ffmpeg`, which may not be that lightweight. If you don't need the download function, feel free to use the `lite` version instead.)

* **Ad blocking, including sponsor ads and video ads.**
* **Video download, supports downloading videos, audio-only, and saving thumbnails.**
* **Background play.**
* **Display video dislike count.**
* **Hide shorts, etc.**

> It is highly recommended to ignore battery optimization and allow background activity for this app to prevent it from being mistakenly killed by the system.

## Screenshots

<img title="" src="https://github.com/HydeYYHH/litube/blob/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" alt="" width="250"><img title="" src="https://github.com/HydeYYHH/litube/blob/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" alt="" width="250">



**Note: This app should not be regarded as an alternative to other YouTube clients. My primary motivation for developing this app is to enhance the experience of watching YouTube in a browser, especially when using extensions may not be convenient on mobile, and to serve as a download tool — which is why I compromised the lightweight feature that a WebView app should have to integrate `yt-dlp` and `ffmpeg`. Personally, I would rather watch YouTube using the official YouTube app or YouTube Vanced, which provide a much smoother experience.**



## Contributing

If you encounter a bug, please check the GitHub repository to see if an issue has already been reported. If not, feel free to open a new one.

