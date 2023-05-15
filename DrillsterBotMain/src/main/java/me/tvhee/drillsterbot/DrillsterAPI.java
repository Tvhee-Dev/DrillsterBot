package me.tvhee.drillsterbot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import me.tvhee.drillsterbot.drill.Drillable;
import me.tvhee.drillsterbot.drill.Playable;
import me.tvhee.drillsterbot.gui.Icon;
import me.tvhee.drillsterbot.run.Answer;
import me.tvhee.drillsterbot.run.Question;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class DrillsterAPI
{
    private String token;
    
    public DrillsterAPI(String token)
    {
        this.token = token;
    }
    
    public List<Drillable> getRepertoire()
    {
        List<Drillable> drillables = new ArrayList<>();
        JsonObject json = JsonParser.parseString(sendGetRequest("3/repertoire?query=&resultSize=1000000")).getAsJsonObject();
        
        if(!json.has("playableRenditions"))
            throw new JsonParseException("This JSON is not a result of /api/3/repertoire!");
        
        //Check if this is in the array of playableRenditions or a playable result
        if(json.has("playableRenditions"))
        {
            for(JsonElement drillableElement : json.getAsJsonArray("playableRenditions"))
            {
                JsonObject drillable = drillableElement.getAsJsonObject();
                String name = drillable.get("name").getAsString();
                String playableType = drillable.get("type").getAsString();
                int proficiency = drillable.get("result").getAsJsonObject().get("proficiency").getAsInt();
                String id = drillable.get("id").getAsString();
                
                drillables.add(new Drillable(name, playableType, proficiency, id));
            }
        }
        
        return drillables;
    }
    
    public Playable getPlayable(Drillable drillable)
    {
        JsonObject json = JsonParser.parseString(sendGetRequest("3/results?playable=" + drillable.getId())).getAsJsonObject();
        //proficiency section - also available for normal drills
        int proficiency = json.get("proficiency").getAsJsonObject().get("overall").getAsInt();
        
        if(!drillable.isCourse())
            return new Playable(drillable.getName(), "DRILL", proficiency, drillable.getId());
        
        //playable section - only in courses
        JsonObject playable = json.get("playable").getAsJsonObject();
        String name = playable.get("name").getAsString();
        String playableType = playable.get("type").getAsString();
        String id = playable.get("id").getAsString();
        
        //results section - only in courses
        Set<Playable> subPlayables = new HashSet<>();
        boolean all100Percent = true;
        
        for(JsonElement stepElement : json.get("results").getAsJsonArray())
        {
            JsonObject stepJson = stepElement.getAsJsonObject();
            
            if(!stepJson.get("type").getAsString().equals("DRILLS"))
                continue;
            
            for(JsonElement resultElement : stepJson.get("results").getAsJsonArray())
            {
                JsonObject resultObject = resultElement.getAsJsonObject();
                //playable section
                JsonObject subPlayable = resultObject.get("playable").getAsJsonObject();
                String subName = subPlayable.get("name").getAsString();
                String subPlayableType = subPlayable.get("type").getAsString();
                String subId = subPlayable.get("id").getAsString();
                //proficiency section
                int subProficiency = resultObject.get("proficiency").getAsJsonObject().get("overall").getAsInt();
                
                if(subProficiency < 100)
                    all100Percent = false;
                
                subPlayables.add(new Playable(subName, subPlayableType, subProficiency, subId));
            }
        }
        
        if(all100Percent)
        {
            DrillsterBot.getGUI().showMessage("Course was already fully completed!", "Completed!", Icon.WARNING_MESSAGE);
            return null;
        }
        
        return new Playable(name, playableType, proficiency, id, subPlayables);
    }
    
    public Answer answer(Playable playable, Function<Question, List<String>> answerFunction)
    {
        JsonObject questionJson = JsonParser.parseString(sendGetRequest("2.1.1/question/" + playable.getId())).getAsJsonObject().get("question").getAsJsonObject();
        //reference section
        String id = questionJson.get("reference").getAsString();
        
        //ask section
        JsonObject ask = questionJson.get("ask").getAsJsonObject();
        //tell section
        String column = questionJson.get("tell").getAsJsonObject().get("name").getAsString();
        String questionString = ask.get("term").getAsJsonObject().get("value").getAsString();
        Question question = new Question(playable, id, column, questionString);
        
        StringBuilder answerBuilder = new StringBuilder();
        List<String> answersToSet = answerFunction.apply(question);
        
        for(String answerToSet : answersToSet)
            answerBuilder.append("answer=").append(answerToSet).append("&");
        
        if(answerBuilder.length() > 0)
            answerBuilder.setLength(answerBuilder.length() - 1);
        
        JsonObject answerInformationJson = JsonParser.parseString(sendPostRequest("2.1.1/answer/" + question.getId(), answerBuilder.toString())).getAsJsonObject();
        JsonObject evaluation = answerInformationJson.get("evaluation").getAsJsonObject();
        
        int proficiency = answerInformationJson.get("proficiency").getAsJsonObject().get("overall").getAsInt();
        String correct = evaluation.get("result").getAsString();
        List<String> correctAnswers = new ArrayList<>();
        
        for(JsonElement correctAnswerElement : evaluation.get("termEvaluations").getAsJsonArray())
            correctAnswers.add(correctAnswerElement.getAsJsonObject().get("value").getAsString());
        
        return new Answer(question, proficiency, correct, correctAnswers);
    }
    
    private String sendPostRequest(String request, String data)
    {
        return sendRequest(request, connection -> {
            try
            {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", String.valueOf(data.length()));
                connection.setRequestMethod("PUT");
                connection.setDoOutput(true);
                
                OutputStream outputStream = connection.getOutputStream();
                byte[] dataToWrite = data.getBytes(StandardCharsets.UTF_8);
                outputStream.write(dataToWrite, 0, dataToWrite.length);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        });
    }
    
    private String sendGetRequest(String request)
    {
        return sendRequest(request, connection -> {
            try
            {
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestMethod("GET");
            }
            catch(ProtocolException e)
            {
                e.printStackTrace();
            }
        });
    }
    
    private String sendRequest(String request, Consumer<HttpURLConnection> connectionEditor)
    {
        try
        {
            String urlString = "https://www.drillster.com/api/" + request;
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            //Request headers
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connectionEditor.accept(connection);
            
            if(connection.getResponseCode() == 401)
            {
                String option = DrillsterBot.getGUI().getOption("Invalid token! Please put a valid token or close the application", "Invalid token", Icon.ERROR_MESSAGE, new String[] {"Input", "Close"});
                
                if(option.equals("Input"))
                {
                    this.token = DrillsterBot.getGUI().getInput("Please put in your token:");
                    return sendRequest(request, connectionEditor);
                }
                else
                {
                    System.exit(0);
                }
            }
            
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line;
            
            while((line = reader.readLine()) != null)
                result.append(line);
            
            return result.toString();
        }
        catch(Exception ex)
        {
            if(ex instanceof UnknownHostException)
            {
                DrillsterBot.getGUI().showMessage("DrillsterBot has lost any connection to the internet!", "Unknown Host", Icon.ERROR_MESSAGE);
                System.exit(0);
            }
            
            ex.printStackTrace();
            return "{}";
        }
    }
}
