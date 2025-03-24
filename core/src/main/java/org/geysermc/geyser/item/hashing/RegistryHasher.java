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

import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.CustomSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.Sound;

import java.util.Arrays;

public interface RegistryHasher extends MinecraftHasher<Integer> {

    RegistryHasher BLOCK = registry(JavaRegistries.BLOCK);

    RegistryHasher ITEM = registry(JavaRegistries.ITEM);

    RegistryHasher ENTITY_TYPE = enumIdRegistry(EntityType.values());

    RegistryHasher ENCHANTMENT = registry(JavaRegistries.ENCHANTMENT);

    MinecraftHasher<Effect> EFFECT = enumRegistry();

    RegistryHasher POTION = enumIdRegistry(Potion.values());

    MinecraftHasher<DataComponentType<?>> DATA_COMPONENT_TYPE = KEY.convert(DataComponentType::getKey);

    MinecraftHasher<BuiltinSound> BUILTIN_SOUND = KEY.convert(sound -> MinecraftKey.key(sound.getName()));

    MinecraftHasher<CustomSound> CUSTOM_SOUND = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_id", KEY, sound -> MinecraftKey.key(sound.getName()))
        .optional("range", FLOAT, CustomSound::getRange, 16.0F));

    MinecraftHasher<Sound> SOUND_EVENT = (sound, encoder) -> {
        if (sound instanceof BuiltinSound builtin) {
            return BUILTIN_SOUND.hash(builtin, encoder);
        }
        return CUSTOM_SOUND.hash((CustomSound) sound, encoder);
    };

    static RegistryHasher registry(JavaRegistryKey<?> registry) {
        MinecraftHasher<Integer> hasher = KEY.sessionConvert(registry::keyFromNetworkId);
        return hasher::hash;
    }

    static <T extends Enum<T>> MinecraftHasher<T> enumRegistry() {
        return KEY.convert(t -> MinecraftKey.key(t.name().toLowerCase()));
    }

    static <T extends Enum<T>> RegistryHasher enumIdRegistry(T[] values) {
        MinecraftHasher<Integer> hasher = KEY.convert(i -> MinecraftKey.key(values[i].name().toLowerCase()));
        return hasher::hash;
    }

    default MinecraftHasher<HolderSet> holderSet() {
        return (holder, encoder) -> {
            if (holder.getLocation() != null) {
                return TAG.hash(holder.getLocation(), encoder);
            } else if (holder.getHolders() != null) {
                if (holder.getHolders().length == 1) {
                    return hash(holder.getHolders()[0], encoder);
                }
                return list().hash(Arrays.stream(holder.getHolders()).boxed().toList(), encoder);
            }
            throw new IllegalStateException("HolderSet must have either tag location or holders");
        };
    }
}
