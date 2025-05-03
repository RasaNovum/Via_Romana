package net.rasanovum.viaromana.network;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.util.datafix.DataFixTypes;
import net.rasanovum.viaromana.PlatformUtils;

import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;

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

    private static ListTag saveArrayList(List<?> list) {
        ListTag listTag = new ListTag();
        for (Object e : list) {
            CompoundTag tag = new CompoundTag();
            if (e instanceof String)
                tag.putString("value", (String) e);
            else if (e instanceof Number)
                tag.putDouble("value", ((Number) e).doubleValue());
            else if (e instanceof Boolean)
                tag.putBoolean("value", (Boolean) e);
            else if (e instanceof List)
                tag.put("value", saveArrayList((List<?>) e));
            listTag.add(tag);
        }
        return listTag;
    }

    private static List<Object> loadArrayList(ListTag listTag) {
        List<Object> list = new ArrayList<>();
        for (Tag e : listTag) {
            if (e instanceof CompoundTag tag) {
                Tag value = tag.get("value");
                if (value instanceof StringTag)
                    list.add(tag.getString("value"));
                else if (value instanceof NumericTag)
                    list.add(tag.getDouble("value"));
                else if (value instanceof ByteTag)
                    list.add(tag.getBoolean("value"));
                else if (value instanceof ListTag)
                    list.add(loadArrayList((ListTag) value));
            }
        }
        return list;
    }

    public static class WorldVariables extends SavedData {
        private static final String DATA_NAME = "via_romana_worldvars";
        public static WorldVariables clientSide = new WorldVariables();
        private boolean dirty = false;

        public static WorldVariables load(CompoundTag tag, HolderLookup.Provider provider) {
            WorldVariables data = new WorldVariables();
            data.read(tag);
            return data;
        }

        public void read(CompoundTag nbt) {
        }

        @Override
        public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
            return nbt;
        }

        @Override
        public void setDirty() {
            super.setDirty();
            this.dirty = true;
        }

        public void markClean() {
            this.dirty = false;
        }

        @Override
        public boolean isDirty() {
            return this.dirty;
        }

        public static WorldVariables get(LevelAccessor world) {
            if (world instanceof ServerLevel level) {
                return level.getDataStorage().computeIfAbsent(new Factory<>(WorldVariables::new, WorldVariables::load, DataFixTypes.LEVEL), DATA_NAME);
            } else {
                return clientSide;
            }
        }

        public void syncToPlayer(ServerPlayer player) {
            if (networkHandler != null) {
                networkHandler.sendToPlayer(player, new SavedDataSyncMessage(1, this, player));
            } else {
                PlatformUtils.getLogger().warn("Network handler not initialized, cannot sync WorldVariables.");
            }
            markClean();
        }
    }

    public static class MapVariables extends SavedData {
        private static final String DATA_NAME = "via_romana_mapvars";
        public static MapVariables clientSide = new MapVariables();
        private boolean dirty = false;

        public String AcceptedBlockIDs = "\"\"";
        public String AcceptedBlockStrings = "\"\"";
        public String AcceptedBlockTags = "\"\"";
        public String AcceptedDimensions = "\"\"";
        public String AcceptedEntities = "\"\"";
        public double InfrastructureCheckQuality = 0.6;
        public double InfrastructureCheckRadius = 1.0;
        public double NodeDistanceMaximum = 20.0;
        public double NodeDistanceMinimum = 10.0;
        public double PathDistanceMaximum = 100000.0;
        public double PathDistanceMinimum = 50.0;
        public double TravelFatigueCooldown = 30.0;
        public List<Object> ValidBlockList = new ArrayList<>();
        public List<Object> ValidStringList = new ArrayList<>();
        public List<Object> ValidEntityList = new ArrayList<>();
        public List<Object> ValidDimensionList = new ArrayList<>();
        public List<Object> ValidSignList = new ArrayList<>();

        public static MapVariables load(CompoundTag tag, HolderLookup.Provider provider) {
            MapVariables data = new MapVariables();
            data.read(tag);
            return data;
        }

        public void read(CompoundTag nbt) {
            AcceptedBlockIDs = nbt.getString("AcceptedBlockIDs");
            AcceptedBlockStrings = nbt.getString("AcceptedBlockStrings");
            AcceptedBlockTags = nbt.getString("AcceptedBlockTags");
            AcceptedDimensions = nbt.getString("AcceptedDimensions");
            AcceptedEntities = nbt.getString("AcceptedEntities");
            InfrastructureCheckQuality = nbt.getDouble("InfrastructureCheckQuality");
            InfrastructureCheckRadius = nbt.getDouble("InfrastructureCheckRadius");
            NodeDistanceMaximum = nbt.getDouble("NodeDistanceMaximum");
            NodeDistanceMinimum = nbt.getDouble("NodeDistanceMinimum");
            PathDistanceMaximum = nbt.getDouble("PathDistanceMaximum");
            PathDistanceMinimum = nbt.getDouble("PathDistanceMinimum");
            TravelFatigueCooldown = nbt.getDouble("TravelFatigueCooldown");

            ValidBlockList = nbt.contains("ValidBlockList", Tag.TAG_LIST) ? loadArrayList(nbt.getList("ValidBlockList", Tag.TAG_COMPOUND)) : new ArrayList<>();
            ValidStringList = nbt.contains("ValidStringList", Tag.TAG_LIST) ? loadArrayList(nbt.getList("ValidStringList", Tag.TAG_COMPOUND)) : new ArrayList<>();
            ValidEntityList = nbt.contains("ValidEntityList", Tag.TAG_LIST) ? loadArrayList(nbt.getList("ValidEntityList", Tag.TAG_COMPOUND)) : new ArrayList<>();
            ValidDimensionList = nbt.contains("ValidDimensionList", Tag.TAG_LIST) ? loadArrayList(nbt.getList("ValidDimensionList", Tag.TAG_COMPOUND)) : new ArrayList<>();
            ValidSignList = nbt.contains("ValidSignList", Tag.TAG_LIST) ? loadArrayList(nbt.getList("ValidSignList", Tag.TAG_COMPOUND)) : new ArrayList<>();
        }

        @Override
        public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
            nbt.putString("AcceptedBlockIDs", AcceptedBlockIDs);
            nbt.putString("AcceptedBlockStrings", AcceptedBlockStrings);
            nbt.putString("AcceptedBlockTags", AcceptedBlockTags);
            nbt.putString("AcceptedDimensions", AcceptedDimensions);
            nbt.putString("AcceptedEntities", AcceptedEntities);
            nbt.putDouble("InfrastructureCheckQuality", InfrastructureCheckQuality);
            nbt.putDouble("InfrastructureCheckRadius", InfrastructureCheckRadius);
            nbt.putDouble("NodeDistanceMaximum", NodeDistanceMaximum);
            nbt.putDouble("NodeDistanceMinimum", NodeDistanceMinimum);
            nbt.putDouble("PathDistanceMaximum", PathDistanceMaximum);
            nbt.putDouble("PathDistanceMinimum", PathDistanceMinimum);
            nbt.putDouble("TravelFatigueCooldown", TravelFatigueCooldown);

            nbt.put("ValidBlockList", saveArrayList(ValidBlockList));
            nbt.put("ValidStringList", saveArrayList(ValidStringList));
            nbt.put("ValidEntityList", saveArrayList(ValidEntityList));
            nbt.put("ValidDimensionList", saveArrayList(ValidDimensionList));
            nbt.put("ValidSignList", saveArrayList(ValidSignList));
            return nbt;
        }

        @Override
        public void setDirty() {
            super.setDirty();
            this.dirty = true;
        }

        public void markClean() {
            this.dirty = false;
        }

        @Override
        public boolean isDirty() {
            return this.dirty;
        }

        public static MapVariables get(LevelAccessor world) {
            if (world == null) {
                PlatformUtils.getLogger().warn("Attempted to get MapVariables with null world, returning clientSide instance.");
                return clientSide;
            }
            if (world instanceof ServerLevel level) {
                MinecraftServer server = level.getServer();
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                if (overworld == null) {
                    PlatformUtils.getLogger().error("Could not get Overworld from server to load MapVariables!");
                    return clientSide;
                }
                return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(MapVariables::new, MapVariables::load, DataFixTypes.LEVEL), DATA_NAME);
            } else {
                return clientSide;
            }
        }

        public void syncToPlayer(ServerPlayer player) {
            if (networkHandler != null) {
                networkHandler.sendToPlayer(player, new SavedDataSyncMessage(0, this, player));
            } else {
                PlatformUtils.getLogger().warn("Network handler not initialized, cannot sync MapVariables.");
            }
            markClean();
        }
    }

    public static class SavedDataSyncMessage {
        public final int type;
        public final CompoundTag dataTag;

        public SavedDataSyncMessage(int type, SavedData data, ServerPlayer player) {
            this.type = type;
            this.dataTag = data.save(new CompoundTag(), player.server.registryAccess());
        }

        public SavedDataSyncMessage(FriendlyByteBuf buffer) {
            this.type = buffer.readInt();
            this.dataTag = buffer.readNbt();
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeInt(type);
            buffer.writeNbt(dataTag);
        }

        public static void handleClient(SavedDataSyncMessage message) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                PlatformUtils.getLogger().warn("Received SavedDataSyncMessage on client with null level.");
                return;
            }

            if (message.type == 0) {
                MapVariables.clientSide.read(message.dataTag);
                PlatformUtils.getLogger().debug("Client received and processed MapVariables sync.");
            } else if (message.type == 1) {
                WorldVariables.clientSide.read(message.dataTag);
                PlatformUtils.getLogger().debug("Client received and processed WorldVariables sync.");
            }
        }
    }

    public static class PlayerVariables {
        public boolean ChartingPath = false;
        public double FadeAmount = 0.0;
        public boolean FadeIncrease = false;
        public String lastMessage = "";
        public double LastNodeX = 0.0;
        public double LastNodeY = 0;
        public double LastNodeZ = 0.0;
        public String LastSignPosition = "";
        public double LeftClickTimer = 0.0;
        public double MessageCooldown = 0.0;
        public String PathData = "";
        public boolean ReceivedTutorial = false;
        public double TitleCooldown = 0.0;
        public String TitleString = "";
        public double ToastTimer = 0.0;
        public boolean AwaitingToast = false;

        public CompoundTag writeNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("ChartingPath", ChartingPath);
            nbt.putDouble("FadeAmount", FadeAmount);
            nbt.putBoolean("FadeIncrease", FadeIncrease);
            nbt.putString("lastMessage", lastMessage);
            nbt.putDouble("LastNodeX", LastNodeX);
            nbt.putDouble("LastNodeY", LastNodeY);
            nbt.putDouble("LastNodeZ", LastNodeZ);
            nbt.putString("LastSignPosition", LastSignPosition);
            nbt.putDouble("LeftClickTimer", LeftClickTimer);
            nbt.putDouble("MessageCooldown", MessageCooldown);
            nbt.putString("PathData", PathData);
            nbt.putBoolean("ReceivedTutorial", ReceivedTutorial);
            nbt.putDouble("TitleCooldown", TitleCooldown);
            nbt.putString("TitleString", TitleString);
            nbt.putDouble("ToastTimer", ToastTimer);
            nbt.putBoolean("AwaitingToast", AwaitingToast);
            return nbt;
        }

        public void readNBT(CompoundTag nbt) {
            ChartingPath = nbt.getBoolean("ChartingPath");
            FadeAmount = nbt.getDouble("FadeAmount");
            FadeIncrease = nbt.getBoolean("FadeIncrease");
            lastMessage = nbt.getString("lastMessage");
            LastNodeX = nbt.getDouble("LastNodeX");
            LastNodeY = nbt.getDouble("LastNodeY");
            LastNodeZ = nbt.getDouble("LastNodeZ");
            LastSignPosition = nbt.getString("LastSignPosition");
            LeftClickTimer = nbt.getDouble("LeftClickTimer");
            MessageCooldown = nbt.getDouble("MessageCooldown");
            PathData = nbt.getString("PathData");
            ReceivedTutorial = nbt.getBoolean("ReceivedTutorial");
            TitleCooldown = nbt.getDouble("TitleCooldown");
            TitleString = nbt.getString("TitleString");
            ToastTimer = nbt.getDouble("ToastTimer");
            AwaitingToast = nbt.getBoolean("AwaitingToast");
        }

        public void syncToClient(ServerPlayer player) {
            if (player == null) return;
            if (networkHandler != null) {
                networkHandler.sendToPlayer(player, new PlayerVariablesSyncMessage(this));
            } else {
                PlatformUtils.getLogger().warn("Network handler not initialized, cannot sync PlayerVariables for {}.", player.getName().getString());
            }
        }
    }

    private static final Map<UUID, PlayerVariables> playerVariables = new ConcurrentHashMap<>();
    private static PlayerVariables clientPlayerVariables = new PlayerVariables();

    public static PlayerVariables getPlayerVariables(Entity entity) {
        if (entity == null) {
            PlatformUtils.getLogger().warn("Attempted to get PlayerVariables for null entity.");
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
                    PlatformUtils.getLogger().debug("Loaded PlayerVariables from file for {}", player.getName().getString());
                } else {
                    PlatformUtils.getLogger().debug("No existing PlayerVariables file found for {}, creating new.", player.getName().getString());
                }
                return vars;
            });
        }

        PlatformUtils.getLogger().warn("Tried to get PlayerVariables for non-ServerPlayer entity: {}", entity.getType().getDescriptionId());
        return new PlayerVariables();
    }

    public static void playerLoggedOut(ServerPlayer player) {
        if (player != null) {
            UUID playerId = player.getUUID();
            PlayerVariables vars = playerVariables.get(playerId);
            if (vars != null) {
                savePlayerVariablesToFile(player.getServer(), playerId.toString(), vars);
                playerVariables.remove(playerId);
                PlatformUtils.getLogger().debug("Saved and unloaded PlayerVariables for {}", player.getName().getString());
            }
        }
    }

    public static PlayerVariables playerLoggedIn(ServerPlayer player) {
        if (player == null) return new PlayerVariables();
        PlayerVariables vars = getPlayerVariables(player);
        vars.syncToClient(player);
        MapVariables.get(player.level()).syncToPlayer(player);
        WorldVariables.get(player.level()).syncToPlayer(player);
        return vars;
    }

    public static void playerRespawned(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean keepAllPlayerData) {
        if (oldPlayer == null || newPlayer == null) return;

        PlayerVariables oldVars = playerVariables.get(oldPlayer.getUUID());
        PlayerVariables newVars = getPlayerVariables(newPlayer);

        if (oldVars != null) {
            if (keepAllPlayerData) {
                newVars.readNBT(oldVars.writeNBT());
                PlatformUtils.getLogger().debug("Copied all PlayerVariables on respawn for {}", newPlayer.getName().getString());
            } else {
                PlatformUtils.getLogger().debug("Did not copy PlayerVariables on respawn (keepAllPlayerData=false) for {}", newPlayer.getName().getString());
            }
            newVars.syncToClient(newPlayer);
        } else {
            PlatformUtils.getLogger().warn("Could not find old PlayerVariables for {} during respawn.", oldPlayer.getName().getString());
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
                PlatformUtils.getLogger().debug("Force-loaded PlayerVariables from file for {}", player.getName().getString());
                playerVar.syncToClient(player);
            }
        }
    }

    private static File getPlayerDataDirectory(MinecraftServer server) {
        return new File(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), PlatformUtils.getModId());
    }

    private static void saveNBTToFile(MinecraftServer server, String fileName, CompoundTag nbt) {
        if (server == null) return;
        try {
            File dataDir = getPlayerDataDirectory(server);
            if (!dataDir.exists()) {
                if (!dataDir.mkdirs()) {
                    PlatformUtils.getLogger().error("Could not create player data directory: {}", dataDir.getAbsolutePath());
                    return;
                } else {
                    PlatformUtils.getLogger().debug("Created player data directory: {}", dataDir.getAbsolutePath());
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
            PlatformUtils.getLogger().error("Failed to save player variables NBT for " + fileName, e);
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
                PlatformUtils.getLogger().error("Failed to load player variables NBT from " + file.getName() + ", trying backup.", e);
                if (backupFile.exists()) {
                    try {
                        PlatformUtils.getLogger().warn("Attempting to load from backup file: {}", backupFile.getName());
                        return NbtIo.readCompressed(backupFile.toPath(), NbtAccounter.unlimitedHeap());
                    } catch (IOException e2) {
                        PlatformUtils.getLogger().error("Failed to load player variables NBT from backup " + backupFile.getName() + " as well.", e2);
                    }
                }
            }
        } else if (backupFile.exists()) {
            try {
                PlatformUtils.getLogger().warn("Main file {} missing, attempting to load from backup file: {}", file.getName(), backupFile.getName());
                return NbtIo.readCompressed(backupFile.toPath(), NbtAccounter.unlimitedHeap());
            } catch (IOException e) {
                PlatformUtils.getLogger().error("Failed to load player variables NBT from backup " + backupFile.getName(), e);
            }
        }
        return null;
    }

    public static class PlayerVariablesSyncMessage {
        public final CompoundTag dataTag;

        public PlayerVariablesSyncMessage(PlayerVariables data) {
            this.dataTag = data.writeNBT();
        }

        public PlayerVariablesSyncMessage(FriendlyByteBuf buffer) {
            this.dataTag = buffer.readNbt();
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeNbt(dataTag);
        }

        public static void handleClient(PlayerVariablesSyncMessage message) {
            clientPlayerVariables.readNBT(message.dataTag);
            PlatformUtils.getLogger().debug("Client received and processed PlayerVariables sync.");
        }

        public static void handleServer(PlayerVariablesSyncMessage message, ServerPlayer player) {
            if (player != null) {
                PlayerVariables variables = getPlayerVariables(player);
                variables.readNBT(message.dataTag);
                PlatformUtils.getLogger().debug("Server received and processed PlayerVariables sync from {}", player.getName().getString());
            }
        }
    }
}