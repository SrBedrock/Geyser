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
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.IntComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Unit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

@SuppressWarnings("UnstableApiUsage")
public class ComponentHashers {
    private static final Map<DataComponentType<?>, Hasher<?>> hashers = new HashMap<>();

    static {
        register(DataComponentTypes.MAX_STACK_SIZE);
        register(DataComponentTypes.MAX_DAMAGE);
        register(DataComponentTypes.DAMAGE);
        register(DataComponentTypes.UNBREAKABLE);

        registerMap(DataComponentTypes.CUSTOM_MODEL_DATA, builder -> builder
            .optionalFloats("floats", CustomModelData::floats)
            .optionalBools("flags", CustomModelData::flags)
            .optionalStrings("strings", CustomModelData::strings)
            .optionalInts("colors", CustomModelData::colors));
    }

    private static void register(DataComponentType<Unit> component) {
        register(component, ComponentHasher::empty);
    }

    private static void register(IntComponentType component) {
        register(component, hasher -> hasher.number(integer -> integer));
    }

    private static <T> void registerMap(DataComponentType<T> component, UnaryOperator<ComponentHasher.MapBuilder<T>> builder) {
        register(component, hasher -> hasher.map(builder));
    }

    private static <T> void register(DataComponentType<T> component, Hasher<T> hasher) {
        if (hashers.containsKey(component)) {
            throw new IllegalArgumentException("Tried to register a hasher for a component twice");
        }
        hashers.put(component, hasher);
    }

    public static <T> HashCode hash(DataComponentType<T> component, T value) {
        Hasher<T> hasher = (Hasher<T>) hashers.get(component);
        if (hasher == null) {
            throw new IllegalStateException("Unregistered hasher for component " + component + "!");
        }
        return hasher.hash(new ComponentHasher<>(value));
    }

    @SuppressWarnings("UnstableApiUsage")
    @FunctionalInterface
    private interface Hasher<T> {

        HashCode hash(ComponentHasher<T> hasher);
    }
}
