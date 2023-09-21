package me.tvhee.drillsterbot.gui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import me.tvhee.drillsterbot.DrillsterAPI;
import me.tvhee.drillsterbot.DrillsterBot;
import me.tvhee.simplesockets.connection.SocketConnection;
import me.tvhee.simplesockets.socket.Socket;
import me.tvhee.simplesockets.socket.SocketHandler;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;

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
            welcomeLabel.setFont(new Font(null, Font.PLAIN, 30));
            
            Image image = ImageIO.read(getClass().getClassLoader().getResource("icon-128.png"));
            JLabel imageLabel = new JLabel(new ImageIcon(image));
            
            JButton loginButton = new JButton("Login");
            loginButton.setFont(new Font(null, Font.PLAIN, 30));
            loginButton.addActionListener(this::loginButtonAction);
            
            JPanel panel = new JPanel(new GridBagLayout());
            
            panel.setOpaque(true);
            panel.add(welcomeLabel, new GridCell(0, 0).toConstraints());
            panel.add(imageLabel, new GridCell(0, 1).toConstraints());
            panel.add(loginButton, new GridCell(0, 2).setInsets(10).toConstraints());
            
            return panel;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return new JPanel();
    }
    
    private void loginButtonAction(ActionEvent e)
    {
        //this.gui.showMessage("Cannot find login token, are you logged in? If so, please wait about 15 seconds before trying again.",
        //                "No token found!", Icon.ERROR_MESSAGE);
        
        SocketConnection socketConnection = SocketConnection.serverConnection(7654);
        
        socketConnection.start();
        socketConnection.addHandler(new SocketHandler()
        {
            @Override
            public void handle(Socket socket, String message)
            {
                if(message.equals("POST / HTTP/1.1"))
                {
                    String response = "Succes!";
                    
                    socket.sendMessage("HTTP/1.1 200 OK");
                    socket.sendMessage("Content-Type: text/plain");
                    socket.sendMessage("Access-Control-Allow-Origin: *"); // Allow requests from any origin
                    socket.sendMessage("Content-Length: " + response.length());
                    socket.sendMessage(""); // Blank line to separate headers from body
                    socket.sendMessage(response);
                }
                
                if(!message.startsWith("token#"))
                    return;
                
                socketConnection.close();
                
                String token = message.replaceAll("token#", "");
                DrillsterAPI drillsterAPI = new DrillsterAPI(token);
                
                DrillsterBot.getStorage().saveToken(token);
                DrillsterBot.getStorage().saveFile();
                
                DrillsterBot.setDrillsterAPI(drillsterAPI);
                gui.switchScreen(new RepertoireScreen());
            }
        });
    }
}
