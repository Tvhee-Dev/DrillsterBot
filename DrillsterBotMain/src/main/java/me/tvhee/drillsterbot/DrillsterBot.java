package me.tvhee.drillsterbot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.tvhee.drillsterbot.drill.Storage;
import me.tvhee.drillsterbot.gui.LoginScreen;
import me.tvhee.drillsterbot.gui.DrillsterBotGUI;
import me.tvhee.drillsterbot.gui.RepertoireScreen;
import me.tvhee.drillsterbot.updater.AutoUpdater;

import java.io.File;
import java.util.Base64;
import java.util.Date;

public class DrillsterBot
{
    private static final Storage STORAGE = new Storage();
    private static final DrillsterBotGUI GUI = new DrillsterBotGUI();
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
            System.exit(0); //Update available, shutdown this instance
            return;
        }
        
        STORAGE.createIfNotExists();
        STORAGE.readFile();
        
        String storedToken = STORAGE.getToken();
        
        if(storedToken != null && validateToken(storedToken))
            GUI.switchScreen(new RepertoireScreen());
        else
            GUI.switchScreen(new LoginScreen());
    }
    
    public static void setDrillsterAPI(DrillsterAPI api)
    {
        drillsterAPI = api;
    }
    
    public static DrillsterAPI getDrillsterAPI()
    {
        return drillsterAPI;
    }
    
    public static Storage getStorage()
    {
        return STORAGE;
    }
    
    public static DrillsterBotGUI getGUI()
    {
        return GUI;
    }
    
    private static boolean validateToken(String token) {
        JsonObject tokenData = JsonParser.parseString(new String(Base64.getDecoder().decode(token.split("\\.")[1].getBytes()))).getAsJsonObject();
        long expirationDate = tokenData.get("exp").getAsLong();
        boolean validToken = expirationDate * 1000 > new Date().getTime();
        
        if(validToken)
            setDrillsterAPI(new DrillsterAPI(getStorage().getToken()));
        
        return validToken;
    }
}
