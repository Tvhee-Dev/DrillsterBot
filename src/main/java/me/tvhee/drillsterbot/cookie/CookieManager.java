package me.tvhee.drillsterbot.cookie;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.platform.win32.Crypt32Util;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CookieManager
{
    private final File cookieStoreCopy;
    private final Map<File, String> cookieLocations; //Storage, Decryption key
    private String chromeKeyringPassword = null;
    
    public CookieManager()
    {
        this.cookieStoreCopy = new File(System.getProperty("java.io.tmpdir"), "cookies.store");
        this.cookieLocations = getCookieLocations();
    }
    
    public Set<Cookie> getCookies()
    {
        return getCookiesForDomain(null);
    }
    
    public Set<Cookie> getCookiesForDomain(String domain)
    {
        Set<Cookie> cookies = new HashSet<>();
        
        for(Map.Entry<File, String> cookieStore : cookieLocations.entrySet())
            cookies.addAll(processCookies(cookieStore.getKey(), cookieStore.getValue(), domain));
        
        return cookies;
    }
    
    private Set<Cookie> processCookies(File cookieStore, String masterKey, String domainFilter)
    {
        boolean chrome = masterKey != null;
        Set<Cookie> cookies = new HashSet<>();
        
        if(!cookieStore.exists())
            return cookies;
        
        try
        {
            cookieStoreCopy.delete();
            Files.copy(cookieStore.toPath(), cookieStoreCopy.toPath());
            
            // load the sqlite-JDBC driver using the current class loader
            Class.forName("org.sqlite.JDBC");
            // create a database connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + cookieStoreCopy.getAbsolutePath());
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 seconds
            ResultSet result;
            String databaseName = chrome ? "cookies" : "moz_cookies";
            String hostRow = chrome ? "host_key" : "host";
            
            if(domainFilter == null || domainFilter.isEmpty())
                result = statement.executeQuery("select * from " + databaseName);
            else
                result = statement.executeQuery("select * from " + databaseName + " where " + hostRow + " like \"%" + domainFilter + "%\"");
            
            while(result.next())
            {
                String name = result.getString(chrome ? "name" : "NAME");
                byte[] cookieData = result.getBytes(chrome ? "encrypted_value" : "VALUE");
                String path = result.getString(chrome ? "path" : "PATH");
                String domain = result.getString(chrome ? "host_key" : "HOST");
                boolean secure = hasColumn(result, chrome ? "secure" : "ISSECURE") && result.getBoolean(chrome ? "secure" : "ISSECURE");
                boolean httpOnly = chrome && hasColumn(result, "httponly") && result.getBoolean("httponly");
                Date expires = result.getDate(chrome ? "expires_utc" : "EXPIRY");
                byte[] data = cookieData; //Firefox cookies are already decrypted
                
                if(masterKey != null)
                    data = decryptChromiumCookie(cookieData, masterKey);
                
                cookies.add(new Cookie(name, data, data != cookieData || !chrome, expires, path, domain, secure, httpOnly, cookieStore));
            }
            
            cookieStoreCopy.delete();
            connection.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            //If the error message is "out of memory", it probably means no database file is found
        }
        
        return cookies;
    }
    
    private byte[] decryptChromiumCookie(byte[] encryptedCookie, String encryptedMasterKeyWithPrefixB64)
    {
        byte[] decryptedCookie = null;
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        boolean mac = System.getProperty("os.name").toLowerCase().contains("mac");
        boolean linux = System.getProperty("os.name").toLowerCase().contains("nix")
                || System.getProperty("os.name").toLowerCase().contains("nux")
                || System.getProperty("os.name").toLowerCase().indexOf("aix") > 0;
        
        if(windows)
        {
            System.setProperty("jna.predictable_field_order", "true");
            
            try
            {
                decryptedCookie = Crypt32Util.cryptUnprotectData(encryptedCookie);
            }
            catch(Exception e)
            {
                try
                {
                    // Remove prefix (DPAPI)
                    byte[] encryptedMasterKeyWithPrefix = Base64.getDecoder().decode(encryptedMasterKeyWithPrefixB64);
                    byte[] encryptedMasterKey = Arrays.copyOfRange(encryptedMasterKeyWithPrefix, 5, encryptedMasterKeyWithPrefix.length);
                    // Decrypt
                    byte[] masterKey = Crypt32Util.cryptUnprotectData(encryptedMasterKey);
                    // Separate prefix (v10), nonce and ciphertext/tag
                    byte[] nonce = Arrays.copyOfRange(encryptedCookie, 3, 3 + 12);
                    byte[] ciphertextTag = Arrays.copyOfRange(encryptedCookie, 3 + 12, encryptedCookie.length);
                    // Decrypt
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, nonce);
                    SecretKeySpec keySpec = new SecretKeySpec(masterKey, "AES");
                    
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
                    decryptedCookie = cipher.doFinal(ciphertextTag);
                }
                catch(Exception ignored)
                {
                }
            }
        }
        else if(linux || mac)
        {
            if(mac && chromeKeyringPassword == null)
            {
                try
                {
                    String application = "Chrome Safe Storage";
                    Runtime runtime = Runtime.getRuntime();
                    String[] commands = { "security", "find-generic-password", "-w", "-s", application };
                    Process process = runtime.exec(commands);
                    BufferedReader processInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String output;
                    
                    while((output = processInput.readLine()) != null)
                        result.append(output);
                    
                    this.chromeKeyringPassword = result.toString();
                }
                catch(IOException ignored)
                {
                }
            }
            
            try
            {
                byte[] salt = "saltysalt".getBytes();
                char[] password = linux ? "peanuts".toCharArray() : chromeKeyringPassword.toCharArray();
                char[] iv = new char[16];
                Arrays.fill(iv, ' ');
                int keyLength = 16;
                int iterations = linux ? 1 : 1003;
                
                PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength * 8);
                SecretKeyFactory pbkdf2 = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                byte[] aesKey = pbkdf2.generateSecret(spec).getEncoded();
                SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
                
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(new String(iv).getBytes()));
                
                // if cookies are encrypted "v10" is a the prefix (has to be removed before decryption)
                if(new String(encryptedCookie).startsWith("v10"))
                    encryptedCookie = Arrays.copyOfRange(encryptedCookie, 3, encryptedCookie.length);
                
                decryptedCookie = cipher.doFinal(encryptedCookie);
            }
            catch(Exception ignored)
            {
            }
        }
        
        return decryptedCookie;
    }
    
    private Map<File, String> getCookieLocations()
    {
        String userHome = System.getProperty("user.home");
        Map<File, String> files = new HashMap<>();
        
        try
        {
            //Windows - Chrome
            File windowsChromeLocalState = new File(userHome + "\\AppData\\Local\\Google\\Chrome\\User Data\\Local State");
            
            if(windowsChromeLocalState.exists())
            {
                JsonObject jsonObjectLocalState = JsonParser.parseReader(new FileReader(windowsChromeLocalState)).getAsJsonObject();
                String lastUsedProfile = jsonObjectLocalState.get("profile").getAsJsonObject().get("last_used").getAsString();
                
                File windowsChromeCookieData = new File(userHome + "\\AppData\\Local\\Google\\Chrome\\User Data\\" + lastUsedProfile + "\\Network\\Cookies");
                
                if(windowsChromeCookieData.exists())
                    files.put(windowsChromeCookieData, getEncryptionKey(windowsChromeLocalState));
            }
            
            //Windows - Edge
            File windowsEdgeCookieData = new File(userHome + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Default\\Network\\Cookies");
            File windowsEdgeLocalState = new File(userHome + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Local State");
            
            if(windowsEdgeLocalState.exists() && windowsEdgeCookieData.exists())
                files.put(windowsEdgeCookieData, getEncryptionKey(windowsChromeLocalState));
            
            //Mac - Chrome
            File macChromeCookieData = new File(userHome + "/Library/Application Support/Google/Chrome/Default/Network/Cookies");
            File macChromeLocalState = new File(userHome + "/Library/Application Support/Google/Chrome/User Data/Local State");
            
            if(macChromeCookieData.exists())
                files.put(macChromeCookieData, getEncryptionKey(macChromeLocalState));
            
            //Linux - Chrome
            File linuxChromeCookieData = new File(userHome + "/.config/chromium/Default/Network/Cookies");
            File linuxChromeLocalState = new File(userHome + "/.config/chromium/User Data/Local State");
            
            if(linuxChromeCookieData.exists())
                files.put(linuxChromeCookieData, getEncryptionKey(linuxChromeLocalState));
            
            //General - Firefox
            for(String path : new String[]{ "\\Application Data\\Mozilla\\Firefox\\Profiles\\", "\\AppData\\Roaming\\Mozilla\\Firefox\\Profiles\\",
                    "/Library/Application Support/Firefox/Profiles/", "/.config/Firefox/", "/.config/firefox/", "/.mozilla/firefox/" })
            {
                File profilesDirectory = new File(userHome + path);
                
                if(profilesDirectory.exists())
                {
                    File[] listFiles = profilesDirectory.listFiles();
                    File[] directoryFiles = listFiles == null ? new File[]{} : listFiles;
                    
                    for(File file : directoryFiles)
                    {
                        if(file.isDirectory() && file.getName().matches("(?iu)^.*?\\.default-release$"))
                        {
                            File cookieDatabase = new File(userHome + path + file.getName() + File.separator + "cookies.sqlite");
                            
                            if(cookieDatabase.exists())
                                files.put(cookieDatabase, null);
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return files;
    }
    
    private String getEncryptionKey(File localState)
    {
        try
        {
            JsonObject jsonObjectLocalState = JsonParser.parseReader(new FileReader(localState)).getAsJsonObject();
            return jsonObjectLocalState.get("os_crypt").getAsJsonObject().get("encrypted_key").getAsString();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    private boolean hasColumn(ResultSet resultSet, String columnName) throws SQLException
    {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columns = metaData.getColumnCount();
        
        for(int x = 1; x <= columns; x++)
        {
            if(columnName.equals(metaData.getColumnName(x)))
                return true;
        }
        
        return false;
    }
}
