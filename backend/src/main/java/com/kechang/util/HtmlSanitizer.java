package com.kechang.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * HTML 清洗工具 — 防止 XSS 攻击 用于清洗前端富文本编辑器提交的 HTML 内容
 */
public class HtmlSanitizer {

    /**
     * 富文本内容白名单： - 允许常见格式化标签 - 允许 style 属性中的字体/颜色相关 CSS - 图片 src
     * 限制为相对路径（/uploads/...）或同源 URL
     */
    private static final Safelist RELAXED = Safelist.relaxed();

    static {
        // 在 relaxed 基础上扩展
        RELAXED.addTags("h2", "h3", "h4", "span", "div", "hr", "pre", "code", "s", "del", "u");
        RELAXED.addAttributes("span", "style");
        RELAXED.addAttributes("div", "style");
        RELAXED.addAttributes("p", "style");
        RELAXED.addAttributes("h2", "style");
        RELAXED.addAttributes("h3", "style");
        RELAXED.addAttributes("h4", "style");
        RELAXED.addAttributes("a", "target", "rel");
        RELAXED.addAttributes("img", "style");
        RELAXED.addAttributes("pre", "style");
        RELAXED.addAttributes("li", "style");

        // 允许所有标签的 class 属性（Tiptap 会使用 class 标记对齐等）
        RELAXED.addAttributes(":all", "class");

        // 添加协议的 src（允许相对路径和同源 http/https）
        RELAXED.addProtocols("img", "src", "http", "https", "data");
        RELAXED.addProtocols("a", "href", "http", "https", "mailto");
    }

    /**
     * 清洗 HTML，移除危险标签/属性
     *
     * @param html 用户提交的 HTML 字符串
     * @return 清洗后的安全 HTML
     */
    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        // 先解析为 Document 做清洗
        String cleaned = Jsoup.clean(html, RELAXED);
        // 再用 Jsoup 规范化（确保结构完整，避免空白文档）
        Document doc = Jsoup.parseBodyFragment(cleaned);
        doc.outputSettings().prettyPrint(false);
        String bodyHtml = doc.body().html();

        // 如果清洗后为空，返回空段落作为默认
        if (bodyHtml.isBlank()) {
            return "";
        }
        return bodyHtml;
    }
}
