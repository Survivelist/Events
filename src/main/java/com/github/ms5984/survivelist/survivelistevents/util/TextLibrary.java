/*
 * MIT License
 *
 * Copyright (c) 2021 Matt (ms5984) <https://github.com/ms5984>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.ms5984.survivelist.survivelistevents.util;

import com.github.ms5984.survivelist.survivelistevents.SurvivelistEvents;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Process color coded text into proper formatted text.
 */
public final class TextLibrary {
    private static TextLibrary instance;
    private static final Pattern HEX_PATTERN = Pattern.compile("&(#(\\d|[A-F]|[a-f]){6})");
    private final boolean hasHexSupport;

    private TextLibrary(boolean hasHexSupport) {
        this.hasHexSupport = hasHexSupport;
    }

    public static String translate(String text) {
        if (text == null) return "null";
        if (hasHexSupport() && HEX_PATTERN.matcher(text).find()) {
            final Matcher matcher = HEX_PATTERN.matcher(text);
            final StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, ChatColor.of(matcher.group(1)).toString());
            }
            matcher.appendTail(sb);
            text = sb.toString();
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // Checks if version 1.16 or higher
    public static boolean hasHexSupport() {
        return instance != null ? instance.hasHexSupport : Bukkit.getVersion().matches("1\\.(1[6-9]|[2-9][0-9]).*");
    }

    public static void setup(SurvivelistEvents plugin) {
        instance = new TextLibrary(hasHexSupport());
        if (instance.hasHexSupport) {
            plugin.getLogger().info("Loaded 1.16 and up hex support!");
        }
    }
}
