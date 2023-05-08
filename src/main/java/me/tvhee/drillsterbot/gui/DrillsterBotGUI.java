package me.tvhee.drillsterbot.gui;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

public class DrillsterBotGUI
{
    private final JFrame frame;
    private JPanel lastScreen;
    
    public DrillsterBotGUI()
    {
        this.frame = new JFrame("DrillsterBot");
        this.create();
    }
    
    public void showMessage(String message, String title, Icon icon)
    {
        JOptionPane.showMessageDialog(lastScreen, message, title, icon.getCode());
    }
    
    public String getInput(String message)
    {
        return JOptionPane.showInputDialog(lastScreen, message);
    }
    
    public String getOption(String message, String title, Icon icon, String[] options)
    {
        int option = JOptionPane.showOptionDialog(lastScreen, message, title, JOptionPane.DEFAULT_OPTION, icon.getCode(), null, options, options[0]);
    
        if(option == JOptionPane.CLOSED_OPTION)
            return "";
        
        return options[option];
    }
    
    public void switchScreen(SimpleScreen newScreen)
    {
        if(this.lastScreen != null)
            this.frame.remove(lastScreen);
        
        JPanel panel = newScreen.create(this);
        
        this.frame.add(panel, BorderLayout.CENTER);
        this.frame.pack();
        
        if(this.lastScreen == null)
            this.frame.setLocationRelativeTo(null);
        
        this.lastScreen = panel;
    }
    
    public void refresh()
    {
        this.frame.pack();
    }
    
    public void dispose()
    {
        this.frame.dispose();
    }
    
    private void create()
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            List<Image> icons = new ArrayList<>();
            
            for(String size : new String[]{"16", "32", "64", "128"})
                icons.add(ImageIO.read(getClass().getClassLoader().getResource("icon-" + size + ".png")));
            
            this.frame.setResizable(false);
            this.frame.setIconImages(icons);
            this.frame.setLayout(new BorderLayout());
            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.frame.setPreferredSize(new Dimension(800, 600));
            this.frame.pack();
            this.frame.setVisible(true);
        }
        catch(Exception ignored)
        {
        }
    }
}
