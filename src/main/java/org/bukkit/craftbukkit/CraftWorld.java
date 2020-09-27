package org.bukkit.craftbukkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.lang.Validate;
import org.bukkit.BlockChangeDelegate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Difficulty;
import org.bukkit.Effect;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameRule;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Raid;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.StructureType;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.DragonBattle;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.metadata.BlockMetadataStore;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.TippedArrow;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.world.SpawnChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Consumer;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.google.common.base.Preconditions;
import com.javazilla.bukkitfabric.Utils;
import com.javazilla.bukkitfabric.interfaces.IMixinArrowEntity;
import com.javazilla.bukkitfabric.interfaces.IMixinChunkHolder;
import com.javazilla.bukkitfabric.interfaces.IMixinEntity;
import com.javazilla.bukkitfabric.interfaces.IMixinServerEntityPlayer;
import com.javazilla.bukkitfabric.interfaces.IMixinThreadedAnvilChunkStorage;
import com.javazilla.bukkitfabric.interfaces.IMixinWorldChunk;
import com.javazilla.bukkitfabric.mixin.world.MixinChunkHolder;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusFlowerBlock;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ReadOnlyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.ConfiguredFeatures;

@SuppressWarnings("deprecation")
public class CraftWorld implements World {

    public static final int CUSTOM_DIMENSION_OFFSET = 10;
    private final BlockMetadataStore blockMetadata = new BlockMetadataStore(this);

    private ServerWorld nms;
    private String name;

    private static final Random rand = new Random();

    public CraftWorld(String name, ServerWorld world) {
        this.nms = world;
        this.name = name;
    }

    public CraftWorld(ServerWorld world) {
        this(((ServerWorldProperties) world.getLevelProperties()).getLevelName(), world);
    }

    @Override
    public Set<String> getListeningPluginChannels() {
        Set<String> result = new HashSet<String>();

        for (Player player : getPlayers())
            result.addAll(player.getListeningPluginChannels());

        return result;
    }

    @Override
    public void sendPluginMessage(Plugin plugin, String channel, byte[] message) {
        for (Player player : getPlayers())
            player.sendPluginMessage(plugin, channel, message);
    }

    @Override
    public List<MetadataValue> getMetadata(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasMetadata(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeMetadata(String arg0, Plugin arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setMetadata(String arg0, MetadataValue arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean addPluginChunkTicket(int arg0, int arg1, Plugin arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canGenerateStructures() {
        // FIXME BROKEN!!!
        return false;//nms.getLevelProperties().hasStructures();
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power) {
        return createExplosion(x, y, z, power, false, true);
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power, boolean setFire) {
        return createExplosion(x, y, z, power, setFire, true);
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power, boolean setFire, boolean breakBlocks) {
        return createExplosion(x, y, z, power, setFire, breakBlocks, null);
    }

    @Override
    public boolean createExplosion(double x, double y, double z, float power, boolean setFire, boolean breakBlocks, Entity source) {
        nms.createExplosion(source == null ? null : ((CraftEntity) source).getHandle(), x, y, z, power, setFire, breakBlocks ? Explosion.DestructionType.BREAK : Explosion.DestructionType.NONE);
        return true; // TODO return wasCanceled
    }

    @Override
    public boolean createExplosion(Location loc, float power) {
        return createExplosion(loc, power, false);
    }

    @Override
    public boolean createExplosion(Location loc, float power, boolean setFire) {
        return createExplosion(loc, power, setFire, true);
    }

    @Override
    public boolean createExplosion(Location loc, float power, boolean setFire, boolean breakBlocks) {
        return createExplosion(loc, power, setFire, breakBlocks, null);
    }

    @Override
    public boolean createExplosion(Location loc, float power, boolean setFire, boolean breakBlocks, Entity source) {
        Preconditions.checkArgument(loc != null, "Location is null");
        Preconditions.checkArgument(this.equals(loc.getWorld()), "Location not in world");

        return createExplosion(loc.getX(), loc.getY(), loc.getZ(), power, setFire, breakBlocks, source);
    }

    @Override
    public Item dropItem(Location loc, ItemStack arg1) {
        ItemEntity entity = new ItemEntity(nms, loc.getX(), loc.getY(), loc.getZ(), CraftItemStack.asNMSCopy(arg1));
        entity.pickupDelay = 10;
        nms.addEntity(entity);
        return (org.bukkit.entity.Item) (((IMixinEntity)entity).getBukkitEntity());
    }

    @Override
    public Item dropItemNaturally(Location loc, ItemStack arg1) {
        double xs = (nms.random.nextFloat() * 0.5F) + 0.25D;
        double ys = (nms.random.nextFloat() * 0.5F) + 0.25D;
        double zs = (nms.random.nextFloat() * 0.5F) + 0.25D;
        loc = loc.clone();
        loc.setX(loc.getX() + xs);
        loc.setY(loc.getY() + ys);
        loc.setZ(loc.getZ() + zs);
        return dropItem(loc, arg1);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean generateTree(Location loc, TreeType type) {
        BlockPos pos = new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        ConfiguredFeature gen;
        switch (type) {
            case BIG_TREE:
                gen = ConfiguredFeatures.FANCY_OAK;
                break;
            case BIRCH:
                gen = ConfiguredFeatures.BIRCH;
                break;
            case REDWOOD:
                gen = ConfiguredFeatures.SPRUCE;
                break;
            case TALL_REDWOOD:
                gen = ConfiguredFeatures.TREES_GIANT_SPRUCE;
                break;
            case JUNGLE:
                gen = ConfiguredFeatures.JUNGLE_TREE;
                break;
            case SMALL_JUNGLE:
                gen = ConfiguredFeatures.JUNGLE_TREE_NO_VINE;
                break;
            case COCOA_TREE:
                gen = ConfiguredFeatures.JUNGLE_TREE;
                break;
            case JUNGLE_BUSH:
                gen = ConfiguredFeatures.JUNGLE_BUSH;
                break;
            case RED_MUSHROOM:
                gen = ConfiguredFeatures.HUGE_RED_MUSHROOM;
                break;
            case BROWN_MUSHROOM:
                gen = ConfiguredFeatures.HUGE_BROWN_MUSHROOM;
                break;
            case SWAMP:
                gen = ConfiguredFeatures.SWAMP_TREE;
                break;
            case ACACIA:
                gen = ConfiguredFeatures.ACACIA;
                break;
            case DARK_OAK:
                gen = ConfiguredFeatures.DARK_OAK;
                break;
            case MEGA_REDWOOD:
                gen = ConfiguredFeatures.MEGA_SPRUCE;
                break;
            case TALL_BIRCH:
                gen = ConfiguredFeatures.BIRCH_TALL;
                break;
            case CHORUS_PLANT:
                ChorusFlowerBlock.generate(nms, pos, rand, 8);
                return true;
            case CRIMSON_FUNGUS:
                gen = ConfiguredFeatures.CRIMSON_FUNGI;
                break;
            case WARPED_FUNGUS:
                gen = ConfiguredFeatures.WARPED_FUNGI;
                break;
            case TREE:
            default:
                gen = ConfiguredFeatures.OAK;
                break;
        }

        return gen.feature.generate(nms, nms.getChunkManager().getChunkGenerator(), rand, pos, gen.config);
    }

    @Override
    public boolean generateTree(Location arg0, TreeType arg1, BlockChangeDelegate arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getAllowAnimals() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean getAllowMonsters() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public int getAmbientSpawnLimit() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getAnimalSpawnLimit() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Biome getBiome(int arg0, int arg1) {
        return getBiome(arg0, 0, arg1);
    }

    @Override
    public Biome getBiome(int arg0, int arg1, int arg2) {
        return CraftBlock.biomeBaseToBiome(getHandle().getRegistryManager().get(Registry.BIOME_KEY), nms.getBiomeForNoiseGen(arg0 >> 2, arg1 >> 2, arg2 >> 2));
    }

    @Override
    public Block getBlockAt(Location loc) {
        return getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public Block getBlockAt(int x, int y, int z) {
        return CraftBlock.at(nms, new BlockPos(x, y, z));
    }

    @Override
    public Chunk getChunkAt(Location arg0) {
        return getChunkAt(arg0.getBlockX() >> 4, arg0.getBlockZ() >> 4);
    }

    @Override
    public Chunk getChunkAt(Block arg0) {
        return getChunkAt(arg0.getX() >> 4, arg0.getZ() >> 4);
    }

    @Override
    public Chunk getChunkAt(int x, int z) {
        return ((IMixinWorldChunk)nms.getChunkManager().getWorldChunk(x, z, true)).getBukkitChunk();
    }

    @Override
    public Difficulty getDifficulty() {
        return Utils.fromFabric(nms.getDifficulty());
    }

    @Override
    public ChunkSnapshot getEmptyChunkSnapshot(int arg0, int arg1, boolean arg2, boolean arg3) {
        return CraftChunk.getEmptyChunkSnapshot(arg0, arg1, this, arg2, arg3);
    }

    @Override
    public DragonBattle getEnderDragonBattle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Entity> getEntities() {
        List<Entity> list = new ArrayList<Entity>();

        for (Object object : nms.entitiesById.values()) {
            if (object instanceof net.minecraft.entity.Entity) {
                net.minecraft.entity.Entity mc = (net.minecraft.entity.Entity) object;
                Entity bukkit = ((IMixinEntity)mc).getBukkitEntity();

                // Assuming that bukkitEntity isn't null
                if (bukkit != null && bukkit.isValid())
                    list.add(bukkit);
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Entity> Collection<T> getEntitiesByClass(Class<T>... arg0) {
        return (Collection<T>) getEntitiesByClasses(arg0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Entity> Collection<T> getEntitiesByClass(Class<T> arg0) {
        Collection<T> list = new ArrayList<T>();

        for (Object entity: nms.entitiesById.values()) {
            if (entity instanceof net.minecraft.entity.Entity) {
                Entity bukkitEntity = ((IMixinEntity)(net.minecraft.entity.Entity) entity).getBukkitEntity();

                if (bukkitEntity == null)
                    continue;

                Class<?> bukkitClass = bukkitEntity.getClass();

                if (arg0.isAssignableFrom(bukkitClass) && bukkitEntity.isValid())
                    list.add((T) bukkitEntity);
            }
        }

        return list;
    }

    @Override
    public Collection<Entity> getEntitiesByClasses(Class<?>... arg0) {
        Collection<Entity> list = new ArrayList<Entity>();

        for (Object entity: nms.entitiesById.values()) {
            if (entity instanceof net.minecraft.entity.Entity) {
                Entity bukkitEntity = ((IMixinEntity)(net.minecraft.entity.Entity) entity).getBukkitEntity();

                if (bukkitEntity == null)
                    continue;

                Class<?> bukkitClass = bukkitEntity.getClass();

                for (Class<?> clazz : arg0) {
                    if (clazz.isAssignableFrom(bukkitClass)) {
                        if (bukkitEntity.isValid())
                            list.add(bukkitEntity);
                        break;
                    }
                }
            }
        }

        return list;
    }

    @Override
    public Environment getEnvironment() {
        // TODO Auto-generated method stub
        return Environment.NORMAL;
    }

    @Override
    public Collection<Chunk> getForceLoadedChunks() {
        Set<Chunk> chunks = new HashSet<>();

        for (long coord : nms.getForcedChunks())
            chunks.add(getChunkAt(ChunkPos.getPackedX(coord), ChunkPos.getPackedZ(coord)));

        return Collections.unmodifiableCollection(chunks);
    }

    @Override
    public long getFullTime() {
        return nms.getTimeOfDay();
    }

    @Override
    public <T> T getGameRuleDefault(GameRule<T> arg0) {
        return convert(arg0, getGameRuleDefinitions().get(arg0.getName()).createRule());
    }

    @Override
    public String getGameRuleValue(String arg0) {
        // In method contract for some reason
        if (arg0 == null)
            return null;

        GameRules.Rule<?> value = getHandle().getGameRules().get(getGameRulesNMS().get(arg0));
        return value != null ? value.toString() : "";
    }

    @Override
    public <T> T getGameRuleValue(GameRule<T> arg0) {
        return convert(arg0, getHandle().getGameRules().get(getGameRulesNMS().get(arg0.getName())));
    }

    private static Map<String, GameRules.Key<?>> gamerules;
    public static synchronized Map<String, GameRules.Key<?>> getGameRulesNMS() {
        if (gamerules != null) {
            return gamerules;
        }

        Map<String, GameRules.Key<?>> gamerules = new HashMap<>();
        GameRules.accept(new GameRules.Visitor() {
            @Override
            public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> gamerules_gamerulekey, GameRules.Type<T> gamerules_gameruledefinition) {
                gamerules.put(gamerules_gamerulekey.getName(), gamerules_gamerulekey);
            }
        });

        return CraftWorld.gamerules = gamerules;
    }

    private <T> T convert(GameRule<T> rule, GameRules.Rule<?> value) {
        if (value == null)
            return null;

        if (value instanceof GameRules.BooleanRule) {
            return rule.getType().cast(((GameRules.BooleanRule) value).get());
        } else if (value instanceof GameRules.IntRule) {
            return rule.getType().cast(value.getCommandResult());
        } else throw new IllegalArgumentException("Invalid GameRule type (" + value + ") for GameRule " + rule.getName());
    }

    private static Map<String, GameRules.Type<?>> gameruleDefinitions;
    public static synchronized Map<String, GameRules.Type<?>> getGameRuleDefinitions() {
        if (gameruleDefinitions != null)
            return gameruleDefinitions;

        Map<String, GameRules.Type<?>> gameruleDefinitions = new HashMap<>();
        GameRules.accept(new GameRules.Visitor() {
            @Override
            public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> gamerules_gamerulekey, GameRules.Type<T> gamerules_gameruledefinition) {
                gameruleDefinitions.put(gamerules_gamerulekey.getName(), gamerules_gameruledefinition);
            }
        });

        return CraftWorld.gameruleDefinitions = gameruleDefinitions;
    }

    @Override
    public String[] getGameRules() {
        return getGameRulesNMS().keySet().toArray(new String[getGameRulesNMS().size()]);
    }

    @Override
    public ChunkGenerator getGenerator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Block getHighestBlockAt(Location arg0) {
        return getHighestBlockAt(arg0.getBlockX(), arg0.getBlockY());
    }

    @Override
    public Block getHighestBlockAt(int x, int z) {
        return getBlockAt(x, getHighestBlockYAt(x, z), z);
    }

    @Override
    public Block getHighestBlockAt(Location arg0, HeightMap arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Block getHighestBlockAt(int arg0, int arg1, HeightMap arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getHighestBlockYAt(Location arg0) {
        return getHighestBlockYAt(arg0.getBlockX(), arg0.getBlockZ());
    }

    @Override
    public int getHighestBlockYAt(int arg0, int arg1) {
        return getHighestBlockYAt(arg0, arg1, HeightMap.MOTION_BLOCKING);
    }

    @Override
    public int getHighestBlockYAt(Location arg0, HeightMap arg1) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getHighestBlockYAt(int arg0, int arg1, HeightMap arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getHumidity(int x, int z) {
        return getHumidity(x, 0, z);
    }

    @Override
    public double getHumidity(int x, int y, int z) {
        return nms.getBiomeForNoiseGen(x >> 2, y >> 2, z >> 2).getDownfall();
    }

    @Override
    public boolean getKeepSpawnInMemory() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<LivingEntity> getLivingEntities() {
        List<LivingEntity> list = new ArrayList<LivingEntity>();

        for (Object o : nms.entitiesById.values()) {
            if (o instanceof net.minecraft.entity.Entity) {
                net.minecraft.entity.Entity mcEnt = (net.minecraft.entity.Entity) o;
                Entity bukkitEntity = ((IMixinEntity)mcEnt).getBukkitEntity();

                // Assuming that bukkitEntity isn't null
                if (bukkitEntity != null && bukkitEntity instanceof LivingEntity && bukkitEntity.isValid())
                    list.add((LivingEntity) bukkitEntity);
            }
        }

        return list;
    }

    @SuppressWarnings("resource")
    @Override
    public Chunk[] getLoadedChunks() {
        Long2ObjectLinkedOpenHashMap<ChunkHolder> chunks = ((IMixinThreadedAnvilChunkStorage)(nms.getChunkManager().threadedAnvilChunkStorage)).getChunkHoldersBF();
        return chunks.values().stream().map(IMixinChunkHolder::getFullChunk).filter(Objects::nonNull).map(CraftWorld::getBukkitChunkForChunk).toArray(Chunk[]::new);
    }

    private static Chunk getBukkitChunkForChunk(WorldChunk mc) {
        return ((IMixinWorldChunk)mc).getBukkitChunk();
    }

    @Override
    public int getMaxHeight() {
        return nms.getHeight();
    }

    @Override
    public int getMonsterSpawnLimit() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<Entity> getNearbyEntities(Location location, double x, double y, double z) {
        return this.getNearbyEntities(location, x, y, z, null);
    }

    @Override
    public Collection<Entity> getNearbyEntities(Location location, double x, double y, double z, Predicate<Entity> filter) {
        BoundingBox aabb = BoundingBox.of(location, x, y, z);
        return this.getNearbyEntities(aabb, filter);
    }

    @Override
    public Collection<Entity> getNearbyEntities(BoundingBox boundingBox) {
        return this.getNearbyEntities(boundingBox, null);
    }

    @Override
    public Collection<Entity> getNearbyEntities(BoundingBox boundingBox, Predicate<Entity> filter) {
        Box bb = new Box(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(), boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
        List<net.minecraft.entity.Entity> entityList = nms.getOtherEntities((net.minecraft.entity.Entity) null, bb, null);
        List<Entity> bukkitEntityList = new ArrayList<org.bukkit.entity.Entity>(entityList.size());

        for (net.minecraft.entity.Entity entity : entityList) {
            Entity bukkitEntity = ((IMixinEntity)entity).getBukkitEntity();
            if (filter == null || filter.test(bukkitEntity))
                bukkitEntityList.add(bukkitEntity);
        }

        return bukkitEntityList;
    }

    @Override
    public boolean getPVP() {
        return nms.getServer().isPvpEnabled();
    }

    @Override
    public List<Player> getPlayers() {
        List<Player> list = new ArrayList<Player>(nms.getPlayers().size());

        for (PlayerEntity human : nms.getPlayers())
            if (human instanceof ServerPlayerEntity)
                list.add((Player) ((IMixinServerEntityPlayer)human).getBukkitEntity());

        return list;
    }

    @Override
    public Map<Plugin, Collection<Chunk>> getPluginChunkTickets() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Plugin> getPluginChunkTickets(int arg0, int arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<BlockPopulator> getPopulators() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Raid> getRaids() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getSeaLevel() {
        return nms.getSeaLevel();
    }

    @Override
    public long getSeed() {
        return nms.getSeed();
    }

    @Override
    public Location getSpawnLocation() {
        BlockPos pos = nms.getSpawnPos();
        return new Location(this, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public double getTemperature(int x, int z) {
        return getTemperature(x, 0, z);
    }

    @Override
    public double getTemperature(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return nms.getBiomeForNoiseGen(x >> 2, y >> 2, z >> 2).getTemperature(pos);
    }

    @Override
    public int getThunderDuration() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTicksPerAmbientSpawns() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTicksPerAnimalSpawns() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTicksPerMonsterSpawns() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTicksPerWaterSpawns() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTime() {
        return nms.getTime();
    }

    @Override
    public UUID getUID() {
        return Utils.getWorldUUID(getWorldFolder());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        final CraftWorld other = (CraftWorld) obj;

        return this.getName().equals(other.getName());
    }

    @Override
    public int getWaterAnimalSpawnLimit() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getWeatherDuration() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WorldBorder getWorldBorder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getWorldFolder() {
        // FIXME BROKEN
        return new File(getName());//((ServerWorld)nms).getSaveHandler().getWorldDir();
    }

    @Override
    public WorldType getWorldType() {
        return nms.isFlat() ? WorldType.FLAT : WorldType.NORMAL;
    }

    @Override
    public boolean hasStorm() {
        return nms.getLevelProperties().isRaining();
    }

    @Override
    public boolean isAutoSave() {
        return !nms.isSavingDisabled();
    }

    @Override
    public boolean isChunkForceLoaded(int arg0, int arg1) {
        return nms.getForcedChunks().contains(ChunkPos.toLong(arg0, arg1));
    }

    @Override
    public boolean isChunkGenerated(int x, int z) {
        try {
            return isChunkLoaded(x, z) || nms.getChunkManager().threadedAnvilChunkStorage.getNbt(new ChunkPos(x, z)) != null;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean isChunkInUse(int arg0, int arg1) {
        return isChunkLoaded(arg0, arg1);
    }

    @Override
    public boolean isChunkLoaded(Chunk arg0) {
        return isChunkLoaded(arg0.getX(), arg0.getZ());
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return (null != nms.getChunkManager().getWorldChunk(x, z, false));
    }

    @Override
    public boolean isGameRule(String arg0) {
        return getGameRulesNMS().containsKey(arg0);
    }

    @Override
    public boolean isHardcore() {
        return nms.getLevelProperties().isHardcore();
    }

    @Override
    public boolean isThundering() {
        return nms.getLevelProperties().isThundering();
    }

    @Override
    public void loadChunk(Chunk arg0) {
        loadChunk(arg0.getX(), arg0.getZ());
    }

    @Override
    public void loadChunk(int arg0, int arg1) {
        loadChunk(arg0, arg1, true);
    }

    @Override
    public boolean loadChunk(int x, int z, boolean generate) {
        net.minecraft.world.chunk.Chunk chunk = nms.getChunkManager().getChunk(x, z, generate ? ChunkStatus.FULL : ChunkStatus.EMPTY, true);

        if (chunk instanceof ReadOnlyChunk)
            chunk = nms.getChunkManager().getChunk(x, z, ChunkStatus.FULL, true);

        if (chunk instanceof net.minecraft.world.chunk.WorldChunk) {
            nms.getChunkManager().addTicket(ChunkTicketType.START, new ChunkPos(x, z), 1, Unit.INSTANCE);
            return true;
        }

        return false;
    }

    @Override
    public Raid locateNearestRaid(Location arg0, int arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Location locateNearestStructure(Location arg0, StructureType arg1, int arg2, boolean arg3) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void playEffect(Location arg0, Effect arg1, int arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public <T> void playEffect(Location arg0, Effect arg1, T arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void playEffect(Location arg0, Effect arg1, int arg2, int arg3) {
        // TODO Auto-generated method stub
    }

    @Override
    public <T> void playEffect(Location arg0, Effect arg1, T arg2, int arg3) {
        // TODO Auto-generated method stub
    }

    @Override
    public void playSound(Location loc, Sound sound, float volume, float pitch) {
        playSound(loc, sound, org.bukkit.SoundCategory.MASTER, volume, pitch);
    }

    @Override
    public void playSound(Location loc, String sound, float volume, float pitch) {
        playSound(loc, sound, org.bukkit.SoundCategory.MASTER, volume, pitch);
    }

    @Override
    public void playSound(Location loc, Sound sound, org.bukkit.SoundCategory category, float volume, float pitch) {
        if (loc == null || sound == null || category == null) return;

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        getHandle().playSound(null, x, y, z, CraftSound.getSoundEffect(CraftSound.getSound(sound)), net.minecraft.sound.SoundCategory.valueOf(category.name()), volume, pitch);
    }

    @Override
    public void playSound(Location loc, String sound, org.bukkit.SoundCategory category, float volume, float pitch) {
        if (loc == null || sound == null || category == null) return;

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        PlaySoundIdS2CPacket packet = new PlaySoundIdS2CPacket(new Identifier(sound), net.minecraft.sound.SoundCategory.valueOf(category.name()), new Vec3d(x, y, z), volume, pitch);
        nms.getServer().getPlayerManager().sendToAround(null, x, y, z, volume > 1.0F ? 16.0F * volume : 16.0D, nms.getRegistryKey(), packet);
    }

    @Override
    public RayTraceResult rayTrace(Location arg0, Vector arg1, double arg2, FluidCollisionMode arg3, boolean arg4, double arg5, Predicate<Entity> arg6) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RayTraceResult rayTraceBlocks(Location arg0, Vector arg1, double arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RayTraceResult rayTraceBlocks(Location arg0, Vector arg1, double arg2, FluidCollisionMode arg3) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RayTraceResult rayTraceBlocks(Location arg0, Vector arg1, double arg2, FluidCollisionMode arg3, boolean arg4) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RayTraceResult rayTraceEntities(Location arg0, Vector arg1, double arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RayTraceResult rayTraceEntities(Location arg0, Vector arg1, double arg2, double arg3) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RayTraceResult rayTraceEntities(Location arg0, Vector arg1, double arg2, Predicate<Entity> arg3) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RayTraceResult rayTraceEntities(Location arg0, Vector arg1, double arg2, double arg3, Predicate<Entity> arg4) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean refreshChunk(int arg0, int arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean regenerateChunk(int arg0, int arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removePluginChunkTicket(int arg0, int arg1, Plugin arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removePluginChunkTickets(Plugin arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void save() {
        boolean oldSave = nms.savingDisabled;
        nms.savingDisabled = false;
        nms.save(null, false, false);
        nms.savingDisabled = oldSave;
    }

    @Override
    public void setAmbientSpawnLimit(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setAnimalSpawnLimit(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setAutoSave(boolean arg0) {
        nms.savingDisabled = !arg0;
    }

    @Override
    public void setBiome(int arg0, int arg1, Biome arg2) {
        for (int y = 0; y < getMaxHeight(); y++)
            setBiome(arg0, y, arg1, arg2);
    }

    @Override
    public void setBiome(int x, int y, int z, Biome bio) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setChunkForceLoaded(int arg0, int arg1, boolean arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setDifficulty(Difficulty diff) {
        // FIXME BROKEN
        //nms.getLevelProperties().setDifficulty(net.minecraft.world.Difficulty.byOrdinal(diff.ordinal()));
    }

    @Override
    public void setFullTime(long time) {
        TimeSkipEvent event = new TimeSkipEvent(this, TimeSkipEvent.SkipReason.CUSTOM, time - nms.getTimeOfDay());
        CraftServer.INSTANCE.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        nms.setTimeOfDay(nms.getTimeOfDay() + event.getSkipAmount());

        for (Player p : getPlayers()) {
            CraftPlayer cp = (CraftPlayer) p;
            if (cp.getHandle().networkHandler == null) continue;

            cp.getHandle().networkHandler.sendPacket(new WorldTimeUpdateS2CPacket(cp.getHandle().world.getTime(), cp.getHandle().getServerWorld().getTime(), cp.getHandle().world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)));
        }
    }

    @Override
    public <T> boolean setGameRule(GameRule<T> arg0, T arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setGameRuleValue(String arg0, String arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setHardcore(boolean arg0) {
        // FIXME BROKEN!!
        //nms.getLevelProperties().setHardcore(arg0);
    }

    @Override
    public void setKeepSpawnInMemory(boolean arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setMonsterSpawnLimit(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setPVP(boolean arg0) {
        nms.getServer().setPvpEnabled(arg0);
    }

    @Override
    public void setSpawnFlags(boolean arg0, boolean arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean setSpawnLocation(Location location) {
        return equals(location.getWorld()) ? setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ()) : false;
    }

    @Override
    public boolean setSpawnLocation(int x, int y, int z) {
        try {
            Location previousLocation = getSpawnLocation();
            nms.setSpawnPos(new BlockPos(x, y, z), 0);

            // Notify anyone who's listening.
            SpawnChangeEvent event = new SpawnChangeEvent(this, previousLocation);
            Bukkit.getPluginManager().callEvent(event);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setStorm(boolean arg0) {
        nms.getLevelProperties().setRaining(arg0);
    }

    @Override
    public void setThunderDuration(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setThundering(boolean arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setTicksPerAmbientSpawns(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setTicksPerAnimalSpawns(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setTicksPerMonsterSpawns(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setTicksPerWaterSpawns(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setTime(long arg0) {
        nms.setTimeOfDay(arg0);
    }

    @Override
    public void setWaterAnimalSpawnLimit(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setWeatherDuration(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public <T extends Entity> T spawn(Location location, Class<T> clazz) throws IllegalArgumentException {
        return spawn(location, clazz, null, SpawnReason.CUSTOM);
    }

    @Override
    public <T extends Entity> T spawn(Location location, Class<T> clazz, Consumer<T> function) throws IllegalArgumentException {
        return spawn(location, clazz, function, SpawnReason.CUSTOM);
    }

    public <T extends Entity> T spawn(Location location, Class<T> clazz, Consumer<T> function, SpawnReason reason) throws IllegalArgumentException {
        net.minecraft.entity.Entity entity = createEntity(location, clazz);

        return addEntity(entity, reason, function);
    }

    public net.minecraft.entity.Entity createEntity(Location location, Class<? extends Entity> clazz) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    public <T extends Entity> T addEntity(net.minecraft.entity.Entity entity, SpawnReason reason) throws IllegalArgumentException {
        return addEntity(entity, reason, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T addEntity(net.minecraft.entity.Entity entity, SpawnReason reason, Consumer<T> function) throws IllegalArgumentException {
        Preconditions.checkArgument(entity != null, "Cannot spawn null entity");

        if (entity instanceof MobEntity)
            ((MobEntity) entity).initialize(nms, getHandle().getLocalDifficulty(entity.getBlockPos()), net.minecraft.entity.SpawnReason.COMMAND, (EntityData) null, null);

        if (function != null)
            function.accept((T) ((IMixinEntity)entity).getBukkitEntity());

        nms.addEntity(entity); // TODO spawn reason
        return (T) ((IMixinEntity)entity).getBukkitEntity();
    }

    @Override
    public Arrow spawnArrow(Location loc, Vector velocity, float speed, float spread) {
        return spawnArrow(loc, velocity, speed, spread, Arrow.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractArrow> T spawnArrow(Location loc, Vector velocity, float speed, float spread, Class<T> clazz) {
        Validate.notNull(loc, "Cant spawn arrow with a null location");
        Validate.notNull(velocity, "Cant spawn arrow with a null velocity");
        Validate.notNull(clazz, "Cant spawn an arrow with no class");

        PersistentProjectileEntity arrow;
        if (TippedArrow.class.isAssignableFrom(clazz)) {
            arrow = net.minecraft.entity.EntityType.ARROW.create(nms);
            ((IMixinArrowEntity) arrow).setType(CraftPotionUtil.fromBukkit(new PotionData(PotionType.WATER, false, false)));
        } else if (SpectralArrow.class.isAssignableFrom(clazz)) {
            arrow = net.minecraft.entity.EntityType.SPECTRAL_ARROW.create(nms);
        } else if (Trident.class.isAssignableFrom(clazz)) {
            arrow = net.minecraft.entity.EntityType.TRIDENT.create(nms);
        } else arrow = net.minecraft.entity.EntityType.ARROW.create(nms);

        arrow.refreshPositionAndAngles(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        arrow.setVelocity(velocity.getX(), velocity.getY(), velocity.getZ(), speed, spread);
        nms.spawnEntity(arrow);
        return (T) ((IMixinEntity)arrow).getBukkitEntity();
    }

    @Override
    public Entity spawnEntity(Location loc, EntityType entityType) {
        return spawn(loc, entityType.getEntityClass());
    }

    @Override
    public FallingBlock spawnFallingBlock(Location location, MaterialData data) throws IllegalArgumentException {
        Validate.notNull(data, "MaterialData cannot be null");
        return spawnFallingBlock(location, data.getItemType(), data.getData());
    }

    @Override
    public FallingBlock spawnFallingBlock(Location location, BlockData data) throws IllegalArgumentException {
        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(data, "Material cannot be null");

        FallingBlockEntity entity = new FallingBlockEntity(nms, location.getX(), location.getY(), location.getZ(), ((CraftBlockData) data).getState());
        entity.timeFalling = 1;

        nms.addEntity(entity/*, SpawnReason.CUSTOM*/);
        return (FallingBlock) ((IMixinEntity)entity).getBukkitEntity();
    }

    @Override
    public FallingBlock spawnFallingBlock(Location location, org.bukkit.Material material, byte data) throws IllegalArgumentException {
        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(material, "Material cannot be null");
        Validate.isTrue(material.isBlock(), "Material must be a block");

        FallingBlockEntity entity = new FallingBlockEntity(nms, location.getX(), location.getY(), location.getZ(), CraftMagicNumbers.getBlock(material).getDefaultState());
        entity.timeFalling = 1;

        nms.addEntity(entity/*, SpawnReason.CUSTOM*/);
        return (FallingBlock) ((IMixinEntity)entity).getBukkitEntity();
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count);
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count) {
        spawnParticle(particle, x, y, z, count, null);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, T data) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, data);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {
        spawnParticle(particle, x, y, z, count, 0, 0, 0, data);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ);
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, null);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, T data) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, data);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, T data) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, 1, data);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, extra);
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, null);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, extra, data);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data, false);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data, boolean force) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, extra, data, force);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data, boolean force) {
        if (data != null && !particle.getDataType().isInstance(data))
            throw new IllegalArgumentException("data should be " + particle.getDataType() + " got " + data.getClass());
        // TODO Bukkit4Fabric: method
        getHandle().addParticle(
                //null, // Sender
                CraftParticle.toNMS(particle, data), // Particle
                x, y, z, // Position
                (double)count,  // Count
                offsetX, offsetY//, offsetZ // Random offset
                //extra // Speed?
                //force
        );
    }
    @Override
    public LightningStrike strikeLightning(Location arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LightningStrike strikeLightningEffect(Location arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean unloadChunk(Chunk chunk) {
        return unloadChunk(chunk.getX(), chunk.getZ());
    }

    @Override
    public boolean unloadChunk(int x, int z) {
        return unloadChunk(x, z, true);
    }

    @Override
    public boolean unloadChunk(int x, int z, boolean save) {
        return unloadChunk0(x, z, save);
    }

    private boolean unloadChunk0(int x, int z, boolean save) {
        // TODO
        return false;
    }


    @Override
    public boolean unloadChunkRequest(int arg0, int arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    public net.minecraft.world.World getHandle() {
        return nms;
    }

    @Override
    public int getViewDistance() {
        // TODO Auto-generated method stub
        return 8;
    }

    public void setWaterAmbientSpawnLimit(int i) {
        // TODO Auto-generated method stub
    }

    @Override
    public Spigot spigot() {
        return new Spigot() {
            // TODO Auto-generated method stub
        };
    }

    public BlockMetadataStore getBlockMetadata() {
        return blockMetadata;
    }

    @Override
    public long getTicksPerWaterAmbientSpawns() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getWaterAmbientSpawnLimit() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setTicksPerWaterAmbientSpawns(int arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean setSpawnLocation(int x, int y, int z, float angle) {
        try {
            Location previousLocation = getSpawnLocation();
            nms.setSpawnPos(new BlockPos(x, y, z), angle);

            SpawnChangeEvent event = new SpawnChangeEvent(this, previousLocation);
            CraftServer.INSTANCE.getPluginManager().callEvent(event);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

}