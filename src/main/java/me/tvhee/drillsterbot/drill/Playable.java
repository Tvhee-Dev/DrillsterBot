package me.tvhee.drillsterbot.drill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Playable extends Drillable
{
    private final List<Playable> subPlayables = new ArrayList<>();
    
    public Playable(String name, String type, int proficiency, String id)
    {
        super(name, type, proficiency, id);
        
        if(isCourse())
            throw new IllegalArgumentException("No subPlayables provided!");
    }
    
    public Playable(String name, String type, int proficiency, String id, List<Playable> subPlayables)
    {
        super(name, type, proficiency, id);
        
        if(isCourse() && subPlayables != null)
            this.subPlayables.addAll(Collections.unmodifiableList(subPlayables));
    }
    
    public List<Playable> getSubPlayables()
    {
        if(isCourse())
            return subPlayables;
        else
            throw new IllegalArgumentException("This Playable is not a course so it does not have any sub playables!");
    }
}
