package Lab_21;

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
                else if(strFromClient.equals("/end")){
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
