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
import java.util.HashSet;
import java.util.Set;

public final class CookieManager
{
    private final File cookieStoreCopy = new File(".cookies.db");
    private String chromeKeyringPassword = null;
    
    public Set<Cookie> getCookies()
    {
        HashSet<Cookie> cookies = new HashSet<>();
        
        for(File cookieStore : getCookieStores())
            cookies.addAll(processCookies(cookieStore, null));
        
        return cookies;
    }
    
    public Set<Cookie> getCookiesForDomain(String domain)
    {
        Set<Cookie> cookies = new HashSet<>();
        
        for(File cookieStore : getCookieStores())
            cookies.addAll(processCookies(cookieStore, domain));
        
        return cookies;
    }
    
    private Set<Cookie> processCookies(File cookieStore, String domainFilter)
    {
        boolean chrome = !cookieStore.getAbsolutePath().contains("Mozilla");
        HashSet<Cookie> cookies = new HashSet<>();
        
        if(cookieStore.exists())
        {
            Connection connection = null;
            
            try
            {
                cookieStoreCopy.delete();
                Files.copy(cookieStore.toPath(), cookieStoreCopy.toPath());
                // load the sqlite-JDBC driver using the current class loader
                Class.forName("org.sqlite.JDBC");
                // create a database connection
                connection = DriverManager.getConnection("jdbc:sqlite:" + cookieStoreCopy.getAbsolutePath());
                Statement statement = connection.createStatement();
                statement.setQueryTimeout(30); // set timeout to 30 seconds
                ResultSet result;
                
                if(domainFilter == null || domainFilter.isEmpty())
                    result = statement.executeQuery("select * from " + (chrome ? "cookies" : "moz_cookies"));
                else
                    result =
                            statement.executeQuery("select * from " + (chrome ? "cookies" : "moz_cookies") + " where host_key like \"%" + domainFilter + "%\"");
                
                while(result.next())
                {
                    String name = result.getString(chrome ? "name" : "NAME");
                    byte[] cookieData = result.getBytes(chrome ? "encrypted_value" : "VALUE");
                    String path = result.getString(chrome ? "path" : "PATH");
                    String domain = result.getString(chrome ? "host_key" : "HOST");
                    boolean secure = hasColumn(result, chrome ? "secure" : "ISSECURE") && result.getBoolean(chrome ? "secure" : "ISSECURE");
                    boolean httpOnly = chrome && hasColumn(result, "httponly") && result.getBoolean("httponly");
                    Date expires = result.getDate(chrome ? "expires_utc" : "EXPIRY");
                    byte[] decryptedData = chrome ? decryptChromeCookie(cookieData) : cookieData;
                    
                    cookies.add(
                            new Cookie(name, decryptedData != null ? decryptedData : cookieData, decryptedData != null, expires, path, domain, secure, httpOnly,
                                    cookieStore));
                    cookieStoreCopy.delete();
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
                // if the error message is "out of memory",
                // it probably means no database file is found
            }
            finally
            {
                try
                {
                    if(connection != null)
                        connection.close();
                }
                catch(SQLException e)
                {
                    // connection close failed
                }
            }
        }
        
        return cookies;
    }
    
    private byte[] decryptChromeCookie(byte[] encryptedCookie)
    {
        byte[] decryptedCookie = null;
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        boolean mac = System.getProperty("os.name").toLowerCase().contains("mac");
        boolean linux = System.getProperty("os.name").toLowerCase().contains("nix") || System.getProperty("os.name").toLowerCase().contains("nux")
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
                    for(File pathLocalState : getLocalStateFiles())
                    {
                        if(!pathLocalState.exists())
                            continue;
                        
                        // Get encrypted master key
                        JsonObject jsonObjectLocalState = JsonParser.parseReader(new FileReader(pathLocalState)).getAsJsonObject();
                        String encryptedMasterKeyWithPrefixB64 = jsonObjectLocalState.get("os_crypt").getAsJsonObject().get("encrypted_key").getAsString();
                        // Remove prefix (DPAPI)
                        byte[] encryptedMasterKeyWithPrefix = Base64.getDecoder().decode(encryptedMasterKeyWithPrefixB64);
                        byte[] encryptedMasterKey = Arrays.copyOfRange(encryptedMasterKeyWithPrefix, 5, encryptedMasterKeyWithPrefix.length);
                        // Decrypt
                        byte[] masterKey = Crypt32Util.cryptUnprotectData(encryptedMasterKey);
                        // Separate praefix (v10), nonce and ciphertext/tag
                        byte[] nonce = Arrays.copyOfRange(encryptedCookie, 3, 3 + 12);
                        byte[] ciphertextTag = Arrays.copyOfRange(encryptedCookie, 3 + 12, encryptedCookie.length);
                        // Decrypt
                        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, nonce);
                        SecretKeySpec keySpec = new SecretKeySpec(masterKey, "AES");
                        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
                        decryptedCookie = cipher.doFinal(ciphertextTag);
                    }
                }
                catch(Exception ex)
                {
                    decryptedCookie = null;
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
            //Is mac
        }
        
        return decryptedCookie;
    }
    
    private Set<File> getLocalStateFiles()
    {
        Set<File> masterKeyLocations = new HashSet<>();
        masterKeyLocations.add(new File(System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\User Data\\Local State"));
        masterKeyLocations.add(new File(System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Local State"));
        masterKeyLocations.add(new File(System.getProperty("user.home") + "/Library/Application Support/Google/Chrome/User Data/Local State")); //Mac
        masterKeyLocations.add(new File(System.getProperty("user.home") + "/.config/chromium/User Data/Local State")); //Linux
        return masterKeyLocations;
    }
    
    private Set<File> getCookieStores()
    {
        Set<File> cookieStores = new HashSet<>();
        
        cookieStores.add(new File(System.getProperty("user.home") + "\\AppData\\Local\\Yandex\\YandexBrowser\\User Data\\Default\\Cookies"));
        cookieStores.add(new File(System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Cookies"));
        cookieStores.add(new File(System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Default\\Network\\Cookies"));
        cookieStores.add(new File(System.getProperty("user.home") + "/Library/Application Support/Google/Chrome/Default/Cookies")); //Mac
        cookieStores.add(new File(System.getProperty("user.home") + "/.config/chromium/Default/Cookies")); //Linux
        
        //Firefox
        for(String path : new String[]{ "\\Application Data\\Mozilla\\Firefox\\Profiles\\", "\\AppData\\Roaming\\Mozilla\\Firefox\\Profiles\\",
                "/Library/Application Support/Firefox/Profiles/", "/.config/Firefox/", "/.config/firefox/", "/.mozilla/firefox/" })
        {
            if(new File(System.getProperty("user.home") + path).exists())
            {
                for(File file : new File(System.getProperty("user.home") + path).listFiles())
                {
                    if(file.isDirectory() && file.getName().matches("(?iu)^.*?\\.default$"))
                        cookieStores.add(new File(System.getProperty("user.home") + path + file.getName() + File.separator + "cookies.sqlite"));
                }
            }
        }
        
        return cookieStores;
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
