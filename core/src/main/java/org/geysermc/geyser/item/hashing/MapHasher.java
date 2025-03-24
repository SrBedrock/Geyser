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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public class MapHasher<T> {
    private final MinecraftHashEncoder encoder;
    private final T object;
    private final Map<HashCode, HashCode> map;

    MapHasher(T object, MinecraftHashEncoder encoder) {
        this.encoder = encoder;
        this.object = object;
        map = new HashMap<>();
    }

    public MapHasher<T> accept(String key, HashCode hash) {
        map.put(encoder.string(key), hash);
        return this;
    }

    public <V> MapHasher<T> accept(String key, MinecraftHasher<V> hasher, Function<T, V> extractor) {
        return accept(key, hasher.hash(extractor.apply(object), encoder));
    }

    public <V> MapHasher<T> optionalNullable(String key, MinecraftHasher<V> hasher, Function<T, V> extractor) {
        V value = extractor.apply(object);
        if (value != null) {
            accept(key, hasher.hash(value, encoder));
        }
        return this;
    }

    public <V> MapHasher<T> optional(String key, MinecraftHasher<V> hasher, Function<T, Optional<V>> extractor) {
        Optional<V> value = extractor.apply(object);
        value.ifPresent(v -> accept(key, hasher.hash(v, encoder)));
        return this;
    }

    public <V> MapHasher<T> optional(String key, MinecraftHasher<V> hasher, Function<T, V> extractor, V defaultValue) {
        V value = extractor.apply(object);
        if (!value.equals(defaultValue)) {
            accept(key, hasher.hash(value, encoder));
        }
        return this;
    }

    public <V> MapHasher<T> acceptList(String key, MinecraftHasher<V> valueHasher, Function<T, List<V>> extractor) {
        return accept(key, valueHasher.list().hash(extractor.apply(object), encoder));
    }

    public <V> MapHasher<T> optionalList(String key, MinecraftHasher<V> valueHasher, Function<T, List<V>> extractor) {
        List<V> list = extractor.apply(object);
        if (!list.isEmpty()) {
            accept(key, valueHasher.list().hash(list, encoder));
        }
        return this;
    }

    public HashCode build() {
        return encoder.map(map);
    }
}
