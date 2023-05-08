package me.tvhee.drillsterbot.gui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import me.tvhee.drillsterbot.DrillsterAPI;
import me.tvhee.drillsterbot.DrillsterBot;
import me.tvhee.drillsterbot.drill.Playable;
import me.tvhee.drillsterbot.drill.Wordlist;
import me.tvhee.drillsterbot.run.Answer;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DoingDrillScreen implements SimpleScreen, Runnable
{
    private final Set<Playable> playables;
    private DrillsterBotGUI gui;
    private JLabel titleLabel;
    private JProgressBar progressbar;
    private JLabel progressLabel;
    private JButton startButton;
    private JButton stopButton;
    
    public DoingDrillScreen(Set<Playable> playables)
    {
        playables.removeIf(playable -> playable.getProficiency() >= 100);
        this.playables = Collections.unmodifiableSet(playables);
    }
    
    @Override
    public JPanel create(DrillsterBotGUI gui)
    {
        this.gui = gui;
        
        try
        {
            JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
            
            this.titleLabel = new JLabel("Doing Drillster...");
            this.titleLabel.setFont(new Font(null, Font.PLAIN, 18));
            
            Image image = ImageIO.read(getClass().getClassLoader().getResource("icon-128.png"));
            JLabel imageLabel = new JLabel(new ImageIcon(image));
            
            this.progressbar = new JProgressBar(0, 100);
            this.progressbar.setFont(new Font(null, Font.PLAIN, 18));
            this.progressbar.setStringPainted(true);
            this.progressbar.setPreferredSize(new Dimension(550, 40));
            
            this.progressLabel = new JLabel("Completed: 0/" + playables.size());
            this.progressLabel.setFont(new Font(null, Font.PLAIN, 18));
            
            this.startButton = new JButton("Start");
            this.startButton.setFont(new Font(null, Font.PLAIN, 18));
            this.startButton.addActionListener((e) -> {
                buttonPanel.remove(this.startButton);
                gui.refresh();
                new Thread(this).start();
            });
            
            this.stopButton = new JButton("Stop");
            this.stopButton.setFont(new Font(null, Font.PLAIN, 18));
            this.stopButton.addActionListener((e) -> System.exit(0));
            
            buttonPanel.add(this.startButton);
            buttonPanel.add(this.stopButton);
            
            JPanel panel = new JPanel(new GridBagLayout());
            panel.add(this.titleLabel, new GridCell(0, 0).setSize(2, 1).setInsets(10).toConstraints());
            panel.add(imageLabel, new GridCell(0, 1).setSize(2, 1).setInsets(10).toConstraints());
            panel.add(this.progressbar, new GridCell(0, 2).setInsets(10).toConstraints());
            panel.add(this.progressLabel, new GridCell(1, 2).setInsets(10).toConstraints());
            panel.add(buttonPanel, new GridCell(0, 3).setSize(2, 1).setInsets(10).toConstraints());
            return panel;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return new JPanel();
    }
    
    @Override
    public void run()
    {
        DrillsterAPI api = DrillsterBot.getDrillsterAPI();
        Wordlist wordlist = DrillsterBot.getWordlist();
        Map<String, Integer> progress = new HashMap<>();
        Set<String> completedDrills = new HashSet<>();
        double totalStartPercentage = 0;
        
        for(Playable playable : playables)
        {
            totalStartPercentage += playable.getProficiency();
            progress.put(playable.getId(), playable.getProficiency());
        }
        
        double startPercentage = totalStartPercentage / playables.size();
        
        //Saving the wordlist every 10 seconds
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            wordlist.saveFile();
            
            if(completedDrills.size() == playables.size())
            {
                this.stopButton.setText("Close");
                this.titleLabel.setText("Finished doing Drillster!");
                this.gui.showMessage("Done with the selected Drills!", "Done!", Icon.INFORMATION_MESSAGE);
                this.gui.refresh();
                
                executor.shutdown();
            }
        }, 10, 1, TimeUnit.SECONDS);
        
        for(Playable playable : playables)
        {
            new Thread(() ->
            {
                int thisDrillPercentage = 0;
                
                while(thisDrillPercentage < 100)
                {
                    Answer answer = api.answer(playable, wordlist::getAnswer);
                    wordlist.saveAnswer(answer);
                    
                    thisDrillPercentage = answer.getProficiency();
                    progress.put(playable.getId(), thisDrillPercentage);
                    List<Integer> percentages = new ArrayList<>(progress.values());
                    
                    double totalPercentage = 0;
                    
                    for(int drillPercentage : percentages)
                        totalPercentage += drillPercentage;
                    
                    totalPercentage = totalPercentage / percentages.size();
                    
                    double deltaPercentageGained = totalPercentage - startPercentage;
                    double deltaPercentage = 100 - startPercentage;
                    double drillPercentage = (deltaPercentageGained / deltaPercentage) * 100;
                    
                    int progressValue = (int) Math.round(drillPercentage);
                    
                    if(progressValue > this.progressbar.getValue())
                        this.progressbar.setValue(progressValue);
                    
                    gui.refresh();
                }
                
                completedDrills.add(playable.getId());
                this.progressLabel.setText("Completed: " + completedDrills.size() + "/" + playables.size());
                this.gui.refresh();
            }).start();
        }
    }
}
