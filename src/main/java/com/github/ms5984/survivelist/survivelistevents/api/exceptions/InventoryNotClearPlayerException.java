package com.github.ms5984.survivelist.survivelistevents.api.exceptions;

import org.bukkit.entity.Player;

/**
 * Thrown when a player is trying to join a game with items in their inventory.
 */
public final class InventoryNotClearPlayerException extends Exception {
    private static final long serialVersionUID = -2050790968074290813L;
    private final Player player;

    public InventoryNotClearPlayerException(Player player, String message) {
        super(message);
        this.player = player;
    }

    /**
     * Get the Player associated with this exception.
     *
     * @return the associated Player
     */
    public Player getPlayer() {
        return player;
    }
}
