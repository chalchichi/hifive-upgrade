package hifive;

public class Completed extends AbstractEvent {

    private Long id;
    private Long roomnumber;

    public Completed(){
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getRoomnumber() {
        return roomnumber;
    }

    public void setRoomnumber(Long roomnumber) {
        this.roomnumber = roomnumber;
    }
}
