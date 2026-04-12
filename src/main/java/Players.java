public class Players {
    public enum Status {ONLINE, OFFLINE, IN_GAME, READY_TO_PLAY}

    private Status status;
    private Server.ClientThread clientThread;

    public Players(Server.ClientThread clientThread) {
        this.clientThread = clientThread;
        this.status = Status.READY_TO_PLAY;
    }

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public Server.ClientThread getClientThread() {
        return clientThread;
    }
}
