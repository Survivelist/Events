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

import com.github.ms5984.survivelist.survivelistevents.api.exceptions.AlreadyPresentPlayerException;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.InventoryNotClearPlayerException;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.NotPresentPlayerException;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * Interface of the current server event.
 */
public interface ServerEvent {
    /**
     * Add a player to the event.
     *
     * @param player player to add
     * @return added player
     * @throws AlreadyPresentPlayerException if the player is already in the event
     * @throws InventoryNotClearPlayerException if the player's inventory is not empty
     */
    @NotNull EventPlayer addPlayer(Player player) throws AlreadyPresentPlayerException, InventoryNotClearPlayerException;

    /**
     * Remove a player from the event.
     *
     * @param player player to remove
     * @throws NotPresentPlayerException if the player is not in the event
     */
    void removePlayer(Player player) throws NotPresentPlayerException;

    /**
     * Get all players in the event.
     *
     * @return all players in the event
     */
    @NotNull Set<EventPlayer> getPlayers();

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

    /**
     * Teleport all players to the start.
     * <p>
     * Does nothing if {@link #getEventLocation()} is not present
     */
    default void teleportAllPlayers() {
        getPlayers().forEach(EventPlayer::teleportToEvent);
    }
}
