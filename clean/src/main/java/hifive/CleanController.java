package hifive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
public class CleanController {

 @Autowired
 RoomService roomService;

 @Autowired
 CleanRepository cleanRepository;
 @GetMapping("/MakeRoom/{roomnumber}")
 public void makeRoom(@PathVariable Long roomnumber)
 {
     Room room = new Room();
     room.setRoomNumber(roomnumber);
     roomService.roomAdd(room);
     Clean clean = new Clean();
     clean.setIscleaned(true);
     clean.setRoomNumber(roomnumber);
     cleanRepository.save(clean);
 }

    @GetMapping("/complete/{roomnumber}")
    public void completeRoom(@PathVariable Long roomnumber)
    {
        Clean clean = cleanRepository.findByRoomNumber(roomnumber);
        clean.setTime(null);
        clean.setIscleaned(true);
        Completed completed = new Completed();
        completed.setRoomnumber(roomnumber);
        completed.publish();
        cleanRepository.save(clean);
    }
}