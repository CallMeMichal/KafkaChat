import org.apache.kafka.clients.producer.ProducerRecord;

import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;

public class Chat extends JFrame {
    private JTextArea chatView;
    private JPanel mainPanel;
    private JTextField message;
    private JButton sendButton;
    private JButton loginButton;
    private JTextField passwordField;
    private JTextField loginField;
    private JButton logOutButton;
    private MessageConsumer messageConsumer;
    private final JTextField welcomeTextField;
    public String userName ="";

    public boolean created=false;
    public String lastUser="";
    public boolean userLogged = true;
    public Chat(String topic) throws HeadlessException {

        message.setEnabled(false);
        sendButton.setEnabled(false);
        chatView.setEnabled(false);

        welcomeTextField = new JTextField();
        loginField.setText("Login");
        passwordField.setText("Password");
        message.setText("Your message here!");
        this.add(mainPanel);
        this.setVisible(true);
        this.pack();
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        //przetwarzanie kazdej wiadomosci

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                MessageProducer.send(new ProducerRecord<>(topic, currentTime + " : " + userName + ": " + message.getText()));
                message.setText("");



            }
        });
        logOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                MessageProducer.send(new ProducerRecord<>(topic, currentTime ,"User "+userName+" left chat"));
                loginButton.setEnabled(true);
                logOutButton.setEnabled(false);
                userLogged = false;
            }
        });
        loginButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                userName = loginField.getText();
                String password = passwordField.getText();
                logOutButton.setEnabled(true);
                if (userName.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(Chat.this, "Please enter username and password.", "Error", JOptionPane.ERROR_MESSAGE);
                }else {
                    loginButton.setEnabled(false);
                    message.setEnabled(true);
                    sendButton.setEnabled(true);
                    chatView.setEnabled(true);
                    setTitle(userName);
                    if(created) {

                        if (userName.equals(lastUser)) {
                            userLogged=true;
                            ramExec();

                        } else {
                            String result = getUser(userName, password);
                            if (result.startsWith("Znaleziono użytkownika.")) {
                                String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                                userLogged = true;
                                messageConsumer = new MessageConsumer(topic, userName);
                                ramExec();
                                MessageProducer.send(new ProducerRecord<>(topic, currentTime, "User " + userName + " joined the chat"));
                                lastUser=userName;
                            }
                        }
                    }
                    else{
                        String result = getUser(userName, password);
                        if (result.startsWith("Znaleziono użytkownika.")) {
                            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                            userLogged = true;
                            messageConsumer = new MessageConsumer(topic, userName);
                            ramExec();
                            MessageProducer.send(new ProducerRecord<>(topic, currentTime, "User " + userName + " joined the chat"));
                            lastUser=userName;
                            created=true;
                        }
                    }
                }
            }


        });
    }


    public String getUser(String name, String password) {
        String result = "";

        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/books", "michal", "zaq1@WSX");
            String query = "SELECT * FROM uzytkownik WHERE name = ? AND password = ?";
            stmt = con.prepareStatement(query);
            stmt.setString(1, name);
            stmt.setString(2, password);
            rs = stmt.executeQuery();
            if (rs.next()) {

                String userName = rs.getString("name");

                result = "Znaleziono użytkownika. ID: " + userName;
                welcomeTextField.setText("Witaj, " + name + "!");
                JOptionPane.showMessageDialog(Chat.this, welcomeTextField, "Witaj!", JOptionPane.PLAIN_MESSAGE);
            } else {
                result = "Użytkownik nie istnieje.";
                welcomeTextField.setText(result);
                JOptionPane.showMessageDialog(Chat.this, welcomeTextField, "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException | ClassNotFoundException e) {

            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public void ramExec(){
        Executors.newSingleThreadExecutor().submit(() -> {
            while (userLogged) {
                messageConsumer.kafkaConsumer.poll(Duration.of(1, ChronoUnit.SECONDS)).forEach(
                        m -> {
                            System.out.println(m);
                            chatView.append(m.value() + System.lineSeparator());
                        }
                );

            }


        });
    }

}
