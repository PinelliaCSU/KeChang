package com.kechang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 阿里百炼（DashScope）API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bailian")
public class BailianConfig {

    /**
     * API Key（通过环境变量 BAILIAN_API_KEY 注入）
     */
    private String apiKey;

    /**
     * 模型名称，默认 qwen-turbo
     */
    private String model = "qwen-turbo";

    /**
     * API 端点（OpenAI 兼容格式）
     */
    private String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /**
     * 连接超时（秒）
     */
    private int connectTimeout = 30;

    /**
     * 读取超时（秒）
     */
    private int readTimeout = 300;

    /**
     * 音频转写配置，用于视频/音频资源 AI 总结前的 ASR。
     */
    private Asr asr = new Asr();

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeout));
        factory.setReadTimeout(Duration.ofSeconds(readTimeout));
        return new RestTemplate(factory);
    }

    @Data
    public static class Asr {

        /**
         * 是否启用 ASR。关闭时视频/音频会回退为明确提示。
         */
        private boolean enabled = true;

        /**
         * OpenAI-compatible 音频转写端点。
         */
        private String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/audio/transcriptions";

        /**
         * ASR 模型名称，按实际账号可用模型调整。
         */
        private String model = "whisper-large-v3";

        /**
         * ffmpeg 可执行文件路径。已加入 PATH 时保持 ffmpeg 即可。
         */
        private String ffmpegPath = "ffmpeg";

        /**
         * ffmpeg 抽音频最长等待秒数。
         */
        private int ffmpegTimeout = 300;
    }
}
