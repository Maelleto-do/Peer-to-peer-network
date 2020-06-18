import java.util.*;

class RequestLook{


    private String read;
    private String request;
    private Command command;
    private String filename;
    private String filesize;
    private List<String> criterions;

    public RequestLook(Command command, String filename, String filesize) {
        this.command = command;
        this.filename = "filename=\""+filename+"\"";
        this.filesize = "filesize>\""+filesize+"\"";
        this.criterions = new ArrayList<String>();;
    }

    // Retourne la requête look sous forme de String
    String buildRequest() {
        String req = "look [";
        for (int i = 0; i < criterions.size(); i++) {
          if (i>0){
            req += " ";
          }
          req += criterions.get(i);
        }
        req += "]\n";
        return req;
    }

    // Méthode pour la requête Look
    public String look() {
        criterions.add(filename);
        criterions.add(filesize);
        request = buildRequest();
        return command.sendToTracker(request);
    }
}
