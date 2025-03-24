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
import org.geysermc.geyser.item.components.Rarity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
@FunctionalInterface
public interface MinecraftHasher<T> {

    MinecraftHasher<Byte> BYTE = hasher -> hasher.number(hasher.object());

    MinecraftHasher<Short> SHORT = hasher -> hasher.number(hasher.object());

    MinecraftHasher<Integer> INT = hasher -> hasher.number(hasher.object());

    MinecraftHasher<Long> LONG = hasher -> hasher.number(hasher.object());

    MinecraftHasher<Float> FLOAT = hasher -> hasher.number(hasher.object());

    MinecraftHasher<Double> DOUBLE = hasher -> hasher.number(hasher.object());

    MinecraftHasher<String> STRING = hasher -> hasher.string(hasher.object());

    MinecraftHasher<Boolean> BOOL = hasher -> hasher.bool(hasher.object());

    MinecraftHasher<Key> KEY = STRING.convert(Key::asString);

    MinecraftHasher<Integer> RARITY = fromIdEnum(Rarity.values(), Rarity::getName);

    MinecraftHasher<Integer> ENCHANTMENT = registry(JavaRegistries.ENCHANTMENT);

    MinecraftHasher<DataComponentType<?>> DATA_COMPONENT_TYPE = KEY.convert(DataComponentType::getKey);

    HashCode hash(ComponentHashEncoder<T> hasher);

    default <M> MinecraftHasher<M> convert(Function<M, T> mapper) {
        // What did I just code
        return mappedHasher -> hash(mappedHasher.create(mapper.apply(mappedHasher.object())));
    }

    default <M> MinecraftHasher<M> sessionConvert(BiFunction<GeyserSession, M, T> mapper) {
        // What did I just code part 2
        return mappedHasher -> mappedHasher.useSession(session -> hash(mappedHasher.create(mapper.apply(session, mappedHasher.object()))));
    }

    static <T extends Enum<T>> MinecraftHasher<Integer> fromIdEnum(T[] values, Function<T, String> toName) {
        return STRING.convert(id -> toName.apply(values[id]));
    }

    static MinecraftHasher<Integer> registry(JavaRegistryKey<?> registry) {
        return KEY.sessionConvert(registry::keyFromNetworkId);
    }
}
