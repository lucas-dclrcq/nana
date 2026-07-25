package org.nana.shared.config;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Single source of truth for which ebook formats are permitted. Reads {@code nana.allowed-formats}
 * (env {@code NANA_ALLOWED_FORMATS}) once at startup and answers "is this extension allowed?" plus
 * "what is the effective allowed list?" for search filtering, download rejection and the UI.
 *
 * <p>The config defaults to the full format universe, so the set is normally non-empty. An operator
 * narrows it to restrict. An explicitly blank value yields an empty set, which is treated as
 * fail-open (all allowed) rather than blocking everything.
 */
@ApplicationScoped
public class FormatPolicy {

    private final Set<String> allowed;

    public FormatPolicy(NanaConfiguration config) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : config.allowedFormats()) {
            String format = normalize(value);
            if (format != null) {
                normalized.add(format);
            }
        }
        // LinkedHashSet preserves configured order for the UI dropdown; List.copyOf reads it in order.
        this.allowed = Collections.unmodifiableSet(normalized);
    }

    /**
     * @return {@code true} when the format may be searched/downloaded. An empty policy (config blanked)
     * and a null/blank extension (undetectable format) are never blocked; otherwise membership decides.
     */
    public boolean isAllowed(String extension) {
        if (allowed.isEmpty()) {
            return true;
        }
        String format = normalize(extension);
        return format == null || allowed.contains(format);
    }

    /** Effective allowed formats in configured order (empty only when explicitly blanked). */
    public List<String> allowedFormats() {
        return List.copyOf(allowed);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.isBlank() ? null : trimmed;
    }
}
