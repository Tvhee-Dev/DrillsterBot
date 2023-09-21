package me.tvhee.drillsterbot.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import me.tvhee.drillsterbot.DrillsterAPI;
import me.tvhee.drillsterbot.DrillsterBot;
import me.tvhee.drillsterbot.drill.Drillable;
import me.tvhee.drillsterbot.drill.Playable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class RepertoireScreen implements SimpleScreen
{
    private DrillsterBotGUI gui;
    
    @Override
    public JPanel create(DrillsterBotGUI gui)
    {
        this.gui = gui;
        
        try
        {
            DrillsterAPI api = DrillsterBot.getDrillsterAPI();
            List<Drillable> repertoire = api.getRepertoire();
            List<JButton> drillButtons = new ArrayList<>();
            
            JLabel titleLabel = new JLabel("Kies een Drill / Course");
            titleLabel.setFont(new Font(null, Font.PLAIN, 18));
            
            JButton logoutButton = new JButton("Logout");
            logoutButton.setFont(new Font(null, Font.PLAIN, 18));
            logoutButton.addActionListener(e ->
            {
                DrillsterBot.getStorage().saveToken(null);
                DrillsterBot.getStorage().saveFile();
                
                gui.switchScreen(new LoginScreen());
            });
            
            for(Drillable drillable : repertoire)
            {
                if(drillable.getProficiency() >= 100)
                   continue;
                
                JButton button = new JButton(drillable.getName() + " (Type: " + (drillable.isCourse() ? "Course" : "Drill") + ", Percentage: " + drillable.getProficiency() + "%)");
                button.setPreferredSize(new Dimension(600, 40));
                button.setFont(new Font(null, Font.PLAIN, 18));
                button.addActionListener(e -> actionPerformed(e, drillable));
                
                drillButtons.add(button);
            }
            
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
            panel.add(titleLabel, new GridCell(0, 0).toConstraints());
            
            JPanel repertoirePane = new JPanel(new GridLayout(0, 1, 0, 10));
            
            for(JButton drillButton : drillButtons)
                repertoirePane.add(drillButton);
            
            JPanel repertoireTab = new JPanel(new BorderLayout());
            repertoireTab.setPreferredSize(new Dimension(700, 400));
            
            JScrollPane scrollPane = new JScrollPane(repertoirePane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getVerticalScrollBar().setUnitIncrement(5);
            repertoireTab.add(scrollPane);
            
            panel.add(repertoireTab, new GridCell(0, 1).setInsets(10).toConstraints());
            panel.add(logoutButton, new GridCell(0, 2).setInsets(10).toConstraints());
            return panel;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return new JPanel();
    }
    
    private void actionPerformed(ActionEvent e, Drillable drillable)
    {
        if(drillable.isCourse())
        {
            gui.switchScreen(new PlayableScreen(drillable));
        }
        else
        {
            DrillsterAPI api = DrillsterBot.getDrillsterAPI();
            Playable playable = api.getPlayable(drillable);
            
            if(playable == null || !playable.isCourse())
                gui.switchScreen(new RepertoireScreen());
            
            gui.switchScreen(new DoingDrillScreen(new HashSet<>(Collections.singletonList(playable))));
        }
    }
}
