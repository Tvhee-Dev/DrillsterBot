package me.tvhee.drillsterbot.run;

import me.tvhee.drillsterbot.drill.Playable;

import java.util.Objects;

public class Question
{
    private final Playable playable;
    private final String id;
    private final String column;
    private final String name;
    
    public Question(Playable playable, String id, String column, String name)
    {
        this.playable = playable;
        this.id = id;
        this.column = column;
        this.name = name;
    }
    
    public Playable getPlayable()
    {
        return playable;
    }
    
    public String getId()
    {
        return id;
    }
    
    public String getColumn()
    {
        return column;
    }
    
    public String getName()
    {
        return name;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;
        
        if(o == null || getClass() != o.getClass())
            return false;
        
        Question question1 = (Question) o;
        return Objects.equals(column, question1.column) && Objects.equals(name, question1.name);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(column, name);
    }
}
