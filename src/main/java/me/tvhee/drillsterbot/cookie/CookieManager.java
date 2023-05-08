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
    private final File cookieStoreCopy;
    private String chromeKeyringPassword = null;
    
    public CookieManager()
    {
        cookieStoreCopy = new File(System.getProperty("java.io.tmpdir"), "cookies.store");
    }
    
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
        
        if(!cookieStore.exists())
            return cookies;
        
        try
        {
            cookieStoreCopy.delete();
            Files.copy(cookieStore.toPath(), cookieStoreCopy.toPath());
            
            /*if(cookieStore.getAbsolutePath().endsWith(".jsonlz4"))
            {
                byte[] fileData = Files.readAllBytes(cookieStore.toPath());
                byte[] compressedData = Arrays.copyOfRange(fileData, 8, fileData.length);
                LZ4Factory factory = LZ4Factory.fastestInstance();
                LZ4FastDecompressor decompressor = factory.fastDecompressor();
                
                int decompressedLength = ByteBuffer.wrap(fileData, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                int blockSize = 2048 * 4096; // 8 megabyte
                byte[] block = new byte[blockSize];
                int remaining = decompressedLength;
                
                try(InputStream inputStream = new BufferedInputStream(new LZ4BlockInputStream(new ByteArrayInputStream(compressedData), decompressor)))
                {
                    PipedOutputStream pipedOutputStream = new PipedOutputStream();
                    
                    try(OutputStream outputStream = new BufferedOutputStream(pipedOutputStream))
                    {
                        while(remaining > 0)
                        {
                            int bytesToRead = Math.min(remaining, blockSize);
                            int bytesRead = inputStream.read(block, 0, bytesToRead);
                            
                            if(bytesRead == -1)
                                throw new EOFException("Unexpected end of input");
                            
                            outputStream.write(block, 0, bytesRead);
                            remaining -= bytesRead;
                        }
                    }
                    
                    // create a BufferedReader from the PipedInputStream
                    PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);
                    
                    try(BufferedReader reader = new BufferedReader(new InputStreamReader(pipedInputStream)))
                    {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        
                        for(JsonElement cookieElement : json.get("cookies").getAsJsonArray())
                        {
                            JsonObject cookie = cookieElement.getAsJsonObject();
                            String name = cookie.get("name").getAsString();
                            String data = cookie.get("value").getAsString();
                            String path = cookie.get("path").getAsString();
                            String domain = cookie.get("host").getAsString();
                            boolean secure = cookie.get("secure").getAsBoolean();
                            boolean httpOnly = cookie.get("httponly").getAsBoolean();
                            Date expires = new Date(cookie.get("expiry").getAsLong());
                            
                            if(!domain.contains(domainFilter))
                                continue;
                            
                            cookies.add(new Cookie(name, data.getBytes(), false, expires, path, domain, secure, httpOnly, cookieStore));
                        }
                    }
                }
            }
            else*/
            
            // load the sqlite-JDBC driver using the current class loader
            Class.forName("org.sqlite.JDBC");
            // create a database connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + cookieStoreCopy.getAbsolutePath());
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 seconds
            ResultSet result;
            
            if(domainFilter == null || domainFilter.isEmpty())
                result = statement.executeQuery("select * from " + (chrome ? "cookies" : "moz_cookies"));
            else
                result = statement.executeQuery(
                        "select * from " + (chrome ? "cookies" : "moz_cookies") + " where " + (chrome ? "host_key" : "host") + " like \"%" + domainFilter
                                + "%\"");
            
            while(result.next())
            {
                String name = result.getString(chrome ? "name" : "NAME");
                byte[] cookieData = result.getBytes(chrome ? "encrypted_value" : "VALUE");
                String path = result.getString(chrome ? "path" : "PATH");
                String domain = result.getString(chrome ? "host_key" : "HOST");
                boolean secure = hasColumn(result, chrome ? "secure" : "ISSECURE") && result.getBoolean(chrome ? "secure" : "ISSECURE");
                boolean httpOnly = chrome && hasColumn(result, "httponly") && result.getBoolean("httponly");
                Date expires = result.getDate(chrome ? "expires_utc" : "EXPIRY");
                byte[] decryptedData = null;
                
                for(File localState : getLocalStateFiles())
                {
                    if(!localState.exists())
                        continue;
                    
                    decryptedData = decryptChromeCookie(cookieData, localState);
                    
                    if(decryptedData != null)
                        break;
                }
                
                cookies.add(new Cookie(name, decryptedData != null ? decryptedData : cookieData, decryptedData != null, expires, path, domain, secure, httpOnly,
                        cookieStore));
            }
            
            cookieStoreCopy.delete();
            connection.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            // if the error message is "out of memory",
            // it probably means no database file is found
        }
        
        return cookies;
    }
    
    private byte[] decryptChromeCookie(byte[] encryptedCookie, File pathLocalState)
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
                    if(!pathLocalState.exists())
                        return null;
                    
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
        
        cookieStores.add(new File(System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Network\\Cookies"));
        cookieStores.add(new File(System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Default\\Network\\Cookies"));
        cookieStores.add(new File(System.getProperty("user.home") + "/Library/Application Support/Google/Chrome/Default/Network/Cookies")); //Mac
        cookieStores.add(new File(System.getProperty("user.home") + "/.config/chromium/Default/Network/Cookies")); //Linux
        
        //Firefox
        for(String path : new String[]{ "\\Application Data\\Mozilla\\Firefox\\Profiles\\", "\\AppData\\Roaming\\Mozilla\\Firefox\\Profiles\\",
                "/Library/Application Support/Firefox/Profiles/", "/.config/Firefox/", "/.config/firefox/", "/.mozilla/firefox/" })
        {
            File profilesDirectory = new File(System.getProperty("user.home") + path);
            
            if(profilesDirectory.exists())
            {
                for(File file : profilesDirectory.listFiles())
                {
                    if(file.isDirectory() && file.getName().matches("(?iu)^.*?\\.default-release$"))
                    {
                        File cookieDatabase = new File(System.getProperty("user.home") + path + file.getName() + File.separator + "cookies.sqlite");
                        //File recoveryFile = new File(System.getProperty("user.home") + path + file.getName() + File.separator + "sessionstore-backups" + File.separator + "recovery.jsonlz4");
                        
                        if(cookieDatabase.exists())
                            cookieStores.add(cookieDatabase);
                        
                        //if(recoveryFile.exists())
                        //cookieStores.add(recoveryFile);
                    }
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
