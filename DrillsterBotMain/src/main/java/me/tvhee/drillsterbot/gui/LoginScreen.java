package me.tvhee.drillsterbot.gui;

import javax.imageio.ImageIO;
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class LoginScreen implements SimpleScreen
{
    private DrillsterBotGUI gui;
    
    @Override
    public JPanel create(DrillsterBotGUI gui)
    {
        this.gui = gui;
        
        try
        {
            JLabel welcomeLabel = new JLabel("Welcome to DrillsterBot!");
            welcomeLabel.setFont(new Font(null, Font.PLAIN, 18));
            
            Image image = ImageIO.read(getClass().getClassLoader().getResource("icon-128.png"));
            JLabel imageLabel = new JLabel(new ImageIcon(image));
            
            JButton browserButton = new JButton("Open Browser");
            browserButton.setFont(new Font(null, Font.PLAIN, 18));
            browserButton.addActionListener(this::browseButtonAction);
            
            JButton loginButton = new JButton("Check Login");
            loginButton.setFont(new Font(null, Font.PLAIN, 18));
            loginButton.addActionListener(this::loginButtonAction);
            
            JButton tokenInputButton = new JButton("Token Input");
            tokenInputButton.setFont(new Font(null, Font.PLAIN, 18));
            tokenInputButton.addActionListener(this::inputButtonAction);
            
            JPanel panel = new JPanel(new GridBagLayout());
            
            panel.setOpaque(true);
            panel.add(welcomeLabel, new GridCell(0, 0).setSize(1000, 1).toConstraints());
            panel.add(imageLabel, new GridCell(0, 1).setSize(1000, 1).toConstraints());
            panel.add(browserButton, new GridCell(0, 2).setInsets(10).toConstraints());
            panel.add(loginButton, new GridCell(1, 2).setInsets(10).toConstraints());
            panel.add(tokenInputButton, new GridCell(2, 2).setInsets(10).toConstraints());
            
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
    
    private void inputButtonAction(ActionEvent e)
    {
        String token = this.gui.getInput("Please put in your token!");
        DrillsterAPI drillsterAPI = new DrillsterAPI(token);
        
        DrillsterBot.setDrillsterAPI(drillsterAPI);
        this.gui.switchScreen(new RepertoireScreen());
    }
}
