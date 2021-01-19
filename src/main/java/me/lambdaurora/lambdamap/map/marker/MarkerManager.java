/*
 * Copyright (c) 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lambdaurora.lambdamap.map.marker;

import me.lambdaurora.lambdamap.map.WorldMap;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Manages the markers of a world map.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class MarkerManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final List<Marker> markers = new ArrayList<>();
    private final WorldMap map;
    private final File file;

    private ItemStack lastFilledMapStack;

    public MarkerManager(WorldMap map) {
        this.map = map;
        this.file = new File(this.map.getDirectory(), "markers.nbt");
    }

    public Marker addMarker(MarkerType type, MarkerSource source, int x, int y, int z, float rotation, @Nullable Text text) {
        Marker marker = new Marker(type, source, x, y, z, rotation, text);
        this.addMarker(marker);
        return marker;
    }

    public void addMarker(Marker marker) {
        this.markers.removeIf(other -> marker.getX() == other.getX() && marker.getY() == other.getY() && marker.getZ() == other.getZ());
        this.markers.add(marker);
    }

    public void removeMarkersAt(BlockPos pos) {
        this.markers.removeIf(other -> pos.getX() == other.getX() && pos.getY() == other.getY() && pos.getZ() == other.getZ());
    }

    public void removeMarker(Marker marker) {
        this.markers.remove(marker);
    }

    public Stream<Marker> streamMarkers() {
        return this.markers.stream();
    }

    public Stream<Marker> streamMarkersInBox(int startX, int startZ, int width, int height) {
        int endX = startX + width;
        int endZ = startZ + height;
        return this.streamMarkers().filter(marker -> marker.getX() >= startX && marker.getZ() >= startZ && marker.getX() < endX && marker.getZ() < endZ);
    }

    public void tick(ClientWorld world) {
        // Check for existence of the banner markers in the world if possible.
        Iterator<Marker> it = this.markers.iterator();
        while (it.hasNext()) {
            Marker marker = it.next();
            if (marker.getSource() != MarkerSource.BANNER)
                continue;
            Chunk chunk = world.getChunk(marker.getChunkX(), marker.getChunkZ(), ChunkStatus.FULL, false);
            if (chunk == null)
                continue;
            Marker bannerMarker = Marker.fromBanner(world, marker.getPos());
            if (bannerMarker == null)
                it.remove();
        }

        MinecraftClient client = MinecraftClient.getInstance();

        // Filled map stuff
        // 1. Try to import the markers of the filled map.
        // 2. Try to import the colors of the filled map if it has absolute coordinates markers.
        ItemStack stack = client.player.getMainHandStack();
        if (!stack.isEmpty() && stack.isOf(Items.FILLED_MAP) && stack.hasTag() && stack != this.lastFilledMapStack) {
            CompoundTag tag = stack.getTag();
            List<Marker> mapMarkers = new ArrayList<>();
            tag.getList("Decorations", NbtType.COMPOUND).stream().map(decoration -> ((CompoundTag) decoration)).forEach(decoration -> {
                MapIcon.Type type = MapIcon.Type.byId(decoration.getByte("type"));
                if (type.isAlwaysRendered()) {
                    mapMarkers.add(this.addMarker(MarkerType.getVanillaMarkerType(type), MarkerSource.FILLED_MAP,
                            (int) decoration.getDouble("x"), 64, (int) decoration.getDouble("z"),
                            decoration.getFloat("rot"), null));
                }
            });

            if (!mapMarkers.isEmpty()) {
                Integer mapId = FilledMapItem.getMapId(stack);
                if (mapId != null) {
                    MapState mapState = FilledMapItem.getMapState(mapId, world);
                    if (mapState != null) {
                        this.map.importMapState(mapState, mapMarkers);
                    }
                }
            }
            this.lastFilledMapStack = stack;
        }
    }

    public void load() {
        if (this.file.exists()) {
            try {
                this.fromTag(NbtIo.readCompressed(this.file));
            } catch (IOException e) {
                LOGGER.error("Failed to read markers from " + this.file + ".", e);
            }
        }
    }

    public void save() {
        try {
            NbtIo.writeCompressed(this.toTag(), this.file);
        } catch (IOException e) {
            LOGGER.error("Failed to save markers to " + this.file + ".", e);
        }
    }

    public void fromTag(CompoundTag tag) {
        ListTag list = tag.getList("markers", NbtType.COMPOUND);
        list.forEach(child -> this.markers.add(Marker.fromTag((CompoundTag) child)));
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        this.markers.stream().map(Marker::toTag).forEach(list::add);
        tag.put("markers", list);
        return tag;
    }
}