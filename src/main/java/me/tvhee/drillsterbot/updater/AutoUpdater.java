package me.tvhee.drillsterbot.updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.tvhee.drillsterbot.gui.DrillsterBotGUI;
import me.tvhee.drillsterbot.gui.UpdatingGUI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Base64;

public class AutoUpdater
{
    private static final String CURRENT_VERSION = "v3.0.0";
    
    public static boolean checkForUpdates()
    {
        try
        {
            String path = AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "utf-8");
            File thisJarFile = new File(decodedPath);
            File removeInstruction = new File(thisJarFile.getParentFile(), "DrillsterBot-FileRemover.txt");
            
            if(removeInstruction.exists())
            {
                BufferedReader reader = new BufferedReader(new FileReader(removeInstruction));
                File toRemove = new File(new String(Base64.getDecoder().decode(reader.readLine().getBytes())));
                
                if(toRemove.exists())
                    toRemove.delete();
                
                reader.close();
                removeInstruction.delete();
            }
            
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.github.com/repos/tvhee-dev/DrillsterBot/releases").openConnection();
            connection.setRequestMethod("GET");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            JsonObject response = JsonParser.parseReader(reader).getAsJsonArray().get(0).getAsJsonObject();
            String latestVersion = response.get("tag_name").getAsString();
            
            if(latestVersion.equals(CURRENT_VERSION) || !latestVersion.startsWith("v3"))
                return false; //No update available
            
            new DrillsterBotGUI().switchScreen(new UpdatingGUI());
            
            String downloadLink = response.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("browser_download_url").getAsString();
            connection = (HttpURLConnection) new URL(downloadLink).openConnection();
            ReadableByteChannel channel = Channels.newChannel(connection.getInputStream());
            
            File destination = new File(thisJarFile.getParentFile(), "DrillsterBot-" + latestVersion.substring(1) + ".jar");
            FileOutputStream output = new FileOutputStream(destination);
            
            output.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
            output.flush();
            output.close();
            
            FileWriter fileWriter = new FileWriter(removeInstruction, false);
            fileWriter.write(new String(Base64.getEncoder().encode(thisJarFile.getAbsolutePath().getBytes())));
            fileWriter.flush();
            fileWriter.close();
            
            Runtime.getRuntime().exec("java -jar \"" + destination.getAbsolutePath() + "\"");
            return true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}
