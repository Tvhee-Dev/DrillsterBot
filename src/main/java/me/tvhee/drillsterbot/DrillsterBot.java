package me.tvhee.drillsterbot;

import me.tvhee.drillsterbot.drill.Wordlist;
import me.tvhee.drillsterbot.gui.LoginScreen;
import me.tvhee.drillsterbot.gui.DrillsterBotGUI;
import me.tvhee.drillsterbot.updater.AutoUpdater;

public class DrillsterBot
{
    private static final Wordlist wordlist = new Wordlist();
    private static DrillsterAPI drillsterAPI;
    
    public static void main(String[] args)
    {
        if(AutoUpdater.checkForUpdates())
            System.exit(0); //Update available, shutdown this instance
        
        wordlist.createIfNotExists();
        wordlist.readFile();
        
        DrillsterBotGUI gui = new DrillsterBotGUI();
        gui.switchScreen(new LoginScreen());
    }
    
    public static void setDrillsterAPI(DrillsterAPI api)
    {
        drillsterAPI = api;
    }
    
    public static DrillsterAPI getDrillsterAPI()
    {
        return drillsterAPI;
    }
    
    public static Wordlist getWordlist()
    {
        return wordlist;
    }
}
