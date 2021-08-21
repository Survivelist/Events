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
package com.github.ms5984.survivelist.survivelistevents.commands;

import com.github.ms5984.survivelist.survivelistevents.SurvivelistEvents;
import com.github.ms5984.survivelist.survivelistevents.api.EventPlayer;
import com.github.ms5984.survivelist.survivelistevents.api.EventService;
import com.github.ms5984.survivelist.survivelistevents.api.Mode;
import com.github.ms5984.survivelist.survivelistevents.model.EventItem;
import com.google.common.collect.ImmutableList;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles /eventtp command.
 *
 * @since 1.0.0
 */
public class EventTpCommand implements TabExecutor {
    private final EventService eventService;

    public EventTpCommand(SurvivelistEvents eventService) {
        this.eventService = eventService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Test permission
        if (!command.testPermission(sender)) {
            return true;
        }
        // Get event
        eventService.getEvent().ifPresentOrElse(event -> {
            final String eventMode = eventService.getEventMode();
            final Mode mode = eventService.getAllModes().get(eventMode);
            if (mode == null) throw new IllegalStateException();
            if (mode.usesEventLocation()) {
                // Teleport all players to the event
                sender.sendMessage(SurvivelistEvents.Messages.EVENT_TP.toString());
                event.teleportAllPlayers();
            } else if (mode.usesTeamLocations()) {
                // Teleport all players to the event, assigning teams
                sender.sendMessage(SurvivelistEvents.Messages.EVENT_TP.toString());
                final Optional<Set<String>> teams = eventService.getTeams();
                if (teams.isEmpty()) {
                    // Message sender "need to configure teams"
                    sender.sendMessage(SurvivelistEvents.Messages.NO_TEAMS.toString());
                    return;
                }
                final Set<EventPlayer> players = event.getPlayers();
                final int teamCount = teams.get().size();
                final List<Set<EventPlayer>> rosters = new ArrayList<>(teamCount);
                for (int i = 0; i < teamCount; i++) {
                    rosters.add(new HashSet<>());
                }
                // assign players
                int i = ThreadLocalRandom.current().nextInt(teamCount); // Slightly randomized
                for (EventPlayer player : players) {
                    rosters.get(i++).add(player);
                    if (!(i < teamCount)) {
                        i = 0;
                    }
                }
                eventService.getTeamLocations().ifPresent(map -> {
                    final List<String> teamListOrdered = new ArrayList<>(teams.get());
                    for (Map.Entry<String, Location> entry : map.entrySet()) {
                        final int indexOf = teamListOrdered.indexOf(entry.getKey());
                        if (indexOf == -1) throw new IllegalStateException();
                        final Location value = entry.getValue();
                        rosters.get(indexOf).forEach(ep -> {
                            final Player player = ep.getPlayer();
                            player.teleportAsync(value);
                            sender.sendMessage(SurvivelistEvents.Messages.ASSIGNED__.replace(player.getName(), entry.getKey()));
                        });
                    }
                });
            }
            // Give items, if needed
            if (!mode.itemsToGivePlayers().isEmpty()) {
                final Set<String> itemsToGivePlayers = mode.itemsToGivePlayers();
                final Map<String, EventItem> eventItems = eventService.getEventItems();
                final List<ItemStack> resolvedItems = new ArrayList<>(itemsToGivePlayers.size());
                for (String item : itemsToGivePlayers) {
                    if (eventItems.containsKey(item)) {
                        resolvedItems.add(eventItems.get(item).getItemCopy());
                    }
                }
                for (EventPlayer player : event.getPlayers()) {
                    resolvedItems.forEach(item -> player.getPlayer().getInventory().addItem(item));
                }
            }
        }, () -> {
            // Message sender "no event running"
            sender.sendMessage(SurvivelistEvents.Messages.NO_EVENT.toString());
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return ImmutableList.of();
    }
}
