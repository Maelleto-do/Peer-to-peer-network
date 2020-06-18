import java.util.*;

class RequestGetpieces{

    private String read;
    private String request;
    private String key;
    private Command command;
    private List<String> indexes;
    private int port;

    public RequestGetpieces(Command command, String key, List<String> indexes, int port) {
        this.command = command;
        this.key = key;
        this.indexes = indexes;
        this.port = port;
    }

    // Retourne la requête announce à envoyer sous forme de String
    public String buildRequest() {
        String req = "getpieces " + key + " [ ";
        for (int i = 0; i < indexes.size(); i++) {
            req += indexes.get(i);
            req += " ";
        }
        req += "]\n";
        return req;
    }

    // Méthode pour la requête getpieces
    public void getpieces() {
        request = buildRequest();
        command.sendToPeer(request,"localhost",port);
    }
}
