
class RequestGetfile{

    private String request;
    private String key;
    private Command command;

    public RequestGetfile(Command command, String key) {
        this.command = command;
        this.key = key;
    }

    // Retourne la requête announce à envoyer sous forme de String
    public String buildRequest() {
        return "getfile " + key + "\n";
    }

    // Méthode pour la requête getfile
    public String getfile() {
      request = buildRequest();
      return command.sendToTracker(request);
    }
}
