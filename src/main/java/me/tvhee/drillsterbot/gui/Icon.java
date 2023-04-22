package me.tvhee.drillsterbot.gui;

public enum Icon
{
    ERROR_MESSAGE(0), INFORMATION_MESSAGE(1), WARNING_MESSAGE(2), QUESTION_MESSAGE(3), PLAIN_MESSAGE(-1);
    
    private final int code;
    
    Icon(int code)
    {
        this.code = code;
    }
    
    public int getCode()
    {
        return code;
    }
}
