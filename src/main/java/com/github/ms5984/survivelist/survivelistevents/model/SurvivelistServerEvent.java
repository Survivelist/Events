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
package com.github.ms5984.survivelist.survivelistevents.model;

import com.github.ms5984.survivelist.survivelistevents.SurvivelistEvents;
import com.github.ms5984.survivelist.survivelistevents.api.EventPlayer;
import com.github.ms5984.survivelist.survivelistevents.api.ServerEvent;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.AlreadyPresentPlayerException;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.InventoryNotClearPlayerException;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.NotPresentPlayerException;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Plugin implementation of ServerEvent.
 */
public class SurvivelistServerEvent implements ServerEvent {
    private final JavaPlugin javaPlugin = JavaPlugin.getProvidingPlugin(getClass());
    private final UUID uuid = UUID.randomUUID();
    private final Map<UUID, EventPlayer> players = new ConcurrentHashMap<>();
    private final EventsListener eventsListener = new EventsListener();
    private Location eventLocation;

    @Override
    public @NotNull EventPlayer addPlayer(Player player) throws AlreadyPresentPlayerException, InventoryNotClearPlayerException {
        // Check for the player in the map
        final UUID uid = player.getUniqueId();
        if (players.containsKey(uid)) {
            throw new AlreadyPresentPlayerException(player, this, SurvivelistEvents.Messages.JOIN_ALREADY_IN.toString());
        }
        // Check if their inventory is empty
        if (!player.getInventory().isEmpty()) {
            throw new InventoryNotClearPlayerException(player, SurvivelistEvents.Messages.PLEASE_EMPTY_INVENTORY.toString());
        }
        // Save their location
        final @NotNull Location originalLocation = player.getLocation().clone();
        final EventPlayer eventPlayer = new EventPlayer(this, player) {
            @Override
            public void teleportBack() {
                player.teleportAsync(originalLocation);
            }
        };
        // Store in map
        players.put(uid, eventPlayer);
        return eventPlayer;
    }

    @Override
    public void removePlayer(Player player) throws NotPresentPlayerException {
        // Search for player in map
        final UUID uid = player.getUniqueId();
        final EventPlayer eventPlayer = players.get(uid);
        if (eventPlayer != null) {
            // Teleport back to original location
            eventPlayer.teleportBack();
            // Remove from map
            players.remove(uid);
            return;
        }
        throw new NotPresentPlayerException(player, SurvivelistEvents.Messages.LEAVE_NOT_IN.toString());
    }

    @Override
    public void sendMessage(String message, Predicate<Player> predicate) {
        Bukkit.getScheduler().runTaskAsynchronously(javaPlugin, () -> players.values().stream()
                .map(EventPlayer::getPlayer)
                .filter(predicate)
                .forEach(p -> p.sendMessage(message))
        );
    }

    @Override
    public @NotNull Set<EventPlayer> getPlayers() {
        return ImmutableSet.copyOf(players.values());
    }

    @Override
    public @NotNull Optional<Location> getEventLocation() {
        return Optional.ofNullable(eventLocation);
    }

    @Override
    public void setEventLocation(Location location) {
        eventLocation = (location == null) ? null : location.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SurvivelistServerEvent event = (SurvivelistServerEvent) o;
        return uuid.equals(event.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    private class EventsListener implements Listener {
        private EventsListener() {
            Bukkit.getPluginManager().registerEvents(this, javaPlugin);
        }

        @EventHandler
        public void onPlayerRespawnEvent(PlayerRespawnEvent e) {
            // Ignore players that haven't joined the event
            if (!players.containsKey(e.getPlayer().getUniqueId())) {
                return;
            }
            getEventLocation().ifPresent(e::setRespawnLocation);
        }
    }
}
