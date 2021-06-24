package hifive;

import hifive.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Service
public class PolicyHandler{
    @Autowired CleanRepository cleanRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverAssigned_Assign(@Payload Assigned assigned){
        SimpleDateFormat format1 = new SimpleDateFormat( "H");

        String format_time1 = format1.format (System.currentTimeMillis());
        if(!assigned.validate()) return;
        Long roomNumber = assigned.getRoomNumber();
        Clean clean = cleanRepository.findByRoomNumber(roomNumber);
        clean.setIscleaned(false);
        clean.setTime(Integer.parseInt(format_time1));
        cleanRepository.save(clean);
        System.out.println("\n\n##### listener Assign : " + assigned.toJson() + "\n\n");
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
