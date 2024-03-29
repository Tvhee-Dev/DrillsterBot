package me.tvhee.drillsterbot.drill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.tvhee.drillsterbot.run.Answer;
import me.tvhee.drillsterbot.run.Question;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
        Playable playable = question.getPlayable();
        
        if(wordlist.get(playable.getId()) == null)
            return new ArrayList<>();
        
        JsonObject drill = wordlist.get(playable.getId()).getAsJsonObject();
        
        if(drill.get(question.getName()) == null)
            return new ArrayList<>();
        
        JsonObject answers = drill.get(question.getName()).getAsJsonObject();
        
        if(!answers.has(question.getColumn()))
            return new ArrayList<>();
        
        List<String> requiredAnswers = new ArrayList<>();
        
        for(JsonElement answerElement : answers.get(question.getColumn()).getAsJsonArray())
            requiredAnswers.add(answerElement.getAsString());
        
        return requiredAnswers;
    }
    
    public void saveAnswer(Answer answer)
    {
        Question question = answer.getQuestion();
        Playable playable = question.getPlayable();
        
        JsonObject drill = new JsonObject();
        JsonObject answers = new JsonObject();
        
        if(wordlist.get(playable.getId()) != null)
            drill = wordlist.get(playable.getId()).getAsJsonObject();
        
        if(drill.get(question.getName()) != null)
            answers = drill.get(question.getName()).getAsJsonObject();
        
        JsonArray requiredAnswers = new JsonArray();
        
        for(String requiredAnswer : answer.getCorrectAnswers())
            requiredAnswers.add(requiredAnswer);
        
        answers.add(question.getColumn(), requiredAnswers);
        drill.add(question.getName(), answers);
        wordlist.add(playable.getId(), drill);
    }
    
    public void readFile()
    {
        try
        {
            byte[] data = Files.readAllBytes(this.wordListFile.toPath());
            
            if(data.length == 0)
                return;
            
            byte[] decodedData = Base64.getDecoder().decode(data);
            this.wordlist = JsonParser.parseString(new String(decodedData, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void saveFile()
    {
        try
        {
            byte[] data = this.wordlist.toString().getBytes(StandardCharsets.UTF_8);
            String encodedData = Base64.getEncoder().encodeToString(data);
            
            FileOutputStream outputStream = new FileOutputStream(this.wordListFile, false);
            outputStream.write(encodedData.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
