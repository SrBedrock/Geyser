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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ListHasher<T> {
    private final MinecraftHashEncoder hasher;
    private final List<HashCode> list;

    ListHasher(MinecraftHashEncoder hasher) {
        this.hasher = hasher;
        list = new ArrayList<>();
    }

    public ListHasher<T> accept(HashCode hash) {
        list.add(hash);
        return this;
    }

    public <V> ListHasher<T> accept(V value, MinecraftHasher<V> valueHasher) {
        return accept(valueHasher.hash(hasher.create()));
    }

    public ListHasher<T> accept(byte value) {
        return accept(hasher.number(value));
    }

    public ListHasher<T> accept(short value) {
        return accept(hasher.number(value));
    }

    public ListHasher<T> accept(int value) {
        return accept(hasher.number(value));
    }

    public ListHasher<T> accept(long value) {
        return accept(hasher.number(value));
    }

    public ListHasher<T> accept(float value) {
        return accept(hasher.number(value));
    }

    public ListHasher<T> accept(double value) {
        return accept(hasher.number(value));
    }

    public ListHasher<T> acceptBytes(List<Byte> bytes) {
        bytes.forEach(this::accept);
        return this;
    }

    public ListHasher<T> acceptBytes(Function<T, List<Byte>> extractor) {
        return acceptBytes(extractor.apply(hasher.object));
    }

    public ListHasher<T> acceptShorts(List<Short> shorts) {
        shorts.forEach(this::accept);
        return this;
    }

    public ListHasher<T> acceptShorts(Function<T, List<Short>> extractor) {
        return acceptShorts(extractor.apply(hasher.object));
    }

    public ListHasher<T> acceptInts(List<Integer> ints) {
        ints.forEach(this::accept);
        return this;
    }

    public ListHasher<T> acceptInts(Function<T, List<Integer>> extractor) {
        return acceptInts(extractor.apply(hasher.object));
    }

    public ListHasher<T> acceptLongs(List<Long> longs) {
        longs.forEach(this::accept);
        return this;
    }

    public ListHasher<T> acceptLongs(Function<T, List<Long>> extractor) {
        return acceptLongs(extractor.apply(hasher.object));
    }

    public ListHasher<T> acceptFloats(List<Float> floats) {
        floats.forEach(this::accept);
        return this;
    }

    public ListHasher<T> acceptFloats(Function<T, List<Float>> extractor) {
        return acceptFloats(extractor.apply(hasher.object));
    }

    public ListHasher<T> acceptDoubles(List<Double> doubles) {
        doubles.forEach(this::accept);
        return this;
    }

    public ListHasher<T> acceptDoubles(Function<T, List<Double>> extractor) {
        return acceptDoubles(extractor.apply(hasher.object));
    }

    public ListHasher<T> accept(String value) {
        return accept(hasher.string(value));
    }

    public ListHasher<T> acceptStrings(List<String> strings) {
        strings.forEach(this::accept);
        return this;
    }

    public ListHasher<T> acceptStrings(Function<T, List<String>> extractor) {
        return acceptStrings(extractor.apply(hasher.object));
    }

    public ListHasher<T> accept(boolean value) {
        return accept(hasher.bool(value));
    }

    public ListHasher<T> acceptBools(List<Boolean> bools) {
        bools.forEach(this::accept);
        return this;
    }

    public ListHasher<T> acceptBools(Function<T, List<Boolean>> extractor) {
        return acceptBools(extractor.apply(hasher.object));
    }

    public <V> ListHasher<T> accept(List<V> list, MinecraftHasher<V> valueHasher) {
        list.forEach(v -> accept(v, valueHasher));
        return this;
    }

    public HashCode build() {
        return hasher.list(list);
    }
}
