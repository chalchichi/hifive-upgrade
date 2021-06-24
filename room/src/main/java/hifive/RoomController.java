package hifive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

 @RestController
 public class RoomController {
  @Autowired
  RoomRepository roomRepository;

  @PostMapping("/addroom")
  public String addroom(Room room)
  {
   room.setRoomStatus("OK");
   roomRepository.save(room);
   return "OK";
  }

 }
