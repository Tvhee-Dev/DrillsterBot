package me.tvhee.drillsterbot.drill;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Playable extends Drillable
{
    private final Set<Playable> subPlayables = new HashSet<>();
    
    public Playable(String name, String type, int proficiency, String id)
    {
        super(name, type, proficiency, id);
        
        if(isCourse())
            throw new IllegalArgumentException("No subPlayables provided!");
    }
    
    public Playable(String name, String type, int proficiency, String id, Set<Playable> subPlayables)
    {
        super(name, type, proficiency, id);
        
        if(isCourse() && subPlayables != null)
            this.subPlayables.addAll(Collections.unmodifiableSet(subPlayables));
    }
    
    public Set<Playable> getSubPlayables()
    {
        if(isCourse())
            return subPlayables;
        else
            throw new IllegalArgumentException("This Playable is not a course so it does not have any sub playables!");
    }
}
