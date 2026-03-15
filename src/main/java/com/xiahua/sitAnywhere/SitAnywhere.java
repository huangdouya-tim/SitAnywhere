package com.xiahua.sitAnywhere;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SitAnywhere extends JavaPlugin implements Listener {

    private final Map<UUID, UUID> playerSitMap = new HashMap<>();
    private boolean requireSneak;
    private float angle;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfigValues();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Plugin SitAnywhere is enabled");
    }

    @Override
    public void onDisable() {
        for (UUID standUUID : playerSitMap.values()) {
            Entity stand = Bukkit.getEntity(standUUID);
            if (stand != null) stand.remove();
        }
        playerSitMap.clear();
        getLogger().info("Plugin SitAnywhere is disabled");
    }

    private void reloadConfigValues() {
        reloadConfig();
        requireSneak = getConfig().getBoolean("require-sneak", true);
        angle = (float) getConfig().getDouble("sit-angle", -75.0);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (player.isInsideVehicle()) return;
        if (requireSneak && !player.isSneaking()) return;
        float pitch = player.getLocation().getPitch();
        if (pitch > angle) return;
        if (event.getItem() != null && !event.getItem().getType().isAir()) return;
        Block blockclicked = event.getClickedBlock();
        Material type = blockclicked.getType();
        if (!type.isSolid()) return;
        sitDown(player, blockclicked);
    }
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) return;
        if (player.isInsideVehicle() && playerSitMap.containsKey(player.getUniqueId())) {
            standUp(player);
        }
    }
    private void standUp(Player player) {
        UUID standUUID = playerSitMap.get(player.getUniqueId());
        Entity stand = Bukkit.getEntity(standUUID);
        if (stand != null) {
            player.leaveVehicle();
            stand.remove();
        }
    }
    private void sitDown (Player player, Block block) {
        Location spawnLoc = block.getLocation().add(0.5, 1.0, 0.5);
        ArmorStand stand = player.getWorld().spawn(spawnLoc, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setMarker(true);
        stand.setRemoveWhenFarAway(false);
        stand.setInvulnerable(true);
        stand.addPassenger(player);
        playerSitMap.put(player.getUniqueId(), stand.getUniqueId());
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeSitForPlayer(event.getPlayer());
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        removeSitForPlayer(event.getEntity());
    }
    private void removeSitForPlayer(Player player) {
        if (playerSitMap.containsKey(player.getUniqueId())) {
            standUp(player);
        }
    }
}
