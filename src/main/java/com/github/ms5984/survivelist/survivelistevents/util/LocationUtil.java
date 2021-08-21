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

import org.bukkit.Location;
import org.bukkit.World;

/**
 * A few utilities for working with Bukkit locations.
 *
 * @since 1.1.0
 */
public class LocationUtil {
    private LocationUtil() { // utility class only
        throw new IllegalStateException();
    }

    /**
     * Convert a Location to a string with similar
     * formatting as the client's F3 debug menu.
     *
     * @param location a Bukkit Location
     * @return formatted string
     */
    public static String prettyPrintLocation(Location location) {
        if (location == null) return "null";
        final World world = location.getWorld();
        if (world == null) {
            return String.format("%.3f, %.5f, %.3f; yaw=%.1f/pitch=%.1f",
                    location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch()
            );
        }
        return String.format("%.3f, %.5f, %.3f; yaw=%.1f/pitch=%.1f in world=%s",
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(),
                world.getName()
        );
    }
}
