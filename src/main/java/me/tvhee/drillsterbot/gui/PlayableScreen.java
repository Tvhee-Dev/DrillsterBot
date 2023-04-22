package me.tvhee.drillsterbot.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import me.tvhee.drillsterbot.DrillsterAPI;
import me.tvhee.drillsterbot.DrillsterBot;
import me.tvhee.drillsterbot.drill.Drillable;
import me.tvhee.drillsterbot.drill.Playable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayableScreen implements SimpleScreen
{
    private final Drillable drillable;
    private DrillsterBotGUI gui;
    private JLabel titleLabel;
    
    public PlayableScreen(Drillable drillable)
    {
        this.drillable = drillable;
    }
    
    @Override
    public JPanel create(DrillsterBotGUI gui)
    {
        this.gui = gui;
        
        try
        {
            DrillsterAPI api = DrillsterBot.getDrillsterAPI();
            Playable playable = api.getPlayable(drillable);
            
            if(!playable.isCourse())
                return new JPanel();
            
            List<Playable> subPlayables = playable.getSubPlayables();
            List<JLabel> selectablePlayables = new ArrayList<>();
            Map<Playable, JCheckBox> checkedPlayables = new HashMap<>();
            
            this.titleLabel = new JLabel("Select Playables...");
            this.titleLabel.setFont(new Font(null, Font.PLAIN, 18));
            
            for(Playable subPlayable : subPlayables)
            {
                if(subPlayable.getProficiency() >= 100)
                    continue;
                
                JLabel label = new JLabel();
                label.setOpaque(true);
                label.setPreferredSize(new Dimension(600, 40));
                label.setLayout(new GridBagLayout());
                
                JCheckBox checkBox = new JCheckBox();
                checkBox.setOpaque(true);
                checkBox.setBackground(Color.white);
                
                JLabel buttonText = new JLabel(subPlayable.getName() + ", Percentage: " + subPlayable.getProficiency() + "%)");
                buttonText.setFont(new Font(null, Font.PLAIN, 18));
                
                label.add(checkBox, new GridCell(0, 0).setInsets(5).toConstraints());
                label.add(buttonText, new GridCell(1, 0).toConstraints());
                label.setBackground(Color.WHITE);
                
                selectablePlayables.add(label);
                checkedPlayables.put(subPlayable, checkBox);
            }
            
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
            panel.add(this.titleLabel, new GridCell(0, 0).toConstraints());
            
            JPanel playablePane = new JPanel(new GridLayout(0, 1, 0, 10));
            playablePane.setOpaque(true);
            playablePane.setBackground(Color.white);
            
            for(JLabel selectablePlayable : selectablePlayables)
                playablePane.add(selectablePlayable);
            
            JPanel playableTab = new JPanel(new BorderLayout());
            playableTab.setPreferredSize(new Dimension(700, 350));
            
            JScrollPane scrollPane = new JScrollPane(playablePane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getVerticalScrollBar().setUnitIncrement(10);
            playableTab.add(scrollPane);
            
            panel.add(playableTab, new GridCell(0, 1).setInsets(10).toConstraints());
            
            JButton playAll = new JButton("Play All");
            playAll.setFont(new Font(null, Font.PLAIN, 18));
            playAll.addActionListener((e) -> allActionPerformed(e, subPlayables));
            
            JButton playSelection = new JButton("Play Selection");
            playSelection.setFont(new Font(null, Font.PLAIN, 18));
            playSelection.addActionListener((e) -> selectionActionPerformed(e, checkedPlayables));
            
            panel.add(playAll, new GridCell(0, 2).setInsets(10).toConstraints());
            panel.add(playSelection, new GridCell(1, 2).setInsets(10).toConstraints());
            
            return panel;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return new JPanel();
    }
    
    private void selectionActionPerformed(ActionEvent e, Map<Playable, JCheckBox> checkedPlayables)
    {
        List<Playable> selectedPlayables = new ArrayList<>();
        
        for(Map.Entry<Playable, JCheckBox> playableEntry : checkedPlayables.entrySet())
        {
            if(playableEntry.getValue().isSelected())
                selectedPlayables.add(playableEntry.getKey());
        }
        
        this.gui.switchScreen(new DoingDrillScreen(selectedPlayables));
    }
    
    private void allActionPerformed(ActionEvent e, List<Playable> playables)
    {
        this.gui.switchScreen(new DoingDrillScreen(playables));
    }
}
