package me.shakeforprotein.stoneores;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.BentoBox;



public class StoneOres extends JavaPlugin {
    private boolean debug = true;
    private BentoBox api;
    private File langConfig = null;
    private YamlConfiguration lang = new YamlConfiguration();

    @Override
    public void onEnable() {
        // Plugin startup logic
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
        int islandLevel = 0;
        String currentWorld = player.getLocation().getWorld().getName();
        String effWorld2 = currentWorld;
        effWorld2.replace("_the_end", "_world").replace("_nether", "_world");


        if ((cmd.getName().equalsIgnoreCase("stoneores") || cmd.getName().equalsIgnoreCase("ores")) && sender instanceof Player) {
            bentoCallLevel(player.getWorld(), player);
    // to set a delay while it runs the level command
            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                public void run() {
                    String generatorGroup = "default";
                    UUID thisIslandOwner = api.getIslands().getIslandAt(player.getLocation()).get().getOwner();
                    generatorGroup = getGeneratorGroup(player.getWorld(), readPlayerLevelYaml(thisIslandOwner, player.getWorld()));
                    String[] blocksList = getBlockList(player.getWorld(), generatorGroup);


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
                        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                            if (player.hasPermission("stoneores.reload")) {
                                reloadConfig();
                                player.sendMessage(getLang().getString(mp + "configReloaded").replace('&', '§'));
                            } else {
                                player.sendMessage(getLang().getString(mp + "noPermission").replace('&', '§'));
                            }
                        } else {

                            String hasPermission = null;
                            int percent = 0;


                            if (generatorGroup != null) {
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

                }


            }, 40L);
        } else {
            player.sendMessage("§3  Sorry, that command does not work in this world");
        }

        return true;
    }

    private void mkdir(File L){
        if(!L.exists()){
            saveResource("lang.yml", false);

        }

    }
    private void loadYamls(){
        try {
            lang.load(langConfig);
        }
        catch(InvalidConfigurationException e){e.printStackTrace();}
        catch(FileNotFoundException e){e.printStackTrace();}
        catch(IOException e){e.printStackTrace();}
    }

    public YamlConfiguration getLang(){
        return lang;
    }

    public void saveLang(){
        try{lang.save(langConfig);}
        catch (IOException e){e.printStackTrace();}
    }


    public void bentoCallLevel(World world, Player p){
        String worldString = world.getName();
        if (worldString.equalsIgnoreCase("AcidIsland_world")){p.performCommand("ai level");}
        if (worldString.equalsIgnoreCase("BSkyBlock_world")){p.performCommand("is level");}
        if (worldString.equalsIgnoreCase("SkyGrid_world")){p.performCommand("sg level");}
    }

    public int readPlayerLevelYaml(UUID ownerID, World world){

        //TODO: Replace this method to use yaml interpreter instead of relying on file to string
        Bukkit.broadcast("playerFile - ","");
        File playerFile = new File(Bukkit.getPluginManager().getPlugin("StoneOres").getDataFolder().getParent() + "/BentoBox/database/LevelsData", ownerID.toString() + ".yml");

        String fileOutput = "";
        String worldStr = world.getName();
        try {fileOutput = readFile(playerFile.toString());}
        catch (IOException IO){}

        if ((fileOutput != null) && (fileOutput != "") && (fileOutput.contains(worldStr+ ":"))) {
            if(fileOutput.length() < 1) {
                playerFile.delete();
            }
            Pattern p = Pattern.compile(worldStr + ": \\d+");
            Matcher m = p.matcher(fileOutput);
            m.find();
            String tempLevel = m.group().split(": ")[1];
            return Integer.parseInt(tempLevel);
        }
        else{
            return 1;
        }
    }


    public String getGeneratorGroup(World world, int isLvl){
        String worldStr = world.getName();
        String tierKeysConf = getConfig().getConfigurationSection("world." + worldStr + ".tiers").getKeys(false).toString();
        String[] tierKeys = tierKeysConf.substring(1, tierKeysConf.length() - 1).replaceAll("\\s+", "").split(",");
        String islandTier = "";
        Integer dummyInt = 0;
        for (String item : tierKeys) {
            Integer tierPoints = getConfig().getInt("world." + worldStr + ".tiers." + item.trim());
            if (tierPoints < isLvl){
                islandTier = item.trim();
                if(debug){Bukkit.broadcast("island Tier = " + islandTier, "*");}

            }
        }
        return islandTier;
    }


    public String[] getBlockList(World world, String genGroup){
        return getConfig().getConfigurationSection("world." + world.getName() + ".blocktypes." + genGroup).getKeys(false).toString().substring(1, getConfig().getConfigurationSection("world." + world.getName() + ".blocktypes." + genGroup).getKeys(false).toString().length() - 1).replaceAll("\\s+", "").split(",");
    }

    public String readFile(String pathname) throws IOException {
        File file = new File(pathname);
        StringBuilder fileContents = new StringBuilder((int)file.length());
        Scanner scanner = new Scanner(file);
        String lineSeparator = System.getProperty("line.separator");

        try {
            while(scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }
}