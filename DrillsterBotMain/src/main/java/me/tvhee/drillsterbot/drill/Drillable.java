package me.tvhee.drillsterbot.drill;

import java.util.Objects;

public class Drillable
{
    private final String name;
    private final boolean course;
    private final int proficiency;
    private final String id;
    
    public Drillable(String name, String type, int proficiency, String id)
    {
        this.name = name;
        this.course = type.equals("COURSE");
        this.proficiency = proficiency;
        this.id = id;
    }
    
    public String getName()
    {
        return name;
    }
    
    public boolean isCourse()
    {
        return course;
    }
    
    public int getProficiency()
    {
        return proficiency;
    }
    
    public String getId()
    {
        return id;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;
        
        if(o == null || getClass() != o.getClass())
            return false;
        
        Drillable drillable = (Drillable) o;
        return Objects.equals(id, drillable.id);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }
}
