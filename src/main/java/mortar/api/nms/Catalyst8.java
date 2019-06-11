package mortar.api.nms;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaBook;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.util.Vector;

import mortar.api.particle.ParticleEffect;
import mortar.api.particle.ParticleEffect.ParticleColor;
import mortar.api.sched.J;
import mortar.api.world.MaterialBlock;
import mortar.bukkit.plugin.Mortar;
import mortar.bukkit.plugin.MortarAPIPlugin;
import mortar.lang.collection.FinalBoolean;
import mortar.lang.collection.GList;
import mortar.lang.collection.GSet;
import mortar.util.reflection.V;
import mortar.util.text.C;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.DataWatcher.WatchableObject;
import net.minecraft.server.v1_8_R3.EntityHuman.EnumChatVisibility;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IScoreboardCriteria.EnumScoreboardHealthDisplay;
import net.minecraft.server.v1_8_R3.MathHelper;
import net.minecraft.server.v1_8_R3.NextTickListEntry;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayInEntityAction;
import net.minecraft.server.v1_8_R3.PacketPlayInFlying;
import net.minecraft.server.v1_8_R3.PacketPlayInSettings;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockAction;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntity.PacketPlayOutRelEntityMove;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutGameStateChange;
import net.minecraft.server.v1_8_R3.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardObjective;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardScore;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardScore.EnumScoreboardAction;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_8_R3.PacketPlayOutSetSlot;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction;
import net.minecraft.server.v1_8_R3.PacketPlayOutUpdateTime;
import net.minecraft.server.v1_8_R3.WorldServer;

public class Catalyst8 extends CatalystPacketListener implements CatalystHost
{
	private Map<Player, PlayerSettings> playerSettings = new HashMap<>();

	@Override
	@SuppressWarnings("deprecation")
	public void setBlock(Location l, MaterialBlock m)
	{
		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();
		net.minecraft.server.v1_8_R3.World w = ((CraftWorld) l.getWorld()).getHandle();
		net.minecraft.server.v1_8_R3.Chunk chunk = w.getChunkAt(x >> 4, z >> 4);
		int combined = m.getMaterial().getId() + (m.getData() << 12);
		IBlockData ibd = net.minecraft.server.v1_8_R3.Block.getByCombinedId(combined);

		if(chunk.getSections()[y >> 4] == null)
		{
			chunk.getSections()[y >> 4] = new net.minecraft.server.v1_8_R3.ChunkSection(y >> 4 << 4, chunk.world.worldProvider.o());
		}

		net.minecraft.server.v1_8_R3.ChunkSection sec = chunk.getSections()[y >> 4];
		sec.setType(x & 15, y & 15, z & 15, ibd);
	}

	@Override
	public MaterialBlock getBlock(Location l)
	{
		return getBlock(l.getWorld(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
	}

	@Override
	public MaterialBlock getBlock(World world, int xx, int yy, int zz)
	{
		int x = xx >> 4;
		int y = yy >> 4;
		int z = zz >> 4;
		FinalBoolean lx = new FinalBoolean(false);
		if(!world.isChunkLoaded(x, z))
		{
			if(Mortar.isMainThread())
			{
				world.loadChunk(x, z);
			}

			else
			{
				int m = J.sr(() -> world.loadChunk(x, z), 20);

				while(!lx.get())
				{
					try
					{
						Thread.sleep(1);
					}

					catch(InterruptedException e)
					{
						e.printStackTrace();
					}
				}

				J.csr(m);
			}
		}

		net.minecraft.server.v1_8_R3.Chunk c = ((CraftChunk) world.getChunkAt(x, z)).getHandle();
		ChunkSection s = c.getSections()[y];

		if(s == null)
		{
			return new MaterialBlock(Material.AIR);
		}

		IBlockData data = s.getType(xx & 15, yy & 15, zz & 15);
		return new MaterialBlock(Block.getId(data.getBlock()), (byte) data.getBlock().toLegacyData(data) << 12);
	}

	@Override
	public void relight(Chunk c)
	{
		((CraftChunk) c).getHandle().initLighting();
	}

	@Override
	public Object packetTime(long full, long day)
	{
		PacketPlayOutUpdateTime t = new PacketPlayOutUpdateTime();
		new V(t).set("a", full);
		new V(t).set("b", day);

		return t;
	}

	@Override
	public void sendAdvancement(Player p, FrameType type, ItemStack is, String text)
	{
		// Not supported
	}

	// START PACKETS
	@Override
	public Object packetChunkUnload(int x, int z)
	{
		return new PacketPlayOutMapChunk(new net.minecraft.server.v1_8_R3.Chunk(((CraftWorld) Bukkit.getWorlds().get(0)).getHandle(), x, z), false, 0);
	}

	@Override
	public Object packetChunkFullSend(Chunk chunk)
	{
		return new PacketPlayOutMapChunk(((CraftChunk) chunk).getHandle(), chunk.getWorld().getEnvironment().equals(Environment.NORMAL), 65535);
	}

	@Override
	public Object packetBlockChange(Location block, int blockId, byte blockData)
	{
		PacketPlayOutBlockChange p = new PacketPlayOutBlockChange();
		new V(p).set("a", toBlockPos(block));
		new V(p).set("b", Block.getByCombinedId(blockId << 4 | (blockData & 15)));

		return p;
	}

	@Override
	public Object packetBlockAction(Location block, int action, int param, int blocktype)
	{
		PacketPlayOutBlockAction p = new PacketPlayOutBlockAction();
		new V(p).set("a", toBlockPos(block));
		new V(p).set("b", action);
		new V(p).set("c", param);
		new V(p).set("d", Block.getById(blocktype));

		return p;
	}

	@Override
	public Object packetAnimation(int eid, int animation)
	{
		PacketPlayOutAnimation p = new PacketPlayOutAnimation();
		new V(p).set("a", eid);
		new V(p).set("b", animation);

		return p;
	}

	@Override
	public Object packetBlockBreakAnimation(int eid, Location location, byte damage)
	{
		return new PacketPlayOutBlockBreakAnimation(eid, toBlockPos(location), damage);
	}

	@Override
	public Object packetGameState(int mode, float value)
	{
		return new PacketPlayOutGameStateChange(mode, value);
	}

	@Override
	public Object packetTitleMessage(String title)
	{
		return new PacketPlayOutTitle(EnumTitleAction.TITLE, IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + title + "\"}"));
	}

	@Override
	public Object packetSubtitleMessage(String subtitle)
	{
		return new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + subtitle + "\"}"));
	}

	@Override
	public Object packetActionBarMessage(String subtitle)
	{
		return new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + subtitle + "\"}"), (byte) 2);
	}

	@Override
	public Object packetResetTitle()
	{
		return new PacketPlayOutTitle(EnumTitleAction.RESET, null);
	}

	@Override
	public Object packetClearTitle()
	{
		return new PacketPlayOutTitle(EnumTitleAction.CLEAR, null);
	}

	@Override
	public Object packetTimes(int in, int stay, int out)
	{
		return new PacketPlayOutTitle(in, stay, out);
	}
	// END PACKETS

	private BlockPosition toBlockPos(Location location)
	{
		return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	@Override
	public String getServerVersion()
	{
		return "1_12_R1";
	}

	@Override
	public String getVersion()
	{
		return "1.12.X";
	}

	@Override
	public void start()
	{
		openListener();
		Bukkit.getPluginManager().registerEvents(this, MortarAPIPlugin.p);
	}

	@Override
	public void stop()
	{
		closeListener();
		HandlerList.unregisterAll(this);
	}

	@Override
	public void onOpened()
	{
		addGlobalIncomingListener(new PacketHandler<Object>()
		{
			@Override
			public Object onPacket(Player player, Object packet)
			{
				if(packet instanceof PacketPlayInSettings)
				{
					PacketPlayInSettings s = (PacketPlayInSettings) packet;
					playerSettings.put(player, new PlayerSettings(new V(s).get("a"), new V(s).get("b"), ChatMode.values()[((EnumChatVisibility) new V(s).get("c")).ordinal()], new V(s).get("d"), new V(s).get("e"), true));
				}

				return packet;
			}
		});
	}

	@Override
	public void sendPacket(Player p, Object o)
	{
		((CraftPlayer) p).getHandle().playerConnection.sendPacket((Packet<?>) o);
	}

	@Override
	public void sendRangedPacket(double radius, Location l, Object o)
	{
		for(Player i : l.getWorld().getPlayers())
		{
			if(canSee(l, i) && l.distanceSquared(i.getLocation()) <= radius * radius)
			{
				sendPacket(i, o);
			}
		}
	}

	@Override
	public void sendGlobalPacket(World w, Object o)
	{
		for(Player i : w.getPlayers())
		{
			sendPacket(i, o);
		}
	}

	@Override
	public void sendUniversalPacket(Object o)
	{
		for(Player i : Bukkit.getOnlinePlayers())
		{
			sendPacket(i, o);
		}
	}

	@Override
	public void sendViewDistancedPacket(Chunk c, Object o)
	{
		for(Player i : getObservers(c))
		{
			sendPacket(i, o);
		}
	}

	@Override
	public boolean canSee(Chunk c, Player p)
	{
		return isWithin(p.getLocation().getChunk(), c, getViewDistance(p));
	}

	@Override
	public boolean canSee(Location l, Player p)
	{
		return canSee(l.getChunk(), p);
	}

	@Override
	public int getViewDistance(Player p)
	{
		try
		{
			return getSettings(p).getViewDistance();
		}

		catch(Throwable e)
		{

		}

		return Bukkit.getServer().getViewDistance();
	}

	public boolean isWithin(Chunk center, Chunk check, int viewDistance)
	{
		return Math.abs(center.getX() - check.getX()) <= viewDistance && Math.abs(center.getZ() - check.getZ()) <= viewDistance;
	}

	@Override
	public List<Player> getObservers(Chunk c)
	{
		List<Player> p = new ArrayList<>();

		for(Player i : c.getWorld().getPlayers())
		{
			if(canSee(c, i))
			{
				p.add(i);
			}
		}

		return p;
	}

	@Override
	public List<Player> getObservers(Location l)
	{
		return getObservers(l.getChunk());
	}

	@EventHandler
	public void on(PlayerQuitEvent e)
	{
		playerSettings.remove(e.getPlayer());
	}

	@Override
	public PlayerSettings getSettings(Player p)
	{
		return playerSettings.get(p);
	}

	@Override
	public ShadowChunk shadowCopy(Chunk at)
	{
		return new ShadowChunk8(at);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<Object> getTickList(World world)
	{
		try
		{
			Field f = WorldServer.class.getDeclaredField("M");
			f.setAccessible(true);
			return (Set<Object>) f.get(((CraftWorld) world).getHandle());
		}

		catch(Throwable ee)
		{

		}

		return new GSet<>();
	}

	@Override
	public org.bukkit.block.Block getBlock(World world, Object tickListEntry)
	{
		BlockPosition pos = ((NextTickListEntry) tickListEntry).a;
		return world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public Set<Object> getTickListFluid(World world)
	{
		return new GSet<>();
	}

	@Override
	public Object packetTabHeaderFooter(String h, String f)
	{
		PacketPlayOutPlayerListHeaderFooter p = new PacketPlayOutPlayerListHeaderFooter();
		new V(p).set("a", IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + h + "\"}"));
		new V(p).set("b", IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + f + "\"}"));

		return p;
	}

	@Override
	public void scroll(Player sender, int previous)
	{
		sendPacket(sender, new PacketPlayOutHeldItemSlot(previous));
	}

	@Override
	public int getAction(Object packetIn)
	{
		return ((PacketPlayInEntityAction) packetIn).b().ordinal();
	}

	@Override
	public Vector getDirection(Object packet)
	{
		float yaw = 0;
		float pitch = 0;

		try
		{
			Field a = PacketPlayInFlying.class.getDeclaredField("yaw");
			Field b = PacketPlayInFlying.class.getDeclaredField("pitch");
			a.setAccessible(true);
			b.setAccessible(true);
			yaw = (float) a.get(packet);
			pitch = (float) b.get(packet);
		}

		catch(Exception e)
		{

		}

		double pitchRadians = Math.toRadians(-pitch);
		double yawRadians = Math.toRadians(-yaw);
		double sinPitch = Math.sin(pitchRadians);
		double cosPitch = Math.cos(pitchRadians);
		double sinYaw = Math.sin(yawRadians);
		double cosYaw = Math.cos(yawRadians);
		Vector v = new Vector(-cosPitch * sinYaw, sinPitch, -cosPitch * cosYaw);
		return new Vector(-v.getX(), v.getY(), -v.getZ());
	}

	@Override
	public void spawnFallingBlock(int eid, UUID id, Location l, Player player, MaterialBlock mb)
	{
		@SuppressWarnings("deprecation")
		int bid = mb.getMaterial().getId() + (mb.getData() << 12);
		PacketPlayOutSpawnEntity m = new PacketPlayOutSpawnEntity();
		new V(m).set("a", eid);
		new V(m).set("b", id);
		new V(m).set("c", l.getX());
		new V(m).set("d", l.getY());
		new V(m).set("e", l.getZ());
		new V(m).set("f", 0);
		new V(m).set("g", 0);
		new V(m).set("h", 0);
		new V(m).set("i", 0);
		new V(m).set("j", 0);
		new V(m).set("k", 70);
		new V(m).set("l", bid);
		sendPacket(player, m);
	}

	@Override
	public void removeEntity(int eid, Player p)
	{
		PacketPlayOutEntityDestroy d = new PacketPlayOutEntityDestroy(eid);
		sendPacket(p, d);
	}

	@Override
	public void moveEntityRelative(int eid, Player p, double x, double y, double z, boolean onGround)
	{
		try
		{
			PacketPlayOutRelEntityMove r = new PacketPlayOutRelEntityMove();
			Field a = PacketPlayOutEntity.class.getDeclaredField("a");
			Field b = PacketPlayOutEntity.class.getDeclaredField("b");
			Field c = PacketPlayOutEntity.class.getDeclaredField("c");
			Field d = PacketPlayOutEntity.class.getDeclaredField("d");
			Field e = PacketPlayOutEntity.class.getDeclaredField("e");
			Field f = PacketPlayOutEntity.class.getDeclaredField("f");
			Field g = PacketPlayOutEntity.class.getDeclaredField("g");
			Field h = PacketPlayOutEntity.class.getDeclaredField("h");
			a.setAccessible(true);
			b.setAccessible(true);
			c.setAccessible(true);
			d.setAccessible(true);
			e.setAccessible(true);
			f.setAccessible(true);
			g.setAccessible(true);
			h.setAccessible(true);
			a.set(r, eid);
			b.set(r, (byte) ((int) (x * 32D)));
			c.set(r, (byte) ((int) (y * 32D)));
			d.set(r, (byte) ((int) (z * 32D)));
			e.set(r, (byte) 0);
			f.set(r, (byte) 0);
			g.set(r, onGround);
			h.set(r, onGround);
			sendPacket(p, r);
		}

		catch(NoSuchFieldException e)
		{
			e.printStackTrace();
		}

		catch(SecurityException e)
		{
			e.printStackTrace();
		}

		catch(IllegalArgumentException e1)
		{
			e1.printStackTrace();
		}

		catch(IllegalAccessException e1)
		{
			e1.printStackTrace();
		}
	}

	@Override
	public void teleportEntity(int eid, Player p, Location l, boolean onGround)
	{
		PacketPlayOutEntityTeleport t = new PacketPlayOutEntityTeleport();
		new V(t).set("a", eid);
		new V(t).set("b", (int) MathHelper.floor(l.getX() * 32D));
		new V(t).set("c", (int) MathHelper.floor(l.getY() * 32D));
		new V(t).set("d", (int) MathHelper.floor(l.getZ() * 32D));
		new V(t).set("e", (byte) 0);
		new V(t).set("f", (byte) 0);
		new V(t).set("g", onGround);
		sendPacket(p, t);
	}

	@Override
	public void spawnArmorStand(int eid, UUID id, Location l, int data, Player player)
	{
		PacketPlayOutSpawnEntity m = new PacketPlayOutSpawnEntity();
		new V(m).set("a", eid);
		new V(m).set("b", MathHelper.floor(l.getX() * 32D));
		new V(m).set("c", MathHelper.floor(l.getY() * 32D));
		new V(m).set("d", MathHelper.floor(l.getZ() * 32D));
		new V(m).set("j", 78);

		sendPacket(player, m);
	}

	@Override
	public void sendTeam(Player p, String id, String name, String prefix, String suffix, C color, int mode)
	{
		PacketPlayOutScoreboardTeam k = new PacketPlayOutScoreboardTeam();
		new V(k).set("a", id);
		new V(k).set("b", name);
		new V(k).set("i", mode); // 0 = new, 1 = remove, 2 = update, 3 = addplayer, 4 = removeplayer
		new V(k).set("c", prefix);
		new V(k).set("d", suffix);
		new V(k).set("j", 0);
		new V(k).set("f", "never");
		new V(k).set("e", "always");
		new V(k).set("g", color.getMeta());
		sendPacket(p, k);
	}

	@Override
	public void addTeam(Player p, String id, String name, String prefix, String suffix, C color)
	{
		sendTeam(p, id, name, prefix, suffix, color, 0);
	}

	@Override
	public void updateTeam(Player p, String id, String name, String prefix, String suffix, C color)
	{
		sendTeam(p, id, name, prefix, suffix, color, 2);
	}

	@Override
	public void removeTeam(Player p, String id)
	{
		sendTeam(p, id, "", "", "", C.WHITE, 1);
	}

	@Override
	public void addToTeam(Player p, String id, String... entities)
	{
		PacketPlayOutScoreboardTeam k = new PacketPlayOutScoreboardTeam();
		new V(k).set("a", id);
		new V(k).set("i", 3);
		Collection<String> h = new V(k).get("h");
		h.addAll(new GList<String>(entities));
		sendPacket(p, k);
	}

	@Override
	public void removeFromTeam(Player p, String id, String... entities)
	{
		PacketPlayOutScoreboardTeam k = new PacketPlayOutScoreboardTeam();
		new V(k).set("a", id);
		new V(k).set("i", 4);
		Collection<String> h = new V(k).get("h");
		h.addAll(new GList<String>(entities));
		sendPacket(p, k);
	}

	@Override
	public void displayScoreboard(Player p, int slot, String id)
	{
		PacketPlayOutScoreboardDisplayObjective k = new PacketPlayOutScoreboardDisplayObjective();
		new V(k).set("a", slot);
		new V(k).set("b", id);
		sendPacket(p, k);
	}

	@Override
	public void displayScoreboard(Player p, C slot, String id)
	{
		displayScoreboard(p, 3 + slot.getMeta(), id);
	}

	@Override
	public void sendNewObjective(Player p, String id, String name)
	{
		PacketPlayOutScoreboardObjective k = new PacketPlayOutScoreboardObjective();
		new V(k).set("d", 0);
		new V(k).set("a", id);
		new V(k).set("b", name);
		new V(k).set("c", EnumScoreboardHealthDisplay.INTEGER);
		sendPacket(p, k);
	}

	@Override
	public void sendDeleteObjective(Player p, String id)
	{
		PacketPlayOutScoreboardObjective k = new PacketPlayOutScoreboardObjective();
		new V(k).set("d", 1);
		new V(k).set("a", id);
		new V(k).set("b", "memes");
		new V(k).set("c", EnumScoreboardHealthDisplay.INTEGER);
		sendPacket(p, k);
	}

	@Override
	public void sendEditObjective(Player p, String id, String name)
	{
		PacketPlayOutScoreboardObjective k = new PacketPlayOutScoreboardObjective();
		new V(k).set("d", 2);
		new V(k).set("a", id);
		new V(k).set("b", name);
		new V(k).set("c", EnumScoreboardHealthDisplay.INTEGER);
		sendPacket(p, k);
	}

	@Override
	public void sendScoreUpdate(Player p, String name, String objective, int score)
	{
		PacketPlayOutScoreboardScore k = new PacketPlayOutScoreboardScore();
		new V(k).set("a", name);
		new V(k).set("b", objective);
		new V(k).set("c", score);
		new V(k).set("d", EnumScoreboardAction.CHANGE);
		sendPacket(p, k);
	}

	@Override
	public void sendScoreRemove(Player p, String name, String objective)
	{
		PacketPlayOutScoreboardScore k = new PacketPlayOutScoreboardScore();
		new V(k).set("a", name);
		new V(k).set("b", objective);
		new V(k).set("c", 0);
		new V(k).set("d", EnumScoreboardAction.REMOVE);
		sendPacket(p, k);
	}

	@Override
	public void sendRemoveGlowingColorMetaEntity(Player p, UUID glowing)
	{
		String c = teamCache.get(p.getUniqueId() + "-" + glowing);

		if(c != null)
		{
			teamCache.remove(p.getUniqueId() + "-" + glowing);
			removeFromTeam(p, c, glowing.toString());
			removeTeam(p, c);
		}
	}

	@Override
	public void sendRemoveGlowingColorMetaPlayer(Player p, UUID glowing, String name)
	{
		String c = teamCache.get(p.getUniqueId() + "-" + glowing);

		if(c != null)
		{
			teamCache.remove(p.getUniqueId() + "-" + glowing);
			removeFromTeam(p, c, name);
			removeTeam(p, c);
		}
	}

	@Override
	public void sendGlowingColorMeta(Player p, Entity glowing, C color)
	{
		if(glowing instanceof Player)
		{
			sendGlowingColorMetaName(p, p.getName(), color);
		}

		else
		{
			sendGlowingColorMetaEntity(p, glowing.getUniqueId(), color);
		}
	}

	@Override
	public void sendGlowingColorMetaEntity(Player p, UUID euid, C color)
	{
		sendGlowingColorMetaName(p, euid.toString(), color);
	}

	@Override
	public void sendGlowingColorMetaName(Player p, String euid, C color)
	{
		String c = teamCache.get(p.getUniqueId() + "-" + euid);

		if(c != null)
		{
			updateTeam(p, c, c, color.toString(), C.RESET.toString(), color);
			sendEditObjective(p, c, c);
		}

		else
		{
			c = "v" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15);
			teamCache.put(p.getUniqueId() + "-" + euid, c);

			addTeam(p, c, c, color.toString(), C.RESET.toString(), color);
			updateTeam(p, c, c, color.toString(), C.RESET.toString(), color);

			addToTeam(p, c, euid.toString());
		}
	}

	@Override
	public void sendRemoveGlowingColorMeta(Player p, Entity glowing)
	{
		String c = teamCache.get(p.getUniqueId() + "-" + glowing.getUniqueId());

		if(c != null)
		{
			teamCache.remove(p.getUniqueId() + "-" + glowing.getUniqueId());
			removeFromTeam(p, c, glowing instanceof Player ? glowing.getName() : glowing.getUniqueId().toString());
			removeTeam(p, c);
		}
	}

	@Override
	public void updatePassengers(Player p, int vehicle, int... passengers)
	{
		throw new UnsupportedOperationException("Unsupported in 1.8!");
	}

	@Override
	public void sendEntityMetadata(Player p, int eid, Object... objects)
	{
		PacketPlayOutEntityMetadata md = new PacketPlayOutEntityMetadata();
		new V(md).set("a", eid);
		List<WatchableObject> items = new GList<WatchableObject>();

		for(Object i : objects)
		{
			if(i == null)
			{
				continue;
			}

			items.add((WatchableObject) i);
		}

		new V(md).set("b", items);
		sendPacket(p, md);
	}

	@Override
	public void sendEntityMetadata(Player p, int eid, List<Object> objects)
	{
		sendEntityMetadata(p, eid, objects.toArray(new Object[objects.size()]));
	}

	@Override
	public Object getMetaEntityRemainingAir(int airTicksLeft)
	{
		// Integer, Index 1
		return new WatchableObject(2, 1, airTicksLeft);
	}

	@Override
	public Object getMetaEntityCustomName(String name)
	{
		// String, Index 2
		return new WatchableObject(4, 2, name);
	}

	@Override
	public Object getMetaEntityProperties(boolean onFire, boolean crouched, boolean sprinting, boolean swimming, boolean invisible, boolean glowing, boolean flyingElytra)
	{
		byte bits = 0;
		bits += onFire ? 1 : 0;
		bits += crouched ? 2 : 0;
		bits += sprinting ? 8 : 0;
		bits += swimming ? 10 : 0;
		bits += invisible ? 20 : 0;

		// Byte, Index 0
		return new WatchableObject(0, 0, bits);
	}

	@Override
	public Object getMetaEntityGravity(boolean gravity)
	{
		return null;
	}

	@Override
	public Object getMetaEntitySilenced(boolean silenced)
	{
		// Byte (Boolean), Index 4
		return new WatchableObject(0, 4, (byte) (silenced ? 1 : 0));
	}

	@Override
	public Object getMetaEntityCustomNameVisible(boolean visible)
	{
		// Byte (Boolean), Index 3
		return new WatchableObject(0, 3, (byte) (visible ? 1 : 0));
	}

	@Override
	public Object getMetaArmorStandProperties(boolean isSmall, boolean hasArms, boolean noBasePlate, boolean marker)
	{
		byte bits = 0;
		bits += isSmall ? 1 : 0;
		bits += hasArms ? 2 : 0;
		bits += noBasePlate ? 8 : 0;
		bits += marker ? 10 : 0;

		return new WatchableObject(0, 10, bits);
	}

	@Override
	public void sendItemStack(Player p, ItemStack is, int slot)
	{
		sendPacket(p, new PacketPlayOutSetSlot(((CraftPlayer) p).getHandle().activeContainer.windowId, slot, CraftItemStack.asNMSCopy(is)));
	}

	@Override
	public void resendChunkSection(Player p, int x, int y, int z)
	{
		ShadowChunk sc = shadowCopy(p.getWorld().getChunkAt(x, z));
		sc.modifySection(y);
		new PacketBuffer().q(sc.flush()).flush(p);
	}

	@Override
	public void redstoneParticle(Player p, Color c, Location l, float size)
	{
		ParticleColor cx = new ParticleEffect.OrdinaryColor(c.getRed(), c.getGreen(), c.getBlue());
		ParticleEffect.REDSTONE.display(cx, l, p);
	}

	@Override
	public void redstoneParticle(double range, Color c, Location l, float size)
	{
		ParticleColor cx = new ParticleEffect.OrdinaryColor(c.getRed(), c.getGreen(), c.getBlue());
		ParticleEffect.REDSTONE.display(cx, l, range);
	}

	@Override
	public Object getIChatBaseComponent(BaseComponent bc)
	{
		return IChatBaseComponent.ChatSerializer.a(ComponentSerializer.toString(bc));
	}

	@Override
	public void add(BookMeta bm, GList<BaseComponent> pages)
	{
		((CraftMetaBook) bm).pages = pages.convert((bc) -> IChatBaseComponent.ChatSerializer.a(ComponentSerializer.toString(bc)));
	}
}
