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

import com.github.ms5984.survivelist.survivelistevents.util.TextLibrary;
import com.google.common.collect.ImmutableMap;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides a simple wrapper for configurable event items.
 *
 * @since 1.1.0
 */
@SerializableAs("EventItem")
public class EventItem implements ConfigurationSerializable {
    private final ItemStack item;

    /**
     * Construct an EventItem based on the provided item.
     *
     * @param itemStack an existing ItemStack to copy
     */
    public EventItem(@NotNull ItemStack itemStack) {
        this.item = new ItemStack(itemStack);
    }
    private EventItem(@NotNull Map<String, Object> serialized) throws IllegalArgumentException {
        final String type;
        final String name;
        final Map<String, Object> enchantments;
        try {
            type = (String) serialized.get("type");
            name = (String) serialized.get("name");
            //noinspection unchecked
            enchantments = (Map<String, Object>) serialized.get("enchantments");
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Unable to process map!", e);
        }
        if (type == null) throw new IllegalArgumentException("Unable to locate the required 'type' field!");
        final ItemStack itemStack = new ItemStack(Material.valueOf(type)); // automatically throws if type invalid
        if (name != null || enchantments != null) {
            final ItemMeta meta = itemStack.getItemMeta();
            //noinspection deprecation
            Optional.ofNullable(name).map(TextLibrary::translate).ifPresent(meta::setDisplayName);
            Optional.ofNullable(enchantments).ifPresent(enchantMap -> enchantMap.forEach((enchant, lvl) -> {
                int level;
                try {
                    level = (int) lvl;
                } catch (ClassCastException ignored) {
                    return;
                }
                final Enchantment byKey = Enchantment.getByKey(NamespacedKey.minecraft(enchant));
                if (byKey == null) return;
                meta.addEnchant(byKey, level, true);
            }));
            itemStack.setItemMeta(meta);
        }
        this.item = itemStack;
    }

    /**
     * Get a new copy of the item encapsulated by this object.
     *
     * @return a new copy of the item encapsulated by this object
     */
    public ItemStack getItemCopy() {
        return new ItemStack(item);
    }

    /**
     * Give this item to a player.
     * <p>
     * This method checks if they already have the item,
     * and if so, returns false.
     *
     * @param player a player
     * @return true only if we needed to give them the item
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean giveToPlayer(@NotNull Player player) {
        if (playerHasExact(player)) {
            return false;
        }
        player.getInventory().addItem(item);
        return true;
    }

    /**
     * Take this item from a player.
     * <p>
     * Delegates to {@link org.bukkit.inventory.Inventory#remove(ItemStack)},
     * so every exact stack match is removed.
     *
     * @param player a player
     * @return false if player did not have this item; true if remove attempted
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean takeFromPlayer(@NotNull Player player) {
        if (!playerHasExact(player)) {
            return false;
        }
        player.getInventory().remove(item);
        return true;
    }

    /**
     * Test if a player has this item.
     *
     * @param player a player
     * @return true for an exact match of the item (includes amount)
     */
    public boolean playerHasExact(@NotNull Player player) {
        return player.getInventory().contains(item);
    }

    /**
     * Check if an existing ItemStack if is an exact match.
     * <p>
     * Matches both meta and amount.
     *
     * @param testItem an item to test
     * @return true if the items have identical meta and amount
     * @implNote delegates to {@link ItemStack#equals(Object)}
     */
    public boolean checkExact(ItemStack testItem) {
        return item.equals(testItem);
    }

    /**
     * Check if an existing ItemStack is similar ignoring stack size.
     * <p>
     * This means they would stack (if possible). Matches meta.
     *
     * @param testItem an item to test
     * @return true if the items have similar meta
     * @implNote delegates to {@link ItemStack#isSimilar(ItemStack)}
     */
    public boolean checkSimilar(ItemStack testItem) {
        return item.isSimilar(testItem);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventItem eventItem = (EventItem) o;
        return item.equals(eventItem.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        builder.put("type", item.getType().name());
        //noinspection deprecation
        Optional.of(item)
                .filter(ItemStack::hasItemMeta)
                .map(ItemStack::getItemMeta)
                .filter(ItemMeta::hasDisplayName)
                .map(ItemMeta::getDisplayName)
                .map(s -> s.replace(ChatColor.COLOR_CHAR, '&'))
                .ifPresent(displayName -> builder.put("name", displayName));
        Optional.of(item)
                .filter(ItemStack::hasItemMeta)
                .map(ItemStack::getItemMeta)
                .filter(ItemMeta::hasEnchants)
                .map(ItemMeta::getEnchants)
                .ifPresent(map -> {
                    if (map.isEmpty()) return;
                    final ImmutableMap.Builder<String, Object> inner = new ImmutableMap.Builder<>();
                    map.forEach((enchant, lvl) -> inner.put(enchant.getKey().getKey(), lvl));
                    builder.put("enchants", inner);
                });
        return builder.build();
    }

    public static EventItem deserialize(@NotNull Map<String, Object> map) throws IllegalArgumentException {
        return new EventItem(map);
    }

    public static EventItem valueOf(@NotNull Map<String, Object> map) throws IllegalArgumentException {
        return new EventItem(map);
    }
}
