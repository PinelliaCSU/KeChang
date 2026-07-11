package com.kechang.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kechang.common.BusinessException;
import com.kechang.entity.AttachmentFile;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileProcessingService {

    private final ObjectMapper objectMapper;
    private final AudioTranscriptionService audioTranscriptionService;

    @Value("${kechang.upload.path:./uploads}")
    private String uploadBasePath;

    @Value("${kechang.office.soffice-path:}")
    private String configuredSofficePath;

    public String ensurePdfPreview(String fileUrl, String displayName) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        String ext = extension(displayName != null ? displayName : fileUrl);
        if ("pdf".equals(ext)) {
            return fileUrl;
        }
        if (!"docx".equals(ext) && !"doc".equals(ext)) {
            return fileUrl;
        }

        try {
            Path source = resolveUploadPath(fileUrl);
            if (!Files.exists(source)) {
                throw BusinessException.notFound("文件不存在，无法生成预览");
            }
            Path previewDir = uploadRoot().resolve("previews");
            Files.createDirectories(previewDir);
            String previewName = stripExtension(source.getFileName().toString()) + "_preview.pdf";
            Path preview = previewDir.resolve(previewName);
            Path marker = previewDir.resolve(previewName + ".ok");
            if (Files.exists(preview) && Files.exists(marker)
                    && Files.getLastModifiedTime(preview).toMillis() >= Files.getLastModifiedTime(source).toMillis()) {
                return "/uploads/previews/" + previewName;
            }
            boolean converted = convertWithLibreOffice(source, previewDir, preview);
            if (converted) {
                Files.writeString(marker, "libreoffice");
            }
            if (!converted && !Files.exists(preview)) {
                createWordPreviewPdf(source, preview, displayName);
            }
            return "/uploads/previews/" + previewName;
        } catch (IOException e) {
            throw new BusinessException("生成 Word PDF 预览失败：" + e.getMessage());
        }
    }

    public Map<String, Object> renderPdfPages(String fileUrl, String displayName) {
        String pdfUrl = ensurePdfPreview(fileUrl, displayName);
        try {
            Path pdfPath = resolveUploadPath(pdfUrl);
            if (!Files.exists(pdfPath)) {
                throw BusinessException.notFound("PDF 文件不存在，无法渲染页面");
            }
            String baseName = stripExtension(pdfPath.getFileName().toString());
            Path pageDir = uploadRoot().resolve("previews").resolve("pages").resolve(baseName);
            Files.createDirectories(pageDir);
            List<Map<String, Object>> pages = new ArrayList<>();
            try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
                PDFRenderer renderer = new PDFRenderer(document);
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    String imageName = "page-" + (i + 1) + ".png";
                    Path imagePath = pageDir.resolve(imageName);
                    boolean staleImage = Files.exists(imagePath)
                            && Files.getLastModifiedTime(imagePath).toMillis() < Files.getLastModifiedTime(pdfPath).toMillis();
                    if (!Files.exists(imagePath) || staleImage) {
                        BufferedImage image = renderer.renderImageWithDPI(i, 140);
                        ImageIO.write(image, "png", imagePath.toFile());
                    }
                    PDRectangle box = document.getPage(i).getCropBox();
                    pages.add(Map.of(
                            "page", i + 1,
                            "url", "/uploads/previews/pages/" + baseName + "/" + imageName,
                            "width", box.getWidth(),
                            "height", box.getHeight()
                    ));
                }
            }
            return Map.of("pdfUrl", pdfUrl, "pages", pages);
        } catch (IOException e) {
            throw new BusinessException("渲染 PDF 页面失败：" + e.getMessage());
        }
    }

    public List<AttachmentFile> buildAnnotatedFiles(Long submitId, List<AttachmentFile> submitFiles, String marksJson) {
        List<FeedbackMark> marks = parseMarks(marksJson);
        List<AttachmentFile> result = new ArrayList<>();
        if (submitFiles == null || submitFiles.isEmpty() || marks.isEmpty()) {
            return result;
        }

        for (AttachmentFile file : submitFiles) {
            List<FeedbackMark> fileMarks = marks.stream()
                    .filter(mark -> matchesFile(mark, file))
                    .toList();
            if (fileMarks.isEmpty()) {
                continue;
            }
            String previewUrl = ensurePdfPreview(file.getFileUrl(), file.getFileName());
            if (!"pdf".equals(extension(previewUrl))) {
                continue;
            }
            String annotatedUrl = createAnnotatedPdf(submitId, file, previewUrl, fileMarks);
            AttachmentFile feedback = new AttachmentFile();
            feedback.setTargetType("FEEDBACK_SUBMIT");
            feedback.setTargetId(submitId);
            feedback.setFileName("批注版-" + safeFileName(stripExtension(file.getFileName())) + ".pdf");
            feedback.setFileUrl(annotatedUrl);
            try {
                feedback.setFileSize(Files.size(resolveUploadPath(annotatedUrl)));
            } catch (IOException e) {
                feedback.setFileSize(0L);
            }
            feedback.setSortOrder(file.getSortOrder());
            result.add(feedback);
        }
        return result;
    }

    private String createAnnotatedPdf(Long submitId, AttachmentFile file, String previewUrl, List<FeedbackMark> marks) {
        try {
            Path source = resolveUploadPath(previewUrl);
            Path feedbackDir = uploadRoot().resolve("feedback").resolve(String.valueOf(submitId));
            Files.createDirectories(feedbackDir);
            String targetName = UUID.randomUUID() + "_annotated_" + safeFileName(stripExtension(file.getFileName())) + ".pdf";
            Path target = feedbackDir.resolve(targetName);

            try (PDDocument document = PDDocument.load(source.toFile())) {
                PDFont textFont = loadTextFont(document);
                for (FeedbackMark mark : marks) {
                    int pageIndex = Math.max(0, Math.min(document.getNumberOfPages() - 1, mark.safePage() - 1));
                    PDPage page = document.getPage(pageIndex);
                    PDRectangle box = page.getCropBox();
                    float x = box.getLowerLeftX() + (float) (box.getWidth() * mark.safeX() / 100.0);
                    float y = box.getUpperRightY() - (float) (box.getHeight() * mark.safeY() / 100.0);
                    drawMark(document, page, textFont, mark, x, y);
                }
                document.save(target.toFile());
            }
            return "/uploads/feedback/" + submitId + "/" + targetName;
        } catch (IOException e) {
            throw new BusinessException("生成批注 PDF 失败：" + e.getMessage());
        }
    }

    private void drawMark(PDDocument document, PDPage page, PDFont textFont, FeedbackMark mark, float x, float y) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            String type = mark.getType() == null ? "text" : mark.getType();
            if ("check".equals(type)) {
                cs.setStrokingColor(new Color(32, 160, 80));
                cs.setLineWidth(3f);
                cs.moveTo(x - 8, y);
                cs.lineTo(x - 2, y - 7);
                cs.lineTo(x + 12, y + 10);
                cs.stroke();
                return;
            }
            if ("wrong".equals(type)) {
                cs.setStrokingColor(new Color(230, 65, 65));
                cs.setLineWidth(3f);
                cs.moveTo(x - 9, y - 9);
                cs.lineTo(x + 9, y + 9);
                cs.moveTo(x + 9, y - 9);
                cs.lineTo(x - 9, y + 9);
                cs.stroke();
                return;
            }
            cs.setNonStrokingColor(new Color(35, 115, 210));
            cs.beginText();
            cs.setFont(textFont, 12);
            cs.newLineAtOffset(x, y);
            cs.showText(safePdfText(mark.getText()));
            cs.endText();
        }
    }

    private void createWordPreviewPdf(Path source, Path target, String displayName) throws IOException {
        List<String> lines = readWordLines(source);
        if (lines.isEmpty()) {
            lines.add("该 Word 文件暂无可提取的文本内容，请下载原文件查看。");
        }
        try (PDDocument document = new PDDocument()) {
            PDFont font = loadTextFont(document);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(document, page);
            float margin = 54;
            float y = page.getMediaBox().getHeight() - margin;
            cs.beginText();
            cs.setFont(font, 13);
            cs.newLineAtOffset(margin, y);
            cs.showText(safePdfText(displayName != null ? displayName : source.getFileName().toString()));
            cs.endText();
            y -= 28;

            for (String rawLine : lines) {
                for (String line : wrapLine(rawLine, 46)) {
                    if (y < margin) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        cs = new PDPageContentStream(document, page);
                        y = page.getMediaBox().getHeight() - margin;
                    }
                    cs.beginText();
                    cs.setFont(font, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(safePdfText(line));
                    cs.endText();
                    y -= 18;
                }
                y -= 6;
            }
            cs.close();
            document.save(target.toFile());
        }
    }

    private boolean convertWithLibreOffice(Path source, Path previewDir, Path target) {
        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(configuredSofficePath)) {
            candidates.add(configuredSofficePath);
        }
        candidates.addAll(List.of(
                "soffice",
                "libreoffice",
                "C:/Program Files/LibreOffice/program/soffice.exe",
                "C:/Program Files (x86)/LibreOffice/program/soffice.exe"
        ));
        for (String command : candidates) {
            try {
                Process process = new ProcessBuilder(
                        command,
                        "--headless",
                        "--convert-to",
                        "pdf",
                        "--outdir",
                        previewDir.toString(),
                        source.toString()
                ).redirectErrorStream(true).start();
                if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
                    continue;
                }
                Path converted = previewDir.resolve(stripExtension(source.getFileName().toString()) + ".pdf");
                if (Files.exists(converted)) {
                    Files.move(converted, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private List<String> readWordLines(Path source) throws IOException {
        if ("docx".equals(extension(source.getFileName().toString()))) {
            try (InputStream in = Files.newInputStream(source); XWPFDocument doc = new XWPFDocument(in)) {
                return doc.getParagraphs().stream()
                        .map(XWPFParagraph::getText)
                        .filter(StringUtils::hasText)
                        .toList();
            }
        }
        return List.of("系统当前只能自动提取 docx 文本；请下载原 doc 文件查看完整内容。");
    }

    private PDFont loadTextFont(PDDocument document) {
        List<String> candidates = List.of(
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simsun.ttc"
        );
        for (String fontPath : candidates) {
            File file = new File(fontPath);
            if (file.exists()) {
                try {
                    return PDType0Font.load(document, file);
                } catch (IOException ignored) {
                }
            }
        }
        return PDType1Font.HELVETICA;
    }

    private List<FeedbackMark> parseMarks(String marksJson) {
        if (!StringUtils.hasText(marksJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(marksJson, new TypeReference<List<FeedbackMark>>() {
            });
        } catch (IOException e) {
            return List.of();
        }
    }

    private boolean matchesFile(FeedbackMark mark, AttachmentFile file) {
        return same(mark.getFileUrl(), file.getFileUrl())
                || same(mark.getFileKey(), file.getFileUrl())
                || same(mark.getFileName(), file.getFileName());
    }

    private Path uploadRoot() {
        return Paths.get(uploadBasePath).toAbsolutePath().normalize();
    }

    private Path resolveUploadPath(String fileUrl) {
        String value = fileUrl == null ? "" : fileUrl;
        int uploads = value.indexOf("/uploads/");
        String relative = uploads >= 0 ? value.substring(uploads + "/uploads/".length()) : value.replaceFirst("^/+", "");
        return uploadRoot().resolve(relative).normalize();
    }

    private String extension(String name) {
        if (name == null) {
            return "";
        }
        String clean = name.split("\\?")[0].toLowerCase(Locale.ROOT);
        int index = clean.lastIndexOf('.');
        return index >= 0 ? clean.substring(index + 1) : "";
    }

    private String stripExtension(String name) {
        if (!StringUtils.hasText(name)) {
            return "file";
        }
        int index = name.lastIndexOf('.');
        return index >= 0 ? name.substring(0, index) : name;
    }

    private String safeFileName(String name) {
        String value = StringUtils.hasText(name) ? name : "file";
        return value.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_");
    }

    private String safePdfText(String value) {
        return value == null ? "" : value.replace("\r", " ").replace("\n", " ");
    }

    private boolean same(String a, String b) {
        return StringUtils.hasText(a) && StringUtils.hasText(b) && a.equals(b);
    }

    // ==================== 文本提取（用于 AI 总结） ====================
    /**
     * 从上传的文件中提取文本内容，用于 AI 总结。 对于无法提取文本的媒体文件（视频/音频/图片），返回包含元信息的占位文本， 以便 AI
     * 仍可据此生成学习建议。
     *
     * @param fileUrl 文件相对路径（如 /uploads/courses/1/xxx.pdf）
     * @param resourceName 资源名称（用于占位文本）
     * @return 提取的文本内容，最多约 10000 字
     */
    public String extractText(String fileUrl, String resourceName) {
        if (!StringUtils.hasText(fileUrl)) {
            return "";
        }
        String safeName = StringUtils.hasText(resourceName) ? resourceName : fileUrl;
        try {
            Path source = resolveUploadPath(fileUrl);
            if (!Files.exists(source)) {
                return "[文件不存在] 资源「" + safeName + "」对应的文件未找到，无法分析内容。";
            }
            String ext = extension(fileUrl);
            String text = switch (ext) {
                case "pdf" ->
                    extractPdfText(source);
                case "docx" ->
                    extractDocxText(source);
                case "pptx" ->
                    extractPptxText(source);
                case "txt", "md", "json", "xml", "csv", "log", "java", "js", "ts", "css", "html", "vue", "py", "c", "cpp", "h", "sql" ->
                    Files.readString(source);
                // 视频 / 音频 — 先抽取音频并调用 ASR，得到转写文本后再交给 AI 总结
                case "mp4", "webm", "ogg", "mov", "m4v", "avi", "mkv", "flv", "wmv" ->
                    audioTranscriptionService.transcribeMedia(source, safeName, ext);
                case "mp3", "wav", "m4a", "aac", "flac", "wma" ->
                    audioTranscriptionService.transcribeMedia(source, safeName, ext);
                case "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg" ->
                    "[图片文件] 资源名称：「" + safeName + "」，格式：" + ext.toUpperCase()
                    + "。图片内容无法自动提取文字，请结合上下文理解图表/示意图。";
                default ->
                    "[不可读文件] 资源名称：「" + safeName + "」，格式："
                    + (ext.isEmpty() ? "未知" : ext.toUpperCase())
                    + "。该文件类型暂不支持内容提取，请直接查看原文件。";
            };
            if (text == null || text.isBlank()) {
                return "";
            }
            // 限制最大 10000 字符
            int maxLen = 10000;
            return text.length() > maxLen ? text.substring(0, maxLen) : text;
        } catch (Exception e) {
            return "[读取失败] 资源「" + safeName + "」的文件读取失败：" + e.getMessage();
        }
    }

    private String extractPdfText(Path pdfPath) {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text != null && text.length() > 12000) {
                text = text.substring(0, 12000);
            }
            return text != null ? text : "";
        } catch (IOException e) {
            return "";
        }
    }

    private String extractDocxText(Path source) {
        try {
            List<String> lines = readWordLines(source);
            return String.join("\n", lines);
        } catch (Exception e) {
            return "";
        }
    }

    private String extractPptxText(Path source) {
        try {
            StringBuilder sb = new StringBuilder();
            try (InputStream in = Files.newInputStream(source); org.apache.poi.xslf.usermodel.XMLSlideShow ppt = new org.apache.poi.xslf.usermodel.XMLSlideShow(in)) {
                for (var slide : ppt.getSlides()) {
                    for (var shape : slide.getShapes()) {
                        if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape textShape) {
                            sb.append(textShape.getText()).append("\n");
                        }
                    }
                    sb.append("\n---\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private List<String> wrapLine(String line, int size) {
        if (!StringUtils.hasText(line)) {
            return List.of("");
        }
        List<String> result = new ArrayList<>();
        String text = line.trim();
        for (int i = 0; i < text.length(); i += size) {
            result.add(text.substring(i, Math.min(text.length(), i + size)));
        }
        return result;
    }

    public String archiveFileName(String assignmentTitle) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return safeFileName((StringUtils.hasText(assignmentTitle) ? assignmentTitle : "作业附件") + "-" + date + ".zip");
    }

    @Data
    public static class FeedbackMark {

        private String id;
        private String type;
        private String text;
        private Double x;
        private Double y;
        private Integer page;
        private String fileKey;
        private String fileUrl;
        private String fileName;

        public double safeX() {
            return x == null ? 50 : Math.max(0, Math.min(100, x));
        }

        public double safeY() {
            return y == null ? 50 : Math.max(0, Math.min(100, y));
        }

        public int safePage() {
            return page == null || page < 1 ? 1 : page;
        }
    }
}
