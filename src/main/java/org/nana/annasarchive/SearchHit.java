package org.nana.annasarchive;

public record SearchHit(
        String md5,
        String title,
        String author,
        String extension,
        Long sizeBytes,
        String language,
        Integer year,
        String coverUrl) {}
