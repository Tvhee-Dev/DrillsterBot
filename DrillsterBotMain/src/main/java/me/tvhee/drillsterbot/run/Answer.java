package me.tvhee.drillsterbot.run;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Answer
{
    private final Question question;
    private final int proficiency;
    private final boolean correct;
    private final List<String> correctAnswers;
    
    public Answer(Question question, int proficiency, String correct, List<String> correctAnswers)
    {
        this.question = question;
        this.proficiency = proficiency;
        this.correct = correct.equals("CORRECT");
        this.correctAnswers = Collections.unmodifiableList(correctAnswers);
    }
    
    public Question getQuestion()
    {
        return question;
    }
    
    public int getProficiency()
    {
        return proficiency;
    }
    
    public boolean isCorrect()
    {
        return correct;
    }
    
    public List<String> getCorrectAnswers()
    {
        return correctAnswers;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;
        
        if(o == null || getClass() != o.getClass())
            return false;
        
        Answer that = (Answer) o;
        return Objects.equals(correctAnswers, that.correctAnswers);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(correctAnswers);
    }
}
