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
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.session.GeyserSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

// Based off the HashOps in mojmap, hashes a component, TODO: documentation
@SuppressWarnings("UnstableApiUsage")
public class ComponentHashEncoder<T> {
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
    private final GeyserSession session;
    private final T object;

    private final HashCode empty;
    private final HashCode falseHash;
    private final HashCode trueHash;

    public ComponentHashEncoder(GeyserSession session, T object) {
        hasher = Hashing.crc32();
        this.session = session;
        this.object = object;

        empty = hasher.hashBytes(EMPTY);
        falseHash = hasher.hashBytes(FALSE);
        trueHash = hasher.hashBytes(TRUE);
    }

    public <V> ComponentHashEncoder<V> create(V value) {
        return new ComponentHashEncoder<>(session, value);
    }

    public T object() {
        return object;
    }

    public HashCode useSession(Function<GeyserSession, HashCode> hasher) {
        return hasher.apply(session);
    }

    public <V> HashCode hashValue(MinecraftHasher<V> valueHasher, Function<T, V> extractor) {
        return valueHasher.hash(create(extractor.apply(object)));
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

    public <K, V> HashCode map(Map<K, V> map, MinecraftHasher<K> keyHasher, MinecraftHasher<V> valueHasher) {
        return map(map.entrySet().stream()
            .map(entry -> Map.entry(keyHasher.hash(create(entry.getKey())), valueHasher.hash(create(entry.getValue()))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public HashCode map(UnaryOperator<MapBuilder<T>> builder) {
        return builder.apply(new MapBuilder<>(this)).build();
    }

    public HashCode nbtMap(NbtMap map) {
        Map<HashCode, HashCode> hashed = new HashMap<>();
        for (String key : map.keySet()) {
            HashCode hashedKey = string(key);
            Object value = map.get(key);
            if (value instanceof NbtList<?> list) {
                hashed.put(hashedKey, nbtList(list));
            } else {
                map.listenForNumber(key, n -> hashed.put(hashedKey, number(n)));
                map.listenForString(key, s -> hashed.put(hashedKey, string(s)));
                map.listenForBoolean(key, b -> hashed.put(hashedKey, bool(b)));
                map.listenForCompound(key, compound -> hashed.put(hashedKey, nbtMap(compound)));

                map.listenForByteArray(key, bytes -> hashed.put(hashedKey, byteArray(bytes)));
                map.listenForIntArray(key, ints -> hashed.put(hashedKey, intArray(ints)));
                map.listenForLongArray(key, longs -> hashed.put(hashedKey, longArray(longs)));
            }
        }
        return map(hashed);
    }

    public HashCode nbtMap(Function<T, NbtMap> extractor) {
        return nbtMap(extractor.apply(object));
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

    @SuppressWarnings("unchecked")
    public HashCode nbtList(NbtList<?> nbtList) {
        NbtType<?> type = nbtList.getType();
        return list(builder -> {
            if (type == NbtType.BYTE) {
                builder.acceptBytes((List<Byte>) nbtList);
            } else if (type == NbtType.SHORT) {
                builder.acceptShorts((List<Short>) nbtList);
            } else if (type == NbtType.INT) {
                builder.acceptInts((List<Integer>) nbtList);
            } else if (type == NbtType.LONG) {
                builder.acceptLongs((List<Long>) nbtList);
            } else if (type == NbtType.FLOAT) {
                builder.acceptFloats((List<Float>) nbtList);
            } else if (type == NbtType.DOUBLE) {
                builder.acceptDoubles((List<Double>) nbtList);
            } else if (type == NbtType.STRING) {
                builder.acceptStrings((List<String>) nbtList);
            } else if (type == NbtType.LIST) {
                for (NbtList<?> list : (List<NbtList<?>>) nbtList) {
                    builder.accept(nbtList(list));
                }
            } else if (type == NbtType.COMPOUND) {
                for (NbtMap compound : (List<NbtMap>) nbtList) {
                    builder.accept(nbtMap(compound));
                }
            }
            return builder;
        });
    }

    public HashCode nbtList(Function<T, NbtList<?>> extractor) {
        return nbtList(extractor.apply(object));
    }

    public HashCode byteArray(byte[] bytes) {
        Hasher arrayHasher = hasher.newHasher();
        arrayHasher.putByte(TAG_BYTE_ARRAY_START);
        arrayHasher.putBytes(bytes);
        arrayHasher.putByte(TAG_BYTE_ARRAY_END);
        return arrayHasher.hash();
    }

    public HashCode intArray(int[] ints) {
        Hasher arrayHasher = hasher.newHasher();
        arrayHasher.putByte(TAG_INT_ARRAY_START);
        for (int i : ints) {
            arrayHasher.putInt(i);
        }
        arrayHasher.putByte(TAG_INT_ARRAY_END);
        return arrayHasher.hash();
    }

    public HashCode longArray(long[] longs) {
        Hasher arrayHasher = hasher.newHasher();
        arrayHasher.putByte(TAG_LONG_ARRAY_START);
        for (long l : longs) {
            arrayHasher.putLong(l);
        }
        arrayHasher.putByte(TAG_LONG_ARRAY_END);
        return arrayHasher.hash();
    }

    public static class MapBuilder<T> {
        private final ComponentHashEncoder<T> hasher;
        private final Map<HashCode, HashCode> map;

        private MapBuilder(ComponentHashEncoder<T> hasher) {
            this.hasher = hasher;
            map = new HashMap<>();
        }

        public MapBuilder<T> accept(String key, HashCode hash) {
            map.put(hasher.string(key), hash);
            return this;
        }

        public <V> MapBuilder<T> accept(String key, MinecraftHasher<V> valueHasher, Function<T, V> extractor) {
            return accept(key, hasher.hashValue(valueHasher, extractor));
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

        public MapBuilder<T> optionalByte(String key, Function<T, Byte> extractor, byte defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> acceptShort(String key, Function<T, Short> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> optionalShort(String key, Function<T, Short> extractor, short defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> acceptInt(String key, Function<T, Integer> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> optionalInt(String key, Function<T, Integer> extractor, int defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> acceptLong(String key, Function<T, Long> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> optionalLong(String key, Function<T, Long> extractor, long defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> acceptFloat(String key, Function<T, Float> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> optionalFloat(String key, Function<T, Float> extractor, float defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> acceptDouble(String key, Function<T, Double> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> optionalDouble(String key, Function<T, Double> extractor, double defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> accept(String key, String value) {
            return accept(key, hasher.string(value));
        }

        public MapBuilder<T> acceptString(String key, Function<T, String> extractor) {
            return accept(key, extractor.apply(hasher.object));
        }

        public MapBuilder<T> optionalString(String key, Function<T, String> extractor, String defaultValue) {
            return optional(key, extractor, defaultValue, this::accept);
        }

        public MapBuilder<T> accept(String key, boolean value) {
            return accept(key, hasher.bool(value));
        }

        public MapBuilder<T> acceptBool(String key, Predicate<T> extractor) {
            return accept(key, extractor.test(hasher.object));
        }

        public MapBuilder<T> optionalBool(String key, Predicate<T> extractor, boolean defaultValue) {
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

        public <V> MapBuilder<T> acceptList(String key, Function<T, List<V>> extractor, MinecraftHasher<V> valueHasher) {
            return accept(key, hasher.list(builder -> builder.accept(extractor.apply(hasher.object), valueHasher)));
        }

        public <V> MapBuilder<T> optionalList(String key, Function<T, List<V>> extractor, MinecraftHasher<V> valueHasher) {
            return optionalList(key, extractor, (builder, value) -> builder.accept(value, valueHasher));
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
        private final ComponentHashEncoder<T> hasher;
        private final List<HashCode> list;

        private ListBuilder(ComponentHashEncoder<T> hasher) {
            this.hasher = hasher;
            list = new ArrayList<>();
        }

        public ListBuilder<T> accept(HashCode hash) {
            list.add(hash);
            return this;
        }

        public <V> ListBuilder<T> accept(V value, MinecraftHasher<V> valueHasher) {
            return accept(valueHasher.hash(hasher.create(value)));
        }

        public ListBuilder<T> accept(byte value) {
            return accept(hasher.number(value));
        }

        public ListBuilder<T> accept(short value) {
            return accept(hasher.number(value));
        }

        public ListBuilder<T> accept(int value) {
            return accept(hasher.number(value));
        }

        public ListBuilder<T> accept(long value) {
            return accept(hasher.number(value));
        }

        public ListBuilder<T> accept(float value) {
            return accept(hasher.number(value));
        }

        public ListBuilder<T> accept(double value) {
            return accept(hasher.number(value));
        }

        public ListBuilder<T> acceptBytes(List<Byte> bytes) {
            bytes.forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptBytes(Function<T, List<Byte>> extractor) {
            return acceptBytes(extractor.apply(hasher.object));
        }

        public ListBuilder<T> acceptShorts(List<Short> shorts) {
            shorts.forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptShorts(Function<T, List<Short>> extractor) {
            return acceptShorts(extractor.apply(hasher.object));
        }

        public ListBuilder<T> acceptInts(List<Integer> ints) {
            ints.forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptInts(Function<T, List<Integer>> extractor) {
            return acceptInts(extractor.apply(hasher.object));
        }

        public ListBuilder<T> acceptLongs(List<Long> longs) {
            longs.forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptLongs(Function<T, List<Long>> extractor) {
            return acceptLongs(extractor.apply(hasher.object));
        }

        public ListBuilder<T> acceptFloats(List<Float> floats) {
            floats.forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptFloats(Function<T, List<Float>> extractor) {
            return acceptFloats(extractor.apply(hasher.object));
        }

        public ListBuilder<T> acceptDoubles(List<Double> doubles) {
            doubles.forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptDoubles(Function<T, List<Double>> extractor) {
            return acceptDoubles(extractor.apply(hasher.object));
        }

        public ListBuilder<T> accept(String value) {
            return accept(hasher.string(value));
        }

        public ListBuilder<T> acceptStrings(List<String> strings) {
            strings.forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptStrings(Function<T, List<String>> extractor) {
            return acceptStrings(extractor.apply(hasher.object));
        }

        public ListBuilder<T> accept(boolean value) {
            return accept(hasher.bool(value));
        }

        public ListBuilder<T> acceptBools(List<Boolean> bools) {
            bools.forEach(this::accept);
            return this;
        }

        public ListBuilder<T> acceptBools(Function<T, List<Boolean>> extractor) {
            return acceptBools(extractor.apply(hasher.object));
        }

        public <V> ListBuilder<T> accept(List<V> list, MinecraftHasher<V> valueHasher) {
            list.forEach(v -> accept(v, valueHasher));
            return this;
        }

        public HashCode build() {
            return hasher.list(list);
        }
    }
}
