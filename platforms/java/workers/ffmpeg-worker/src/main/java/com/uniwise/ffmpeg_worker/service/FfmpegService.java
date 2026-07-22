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
import java.math.BigDecimal;
import java.math.RoundingMode;
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
        CommandResult result = executeCommand("ffmpeg", ffmpegArgs, true);
        return result.exitCode() == 0;
    }

    public long probeDurationMillis(String containerInputPath) {
        List<String> ffprobeArgs = List.of(
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                containerInputPath
        );

        CommandResult result = executeCommand("ffprobe", ffprobeArgs, false);
        if (result.exitCode() != 0) {
            throw new IllegalStateException("FFprobe failed with exit code " + result.exitCode());
        }

        return parseDurationMillis(result.output());
    }

    static long parseDurationMillis(String output) {
        if (output == null || output.isBlank()) {
            throw new IllegalArgumentException("FFprobe returned an empty duration");
        }

        try {
            long durationMillis = new BigDecimal(output.trim())
                    .multiply(BigDecimal.valueOf(1000))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            if (durationMillis <= 0) {
                throw new IllegalArgumentException("Video duration must be greater than zero");
            }
            return durationMillis;
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Invalid FFprobe duration: " + output.trim(), e);
        }
    }

    private CommandResult executeCommand(String executable, List<String> args, boolean logOutput) {
        List<String> command = new ArrayList<>();

        // Add command prefix if present (e.g., ["docker", "exec", "ffmpeg-service"])
        String prefix = ffmpegProperties.getCommandPrefix();
        if (prefix != null && !prefix.trim().isEmpty()) {
            command.addAll(Arrays.asList(prefix.split(" ")));
        }

        // Add the actual executable and its arguments
        command.add(executable);
        command.addAll(args);

        log.info("Executing command: {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Merge standard error into standard output

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            // Read the process output stream
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!output.isEmpty()) {
                        output.append(System.lineSeparator());
                    }
                    output.append(line);
                    if (logOutput) {
                        log.info("[{}] {}", executable, line);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("{} process completed successfully.", executable);
            } else {
                log.error("{} process failed with exit code: {}. Output: {}", executable, exitCode, output);
            }
            return new CommandResult(exitCode, output.toString());
        } catch (IOException e) {
            log.error("I/O error occurred during {} execution", executable, e);
            return new CommandResult(-1, "");
        } catch (InterruptedException e) {
            log.error("{} execution was interrupted", executable, e);
            Thread.currentThread().interrupt();
            return new CommandResult(-1, "");
        }
    }

    private record CommandResult(int exitCode, String output) {
    }
}
