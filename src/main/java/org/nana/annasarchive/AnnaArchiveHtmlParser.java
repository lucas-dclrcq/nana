package org.nana.annasarchive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class AnnaArchiveHtmlParser {

    private static final Pattern MD5 = Pattern.compile("/md5/([0-9a-fA-F]{32})");
    private static final Pattern SIZE = Pattern.compile("([\\d.]+)\\s*(GB|MB|KB|B)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LANGUAGE = Pattern.compile("\\[([a-zA-Z]{2,3})\\]");
    private static final Pattern YEAR = Pattern.compile("\\b(1[5-9]\\d{2}|20\\d{2})\\b");
    private static final Pattern EXTENSION =
            Pattern.compile("\\b(epub|kepub|mobi|azw3|pdf|cbz|djvu|fb2)\\b", Pattern.CASE_INSENSITIVE);

    private AnnaArchiveHtmlParser() {}

    public static List<SearchHit> parse(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(stripComments(html));
        Map<String, SearchHit> byMd5 = new LinkedHashMap<>();
        for (Element titleLink : doc.select("a[href*=/md5/]")) {
            String title = text(titleLink);
            if (title == null) {
                continue;
            }
            String md5 = extractMd5(titleLink.attr("href"));
            if (md5 == null || byMd5.containsKey(md5)) {
                continue;
            }
            Element container = resultContainer(titleLink);
            String text = container.text();
            byMd5.put(md5, new SearchHit(
                    md5,
                    title,
                    author(container),
                    lower(match(EXTENSION, text)),
                    parseSize(text),
                    lower(match(LANGUAGE, text)),
                    parseYear(text),
                    absoluteImage(container.selectFirst("img"))));
        }
        return new ArrayList<>(byMd5.values());
    }

    // Anna's Archive renders the title, author and metadata in a sibling block next to the cover
    // anchor; the result container is the nearest ancestor that also holds the cover image.
    private static Element resultContainer(Element titleLink) {
        for (Element parent : titleLink.parents()) {
            if (parent.selectFirst("img") != null) {
                return parent;
            }
        }
        Element parent = titleLink.parent();
        return parent != null ? parent : titleLink;
    }

    private static String author(Element container) {
        Elements searchLinks = container.select("a[href*=/search]");
        for (Element link : searchLinks) {
            if (link.html().contains("user")) {
                return text(link);
            }
        }
        return searchLinks.isEmpty() ? null : text(searchLinks.first());
    }

    // Anna's Archive renders search results inside HTML comments and reveals them with JS to deter
    // scrapers; the markup is real once the comment markers are removed.
    private static String stripComments(String html) {
        return html.replace("<!--", "").replace("-->", "");
    }

    private static String extractMd5(String href) {
        Matcher m = MD5.matcher(href == null ? "" : href);
        return m.find() ? m.group(1).toLowerCase(Locale.ROOT) : null;
    }

    private static Long parseSize(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = SIZE.matcher(text);
        if (!m.find()) {
            return null;
        }
        double value;
        try {
            value = Double.parseDouble(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
        long multiplier = switch (m.group(2).toUpperCase(Locale.ROOT)) {
            case "GB" -> 1024L * 1024L * 1024L;
            case "MB" -> 1024L * 1024L;
            case "KB" -> 1024L;
            default -> 1L;
        };
        return (long) (value * multiplier);
    }

    private static Integer parseYear(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = YEAR.matcher(text);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static String absoluteImage(Element img) {
        if (img == null) {
            return null;
        }
        String src = firstNonNull(blankToNull(img.attr("src")), blankToNull(img.attr("data-src")));
        return src != null && (src.startsWith("http://") || src.startsWith("https://")) ? src : null;
    }

    private static String match(Pattern pattern, String text) {
        if (text == null) {
            return null;
        }
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static String text(Element element) {
        return element == null ? null : blankToNull(element.text());
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
