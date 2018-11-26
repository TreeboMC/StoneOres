package me.shakeforprotein.stoneores;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import world.bentobox.bentobox.BentoBox;


import java.util.UUID;


public class PlayerListener implements Listener {
    private StoneOres plugin;
    private BentoBox api;


    public PlayerListener(StoneOres instance) {

        instance.getServer().getPluginManager().registerEvents(this, instance);
        BentoBox.getInstance().getServer().getPluginManager().registerEvents(this, instance);
        this.plugin = instance;
    }


    @EventHandler
    public void onBlockFromToEvent(BlockFromToEvent e) {
        api = BentoBox.getInstance();

        int islandLevel = 0;
        String generatorGroup = "default";


        String blockToMake = null;
        String effectedWorld = e.getBlock().getLocation().getWorld().getName();
        String effWorld2 = effectedWorld;
        effWorld2.replace("_the_end", "_world").replace("_nether", "_world");


            if (e.getBlock().getType() == Material.LAVA || (e.getBlock().getType() == Material.WATER)) {
                if (e.getToBlock().getType() == Material.AIR) {
                    if (wouldMakeCobble(e.getBlock().getType(), e.getToBlock())) {
                        if (getBlockLocation(e.getBlock().getType(), e.getToBlock()) != null) {
                            Location location = getBlockLocation(e.getBlock().getType(), e.getToBlock());
                            if (plugin.getConfig().getConfigurationSection("world." + effectedWorld) != null) {
                                UUID ownerUUID = api.getIslands().getIslandAt(e.getBlock().getLocation()).get().getOwner();
                                generatorGroup = plugin.getGeneratorGroup(e.getBlock().getWorld(), plugin.readPlayerLevelYaml(ownerUUID,  e.getBlock().getWorld()));
                                String[] blocksList = plugin.getBlockList(e.getBlock().getWorld(), generatorGroup);

                                int totalChance = 0, arrayId = 0, i = 0;
                                String[] blocktypes = new String[blocksList.length];
                                String[] cases = new String[50000];

                                for (String item : blocksList) {
                                    totalChance += plugin.getConfig().getInt("world." + effectedWorld + ".blocktypes." + generatorGroup + "." + item);
                                    blocksList[arrayId] = item;
                                    arrayId++;

                                    while (i < totalChance) {
                                        cases[i] = item;
                                        i++;
                                    }
                                }

                                int random = 1 + (int) (Math.random() * totalChance);

                                blockToMake = cases[random];


                            try{e.getBlock().getWorld().getBlockAt(location).setType(Material.getMaterial(blockToMake));}
                            catch(NullPointerException err){}
                            e.setCancelled(true);


                        }

                    }
                }
            }
        }
    }

    private final BlockFace[] sides = new BlockFace[]{BlockFace.SELF, BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};


    public boolean wouldMakeCobble(Material mat, Block block) {
        for (BlockFace side : sides) {
            if (block.getRelative(side, 1).getType() == (mat == Material.WATER ? Material.LAVA : Material.WATER) || block.getRelative(side, 1).getType() == (mat == Material.WATER ? Material.LAVA : Material.WATER)) {
                return true;
            }
        }
        return true;
    }

    public Location getBlockLocation(Material material, Block block) {
        Location location = null;

        for (BlockFace side : sides) {
            if (block.getRelative(side, 1).getType() == (material == Material.LAVA ? Material.WATER : Material.LAVA)) {
                if (block != null && block.getLocation() != null) {
                    location = block.getLocation();
                } else {
                    break;
                }
            }
        }
        return location;
    }

}

