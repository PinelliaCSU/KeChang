package com.kechang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kechang.config.BailianConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioTranscriptionService {

    private final BailianConfig bailianConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String transcribeMedia(Path source, String resourceName, String ext) {
        BailianConfig.Asr asr = bailianConfig.getAsr();
        if (asr == null || !asr.isEnabled()) {
            return "[ASR 未启用] 当前未开启音频转写，无法读取视频/音频内容。";
        }
        if (!StringUtils.hasText(bailianConfig.getApiKey())) {
            return "[ASR 未配置] 缺少 AI_API_KEY 或 BAILIAN_API_KEY，无法调用音频转写服务。";
        }
        if (!StringUtils.hasText(asr.getEndpoint())) {
            return "[ASR 未配置] 缺少 bailian.asr.endpoint，无法调用音频转写服务。";
        }

        Path audio = null;
        Path tempDir = null;
        try {
            AudioFile audioFile = prepareAudio(source, ext, asr);
            audio = audioFile.path();
            tempDir = audioFile.tempDir();
            String text = requestTranscription(audio, asr);
            if (!StringUtils.hasText(text)) {
                return "[ASR 无结果] 音频转写服务未返回文字内容。";
            }
            String mediaType = isVideo(ext) ? "视频" : "音频";
            return "[" + mediaType + "转写文本] 资源名称：「" + resourceName + "」\n\n" + text.trim();
        } catch (Exception e) {
            log.warn("Media transcription failed, file={}, reason={}", source, e.getMessage());
            return "[ASR 失败] 资源「" + resourceName + "」音频转写失败：" + e.getMessage();
        } finally {
            if (audio != null && !audio.equals(source)) {
                try {
                    Files.deleteIfExists(audio);
                } catch (IOException ignored) {
                }
            }
            if (tempDir != null) {
                try {
                    Files.deleteIfExists(tempDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private AudioFile prepareAudio(Path source, String ext, BailianConfig.Asr asr) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("kechang-asr-");
        Path target = tempDir.resolve(UUID.randomUUID() + ".mp3");
        Path ffmpegLog = tempDir.resolve("ffmpeg.log");
        List<String> command = List.of(
                asr.getFfmpegPath(),
                "-nostdin",
                "-y",
                "-i", source.toString(),
                "-vn",
                "-ac", "1",
                "-ar", "16000",
                "-b:a", "48k",
                "-f", "mp3",
                target.toString()
        );
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ffmpegLog.toFile())
                .start();
        boolean finished = process.waitFor(Math.max(30, asr.getFfmpegTimeout()), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("ffmpeg 抽取音频超时，请检查视频大小或 ffmpeg 配置");
        }
        if (process.exitValue() != 0 || !Files.exists(target) || Files.size(target) == 0) {
            throw new IllegalStateException("ffmpeg 抽取音频失败，请确认服务器已安装 ffmpeg。输出：" + readTail(ffmpegLog));
        }
        return new AudioFile(target, tempDir);
    }

    private String readTail(Path logFile) {
        try {
            if (logFile == null || !Files.exists(logFile)) {
                return "无输出";
            }
            String content = Files.readString(logFile, StandardCharsets.UTF_8).trim();
            if (content.isBlank()) {
                return "无输出";
            }
            int max = 600;
            return content.length() > max ? content.substring(content.length() - max) : content;
        } catch (Exception e) {
            return "无法读取 ffmpeg 输出：" + e.getMessage();
        }
    }

    private String requestTranscription(Path audio, BailianConfig.Asr asr) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(bailianConfig.getApiKey());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", asr.getModel());
        body.add("file", new FileSystemResource(audio.toFile()));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(asr.getEndpoint(), request, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("ASR 服务返回异常：" + response.getStatusCode());
            }
            return extractText(response.getBody());
        } catch (RestClientException e) {
            throw new IllegalStateException("调用 ASR 服务失败：" + e.getMessage(), e);
        }
    }

    private String extractText(Map<?, ?> response) {
        Object text = response.get("text");
        if (text instanceof String value && StringUtils.hasText(value)) {
            return value;
        }

        Object output = response.get("output");
        if (output instanceof Map<?, ?> outputMap) {
            Object outputText = outputMap.get("text");
            if (outputText instanceof String value && StringUtils.hasText(value)) {
                return value;
            }
        }

        Object choices = response.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object message = first.get("message");
            if (message instanceof Map<?, ?> messageMap) {
                Object content = messageMap.get("content");
                if (content instanceof String value && StringUtils.hasText(value)) {
                    return value;
                }
            }
        }

        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isVideo(String ext) {
        return List.of("mp4", "webm", "ogg", "mov", "m4v", "avi", "mkv", "flv", "wmv")
                .contains(normalize(ext));
    }

    private boolean isAudio(String ext) {
        return List.of("mp3", "wav", "m4a", "aac", "flac", "wma", "ogg")
                .contains(normalize(ext));
    }

    private String normalize(String ext) {
        return ext == null ? "" : ext.toLowerCase(Locale.ROOT);
    }

    private record AudioFile(Path path, Path tempDir) {
    }
}
