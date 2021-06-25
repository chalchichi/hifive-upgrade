package hifive;

import hifive.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 회의실 현황 조회
 */
@Service
public class RoomStateViewHandler {


    @Autowired
    private RoomStateRepository roomStateRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenApplied_then_CREATE (@Payload Applied applied) {
        try {
            
            if (applied.validate()) {
                // view 객체 생성
                RoomState roomState = new RoomState();
                // view 객체에 이벤트의 Value 를 set 함
                roomState.setRoomNumber(applied.getRoomNumber());
                roomState.setRoomStatus(applied.getConferenceStatus());
                roomState.setConferenceId(applied.getConferenceId());

                // view 레파지 토리에 save
                roomStateRepository.save(roomState);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenAssigned_then_UPDATE(@Payload Assigned assigned) {
        try {
            if (assigned.validate()) {
                // view 객체 조회
                List<RoomState> roomStateList = roomStateRepository.findByRoomNumber(assigned.getRoomNumber());
                for(RoomState roomState : roomStateList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    // view 레파지 토리에 save
//                    roomState.setId(assigned.getPayId());
//                    roomState.setRoomNumber(assigned.getRoomNumber());
                    roomState.setRoomStatus(assigned.getRoomStatus());
                    roomState.setConferenceId(assigned.getConferenceId());
                    roomState.setPayId(assigned.getPayId());

                    roomStateRepository.save(roomState);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenCancelAssigned_then_UPDATE(@Payload CancelAssigned cancelAssigned) {
        try {
            if (cancelAssigned.validate()) {
                // view 객체 조회
                List<RoomState> roomStateList = roomStateRepository.findByRoomNumber(cancelAssigned.getRoomNumber());
                for(RoomState roomState : roomStateList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    // view 레파지 토리에 save 
//                    roomState.setId(cancelAssigned.getId());
                    roomState.setRoomStatus(cancelAssigned.getRoomStatus());
                    roomState.setConferenceId(cancelAssigned.getConferenceId());
                    roomState.setPayId(0L);
                    
                    roomStateRepository.save(roomState);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void whenPaid_then_UPDATE(@Payload Paid paid) {
        try {
            if (paid.validate()) {
                // view 객체 조회
                List<RoomState> roomStateList = roomStateRepository.findByRoomNumber(paid.getRoomNumber());
                for(RoomState roomState : roomStateList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    // view 레파지 토리에 save 
//                    roomState.setId(paid.getPayId());
                    roomState.setRoomStatus(paid.getPayStatus());
                    roomState.setConferenceId(paid.getConferenceId());

                    roomStateRepository.save(roomState);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCompleted_SendAlarm(@Payload Completed Completed){

        if(!Completed.validate()) return;

        try {
            if (Completed.validate()) {
                // view 객체 조회
                List<RoomState> roomStateList = roomStateRepository.findByRoomNumber(Completed.getRoomnumber());
                for(RoomState roomState : roomStateList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    // view 레파지 토리에 save
//                    roomState.setId(paid.getPayId());
                    roomStateRepository.save(roomState);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("\n\n##### listener SendAlarm : " + Completed.toJson() + "\n\n");

        // Sample Logic //

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverClean_SendAlarm(@Payload Made made){

        if(!made.validate()) return;

        try {
            if (made.validate()) {
                // view 객체 조회
                if(made.getEventType().equals("Create"))
                {
                    RoomState roomState = new RoomState();
                    roomState.setRoomStatus("EMPTY");
                    roomState.setRoomNumber(made.getRoomNumber());
                    roomStateRepository.save(roomState);
                }
                else
                {
                 List<RoomState> roomStates = roomStateRepository.findByRoomNumber(made.getRoomNumber());
                 for(RoomState roomState : roomStates)
                 {
                     roomStateRepository.delete(roomState);
                 }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("\n\n##### listener SendAlarm : " + Completed.toJson() + "\n\n");

        // Sample Logic //

    }
}