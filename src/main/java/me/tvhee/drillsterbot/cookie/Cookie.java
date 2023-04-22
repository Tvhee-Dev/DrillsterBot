package me.tvhee.drillsterbot.cookie;

import java.io.File;
import java.util.Date;

public final class Cookie
{
    private final String name;
    private final byte[] data;
    private final boolean decrypted;
    private final Date expires;
    private final String path;
    private final String domain;
    private final boolean secure;
    private final boolean httpOnly;
    private final File cookieStore;
    
    public Cookie(String name, byte[] data, boolean decrypted, Date expires, String path, String domain, boolean secure, boolean httpOnly, File cookieStore)
    {
        this.name = name;
        this.data = data;
        this.decrypted = decrypted;
        this.expires = expires;
        this.path = path;
        this.domain = domain;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.cookieStore = cookieStore;
    }
    
    public String getName()
    {
        return name;
    }
    
    public boolean isDecrypted()
    {
        return decrypted;
    }
    
    public byte[] getData()
    {
        return data;
    }
    
    public Date getExpires()
    {
        return expires;
    }
    
    public String getPath()
    {
        return path;
    }
    
    public String getDomain()
    {
        return domain;
    }
    
    public boolean isSecure()
    {
        return secure;
    }
    
    public boolean isHttpOnly()
    {
        return httpOnly;
    }
    
    public File getCookieStore()
    {
        return cookieStore;
    }
}
