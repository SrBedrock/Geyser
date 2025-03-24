/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.item.hashing;

import com.google.common.hash.HashCode;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.item.components.Rarity;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.IntComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.TooltipDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Unit;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseCooldown;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@SuppressWarnings("UnstableApiUsage")
public class ComponentHashers {
    private static final Map<DataComponentType<?>, MinecraftHasher<?>> hashers = new HashMap<>();

    static {
        register(DataComponentTypes.CUSTOM_DATA, MinecraftHasher.NBT_MAP);
        register(DataComponentTypes.MAX_STACK_SIZE);
        register(DataComponentTypes.MAX_DAMAGE);
        register(DataComponentTypes.DAMAGE);
        register(DataComponentTypes.UNBREAKABLE);

        // TODO custom name, component
        // TODO item name, component

        register(DataComponentTypes.ITEM_MODEL, MinecraftHasher.KEY);

        // TODO lore, component

        register(DataComponentTypes.RARITY, MinecraftHasher.RARITY);
        register(DataComponentTypes.ENCHANTMENTS, MinecraftHasher.map(RegistryHasher.ENCHANTMENT, MinecraftHasher.INT).convert(ItemEnchantments::getEnchantments));

        // TODO can place on/can break on, complicated
        // TODO attribute modifiers, attribute registry and equipment slot group hashers

        registerMap(DataComponentTypes.CUSTOM_MODEL_DATA, builder -> builder
            .optionalList("floats", MinecraftHasher.FLOAT, CustomModelData::floats)
            .optionalList("flags", MinecraftHasher.BOOL, CustomModelData::flags)
            .optionalList("strings", MinecraftHasher.STRING, CustomModelData::strings)
            .optionalList("colors", MinecraftHasher.INT, CustomModelData::colors));

        registerMap(DataComponentTypes.TOOLTIP_DISPLAY, builder -> builder
            .optional("hide_tooltip", MinecraftHasher.BOOL, TooltipDisplay::hideTooltip, false)
            .optionalList("hidden_components", RegistryHasher.DATA_COMPONENT_TYPE, TooltipDisplay::hiddenComponents));

        register(DataComponentTypes.REPAIR_COST);

        register(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, MinecraftHasher.BOOL);
        //register(DataComponentTypes.INTANGIBLE_PROJECTILE); // TODO MCPL is wrong

        registerMap(DataComponentTypes.FOOD, builder -> builder
            .accept("nutrition", MinecraftHasher.INT, FoodProperties::getNutrition)
            .accept("saturation", MinecraftHasher.FLOAT, FoodProperties::getSaturationModifier)
            .optional("can_always_eat", MinecraftHasher.BOOL, FoodProperties::isCanAlwaysEat, false));
        registerMap(DataComponentTypes.CONSUMABLE, builder -> builder
            .optional("consume_seconds", MinecraftHasher.FLOAT, Consumable::consumeSeconds, 1.6F)
            .optional("animation", MinecraftHasher.ITEM_USE_ANIMATION, Consumable::animation, Consumable.ItemUseAnimation.EAT)
            .optional("sound", RegistryHasher.SOUND_EVENT, Consumable::sound, BuiltinSound.ENTITY_GENERIC_EAT)
            .optional("has_consume_particles", MinecraftHasher.BOOL, Consumable::hasConsumeParticles, true)); // TODO consume effect needs identifier in MCPL

        // TODO use remainder needs item stack codec, recursion go brr

        registerMap(DataComponentTypes.USE_COOLDOWN, builder -> builder
            .accept("seconds", MinecraftHasher.FLOAT, UseCooldown::seconds)
            .optionalNullable("cooldown_group", MinecraftHasher.KEY, UseCooldown::cooldownGroup));
        registerMap(DataComponentTypes.DAMAGE_RESISTANT, builder -> builder
            .accept("types", MinecraftHasher.TAG, Function.identity()));
        registerMap(DataComponentTypes.TOOL, builder -> builder
            .accept("rules", MinecraftHasher.TOOL_RULE.list(), ToolData::getRules)
            .optional("default_mining_speed", MinecraftHasher.FLOAT, ToolData::getDefaultMiningSpeed, 1.0F)
            .optional("damage_per_block", MinecraftHasher.INT, ToolData::getDamagePerBlock, 1)
            .optional("can_destroy_blocks_in_creative", MinecraftHasher.BOOL, ToolData::isCanDestroyBlocksInCreative, true));
    }

    private static void register(DataComponentType<Unit> component) {
        register(component, MinecraftHasher.UNIT);
    }

    private static void register(IntComponentType component) {
        register(component, MinecraftHasher.INT);
    }

    private static <T> void registerMap(DataComponentType<T> component, UnaryOperator<MapHasher<T>> builder) {
        register(component, MinecraftHasher.mapBuilder(builder));
    }

    private static <T> void register(DataComponentType<T> component, MinecraftHasher<T> hasher) {
        if (hashers.containsKey(component)) {
            throw new IllegalArgumentException("Tried to register a hasher for a component twice");
        }
        hashers.put(component, hasher);
    }

    public static <T> HashCode hash(GeyserSession session, DataComponentType<T> component, T value) {
        MinecraftHasher<T> hasher = (MinecraftHasher<T>) hashers.get(component);
        if (hasher == null) {
            throw new IllegalStateException("Unregistered hasher for component " + component + "!");
        }
        return hasher.hash(value, new MinecraftHashEncoder(session));
    }

    public static void testHashing(GeyserSession session) {
        // Hashed values generated by vanilla Java

        NbtMap customData = NbtMap.builder()
            .putString("hello", "g'day")
            .putBoolean("nice?", false)
            .putByte("coolness", (byte) 100)
            .putCompound("geyser", NbtMap.builder()
                .putString("is", "very cool")
                .build())
            .putList("a list", NbtType.LIST, List.of(new NbtList<>(NbtType.STRING, "in a list")))
            .build();

        testHash(session, DataComponentTypes.CUSTOM_DATA, customData, -385053299);

        testHash(session, DataComponentTypes.MAX_STACK_SIZE, 64, 733160003);
        testHash(session, DataComponentTypes.MAX_DAMAGE, 13, -801733367);
        testHash(session, DataComponentTypes.DAMAGE, 459, 1211405277);
        testHash(session, DataComponentTypes.UNBREAKABLE, Unit.INSTANCE, -982207288);

        testHash(session, DataComponentTypes.ITEM_MODEL, MinecraftKey.key("testing"), -689946239);

        testHash(session, DataComponentTypes.RARITY, Rarity.COMMON.ordinal(), 75150990);
        testHash(session, DataComponentTypes.RARITY, Rarity.RARE.ordinal(), -1420566726);
        testHash(session, DataComponentTypes.RARITY, Rarity.EPIC.ordinal(), -292715907);

        testHash(session, DataComponentTypes.ENCHANTMENTS, new ItemEnchantments(Map.of(
            0, 1
        ), false), 0); // TODO identifier lookup

        testHash(session, DataComponentTypes.CUSTOM_MODEL_DATA,
            new CustomModelData(List.of(5.0F, 3.0F, -1.0F), List.of(false, true, false), List.of("1", "3", "2"), List.of(3424, -123, 345)), 1947635619);

        testHash(session, DataComponentTypes.CUSTOM_MODEL_DATA,
            new CustomModelData(List.of(5.03F, 3.0F, -1.11F), List.of(true, true, false), List.of("2", "5", "7"), List.of()), -512419908);

        testHash(session, DataComponentTypes.TOOLTIP_DISPLAY, new TooltipDisplay(false, List.of(DataComponentTypes.CONSUMABLE, DataComponentTypes.DAMAGE)), -816418453);
        testHash(session, DataComponentTypes.TOOLTIP_DISPLAY, new TooltipDisplay(true, List.of()), 14016722);
        testHash(session, DataComponentTypes.TOOLTIP_DISPLAY, new TooltipDisplay(false, List.of()), -982207288);

        testHash(session, DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true, -1019818302);
        testHash(session, DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false, 828198337);

        testHash(session, DataComponentTypes.FOOD, new FoodProperties(5, 1.4F, false), 445786378);
        testHash(session, DataComponentTypes.FOOD, new FoodProperties(3, 5.7F, true), 1917653498);
        testHash(session, DataComponentTypes.FOOD, new FoodProperties(7, 0.15F, false), -184166204);

        testHash(session, DataComponentTypes.DAMAGE_RESISTANT, Key.key("testing"), -1230493835);

        testHash(session, DataComponentTypes.TOOL, new ToolData(List.of(), 5.0F, 3, false), -1789071928);
        testHash(session, DataComponentTypes.TOOL, new ToolData(List.of(), 3.0F, 1, true), -7422944);
        testHash(session, DataComponentTypes.TOOL, new ToolData(List.of(
            new ToolData.Rule(new HolderSet(Key.key("acacia_logs")), null, null),
            new ToolData.Rule(new HolderSet(new int[]{Blocks.JACK_O_LANTERN.javaId(), Blocks.WALL_TORCH.javaId()}), 4.2F, true),
            new ToolData.Rule(new HolderSet(new int[]{Blocks.PUMPKIN.javaId()}), 7.0F, false)),
            1.0F, 1, true), 2103678261);

        // Chunk errors are spamming logs and I don't need to log in anyway
        session.disconnect("AAAAAA");
    }

    private static <T> void testHash(GeyserSession session, DataComponentType<T> component, T value, int expected) {
        int got = hash(session, component, value).asInt();
        System.out.println("Testing hashing component " + component.getKey() + ", expected " + expected + ", got " + got + " " + (got == expected ? "PASS" : "ERROR"));
    }
}
