
class RequestHave{

    private String request;
    private String key;
    private String buffermap;
    private String IPPeer;
    private int portPeer;
    private Command command;

    public RequestHave(String key, String buffermap, String IPPeer, int portPeer, Command command) {
        this.key = key;
        this.buffermap = buffermap;
        this.IPPeer = IPPeer;
        this.portPeer = portPeer;
        this.command = command;
    }

    // Retourne la requête look sous forme de String
    String buildRequest(String key, String buffermap) {
        return "have " + key + " " + buffermap + "\n";
    }

    // Méthode pour la requête Have
    public void have() {
      request = buildRequest(this.key,this.buffermap);
      command.sendToPeer(this.request, this.IPPeer, this.portPeer);
    }
}
