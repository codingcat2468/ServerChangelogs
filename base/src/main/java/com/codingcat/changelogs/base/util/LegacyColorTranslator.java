package com.codingcat.changelogs.base.util;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates legacy ampersand-based formatting codes (e.g. {@code &a}, {@code &l}, {@code &r})
 * into their MiniMessage tag equivalents, so that changelog authors who are used to the
 * legacy formatting system can keep using it, optionally alongside regular MiniMessage tags.
 * <p>
 * Color codes are translated into a {@code <reset>} tag followed by the matching color tag,
 * mirroring vanilla behavior where selecting a new color also clears any previously active
 * formatting (bold, italic, etc.). Formatting codes (bold, italic, ...) simply stack on top of
 * each other, and {@code &r} maps directly to {@code <reset>}.
 */
public final class LegacyColorTranslator {
    private static final @NotNull Pattern LEGACY_CODE_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    private static final @NotNull Map<Character, String> COLORS = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white")
    );

    private static final @NotNull Map<Character, String> FORMATS = Map.of(
            'k', "obfuscated", 'l', "bold", 'm', "strikethrough", 'n', "underlined", 'o', "italic"
    );

    private static volatile boolean enabled = true;

    private LegacyColorTranslator() {
    }

    /**
     * Enables/disables legacy code translation plugin-wide. Should be called whenever the
     * plugin configuration is (re-)loaded, mirroring the {@code legacy_color_codes} option.
     */
    public static void setEnabled(boolean isEnabled) {
        enabled = isEnabled;
    }

    /**
     * Replaces every legacy formatting code in the given raw (unparsed) string with its
     * MiniMessage tag equivalent. Regular MiniMessage tags already present in the input are
     * left untouched, so both can be freely mixed. Returns the input unchanged if legacy code
     * translation is currently disabled, or if the input doesn't contain any {@code &} at all.
     */
    public static @NotNull String translate(@NotNull String rawInput) {
        if (!enabled || rawInput.indexOf('&') == -1) return rawInput;
        Matcher matcher = LEGACY_CODE_PATTERN.matcher(rawInput);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            char code = Character.toLowerCase(matcher.group(1).charAt(0));
            String replacement;
            if (code == 'r') replacement = "<reset>";
            else if (COLORS.containsKey(code)) replacement = "<reset><" + COLORS.get(code) + ">";
            else replacement = "<" + FORMATS.get(code) + ">";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
