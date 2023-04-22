package me.tvhee.drillsterbot.drill;

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
}
