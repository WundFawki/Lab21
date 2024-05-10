package Lab_21;

import javax.swing.*;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends JFrame {
    private final String SERVER_ADDR = "localhost";
    private final int SERVER_PORT = 8189;

    private JTextField msgInputField;
    private JTextArea chatArea;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public Client(){
        try{
            openConnection();
        }catch (IOException e){
            e.printStackTrace();
        }
        prepareGUI();
    }
    public void openConnection() throws IOException{
        socket = new Socket(SERVER_ADDR,SERVER_PORT);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    while (true){
                        String strFormServer = in.readUTF();
                        if(strFormServer.startsWith("/authok")){
                            break;
                        }
                        chatArea.append(strFormServer);
                        chatArea.append("\n");
                    }
                    while (true){
                        String strFormServer = in.readUTF();
                        if(strFormServer.startsWith("/end")){
                            break;
                        }
                        chatArea.append(strFormServer);
                        chatArea.append("\n");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }
    public void closeConnection(){
        try{
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

    public void sendMessage(){
        if(!msgInputField.getText().trim().isEmpty()){
            try {
                out.writeUTF(msgInputField.getText());
                msgInputField.setText("");
                msgInputField.grabFocus();
            }catch (IOException e){
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка отправки сообщения");
            }
        }
    }
    public void prepareGUI(){
        setBounds(600,300,500,500);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel botttomPanel = new JPanel(new BorderLayout());
        JButton btnSend = new JButton("Отправить");
        botttomPanel.add(btnSend,BorderLayout.EAST);
        msgInputField = new JTextField();
        add(botttomPanel, BorderLayout.SOUTH);
        botttomPanel.add(msgInputField,BorderLayout.CENTER);
        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        msgInputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    out.writeUTF("end");
                    closeConnection();
                }catch (IOException exc){
                    exc.printStackTrace();
                }
            }
        });

        setVisible(true);
    }
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Client();
            }
        });
    }
}
