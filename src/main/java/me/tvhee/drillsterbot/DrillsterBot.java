package me.tvhee.drillsterbot;

import me.tvhee.drillsterbot.drill.Wordlist;
import me.tvhee.drillsterbot.gui.LoginScreen;
import me.tvhee.drillsterbot.gui.DrillsterBotGUI;
import me.tvhee.drillsterbot.updater.AutoUpdater;

import java.io.File;

public class DrillsterBot
{
    private static final Wordlist wordlist = new Wordlist();
    private static DrillsterAPI drillsterAPI;
    
    public static void main(String[] args)
    {
        //args[0] = Argument of old program file after update
        if(args.length > 0)
        {
            File oldProgram = new File(args[0].substring(1));
            oldProgram.delete();
        }
        
        if(AutoUpdater.checkForUpdates())
        {
            System.exit(0); //Update available, shutdown this
            return;
        }
        
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
