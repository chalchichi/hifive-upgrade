package hifive;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="room", url="${api.url.room}")
public interface RoomService {
    @RequestMapping(method= RequestMethod.POST, path="/rooms/addroom")
    public String roomAdd(@RequestBody Room room);

}