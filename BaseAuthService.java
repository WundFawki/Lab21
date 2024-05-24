package Lab_22;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class BaseAuthService implements AuthService{

    @Override
    public void start() {
        System.out.println("Сервис аутентификации запущен");
    }

    @Override
    public String getNickByLoginPass(String login, String pass) {
        String nick = null;
        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:D:\\sqlite\\new.db");
            PreparedStatement stmt = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND password = ?");
            stmt.setString(1,login);
            stmt.setString(2,pass);
            ResultSet resSet = stmt.executeQuery();
            while(resSet.next()){
                nick = resSet.getString("nickname");
            }
            connection.close();
        }catch ( SQLException|ClassNotFoundException e){
            e.printStackTrace();
        }
        return nick;
    }

    @Override
    public void stop() {
        System.out.println("Сервис аутентификации остановлен");
    }

    private static Connection connection;
    private static Statement stmt;
    private static ResultSet resSet;
    public BaseAuthService(){

    }
}
