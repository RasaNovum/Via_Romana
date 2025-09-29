package net.rasanovum.viaromana.network;

import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.path.Node.NodeData;
import net.rasanovum.viaromana.util.PathSyncUtils;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.io.File;

public class ViaRomanaModVariables {
    public static List<Object> ValidTagList = new ArrayList<>();
    public static NetworkHandler networkHandler = null;

    public static ListTag saveNodeDataList(List<NodeData> nodeDataList) {
        ListTag listTag = new ListTag();
        for (NodeData data : nodeDataList) {
            CompoundTag nodeCompound = new CompoundTag();
            nodeCompound.putLong("pos", data.pos().asLong());
            nodeCompound.putFloat("quality", data.quality());
            nodeCompound.putFloat("clearance", data.clearance());
            listTag.add(nodeCompound);
        }
        return listTag;
    }

    public static List<NodeData> loadNodeDataList(ListTag listTag) {
        List<NodeData> loadedList = new ArrayList<>();

        if (listTag.getElementType() != Tag.TAG_COMPOUND) {
            return loadedList;
        }

        for (Tag tag : listTag) {
            CompoundTag nodeCompound = (CompoundTag) tag;
            BlockPos pos = BlockPos.of(nodeCompound.getLong("pos"));
            float quality = nodeCompound.getFloat("quality");
            float clearance = nodeCompound.getFloat("clearance");
            loadedList.add(new NodeData(pos, quality, clearance));
        }
        return loadedList;
    }

    public static class PlayerVariables {
        public boolean ChartingPath = false;
        public double FadeAmount = 0.0;
        public boolean FadeIncrease = false;
        public BlockPos lastNodePos = BlockPos.ZERO;
        public boolean ReceivedTutorial = false;

        public CompoundTag writeNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("ChartingPath", ChartingPath);
            nbt.putDouble("FadeAmount", FadeAmount);
            nbt.putBoolean("FadeIncrease", FadeIncrease);
            nbt.putLong("lastNodePos", lastNodePos != null ? lastNodePos.asLong() : BlockPos.ZERO.asLong());
            nbt.putBoolean("ReceivedTutorial", ReceivedTutorial);
            return nbt;
        }

        public void readNBT(CompoundTag nbt) {
            ChartingPath = nbt.getBoolean("ChartingPath");
            FadeAmount = nbt.getDouble("FadeAmount");
            FadeIncrease = nbt.getBoolean("FadeIncrease");
            long nodePos = nbt.getLong("lastNodePos");
            lastNodePos = nodePos != BlockPos.ZERO.asLong() ? BlockPos.of(nodePos) : BlockPos.ZERO;
            ReceivedTutorial = nbt.getBoolean("ReceivedTutorial");
        }

        public void syncToClient(ServerPlayer player) {
            if (player == null) return;
            if (networkHandler != null) {
                networkHandler.sendToPlayer(player, new PlayerVariablesSyncMessage(this));
            } else {
                ViaRomana.LOGGER.warn("Network handler not initialized, cannot sync PlayerVariables for {}.", player.getName().getString());
            }
        }
    }

    private static final Map<UUID, PlayerVariables> playerVariables = new ConcurrentHashMap<>();
    private static PlayerVariables clientPlayerVariables = new PlayerVariables();

    public static PlayerVariables getPlayerVariables(Entity entity) {
        if (entity == null) {
            ViaRomana.LOGGER.warn("Attempted to get PlayerVariables for null entity.");
            return new PlayerVariables();
        }

        if (entity.level().isClientSide()) {
            return clientPlayerVariables;
        }

        if (entity instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            return playerVariables.computeIfAbsent(playerId, id -> {
                PlayerVariables vars = new PlayerVariables();
                CompoundTag playerData = loadNBTFromFile(player.getServer(), id.toString());
                if (playerData != null) {
                    vars.readNBT(playerData);
                    ViaRomana.LOGGER.debug("Loaded PlayerVariables from file for {}", player.getName().getString());
                } else {
                    ViaRomana.LOGGER.debug("No existing PlayerVariables file found for {}, creating new.", player.getName().getString());
                }
                return vars;
            });
        }

        ViaRomana.LOGGER.warn("Tried to get PlayerVariables for non-ServerPlayer entity: {}", entity.getType().getDescriptionId());
        return new PlayerVariables();
    }

    public static void playerLoggedOut(ServerPlayer player) {
        if (player != null) {
            UUID playerId = player.getUUID();
            PlayerVariables vars = playerVariables.get(playerId);
            if (vars != null) {
                savePlayerVariablesToFile(player.getServer(), playerId.toString(), vars);
                playerVariables.remove(playerId);
                ViaRomana.LOGGER.debug("Saved and unloaded PlayerVariables for {}", player.getName().getString());
            }
        }
    }
    
    public static PlayerVariables playerLoggedIn(ServerPlayer player) {
        if (player == null) return new PlayerVariables();
        PlayerVariables vars = getPlayerVariables(player);
        vars.syncToClient(player);
        PathSyncUtils.syncPathGraphToPlayer(player);
        
        return vars;
    }

    public static void playerRespawned(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean keepAllPlayerData) {
        if (oldPlayer == null || newPlayer == null) return;

        PlayerVariables oldVars = playerVariables.get(oldPlayer.getUUID());
        PlayerVariables newVars = getPlayerVariables(newPlayer);

        if (oldVars != null) {
            if (keepAllPlayerData) {
                newVars.readNBT(oldVars.writeNBT());
                ViaRomana.LOGGER.debug("Copied all PlayerVariables on respawn for {}", newPlayer.getName().getString());
            } else {
                ViaRomana.LOGGER.debug("Did not copy PlayerVariables on respawn (keepAllPlayerData=false) for {}", newPlayer.getName().getString());
            }
            newVars.syncToClient(newPlayer);
        } else {
            ViaRomana.LOGGER.warn("Could not find old PlayerVariables for {} during respawn.", oldPlayer.getName().getString());
            newVars.syncToClient(newPlayer);
        }
    }

    public static void savePlayerVariablesToFile(MinecraftServer server, String playerUUID, PlayerVariables data) {
        if (server == null || data == null) return;
        saveNBTToFile(server, playerUUID, data.writeNBT());
    }

    public static void loadPlayerVariablesFromFile(ServerPlayer player) {
        if (player != null && player.getServer() != null) {
            UUID playerId = player.getUUID();
            CompoundTag playerData = loadNBTFromFile(player.getServer(), playerId.toString());
            if (playerData != null) {
                PlayerVariables playerVar = playerVariables.computeIfAbsent(playerId, id -> new PlayerVariables());
                playerVar.readNBT(playerData);
                ViaRomana.LOGGER.debug("Force-loaded PlayerVariables from file for {}", player.getName().getString());
                playerVar.syncToClient(player);
            }
        }
    }

    private static File getPlayerDataDirectory(MinecraftServer server) {
        return new File(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), ViaRomana.MODID);
    }

    private static void saveNBTToFile(MinecraftServer server, String fileName, CompoundTag nbt) {
        if (server == null) return;
        try {
            File dataDir = getPlayerDataDirectory(server);
            if (!dataDir.exists()) {
                if (!dataDir.mkdirs()) {
                    ViaRomana.LOGGER.error("Could not create player data directory: {}", dataDir.getAbsolutePath());
                    return;
                } else {
                    ViaRomana.LOGGER.debug("Created player data directory: {}", dataDir.getAbsolutePath());
                }
            }
            File file = new File(dataDir, fileName + ".dat");
            File backupFile = new File(dataDir, fileName + ".dat_old");

            if (file.exists()) {
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                file.renameTo(backupFile);
            }

            NbtIo.writeCompressed(nbt, file.toPath());

        } catch (IOException e) {
            ViaRomana.LOGGER.error("Failed to save player variables NBT for " + fileName, e);
        }
    }

    private static CompoundTag loadNBTFromFile(MinecraftServer server, String fileName) {
        if (server == null) return null;
        File dataDir = getPlayerDataDirectory(server);
        File file = new File(dataDir, fileName + ".dat");
        File backupFile = new File(dataDir, fileName + ".dat_old");

        if (file.exists()) {
            try {
                return NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
            } catch (IOException e) {
                ViaRomana.LOGGER.error("Failed to load player variables NBT from " + file.getName() + ", trying backup.", e);
                if (backupFile.exists()) {
                    try {
                        ViaRomana.LOGGER.warn("Attempting to load from backup file: {}", backupFile.getName());
                        return NbtIo.readCompressed(backupFile.toPath(), NbtAccounter.unlimitedHeap());
                    } catch (IOException e2) {
                        ViaRomana.LOGGER.error("Failed to load player variables NBT from backup " + backupFile.getName() + " as well.", e2);
                    }
                }
            }
        } else if (backupFile.exists()) {
            try {
                ViaRomana.LOGGER.warn("Main file {} missing, attempting to load from backup file: {}", file.getName(), backupFile.getName());
                return NbtIo.readCompressed(backupFile.toPath(), NbtAccounter.unlimitedHeap());
            } catch (IOException e) {
                ViaRomana.LOGGER.error("Failed to load player variables NBT from backup " + backupFile.getName(), e);
            }
        }
        return null;
    }

    public static record PlayerVariablesSyncMessage(CompoundTag dataTag) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlayerVariablesSyncMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:player_variables_sync"));

        public static final StreamCodec<FriendlyByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PlayerVariablesSyncMessage decode(FriendlyByteBuf buffer) {
                return new PlayerVariablesSyncMessage(buffer.readNbt());
            }

            @Override
            public void encode(FriendlyByteBuf buffer, PlayerVariablesSyncMessage packet) {
                buffer.writeNbt(packet.dataTag);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public PlayerVariablesSyncMessage(PlayerVariables data) {
            this(data.writeNBT());
        }

        public static void handleClient(PlayerVariablesSyncMessage message) {
            clientPlayerVariables.readNBT(message.dataTag);
            ViaRomana.LOGGER.debug("Client received and processed PlayerVariables sync.");
        }

        public static void handleServer(PlayerVariablesSyncMessage message, ServerPlayer player) {
            if (player != null) {
                PlayerVariables variables = getPlayerVariables(player);
                variables.readNBT(message.dataTag);
                ViaRomana.LOGGER.debug("Server received and processed PlayerVariables sync from {}", player.getName().getString());
            }
        }
    }
}
