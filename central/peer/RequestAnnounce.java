import java.util.*;


class RequestAnnounce{

  private String read;
  private String request;
  private List<String> files;
  private Command command;

  public RequestAnnounce(Command command) {
    this.command = command;
    files = new ArrayList<String>();
  }

  // Retourne la requête announce à envoyer sous forme de String
  public String buildRequest() {
    String req = "announce listen on port " + command.portNumberPeer + " seed " + "[";
    if(files.size() > 0){
      for (int i = 0; i < files.size() - 1; i++) {
          req += command.findFile(files.get(i), 1024);
          req += " ";
          command.addBuffermap(files.get(i));
      }
      req += command.findFile(files.get(files.size() - 1), 1024);
      command.addBuffermap(files.get(files.size() - 1));
    }
    req += "]\n";
    return req;
  }

  // Méthode pour la requête announce
  public void announce() {
    System.out.println("Entrez un fichier à envoyer (ou " + command.doneCmd + " pour terminer)");
    read = command.readInput();
    while (!read.equals(command.doneCmd)) {
      files.add(read);
      System.out.println("Entrez un fichier à envoyer (ou " + command.doneCmd + " pour terminer)");
      read = command.readInput();
    }

    // output contiendra la liste des fichiers du peer
    // ainsi que leur caractéristiques, danc cet ordre :
    // [$Filename1 $Length1 $Key1 $Filename2 $Length2 $Key2]
    StringBuffer output = new StringBuffer();

    // Assemblage de la requête finale
    // String request = "< announce listen on port " + portNumberPeer + " seed " +
    // "[ " + output.toString() + " ] \n";
    request = buildRequest();
    command.sendToTracker(request);
  }

}
