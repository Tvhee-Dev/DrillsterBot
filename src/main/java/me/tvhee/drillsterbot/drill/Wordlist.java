package me.tvhee.drillsterbot.drill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.tvhee.drillsterbot.run.Answer;
import me.tvhee.drillsterbot.run.Question;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class Wordlist
{
    private final File wordListFile;
    private JsonObject wordlist = new JsonObject();
    
    public Wordlist()
    {
        this.wordListFile = new File(System.getProperty("user.home"), ".drillsterbot");
    }
    
    public void createIfNotExists()
    {
        if(!wordListFile.exists())
        {
            try
            {
                wordListFile.createNewFile();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    public List<String> getAnswer(Question question)
    {
        if(!wordlist.has(question.getName()))
            return new ArrayList<>();
        
        JsonObject answers = wordlist.get(question.getName()).getAsJsonObject();
        
        if(!answers.has(question.getColumn()))
            return new ArrayList<>();
        
        List<String> requiredAnswers = new ArrayList<>();
        
        for(JsonElement answerElement : answers.get(question.getColumn()).getAsJsonArray())
            requiredAnswers.add(answerElement.getAsString());
        
        return requiredAnswers;
    }
    
    public void saveAnswer(Answer answer)
    {
        JsonObject answers = new JsonObject();
        
        if(wordlist.has(answer.getQuestion().getName()))
            answers = wordlist.get(answer.getQuestion().getName()).getAsJsonObject();
        
        JsonArray requiredAnswers = new JsonArray();
        
        for(String requiredAnswer : answer.getCorrectAnswers())
            requiredAnswers.add(requiredAnswer);
        
        answers.add(answer.getQuestion().getColumn(), requiredAnswers);
        wordlist.add(answer.getQuestion().getName(), answers);
    }
    
    public void readFile()
    {
        try
        {
            Scanner reader = new Scanner(this.wordListFile);
            
            if(!reader.hasNext())
                return;
            
            byte[] data = reader.nextLine().getBytes();
            
            if(data.length == 0)
                return;
            
            byte[] decodedData = Base64.getDecoder().decode(data);
            this.wordlist = JsonParser.parseString(new String(decodedData)).getAsJsonObject();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }
    
    public void saveFile()
    {
        byte[] data = this.wordlist.toString().getBytes();
        byte[] encodedData = Base64.getEncoder().encode(data);
        
        try
        {
            FileWriter fileWriter = new FileWriter(this.wordListFile, false);
            fileWriter.write(new String(encodedData));
            fileWriter.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
