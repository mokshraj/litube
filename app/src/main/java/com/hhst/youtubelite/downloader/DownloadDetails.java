package com.hhst.youtubelite.downloader;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadDetails {
  private String id;
  private String title;
  private String author;
  private String description;
  private Long duration;
  private String thumbnail;
}
