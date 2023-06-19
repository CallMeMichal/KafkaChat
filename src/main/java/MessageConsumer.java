import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;



class MessageConsumer{
    //odopowiada za to zeby kafka wiedziala czy server przeczytal jaki status jest itd
    KafkaConsumer<String,String> kafkaConsumer;

    public MessageConsumer(String topic, String id) {
        kafkaConsumer = new KafkaConsumer<String, String>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                        ConsumerConfig.GROUP_ID_CONFIG, id,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,true
                )
        );

        //czytanie tematow
        kafkaConsumer.subscribe(Collections.singletonList(topic));


        //.poll przeczyatnie wiadomosci
        kafkaConsumer.poll(Duration.of(1, ChronoUnit.SECONDS)).forEach(cr->
        {
            System.out.println(id + ": " + cr.value());
        })
        ;
    }
}