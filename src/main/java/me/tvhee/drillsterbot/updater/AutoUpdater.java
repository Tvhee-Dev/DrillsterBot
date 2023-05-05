package me.tvhee.drillsterbot.updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class AutoUpdater
{
    private static final String CURRENT_VERSION = "v3.0.0";
    
    public static boolean checkForUpdates()
    {
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.github.com/repos/tvhee-dev/DrillsterBot/releases").openConnection();
            connection.setRequestMethod("GET");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            JsonObject response = JsonParser.parseReader(reader).getAsJsonArray().get(0).getAsJsonObject();
            String latestVersion = response.get("tag_name").getAsString();
            
            if(latestVersion.equals(CURRENT_VERSION))
                return false; //No update available
            
            String downloadLink = response.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("browser_download_url").getAsString();
            connection = (HttpURLConnection) new URL(downloadLink).openConnection();
            ReadableByteChannel channel = Channels.newChannel(connection.getInputStream());
            
            String path = AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "utf-8");
            File thisJarFile = new File(decodedPath);
            File destination = new File(thisJarFile.getParentFile(), "DrillsterBot-" + latestVersion + ".jar");
            FileOutputStream output = new FileOutputStream(destination);
            
            output.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
            output.flush();
            output.close();
            
            Runtime.getRuntime().exec("java -jar \"" + destination.getAbsolutePath() + "\" -" + "\"" + thisJarFile + "\"");
            return true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}
