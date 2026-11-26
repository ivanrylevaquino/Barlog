package com.barlog.barlog_back.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Private video storage on the API server's disk (outside any web root).
 * Stand-in for the AWS S3 bucket in the SRS (FR-013): swap this class for an S3 implementation later.
 */
@Component
public class VideoStorage {

    private final Path root;

    public VideoStorage(@Value("${barlog.storage.dir:uploads}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create video storage directory " + root, e);
        }
    }

    public void save(MultipartFile file, String key) {
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, resolve(key), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the video.", e);
        }
    }

    public Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("Invalid storage key.");
        return p;
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ignored) {
            // A leftover file is harmless; the database row is what the app reads.
        }
    }
}
