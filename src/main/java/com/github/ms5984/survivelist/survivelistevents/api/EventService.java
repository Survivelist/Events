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
import com.github.ms5984.survivelist.survivelistevents.model.EventItem;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    // new features

    /**
     * Get the team locations of the event if they have been set.
     *
     * @return an optional describing teams and locations
     * @implSpec The implementation should shield the contained
     *           Locations from modification; therefore, a deep
     *           copy should be performed.
     * @since 1.1.0
     */
    @NotNull Optional<Map<String, Location>> getTeamLocations();

    /**
     * Set the team locations of the event.
     * <p>
     * Team names cannot contain periods.
     *
     * @param team name of the team
     * @param teamLocation a valid location or null to clear
     * @throws IllegalArgumentException if team is not a valid team name
     * @implSpec All implementations should clone valid passed Locations.
     * @since 1.1.0
     */
    void setTeamLocation(@NotNull String team, @Nullable Location teamLocation) throws IllegalArgumentException;

    /**
     * Get the set of team names whose locations have been set.
     *
     * @return the set of team names whose locations have been set
     * @since 1.1.0
     */
    default @NotNull Optional<Set<String>> getTeams() {
        return getTeamLocations().map(Map::keySet);
    }

    /**
     * Get all event modes supported by this service.
     *
     * @return all event modes supported by this service
     * @since 1.1.0
     */
    @NotNull Map<String, Mode> getAllModes();

    /**
     * Get the current event mode.
     *
     * @return the current event mode
     * @since 1.1.0
     */
    @NotNull String getEventMode();

    /**
     * Set the current event mode.
     * <p>
     * <b>Ends the current event if a different type is running.</b>
     * <p>
     * Useful for designing mode-driven event behaviors.
     *
     * @param eventMode a valid event mode name
     * @throws IllegalArgumentException if eventMode is not a valid mode
     * @return true if the current event was stopped
     * @since 1.1.0
     */
    boolean setEventMode(String eventMode) throws IllegalArgumentException;

    /**
     * Get all loaded EventItems.
     *
     * @return map of all loaded EventItems
     * @since 1.1.0
     */
    @NotNull Map<String, EventItem> getEventItems();
}
