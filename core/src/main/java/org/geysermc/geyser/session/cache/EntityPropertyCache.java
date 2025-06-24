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

package org.geysermc.geyser.session.cache;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.packet.SyncEntityPropertyPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.Map;
import java.util.function.Consumer;

public class EntityPropertyCache {
    private final GeyserSession session;

    private final Map<EntityType, GeyserEntityProperties> entityProperties = new Object2ObjectOpenHashMap<>();

    public EntityPropertyCache(GeyserSession session) {
        this.session = session;
        resetAllProperties();
    }

    public void addProperties(EntityType type, Consumer<GeyserEntityProperties.Builder> builder) {
        GeyserEntityProperties.Builder newProperties = new GeyserEntityProperties.Builder(entityProperties.get(type));
        builder.accept(newProperties);
        entityProperties.put(type, newProperties.build());
        if (session.isSentSpawnPacket()) {
            syncEntityProperties(type);
            reloadEntities(type);
        }
    }

    public void resetProperties(EntityType type) {
        entityProperties.put(type, Registries.DEFAULT_BEDROCK_ENTITY_PROPERTIES.get(type));
        if (session.isSentSpawnPacket()) {
            syncEntityProperties(type);
            reloadEntities(type);
        }
    }

    public void resetAllProperties() {
        for (EntityType type : EntityType.values()) {
            GeyserEntityProperties properties = Registries.DEFAULT_BEDROCK_ENTITY_PROPERTIES.get(type);
            if (properties == null) {
                properties = GeyserEntityProperties.EMPTY;
            }
            entityProperties.put(type, properties);
        }
        if (session.isSentSpawnPacket()) {
            syncAllEntityProperties();
            reloadAllEntities();
        }
    }

    public GeyserEntityProperties getProperties(EntityType type) {
        return entityProperties.get(type);
    }

    public void syncEntityProperties(EntityType type) {
        EntityDefinition<?> definition = Registries.ENTITY_DEFINITIONS.get(type);
        if (definition == null) {
            // Not sent over network
            System.out.println("NULL FOR TYPE " + type);
            return;
        }
        NbtMap encoded = entityProperties.get(type).toNbtMap(definition.identifier());

        SyncEntityPropertyPacket syncEntityPropertyPacket = new SyncEntityPropertyPacket();
        syncEntityPropertyPacket.setData(encoded);
        session.sendUpstreamPacket(syncEntityPropertyPacket);

        if (type == EntityType.WOLF) {
            System.out.println("syncing entity properties for wolf " + encoded);
        }
    }

    public void syncAllEntityProperties() {
        for (EntityType type : entityProperties.keySet()) {
            syncEntityProperties(type);
        }
    }

    private void reloadAllEntities() {
        session.getEntityCache().getEntities().values().forEach(Entity::reloadPropertyManager);
    }

    private void reloadEntities(EntityType type) {
        session.getEntityCache().getEntities().values().stream()
            .filter(entity -> entity.getDefinition().entityType() == type)
            .forEach(Entity::reloadPropertyManager);
    }
}
