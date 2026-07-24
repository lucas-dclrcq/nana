package org.nana.download;

public record DownloadJob(long id, String md5, String title, String extension) {}
