package me.tvhee.drillsterbot.gui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.io.IOException;

public class UpdatingGUI implements SimpleScreen
{
    @Override
    public JPanel create(DrillsterBotGUI gui)
    {
        try
        {
            JLabel welcomeLabel = new JLabel("DrillsterBot is updating to a newer version, please wait!");
            welcomeLabel.setFont(new Font(null, Font.PLAIN, 18));
            
            Image image = ImageIO.read(getClass().getClassLoader().getResource("update.png"));
            JLabel imageLabel = new JLabel(new ImageIcon(image));
            
            JPanel panel = new JPanel(new GridBagLayout());
            
            panel.setOpaque(true);
            panel.add(welcomeLabel, new GridCell(0, 0).toConstraints());
            panel.add(imageLabel, new GridCell(0, 1).toConstraints());
            
            return panel;
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return new JPanel();
        }
    }
}
