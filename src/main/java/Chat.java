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
//    private final boolean loggedIn = false;
//    private String loggedInUserName;
    private final MessageConsumer messageConsumer;
    private final JTextField welcomeTextField;

    public Chat(String id, String topic) throws HeadlessException {

        message.setEnabled(false);
        sendButton.setEnabled(false);
        chatView.setEnabled(false);
        messageConsumer = new MessageConsumer(topic, id);
        welcomeTextField = new JTextField();
        loginField.setText("Login");
        passwordField.setText("Password");
        message.setText("Your message here!");
        this.add(mainPanel);
        this.setVisible(true);
        this.setTitle(id);
        this.pack();
        //przetwarzanie kazdej wiadomosci
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                messageConsumer.kafkaConsumer.poll(Duration.of(1, ChronoUnit.SECONDS)).forEach(
                        m -> {
                            System.out.println(m);
                            chatView.append(m.value() + System.lineSeparator());
                        }
                );
            }
        });
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                MessageProducer.send(new ProducerRecord<>(topic, currentTime + " : " + id + ": " + message.getText()));
                message.setText("");



            }
        });
        logOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                JFrame currentFrame = (JFrame) SwingUtilities.getWindowAncestor(logOutButton);
                currentFrame.dispose();
                String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                MessageProducer.send(new ProducerRecord<>(topic, currentTime ,"User "+id+" left chat"));
            }
        });
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String userName = loginField.getText();
                String password = passwordField.getText();

                if (userName.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(Chat.this, "Please enter username and password.", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    String result = getUser(userName, password);
                    if (result.startsWith("Znaleziono użytkownika.")) {
                        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                        MessageProducer.send(new ProducerRecord<>(topic, currentTime ,"User "+id+" joined the chat"));
                        loginButton.setEnabled(false);
                        message.setEnabled(true);
                        sendButton.setEnabled(true);
                        chatView.setEnabled(true);

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


}
