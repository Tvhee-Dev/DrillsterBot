package me.tvhee.drillsterbot.gui;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import me.tvhee.drillsterbot.DrillsterAPI;
import me.tvhee.drillsterbot.DrillsterBot;
import me.tvhee.drillsterbot.cookie.Cookie;
import me.tvhee.drillsterbot.cookie.CookieManager;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class LoginScreen implements SimpleScreen
{
    private DrillsterBotGUI gui;
    private JLabel welcomeLabel;
    private JLabel imageLabel;
    private JButton browserButton;
    private JButton loginButton;
    
    @Override
    public JPanel create(DrillsterBotGUI gui)
    {
        this.gui = gui;
        
        try
        {
            this.welcomeLabel = new JLabel("Welcome to DrillsterBot!");
            this.welcomeLabel.setFont(new Font(null, Font.PLAIN, 18));
            
            Image image = ImageIO.read(new FileInputStream("src/main/resources/icon-128.png"));
            this.imageLabel = new JLabel(new ImageIcon(image));
            
            this.browserButton = new JButton("Open Browser");
            this.browserButton.setFont(new Font(null, Font.PLAIN, 18));
            this.browserButton.addActionListener(this::browseButtonAction);
            
            this.loginButton = new JButton("Check Login");
            this.loginButton.setFont(new Font(null, Font.PLAIN, 18));
            this.loginButton.addActionListener(this::loginButtonAction);
            
            JPanel panel = new JPanel(new GridBagLayout());
            
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
            panel.add(this.welcomeLabel, new GridCell(0, 0).setSize(1000, 1).toConstraints());
            panel.add(this.imageLabel, new GridCell(0, 1).setSize(1000, 1).toConstraints());
            panel.add(this.browserButton, new GridCell(0, 2).setInsets(10).toConstraints());
            panel.add(this.loginButton, new GridCell(1, 2).setInsets(10).toConstraints());
            
            return panel;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return new JPanel();
    }
    
    private void browseButtonAction(ActionEvent e)
    {
        try
        {
            Desktop.getDesktop().browse(new URI("https://www.drillster.com/daas/identify"));
        }
        catch(IOException | URISyntaxException ex)
        {
            ex.printStackTrace();
        }
    }
    
    private void loginButtonAction(ActionEvent e)
    {
        CookieManager cookieManager = new CookieManager();
        Set<Cookie> cookies = cookieManager.getCookiesForDomain("drillster.com");
        
        for(Cookie cookie : cookies)
        {
            if(cookie.getName().equals("stroop"))
            {
                if(!cookie.isDecrypted())
                {
                    this.gui.showMessage("Cannot decrypt token!", "Cannot decrypt token!", Icon.ERROR_MESSAGE);
                    return;
                }
                
                String token = new String(cookie.getData());
                DrillsterAPI drillsterAPI = new DrillsterAPI(token);
                
                DrillsterBot.setDrillsterAPI(drillsterAPI);
                this.gui.switchScreen(new RepertoireScreen());
                return;
            }
        }
        
        this.gui.showMessage("Cannot find login token, are you logged in? If so, please wait about 15 seconds before trying again.",
                "No token found!", Icon.ERROR_MESSAGE);
    }
}
