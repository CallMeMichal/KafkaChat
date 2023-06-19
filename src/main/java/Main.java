import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import javax.swing.*;

//@Slf4j
public class Main {
    public static void main(String[] args) {

        //uruchamia kafke na porcie 9092
        EmbeddedKafkaBroker embeddedKafkaBroker = new EmbeddedKafkaBroker(1)
                .kafkaPorts(9092);

        //zatrzymanie kodu do poki kafka sie nie uruchomi
        embeddedKafkaBroker.afterPropertiesSet();


        SwingUtilities.invokeLater(()->new Chat("Kinga","chat"));
        SwingUtilities.invokeLater(()->new Chat("Jakub","chat"));



    }
}



