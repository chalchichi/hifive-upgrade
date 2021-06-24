package hifive;

import hifive.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PostPersist;
import java.util.Optional;

@Service
public class PolicyHandler{
    @Autowired PayRepository payRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverApplyCanceled_CancelPay(@Payload ApplyCanceled applyCanceled){

        if(!applyCanceled.validate()) return;
        
        System.out.println("\n\n##### listener CancelPay : " + applyCanceled.toJson() + "\n\n");
//        Long con_id = applyCanceled.getConferenceId();
//        Long pay_Id = applyCanceled.getPayId();
//        String con_status = applyCanceled.getConferenceStatus();

//        PayCanceled payCanceled = new PayCanceled();
//        payCanceled.setPayId(pay_Id);
//        payCanceled.setConferenceId(con_id);
//        payCanceled.setPayStatus(con_status);
//        payCanceled.publish();
        if (applyCanceled.getPayId() == 0) return;
        Optional<Pay> optionalPay = payRepository.findById(applyCanceled.getPayId());
        Pay pay = optionalPay.get();
        pay.setStatus("CANCELED");
        payRepository.delete(pay);
//        payRepository.deleteById(applyCanceled.getPayId());
//        entityManager.flush();
        
    }

}
