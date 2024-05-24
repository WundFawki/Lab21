package Lab_22;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.*;

public class ClientHandler{
        private MyServer myServer;
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;

        private String name;
        public String getName() {
            return name;
        }
        public ClientHandler(MyServer myServer,Socket socket){
            try{
                this.myServer = myServer;
                this.socket = socket;
                this.in = new DataInputStream(socket.getInputStream());
                this.out = new DataOutputStream(socket.getOutputStream());
                this.name = "";
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            authentication();
                            readMessages();
                        }catch (IOException e){
                            e.printStackTrace();
                        }finally {
                            closeConnection();
                        }
                    }
                }).start();
            }catch (IOException e){
                throw new RuntimeException("Проблемы при создании обработчика клиента");
            }
        }
        public void authentication() throws IOException{
            while(true){
                String str = in.readUTF();
                if(str.startsWith("/auth")){
                    String[] parts = str.split("\\s");
                    String nick = myServer.getAuthService().getNickByLoginPass(parts[1],parts[2]);
                    if(nick != null){
                        if(!myServer.isNickBusy(nick)){
                            sendMsg("/authok " + nick);
                            name = nick;
                            myServer.broadcastMsg(name + " зашел в чат");
                            myServer.subscribe(this);
                            return;
                        }else {
                            sendMsg("Учетная запись уже используется");
                        }
                    }else {
                        sendMsg("Неверные логин/пароль");
                    }
                }
            }
        }

        public void readMessages() throws IOException{
            while(true){
                String strFromClient = in.readUTF();
                if(strFromClient.startsWith("/w")){
                    String[] parts = strFromClient.split(" ");
                    // /w parts[0]
                    // message parts[1]
                    myServer.broadcastPrivateMsg(parts[1],parts[2], name);
                }
                else if(strFromClient.startsWith("/changenick")){
                    String[] parts = strFromClient.split(" ");
                    try {
                        Class.forName("org.sqlite.JDBC");
                        Connection connection = DriverManager.getConnection("jdbc:sqlite:D:\\sqlite\\new.db");
                        PreparedStatement stmt = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?");
                        stmt.setString(1,parts[1]);
                        stmt.setString(2,name);
                        stmt.executeUpdate();
                        name = parts[1];
                        connection.close();
                    }catch (SQLException | ClassNotFoundException e){
                        e.printStackTrace();
                    }
                }
                else if(strFromClient.startsWith("/adduser") && name.equals("admin")){
                    String[] parts = strFromClient.split(" ");
                    try {
                        Class.forName("org.sqlite.JDBC");
                        Connection connection = DriverManager.getConnection("jdbc:sqlite:D:\\sqlite\\new.db");
                        PreparedStatement stmt = connection.prepareStatement("INSERT INTO users(login,password,nickname) VALUES(?,?,?)");
                        stmt.setString(1,parts[1]);
                        stmt.setString(2,parts[2]);
                        stmt.setString(3,parts[3]);
                        stmt.executeUpdate();
                        connection.close();
                    }catch (SQLException | ClassNotFoundException e){
                        e.printStackTrace();
                    }
                }else if(strFromClient.startsWith("/deluser") && name.equals("admin")){
                    String[] parts = strFromClient.split(" ");
                    if(myServer.isNickBusy(parts[1])){
                            ClientHandler o = myServer.clientFromNick(parts[1]);
                            o.closeConnection();
                    }
                    try {
                        Class.forName("org.sqlite.JDBC");
                        Connection connection = DriverManager.getConnection("jdbc:sqlite:D:\\sqlite\\new.db");
                        PreparedStatement stmt = connection.prepareStatement("DELETE FROM users WHERE nickname = ?");
                        stmt.setString(1,parts[1]);
                        stmt.executeUpdate();
                        connection.close();
                    }catch (SQLException | ClassNotFoundException e){
                        e.printStackTrace();
                    }
                }
                else if(strFromClient.equals("/end")){
                    closeConnection();
                    return;
                }
                else {
                    System.out.println("от " + name + ": " + strFromClient);
                    myServer.broadcastMsg(name + ": " + strFromClient);
                }
            }
        }
        public void sendMsg(String msg){
            try {
                out.writeUTF(msg);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        public void closeConnection(){
            myServer.unsubscribe(this);
            myServer.broadcastMsg(name + " вышел из чата");
            try {
                in.close();
            }catch (IOException e){
                e.printStackTrace();
            }
            try {
                out.close();
            }catch (IOException e){
                e.printStackTrace();
            }
            try {
                socket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
}
