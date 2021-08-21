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
package com.github.ms5984.survivelist.survivelistevents.api;

import com.github.ms5984.survivelist.survivelistevents.api.exceptions.EventAlreadyRunningException;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Service which provides ServerEvent information.
 *
 * @since 1.0.0
 */
public interface EventService {
    /**
     * Start the event.
     *
     * @return new event instance
     * @throws EventAlreadyRunningException if there is an existing event instance
     */
    @NotNull ServerEvent startEvent() throws EventAlreadyRunningException;

    /**
     * Ends currently running event.
     *
     * @return true if event was ended; false otherwise
     */
    boolean endEvent();

    /**
     * Get the current server event if one is running.
     *
     * @return an Optional describing the current server event
     */
    @NotNull Optional<ServerEvent> getEvent();

    /**
     * Get the location of the event if it has been set.
     *
     * @return the location of the event if set
     */
    @NotNull Optional<Location> getEventLocation();

    /**
     * Set the location of the event.
     *
     * @param location location of the event
     */
    void setEventLocation(Location location);
}
