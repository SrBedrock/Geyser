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

package org.geysermc.geyser.platform.fabric.test;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.HashOps;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public class MinecraftComponentHashes {

    static List<DataComponentType<?>> components = new ArrayList<>();
    static List<Integer> hashes = new ArrayList<>();

    static {
        CompoundTag customData = new CompoundTag();
        customData.putString("hello", "g'day");
        customData.putBoolean("nice?", false);
        customData.putByte("coolness", (byte) 100);

        CompoundTag subTag = new CompoundTag();
        subTag.putString("is", "very cool");
        customData.put("geyser", subTag);

        ListTag list = new ListTag();
        ListTag subList = new ListTag();
        subList.add(StringTag.valueOf("in a list"));
        list.add(subList);
        customData.put("a list", list);

        addHash(DataComponents.CUSTOM_DATA, CustomData.of(customData));
    }

    private static <T> void addHash(DataComponentType<T> component, T value) {
        components.add(component);
        hashes.add(component.codecOrThrow().encodeStart(HashOps.CRC32C_INSTANCE, value).getOrThrow().asInt());
    }
}
