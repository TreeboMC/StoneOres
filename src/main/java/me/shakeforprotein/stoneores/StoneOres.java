package me.shakeforprotein.stoneores;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.BentoBox;


public class StoneOres extends JavaPlugin {
    private BentoBox api;
    private File langConfig = null;
    private File playerFile = null;
    private YamlConfiguration lang = new YamlConfiguration();
    private YamlConfiguration playerYaml = new YamlConfiguration();

    @Override
    public void onEnable() {
        // Plugin startup logic
        Boolean debug = false;

        System.out.println("StoneOres is Starting");
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getConfig().options().copyDefaults(true);
        saveConfig();
        langConfig = new File(getDataFolder(), "lang.yml");
        mkdir(langConfig);
        loadYamls();

        System.out.println("StoneOres has finished loading");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("StoneOres has Terminated successfully");
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String Language = getConfig().getString("language");
        String mp = Language + ".messages.";
        api = BentoBox.getInstance();
        Player player = (Player) sender;
        Player p = player;
        int islandLevel = 0;
        String currentWorld = player.getLocation().getWorld().getName();
        String effWorld2 = currentWorld;
        effWorld2.replace("_the_end", "_world").replace("_nether", "_world");

        if (cmd.getName().equalsIgnoreCase("value")) {
            HumanEntity hE = (HumanEntity) sender;
            Integer itemValue = -1;
            Integer itemLimit = -1;
            String blockLimit = "";
            String itemInHand = hE.getInventory().getItemInMainHand().getType().toString();
            try {
                itemValue = api.getAddonsManager().getAddonByName("Level").get().getConfig().getInt("blocks." + itemInHand);
                itemLimit = api.getAddonsManager().getAddonByName("Level").get().getConfig().getInt("limits." + itemInHand);
                if (itemLimit > 0) {
                    blockLimit = " but is worth nothing after " + itemLimit + " blocks of " + itemInHand + " are on your island";
                }
            } catch (NullPointerException e) {
            }
            if (itemValue < 1) {
                itemValue = 0;
            }
            p.sendMessage(itemInHand + " has a value of " + itemValue + blockLimit);

        } else if ((cmd.getName().equalsIgnoreCase("stoneores")) || (cmd.getName().equalsIgnoreCase("ores"))) {
            if (args.length == 3 && args[0].equalsIgnoreCase("query")) {
                if (p.hasPermission("stoneores.reload")) {
                    if (args[1].equalsIgnoreCase("int")) {
                        p.sendMessage("" + getConfig().getInt("" + args[2]));
                    } else if (args[1].equalsIgnoreCase("string")) {
                        p.sendMessage(getConfig().getString(args[2]));
                    }
                }
            }
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("all")) {
                    getAllGeneratorGroups(player);
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (player.hasPermission("stoneores.reload")) {
                        try {
                            lang.load(langConfig);
                        } catch (InvalidConfigurationException | IOException e) {
                        }
                        reloadConfig();
                        player.sendMessage(getLang().getString(mp + "configReloaded").replace('&', '§'));
                    } else {
                        player.sendMessage(getLang().getString(mp + "noPermission").replace('&', '§'));
                    }
                } else if (args[0].equalsIgnoreCase("debug")) {
                    if (player.hasPermission("stoneores.reload")) {
                        if (getConfig().get("debug") == "false") {
                            getConfig().set("debug", "true");
                            p.sendMessage("debug enabled");
                        } else {
                            getConfig().set("debug", "false");
                            p.sendMessage("debug Disabled");
                        }
                    }
                } else if (args[0].equalsIgnoreCase("version")) {
                    p.sendMessage("StoneOres - " + this.getDescription().getVersion());
                }
            } else {
                bentoCallLevel(player.getWorld(), player);
                // to set a delay while it runs the level command
                getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                    public void run() {
                        Debug("Debug mode enabled", p);
                        String generatorGroup = "default";
                        Debug("Generator group = " + generatorGroup, p);
                        UUID thisIslandOwner = api.getIslands().getIslandAt(player.getLocation()).get().getOwner();
                        Debug("thisIslandOwner = " + thisIslandOwner, p);

                        generatorGroup = getGeneratorGroup(player.getWorld(), readPlayerLevelYaml(thisIslandOwner, player.getWorld()));
                        Debug("Calculated generator group is " + generatorGroup, p);
                        String[] blocksList = getBlockList(player.getWorld(), generatorGroup);
                        Debug("Blockslist is " + blocksList, p);


                        if (getConfig().getConfigurationSection("world." + currentWorld) != null) {


                            int percentCalc = 0, arrayId = 0, i = 0;
                            String[] blocktypes = new String[blocksList.length];
                            String[] cases = new String[50000];


                            for (String item : blocksList) {
                                percentCalc += getConfig().getInt("world." + currentWorld + ".blocktypes." + generatorGroup + "." + item);
                                blocktypes[arrayId] = item;
                                arrayId++;

                                while (i < percentCalc) {
                                    cases[i] = item;
                                    i++;
                                }
                            }


                            String hasPermission = null;
                            int percent = 0;


                            if (generatorGroup != null) {
                                Debug("Generator permission granted", p);
                                player.sendMessage(getLang().getString(mp + "hasTier").replace("{permission}", generatorGroup).replace('&', '§'));
                                player.sendMessage(getLang().getString(mp + "rates").replace('&', '§'));

                            }
                            for (String item : getConfig().getConfigurationSection("world." + currentWorld + ".blocktypes." + generatorGroup).getKeys(false)) {
                                percent = getConfig().getInt("world." + currentWorld + ".blocktypes." + generatorGroup + "." + item);
                                double percentDouble = ((double) percent);
                                player.sendMessage("§3" + item + ": §f" + Math.rint((percentDouble / percentCalc) * 100) + "%");
                            }
                        }
                    }
                }, 40L);
            }
        } else {
            player.sendMessage("§3  Sorry, that command does not work in this world");
        }

        return true;
    }

    private void mkdir(File L) {
        if (!L.exists()) {
            saveResource("lang.yml", false);

        }

    }

    private void loadYamls() {
        try {
            lang.load(langConfig);
        } catch (InvalidConfigurationException | IOException e) {
        }
    }

    public YamlConfiguration getLang() {
        return lang;
    }

    public YamlConfiguration getPlayerYaml() {
        return playerYaml;
    }

    public void saveLang() {
        try {
            lang.save(langConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void bentoCallLevel(World world, Player p) {
        Debug("Start bento level call", p);
        String worldString = world.getName();
        String commandString = getConfig().getString("world." + worldString + ".lvlcommand");
        p.performCommand(commandString);
    }

    public int readPlayerLevelYaml(UUID ownerID, World world) {

        playerFile = new File(Bukkit.getPluginManager().getPlugin("StoneOres").getDataFolder().getParent() + "/BentoBox/database/LevelsData", ownerID.toString() + ".yml");
        playerYaml = new YamlConfiguration();
        mkdir(playerFile);
        try {
            playerYaml.load(playerFile);
        } catch (FileNotFoundException e) {
        } catch (InvalidConfigurationException | IOException e) {
        }

        int islandLevel = getPlayerYaml().getInt("levels." + world.getName(), 0);
        if (islandLevel < 1) {
            islandLevel = 0;
        }
        return islandLevel;
    }


    public void getAllGeneratorGroups(Player player) {
        Debug("Start get all generator groups", player);
        String worldStr = player.getWorld().getName();
        String tierKeysConf = getConfig().getConfigurationSection("world." + worldStr + ".tiers").getKeys(false).toString();
        String[] tierKeys = tierKeysConf.substring(1, tierKeysConf.length() - 1).replaceAll("\\s+", "").split(",");
        String generatorGroup = "default";
        for (String item : tierKeys) {
            Integer tierPoints = getConfig().getInt("world." + worldStr + ".tiers." + item.trim());
            player.sendMessage(" ");
            player.sendMessage(" ");
            player.sendMessage("The generator - " + item + " - requires an island level of " + tierPoints + " and generates ores at the following rates");
            generatorGroup = getGeneratorGroup(player.getWorld(), tierPoints + 1);
            Integer percent, percentCalc = 0;
            for (String item2 : getConfig().getConfigurationSection("world." + worldStr + ".blocktypes." + generatorGroup).getKeys(false)) {
                percent = getConfig().getInt("world." + worldStr + ".blocktypes." + generatorGroup + "." + item2);
                percentCalc += percent;
                double percentDouble = ((double) percent);
                player.sendMessage("§3" + item2 + ": §f" + Math.rint((percentDouble / percentCalc) * 100) + "%");
            }
        }
        Debug("Finish get all generator groups", player);
    }

    public String getGeneratorGroup(World world, int isLvl) {
        String worldStr = world.getName();
        String tierKeysConf = getConfig().getConfigurationSection("world." + worldStr + ".tiers").getKeys(false).toString();
        String[] tierKeys = tierKeysConf.substring(1, tierKeysConf.length() - 1).replaceAll("\\s+", "").split(",");
        String islandTier = "default";
        for (String item : tierKeys) {
            Integer tierPoints = getConfig().getInt("world." + worldStr + ".tiers." + item.trim());
            if (tierPoints < isLvl) {
                islandTier = item.trim();
            }
        }
        return islandTier;
    }


    public String[] getBlockList(World world, String genGroup) {
        return getConfig().getConfigurationSection("world." + world.getName() + ".blocktypes." + genGroup).getKeys(false).toString().substring(1, getConfig().getConfigurationSection("world." + world.getName() + ".blocktypes." + genGroup).getKeys(false).toString().length() - 1).replaceAll("\\s+", "").split(",");
    }

    public void Debug(String str, Player p) {
        if (getConfig().getString("debug").equalsIgnoreCase("true")) {
            p.sendMessage(str + "");
        }
    }
}