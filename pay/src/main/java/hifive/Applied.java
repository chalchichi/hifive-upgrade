package hifive;

public class Applied extends AbstractEvent {

    private Long conferenceId;
    private String conferenceStatus;
    private Long roomNumber;

    public Long getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(Long conferenceId) {
        this.conferenceId = conferenceId;
    }
    public String getConferenceStatus() {
        return conferenceStatus;
    }

    public void setConferenceStatus(String conferenceStatus) {
        this.conferenceStatus = conferenceStatus;
    }
    public Long getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(Long roomNumber) {
        this.roomNumber = roomNumber;
    }
}