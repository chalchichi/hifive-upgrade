package hifive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@RestController
public class CleanController {

 @Autowired
 RoomService roomService;

 @Autowired
 CleanRepository cleanRepository;

 @GetMapping("/MakeRoom/{roomnumber}")
 public List<Object> makeRoom(@PathVariable Long roomnumber)
 {
     Room room = new Room();
     room.setRoomStatus("EMPTY");
     room.setRoomNumber(roomnumber);
     String isadded = roomService.roomAdd(room);
     if(isadded.equals("OK"))
     {
         Clean clean = new Clean();
         clean.setIscleaned(true);
         clean.setRoomNumber(roomnumber);
         cleanRepository.save(clean);
         try {
             Thread.currentThread().sleep((long) (400 + Math.random() * 220));
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
         Made made = new Made();
         made.setRoomNumber(roomnumber);
         made.setEventType("Create");
         SimpleDateFormat format1 = new SimpleDateFormat( "HH:mm:ss");
         String format_time1 = format1.format (System.currentTimeMillis());
         made.setTimestamp(format_time1);
         List<Object> res = new ArrayList<>();
         res.add(room);
         res.add(clean);
         return res;
     }
     else
     {
         return null;
     }
 }

    @GetMapping("/complete/{roomnumber}")
    public String completeRoom(@PathVariable Long roomnumber)
    {
        Clean clean = cleanRepository.findByRoomNumber(roomnumber);
        clean.setTime(null);
        clean.setIscleaned(true);
        Completed completed = new Completed();
        completed.setRoomnumber(roomnumber);
        completed.publish();
        cleanRepository.save(clean);
        return "OK";
    }

    @DeleteMapping("/complete/{roomnumber}")
    public String DeleteRoom(@PathVariable Long roomnumber)
    {
        Clean clean = cleanRepository.findByRoomNumber(roomnumber);
        cleanRepository.delete(clean);
        Made made = new Made();
        made.setRoomNumber(roomnumber);
        made.setEventType("Delete");
        return "OK";
    }
}