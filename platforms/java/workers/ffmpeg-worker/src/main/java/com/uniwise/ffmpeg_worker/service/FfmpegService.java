package com.uniwise.ffmpeg_worker.service;

import com.uniwise.ffmpeg_worker.configuration.FfmpegProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FfmpegService {

    private final FfmpegProperties ffmpegProperties;
    private Path absoluteLocalTempPath;

    @PostConstruct
    public void init() {
        // Resolve relative paths to absolute to avoid workspace relative path bugs
        String localTempDir = ffmpegProperties.getLocalTempDir();
        absoluteLocalTempPath = Paths.get(localTempDir).toAbsolutePath().normalize();
        log.info("Resolved absolute local temp directory path: {}", absoluteLocalTempPath);

        // Ensure directory exists
        try {
            Files.createDirectories(absoluteLocalTempPath);
            log.info("Local temp directory is ready: {}", absoluteLocalTempPath);
        } catch (IOException e) {
            log.error("Could not create local temp directory: {}", absoluteLocalTempPath, e);
        }
    }

    public File getLocalTempFile(String fileName) {
        return absoluteLocalTempPath.resolve(fileName).toFile();
    }

    public String getContainerFilePath(String fileName) {
        // Return Linux-style path for container (e.g. /temp/input.mp4)
        String baseDir = ffmpegProperties.getContainerTempDir();
        if (baseDir.endsWith("/")) {
            return baseDir + fileName;
        }
        return baseDir + "/" + fileName;
    }

    public boolean execute(List<String> ffmpegArgs) {
        List<String> command = new ArrayList<>();

        // Add command prefix if present (e.g., ["docker", "exec", "ffmpeg-service"])
        String prefix = ffmpegProperties.getCommandPrefix();
        if (prefix != null && !prefix.trim().isEmpty()) {
            command.addAll(Arrays.asList(prefix.split(" ")));
        }

        // Add the actual executable and its arguments
        command.add("ffmpeg");
        command.addAll(ffmpegArgs);

        log.info("Executing command: {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Merge standard error into standard output

            Process process = pb.start();

            // Read the process output stream
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[FFmpeg] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("FFmpeg process completed successfully.");
                return true;
            } else {
                log.error("FFmpeg process failed with exit code: {}", exitCode);
                return false;
            }
        } catch (IOException e) {
            log.error("I/O error occurred during FFmpeg execution", e);
            return false;
        } catch (InterruptedException e) {
            log.error("FFmpeg execution was interrupted", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
