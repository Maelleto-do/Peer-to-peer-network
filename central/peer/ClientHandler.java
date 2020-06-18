import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

// ServerHandler class
class ClientHandler extends Thread {
  public Socket sock = null;
  public boolean available = true;
  public int portClient;
  public String IPclient;
  public boolean sending; // boolean to check if there is a message to be sent or is sending a message
  public String response;

  private String ToSend; // message to send
  private PrintWriter writer = null;
  private BufferedInputStream reader = null;
  private boolean socketRunning = false;
  private int port;
  private Command command;

  public ClientHandler(Command command, Socket psock) {
    this.command = command;
    sock = psock;
    available = true;
    socketRunning = false;
    ToSend = "";
    sending = false;
    port = command.portNumberPeer;
  }

  // Traitement de la connection lancé dans un thread spécial
  public void run(){
    available = false;
    socketRunning = true;

    // boucle infinie pour récupérer les messages ou les envoyer
    while(socketRunning){
      try{
        if(sending){ // mode "envoi de message"
          writer = new PrintWriter(sock.getOutputStream());
          reader = new BufferedInputStream(sock.getInputStream());
          writer.write(ToSend);
          writer.flush();
          System.out.println("< " + ToSend);
          String log = "< " + ToSend.substring(0, ToSend.length() - 1) + " sent to " + IPclient + ":" + portClient;
          writeLog(log);
          if(ToSend.equals("exit\n")){
            command.clients.remove(this);
            socketRunning = false;
          }
          do{}while(reader.available()<=0); // wait for response
          if(reader.available()>0){
            response = read();
            InetSocketAddress remote = (InetSocketAddress)sock.getRemoteSocketAddress();
            portClient = remote.getPort();
            IPclient = remote.getAddress().getHostAddress();
            log = "> " + response.substring(0, response.length() - 1) + " received from " + IPclient + ":" + portClient;
            writeLog(log);

            switch(response.split(" ")[0]){
              case "data":
                System.out.println("> downloading ...");
                String key = response.split(" ")[1];
                String[] pieces = command.splitPieces(response);
                int[] indexes = command.splitIndexes(response);
                command.writePieces(key,pieces,indexes,command.save_filename);
                break;

              default:
                System.out.println("> " + response);
                break;

            }
            sending = false;
            ToSend = "";
          }
        }
        else{  // vérifie si on a reçu un message
          writer = new PrintWriter(sock.getOutputStream());
          reader = new BufferedInputStream(sock.getInputStream());
          if(reader.available()>0){
            String request = read();
            InetSocketAddress remote = (InetSocketAddress)sock.getRemoteSocketAddress();
            portClient = remote.getPort();
            IPclient = remote.getAddress().getHostAddress();
            System.out.println("> " + request);
            String log = "> " + request.substring(0, request.length() - 1) + " received from " + IPclient + ":" + portClient;
            writeLog(log);
            String[] parsedRequest = request.split(" ");
            ToSend = "ok\n";

            String key = "";
            String buffermap = "";
            switch(parsedRequest[0]){ // traitement des cas en fonction de la requête reçue
              case "getpieces":
                System.out.println("commande getpieces reçue");
                key = parsedRequest[1];
                ToSend = "data " + key + " [ ";
                buffermap = command.buffermaps.get(key);
                if(buffermap == null){ // fichier pas sur disque
                  buffermap = command.buffermaps_leech.get(key);
                  if(buffermap == null){ // pas de buffermap
                    buffermap = "0";
                  }
                }
                for (int i = 3; i < parsedRequest.length - 1; i++) {
                  int index = Integer.parseInt(parsedRequest[i]);
                  String piece = command.getPiece(key, index, command.getPieceSize(), buffermap);
                  ToSend += index + ":" + piece;
                }
                ToSend += "]\n";
                System.out.println("< uploading ...");
                break;
              case "have":
                System.out.println("commande have reçue");
                key = parsedRequest[1];
                buffermap = command.buffermaps.get(key);
                if(buffermap == null){ // pas trouvé de buffermap correspondante
                  buffermap = command.buffermaps_leech.get(key);
                  if(buffermap == null){
                    buffermap = "";
                  }
                }
                ToSend = "have " + key + " " + buffermap + "\n";
                System.out.println("< " + ToSend);
                break;
              case "interested":
                System.out.println("commande interested reçue");
                key = parsedRequest[1];
                buffermap = command.buffermaps.get(key);
                if(buffermap == null){ // pas de buffermap correspondant à key
                  buffermap = command.buffermaps_leech.get(key);
                  if(buffermap == null){
                    System.out.println("Buffermap not found !");
                    buffermap = "0";
                  }
                }
                ToSend = "have " + key + " " + buffermap;
                System.out.println("< " + ToSend);
                break;
              default :
                if(request.equals("exit\n")){
                  ToSend= "OK\n";
                  command.clients.remove(this);
                  socketRunning = false;
                }
                else{
                  System.out.println("commande inconnue : " + request);
                  ToSend= "ok\n";
                }
                System.out.println("< " + ToSend);
                break;
            }
            writer.write(ToSend);
            writer.flush();
            log = "< " + ToSend.substring(0, ToSend.length() - 1) + " sent to " + IPclient + ":" + portClient;
            ToSend = "";
            writeLog(log);
          }
        }
      }
      catch(Exception e){
        System.out.println("connexion interrupted on socket " + sock);
        // e.printStackTrace();
        socketRunning = false;
        available = true;
      }
    }
  }

  // Méthode pour envoyer un message a l'élément connecté
  public void sendMessage(String message) {
    sending = true;
    ToSend = message;
  }

  // Méthode pour savoir si le message a terminé de s'envoyer
  public boolean getSending() {
    System.out.print("");
    return sending;
  }

  // Méthode pour obtenir la réponse d'un élément
  public String getResponse(){
    return response;
  }

  // Ecit la requête reçue dans le fichier log du peer avec plus d'infos
  private void writeLog(String msg) {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    FileWriter myWriter = null;
    LocalDateTime now;

    try {
      myWriter = new FileWriter("Logs/log" + port + ".txt", true);
    } catch (IOException e) {
      System.out.println("An error occurred while searching Logs/log" + port + ".txt");
      // e.printStackTrace();
    }

    try {
      now = LocalDateTime.now();
      myWriter.write("[" + dtf.format(now) + "] " + msg + "\n");
      myWriter.close();
    } catch (IOException e) {
      System.out.println("An error occurred while writing on Logs/log" + port + ".txt");
      // e.printStackTrace();
    }
  }

  // Retourne la réponse client sous forme de String
  private String read(){
    String response = "";
    int stream = 0;
    byte[] b = new byte[command.getMaxSize()];
    try{
      stream = reader.read(b);
    }
    catch(Exception e){
      System.out.println("Error while reading client response");
      // e.printStackTrace();
    }
    response = new String(b, 0, stream);
    return response;
  }
}
