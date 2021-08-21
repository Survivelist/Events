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

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player in an event.
 *
 * @since 1.0.0
 */
public abstract class EventPlayer {
    protected final ServerEvent event;
    protected final Player player;

    public EventPlayer(@NotNull ServerEvent event, @NotNull Player player) {
        this.event = event;
        this.player = player;
    }

    /**
     * Get the ServerEvent associated with this object.
     *
     * @return the ServerEvent associated with this object
     */
    public ServerEvent getEvent() {
        return event;
    }

    /**
     * Get the Player represented by this object.
     *
     * @return Bukkit player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Teleport the player to the event location if set.
     */
    public void teleportToEvent() {
        event.getEventService().getEventLocation().ifPresent(player::teleportAsync);
    }

    /**
     * Teleport the player back to where they were
     * when they joined the event.
     */
    public abstract void teleportBack();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventPlayer that = (EventPlayer) o;
        return event.equals(that.event) &&
                player.equals(that.player);
    }

    @Override
    public int hashCode() {
        return player.hashCode();
    }
}
