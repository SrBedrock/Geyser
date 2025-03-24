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
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

// Based off the HashOps in mojmap, hashes a component, TODO: documentation
@SuppressWarnings("UnstableApiUsage")
public class ComponentHasher<T> {
    private static final byte TAG_EMPTY = 1;
    private static final byte TAG_MAP_START = 2;
    private static final byte TAG_MAP_END = 3;
    private static final byte TAG_LIST_START = 4;
    private static final byte TAG_LIST_END = 5;
    private static final byte TAG_BYTE = 6;
    private static final byte TAG_SHORT = 7;
    private static final byte TAG_INT = 8;
    private static final byte TAG_LONG = 9;
    private static final byte TAG_FLOAT = 10;
    private static final byte TAG_DOUBLE = 11;
    private static final byte TAG_STRING = 12;
    private static final byte TAG_BOOLEAN = 13;
    private static final byte TAG_BYTE_ARRAY_START = 14;
    private static final byte TAG_BYTE_ARRAY_END = 15;
    private static final byte TAG_INT_ARRAY_START = 16;
    private static final byte TAG_INT_ARRAY_END = 17;
    private static final byte TAG_LONG_ARRAY_START = 18;
    private static final byte TAG_LONG_ARRAY_END = 19;

    private static final Comparator<HashCode> HASH_COMPARATOR = Comparator.comparingLong(HashCode::padToLong);
    private static final Comparator<Map.Entry<HashCode, HashCode>> MAP_ENTRY_ORDER = Map.Entry.<HashCode, HashCode>comparingByKey(HASH_COMPARATOR)
        .thenComparing(Map.Entry.comparingByValue(HASH_COMPARATOR));

    private static final byte[] EMPTY = new byte[]{TAG_EMPTY};
    private static final byte[] FALSE = new byte[]{TAG_BOOLEAN, 0};
    private static final byte[] TRUE = new byte[]{TAG_BOOLEAN, 1};

    private final HashFunction hasher;
    private final T object;

    private final HashCode empty;
    private final HashCode falseHash;
    private final HashCode trueHash;

    public ComponentHasher(T object) {
        hasher = Hashing.crc32();
        this.object = object;

        empty = hasher.hashBytes(EMPTY);
        falseHash = hasher.hashBytes(FALSE);
        trueHash = hasher.hashBytes(TRUE);
    }

    public HashCode empty() {
        return empty;
    }

    public HashCode number(Number number) {
        if (number instanceof Byte b) {
            return hasher.newHasher(2).putByte(TAG_BYTE).putByte(b).hash();
        } else if (number instanceof Short s) {
            return hasher.newHasher(3).putByte(TAG_SHORT).putShort(s).hash();
        } else if (number instanceof Integer i) {
            return hasher.newHasher(5).putByte(TAG_INT).putInt(i).hash();
        } else if (number instanceof Long l) {
            return hasher.newHasher(9).putByte(TAG_LONG).putLong(l).hash();
        } else if (number instanceof Float f) {
            return hasher.newHasher(5).putByte(TAG_FLOAT).putFloat(f).hash();
        }

        return hasher.newHasher(9).putByte(TAG_DOUBLE).putDouble(number.doubleValue()).hash();
    }

    public HashCode number(Function<T, Number> extractor) {
        return number(extractor.apply(object));
    }

    public HashCode string(String string) {
        return hasher.newHasher().putByte(TAG_STRING).putInt(string.length()).putUnencodedChars(string).hash();
    }

    public HashCode string(Function<T, String> extractor) {
        return string(extractor.apply(object));
    }

    public HashCode bool(boolean b) {
        return b ? trueHash : falseHash;
    }

    public HashCode bool(Predicate<T> extractor) {
        return bool(extractor.test(object));
    }

    public HashCode map(Map<HashCode, HashCode> map) {
        Hasher mapHasher = hasher.newHasher();
        mapHasher.putByte(TAG_MAP_START);
        map.entrySet().stream()
            .sorted(MAP_ENTRY_ORDER)
            .forEach(entry -> mapHasher.putBytes(entry.getKey().asBytes()).putBytes(entry.getValue().asBytes()));
        mapHasher.putByte(TAG_MAP_END);
        return mapHasher.hash();
    }

    public HashCode map(UnaryOperator<MapBuilder<T>> builder) {
        return builder.apply(new MapBuilder<>(this)).build();
    }

    public HashCode list(List<HashCode> list) {
        Hasher listHasher = hasher.newHasher();
        listHasher.putByte(TAG_LIST_START);
        list.forEach(hash -> listHasher.putBytes(hash.asBytes()));
        listHasher.putByte(TAG_LIST_END);
        return listHasher.hash();
    }

    public HashCode list(UnaryOperator<ListBuilder<T>> builder) {
        return builder.apply(new ListBuilder<>(this)).build();
    }

    public static class MapBuilder<T> {
        private final ComponentHasher<T> hasher;
        private final Map<HashCode, HashCode> map;

        private MapBuilder(ComponentHasher<T> hasher) {
            this.hasher = hasher;
            map = new HashMap<>();
        }

        public MapBuilder<T> accept(String key, HashCode hash) {
            map.put(hasher.string(key), hash);
            return this;
        }

        // Not using abstract Number class here so that we can refer to these methods using the shorthand MapBuilder::accept notation
        // Same for further methods below
        public MapBuilder<T> accept(String key, byte value) {
            return accept(key, hasher.number(value));
        }

        public MapBuilder<T> accept(String key, short value) {
            return accept(key, hasher.number(value));
        }

        public MapBuilder<T> accept(String key, int value) {
            return accept(key, hasher.number(value));
        }

        public MapBuilder<T> accept(String key, long value) {
            return accept(key, hasher.number(value));
        }

        public MapBuilder<T> accept(String key, float value) {
            return accept(key, hasher.number(value));
        }

        public MapBuilder<T> accept(String key, double value) {
            return accept(key, hasher.number(value));
        }

        public MapBuilder<T> acceptByte(String key, Function<T, Byte> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> acceptByte(String key, Function<T, Byte> extractor, byte defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> acceptShort(String key, Function<T, Short> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> acceptShort(String key, Function<T, Short> extractor, short defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> acceptInt(String key, Function<T, Integer> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> acceptInt(String key, Function<T, Integer> extractor, int defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> acceptLong(String key, Function<T, Long> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> acceptLong(String key, Function<T, Long> extractor, long defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> acceptFloat(String key, Function<T, Float> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> acceptFloat(String key, Function<T, Float> extractor, float defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> acceptDouble(String key, Function<T, Double> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> acceptDouble(String key, Function<T, Double> extractor, double defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> accept(String key, String value) {
            return accept(key, hasher.string(value));
        }

        public MapBuilder<T> acceptString(String key, Function<T, String> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> acceptString(String key, Function<T, String> extractor, String defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> accept(String key, boolean value) {
            return accept(key, hasher.bool(value));
        }

        public MapBuilder<T> acceptBool(String key, Predicate<T> extractor) {
            return accept(key, extractor.test(hasher.object));
        }

        public MapBuilder<T> acceptBool(String key, Predicate<T> extractor, boolean defaultValue) {
            return optional(key, extractor::test, defaultValue, this::accept);
        }

        private <V> MapBuilder<T> optional(String key, Function<T, V> extractor, V defaultValue, BiConsumer<String, V> acceptor) {
            V value = extractor.apply(hasher.object);
            if (!value.equals(defaultValue)) {
                acceptor.accept(key, value);
            }
            return this;
        }

        public MapBuilder<T> acceptBytes(String key, Function<T, List<Byte>> extractor) {
            return accept(key, hasher.list(list -> list.acceptBytes(extractor)));
        }

        public MapBuilder<T> optionalBytes(String key, Function<T, List<Byte>> extractor) {
            return optionalList(key, extractor, ListBuilder::accept);
        }

        public MapBuilder<T> acceptShorts(String key, Function<T, List<Short>> extractor) {
            return accept(key, hasher.list(list -> list.acceptShorts(extractor)));
        }

        public MapBuilder<T> optionalShorts(String key, Function<T, List<Short>> extractor) {
            return optionalList(key, extractor, ListBuilder::accept);
        }

        public MapBuilder<T> acceptInts(String key, Function<T, List<Integer>> extractor) {
            return accept(key, hasher.list(list -> list.acceptInts(extractor)));
        }

        public MapBuilder<T> optionalInts(String key, Function<T, List<Integer>> extractor) {
            return optionalList(key, extractor, ListBuilder::accept);
        }

        public MapBuilder<T> acceptLongs(String key, Function<T, List<Long>> extractor) {
            return accept(key, hasher.list(list -> list.acceptLongs(extractor)));
        }

        public MapBuilder<T> optionalLongs(String key, Function<T, List<Long>> extractor) {
            return optionalList(key, extractor, ListBuilder::accept);
        }

        public MapBuilder<T> acceptFloats(String key, Function<T, List<Float>> extractor) {
            return accept(key, hasher.list(list -> list.acceptFloats(extractor)));
        }

        public MapBuilder<T> optionalFloats(String key, Function<T, List<Float>> extractor) {
            return optionalList(key, extractor, ListBuilder::accept);
        }

        public MapBuilder<T> acceptDoubles(String key, Function<T, List<Double>> extractor) {
            return accept(key, hasher.list(list -> list.acceptDoubles(extractor)));
        }

        public MapBuilder<T> optionalDoubles(String key, Function<T, List<Double>> extractor) {
            return optionalList(key, extractor, ListBuilder::accept);
        }

        public MapBuilder<T> acceptStrings(String key, Function<T, List<String>> extractor) {
            return accept(key, hasher.list(list -> list.acceptStrings(extractor)));
        }

        public MapBuilder<T> optionalStrings(String key, Function<T, List<String>> extractor) {
            return optionalList(key, extractor, ListBuilder::accept);
        }

        public MapBuilder<T> acceptBools(String key, Function<T, List<Boolean>> extractor) {
            return accept(key, hasher.list(list -> list.acceptBools(extractor)));
        }

        public MapBuilder<T> optionalBools(String key, Function<T, List<Boolean>> extractor) {
            return optionalList(key, extractor, ListBuilder::accept);
        }

        private <V> MapBuilder<T> optionalList(String key, Function<T, List<V>> extractor, BiConsumer<ListBuilder<T>, V> acceptor) {
            List<V> list = extractor.apply(hasher.object);
            if (!list.isEmpty()) {
                accept(key, hasher.list(builder -> {
                    for (V value : list) {
                        acceptor.accept(builder, value);
                    }
                    return builder;
                }));
            }
            return this;
        }

        public HashCode build() {
            return hasher.map(map);
        }
    }

    public static class ListBuilder<T> {
        private final ComponentHasher<T> hasher;
        private final List<HashCode> list;

        private ListBuilder(ComponentHasher<T> hasher) {
            this.hasher = hasher;
            list = new ArrayList<>();
        }

        public ListBuilder<T> accept(byte value) {
            list.add(hasher.number(value));
            return this;
        }

        public ListBuilder<T> accept(short value) {
            list.add(hasher.number(value));
            return this;
        }

        public ListBuilder<T> accept(int value) {
            list.add(hasher.number(value));
            return this;
        }

        public ListBuilder<T> accept(long value) {
            list.add(hasher.number(value));
            return this;
        }

        public ListBuilder<T> accept(float value) {
            list.add(hasher.number(value));
            return this;
        }

        public ListBuilder<T> accept(double value) {
            list.add(hasher.number(value));
            return this;
        }

        public ListBuilder<T> acceptBytes(Function<T, List<Byte>> extractor) {
            extractor.apply(hasher.object).forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptShorts(Function<T, List<Short>> extractor) {
            extractor.apply(hasher.object).forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptInts(Function<T, List<Integer>> extractor) {
            extractor.apply(hasher.object).forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptLongs(Function<T, List<Long>> extractor) {
            extractor.apply(hasher.object).forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptFloats(Function<T, List<Float>> extractor) {
            extractor.apply(hasher.object).forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptDoubles(Function<T, List<Double>> extractor) {
            extractor.apply(hasher.object).forEach(this::accept);
            return this;
        }

        public ListBuilder<T> accept(String value) {
            list.add(hasher.string(value));
            return this;
        }

        public ListBuilder<T> acceptString(Function<T, String> extractor) {
            return accept(extractor.apply(hasher.object));
        }

        public ListBuilder<T> acceptStrings(Function<T, List<String>> extractor) {
            extractor.apply(hasher.object).forEach(this::accept);
            return this;
        }

        public ListBuilder<T> accept(boolean value) {
            list.add(hasher.bool(value));
            return this;
        }

        public ListBuilder<T> acceptBool(Predicate<T> extractor) {
            return accept(extractor.test(hasher.object));
        }

        public ListBuilder<T> acceptBools(Function<T, List<Boolean>> extractor) {
            extractor.apply(hasher.object).forEach(this::accept);
            return this;
        }

        public HashCode build() {
            return hasher.list(list);
        }
    }
}
