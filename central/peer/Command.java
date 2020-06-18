import java.util.*;
import java.io.File; // Import the File class
import java.io.IOException; // Import the IOException class to handle errors
import java.io.RandomAccessFile;
import java.net.*;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Command {

  private int numberOfThreads = 50; // nombre de threads (clients et serveur)
  private ArrayList<String> peerData = new ArrayList<>();
  private Map<String, String[]> DLPieces = new HashMap<String, String[]>();


  public int portNumberPeer;
  public String save_filename;
  public ArrayList<ClientHandler> clients = new ArrayList<>();
  public ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads); // Pool de 50 threads
  public Map<String, String> buffermaps = new HashMap<String, String>();
  public Map<String, String> buffermaps_leech = new HashMap<String, String>();
  public Map<String, String> fileNames = new HashMap<String, String>();
  public static String doneCmd = "DONE"; // commande à entrer pour l'utilisateur quand il a fini d'entrer des paramètres

  public Command(int portNumberPeer) {
    this.portNumberPeer = portNumberPeer;
  }

  ///////////////////////////////////////// Fonctions de parsing de config //////////////////////////////////////////////////////
  // Initialise le tableau des données config à l'exécution du Peer
  public void initPeerData() {
    try {
      File file = new File("Config/config_peer" + portNumberPeer + ".ini");
      FileReader fr = new FileReader(file);
      BufferedReader br = new BufferedReader(fr);
      String line;
      while ((line = br.readLine()) != null) {
        peerData.add(line);
      }
      br.close();
      fr.close();
    } catch (Exception e) {
      System.out.println("Config/config_peer" + portNumberPeer + ".ini not found !");
      // e.printStackTrace();
      System.exit(0);
    }
    numberOfThreads = getPeerMaxConnections();
  }

  // Renvoie le nombre max de peers pouvant être connectés
  public int getPeerMaxConnections() {
    String retString = peerData.get(1).substring("peer-max-connexion = ".length());
    return Integer.parseInt(retString);
  }

  // Renvoie la taille maximale d'un message pour le peer
  public int getMaxSize() {
    String retString = peerData.get(2).substring("peer-message-max-size = ".length());
    return Integer.parseInt(retString);
  }

  // Renvoie le temps pour les timers pour have/update
  public int getTimeout() {
    String retString = peerData.get(3).substring("peer-update = ".length());
    return Integer.parseInt(retString);
  }

  // Renvoie l'adresse IP du tracker sous forme de String à partir de config.txt
  public String getTrackerIP() {
    String ret = peerData.get(4).substring("tracker-hostname =  ".length()); // tracker-hostname
    return ret;
  }

  // Renvoie le port du tracker sous forme de int à partir de config.txt
  public int getTrackerPort() {
    int ret = 0;
    String retString = peerData.get(5).substring("tracker-port =  ".length()); // tracker-port
    ret = Integer.parseInt(retString);
    return ret;
  }

  // Renvoie la taille d'une piece pour séparer un fichier
  public int getPieceSize() {
    String retString = peerData.get(6).substring("piece-size =  ".length());
    return Integer.parseInt(retString);
  }

  //////////////////////////////////// Fonctions diverses utiles ///////////////////////////////////////////////////

  // Renvoie la chaine de caracteres entree par l'utilisateur
  public String readInput() {
    Scanner scan = new Scanner(System.in);
    String s = scan.next();
    System.out.println("");
    return s;
  }

  // Renvoie la clé md5 d'un fichier
  public String getmd5(String filename){
    String res = "";
    try {
      String[] cmd = {"/bin/sh","-c","md5sum " + filename + " | cut -d\" \" -f1"};
      Process p = Runtime.getRuntime().exec(cmd);
      StringBuilder output = new StringBuilder();
      BufferedReader reader = new BufferedReader(
      new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line);
      }
      int exitVal = p.waitFor();
      if (exitVal == 0) {
        res = output.toString();
      }
      else {
        System.out.println("Error in process of generating md5 hash");
      }
    }
    catch (Exception e) {
      System.out.println("Error while trying to generate md5 hash");
      // e.printStackTrace();
    }
    return res;
  }

  // Retourne un ClientHandler déjà connecté à IP:port ou en crée un nouveau dans
  // un thread séparé si il n'y en a pas
  private ClientHandler findThread(String IP, int port) {
    ClientHandler clientThread = null;
    boolean exists = false;
    String inetIP = null;
    try {
      inetIP = InetAddress.getByName(IP).toString();
    } catch (Exception e) {
      System.out.println("Error in findThread : cannot convert " + IP + "to InetAddress");
    }

    for (int i = 0; i < clients.size(); i++) {
      ClientHandler thread = clients.get(i);
      if (thread.sock.getInetAddress().toString().equals(inetIP) && thread.sock.getPort() == port) {
        clientThread = thread;
        exists = true;
        break;
      }
    }
    if (!exists) {
      Socket psock = null;
      try {
        psock = new Socket(IP, port);
        System.out.println("creating new thread on socket" + psock);
      } catch (Exception e) {
        System.out.println("Error in findThread : cannot open new socket on " + IP + ":" + port);
      }
      clientThread = new ClientHandler(this, psock);
      clients.add(clientThread);
      executor.execute(clientThread);
    }

    return clientThread;
  }

  // Renvoie une chaine de longueur length contenant uniquement des 1
  private String fillWith1(long length) {
    String outputBuffer = "";
    for (long i = 0; i < length; i++) {
      outputBuffer += 1;
    }
    return outputBuffer;
  }

  /////////////////////////////// Fonctions pour envoyer des messages //////////////////////////////////////////////

  // Envoie la requête au tracker
  public String sendToTracker(String request) {
    ClientHandler thread = findThread(getTrackerIP(), getTrackerPort());
    thread.sendMessage(request);
    do {} while (thread.getSending()); // wait until done sending and got response
    return thread.getResponse();
  }

  // Envoie la requête au Peer
  public String sendToPeer(String request, String IP, int port) {
    ClientHandler thread = findThread(IP, port);
    thread.sendMessage(request);
    do {} while (thread.getSending()); // wait until done sending and got response
    return thread.getResponse();
  }

  ///////////////////////////////////// Fonctions pour Announce ////////////////////////////////////////////////////

  // Ajoute une correspondance clé/filename
  public void addFileName(String key, String filename) {
    fileNames.put(key, filename);
  }

  // Ajoute un buffermap plein pour le fichier filename
  public void addBuffermap(String fileName) {
    File f = new File(fileName);
    if (f.exists()) {
      String key = getmd5(fileName);

      addFileName(key, fileName);

      int pieceSize = getPieceSize();
      long fileLength = f.length();
      long nbOfPieces = 0;
      if (fileLength % pieceSize == 0) {
        nbOfPieces = fileLength / pieceSize;
      } else {
        nbOfPieces = 1 + fileLength / pieceSize; // cas où il reste un peu mais pas un morceau entier
      }
      String buffermap = fillWith1(nbOfPieces);
      buffermaps.put(key, buffermap);
    }
  }

  // Retourne les informations d'un fichier en tant que String : <nom> <taille>
  // <taille du segment> <cle MD5>
  public String findFile(String fileName, int pieceSize){
    String file = "";
    try{
      File f = new File(fileName);
      if (f.exists()) {
        String key = getmd5(fileName);
        file = fileName + " " + f.length() + " " + pieceSize + " " + key;
      }
    }
    catch(Exception e){
      System.out.println("Error in findFile : cannot read in " + fileName);
      // e.printStackTrace();
    }
    return file;
  }

  /////////////////////////////////////// Fonctions pour Update ///////////////////////////////////////////////////////

  // Récupère la liste des Index à partir des morceaux reçus
  public int[] splitIndexes(String totalRequest){
    List<Integer> indexes = new ArrayList<Integer>();
    int pieceSize = getPieceSize();
    String message = totalRequest.substring(totalRequest.indexOf('[') + 1,totalRequest.length() - 2);
    for (int i = 1; i<message.length(); i += pieceSize+2) {
      String cIndex = message.substring(i,i+1);
      indexes.add(Integer.parseInt(cIndex));
    }
    int[] res = new int[indexes.size()];
    for (int i = 0; i < indexes.size(); i++) {
      res[i] = indexes.get(i);
    }
    return res;
  }

  // Récupère la liste morceaux reçus
  public String[] splitPieces(String totalRequest) {
    List<String> pieces = new ArrayList<String>();
    int pieceSize = getPieceSize();
    String message = totalRequest.substring(totalRequest.indexOf('[') + 1,totalRequest.length() - 1);
    for (int i = 3; i<message.length(); i += pieceSize+2) {
      if(i+pieceSize >= message.length()){
        pieces.add(message.substring(i,message.length()-1));
      }
      else{
        pieces.add(message.substring(i,i+pieceSize));
      }
    }
    String[] res = new String[pieces.size()];
    for (int i = 0; i < pieces.size(); i++) {
      res[i] = pieces.get(i);
    }
    return res;
  }

  // returns a part of a file as a String
  public String getPiece(String key, int part, int pieceSize, String buffermap) {
    String piece = "";
    int offset = 0;
    if(part <= buffermap.length()){
      for (int i = 0; i < part; i++) {
        if (buffermap.charAt(i) == '1') {
          offset += pieceSize;
        }
      }
      String filename = fileNames.get(key);
      if(filename != null){ // on a le fichier physiquement
        File f = new File(filename);
        BufferedReader br = null;
        try {
          br = new BufferedReader(new FileReader(f));
          br.skip(offset);
          for (int i = 0; i < pieceSize; i++) {
            int c = br.read();
            if (c != -1) {
              piece += (char) c;
            } else { // atteint la fin du fichier
              break;
            }
          }
        } catch (Exception e) {
          System.out.println("error trying reading file " + filename + " in getPiece");
          // e.printStackTrace();
          return "";
        }
      }
      else{ // on a pas le fichier physiquement
        String[] pieces = DLPieces.get(key);
        if (pieces != null){
          piece = pieces[part];
        }
      }
    }

    return piece;
  }

  // Mémorise le morceau dans le tableau DLPieces
  private void writePiece(String key, String piece, int index) {
    String buffermap = buffermaps_leech.get(key);
    int pieceSize = getPieceSize();
    for (int i = 0; i < index; i++) {
      if (buffermap.length() == i) {
        buffermap += '0';
      }
    }
    String[] new_pieces = DLPieces.get(key);
    if (new_pieces == null) { // premier morceau du fichier
      new_pieces = new String[index + 1];
      new_pieces[index] = piece;
      DLPieces.put(key, new_pieces);
    } else if (new_pieces.length <= index) { // not enough space
      new_pieces = Arrays.copyOf(new_pieces, index + 1);
      new_pieces[index] = piece;
      DLPieces.remove(key);
      DLPieces.put(key, new_pieces);
    } else {
      new_pieces[index] = piece;
      DLPieces.remove(key);
      DLPieces.put(key, new_pieces);
    }
    if (buffermap.length() == index) {
      buffermap = buffermap.substring(0, index) + '1';
    } else {
      buffermap = buffermap.substring(0, index) + '1' + buffermap.substring(index + 1);
    }
    buffermaps_leech.remove(key);
    buffermaps_leech.put(key, buffermap);
  }

  // Ecrit tous les morceaux renseignés dans la mémoire (dans DLPieces, et éventuellement sur le disque)
  // Avec vérification de la clé md5 si écriture sur le disque
  public void writePieces(String key, String[] pieces, int[] indexes, String filename) {
    if (buffermaps_leech.get(key) == null) {
      if(buffermaps.get(key) == null){
        buffermaps_leech.put(key, "0");
      }
      else{
        buffermaps_leech.put(key, buffermaps.get(key));
        buffermaps.remove(key);
      }
    }
    for (int i = 0; i < pieces.length; i++) {
      writePiece(key, pieces[i], indexes[i]);
    }
    String buffermap = buffermaps_leech.get(key);
    boolean canWrite = true;
    for (int i = 0; i < buffermap.length(); i++) {
      if (buffermap.charAt(i) == '0') {
        canWrite = false;
      }
    }
    if (canWrite) {
      try {
        FileWriter myWriter = new FileWriter(filename);
        String[] text = DLPieces.get(key);
        for (int i = 0; i < text.length; i++) {
          myWriter.write(text[i]);
        }
        myWriter.close();
        buffermaps.put(key, buffermap);
        buffermaps_leech.remove(key);
        fileNames.put(key,filename);
        String md5 = getmd5(filename);
        if(md5.equals(key)){
          System.out.println("File " + filename + " created !");
          System.out.println("keys are the same : " + key + ", " + md5);
        }
        else{
          System.out.println("File " + filename + " created !");
          System.out.println("keys are not the same : " + key + ", " + md5);
        }
      } catch (IOException e) {
        System.out.println("Error in writePieces : can't write file " + filename);
        // e.printStackTrace();
      }
    }
  }

  /////////////////////////////////////// Fonctions pour download ///////////////////////////////////////////////////////

  // récupère la clé dans la requête look
  public String parseLook(String look){
    if(look.split(" ")[1].equals("[]\n")){
      return "";
    }
    else{
      String keyTemp = look.split(" ")[4];
      return keyTemp.substring(0, keyTemp.length() - 2);
    }
  }

  // retourne la liste des peers possédant un fichier dans getFile
  public String[] parseGetFile(String getfileRes){
    String[] peersTemp = getfileRes.split(" ");
    ArrayList<String> peers = new ArrayList<>();
    if(peersTemp[2].equals("[]")){ // pas de peers à contacter
      return null;
    }
    for (int i = 2; i < peersTemp.length; i++) {
      if(i == 2){ // premier élément
        if(peersTemp[i].charAt(peersTemp[i].length() - 2) == ']'){ // et dernier élément
          peers.add(peersTemp[i].substring(1,peersTemp[i].length()-2));
        }
        else{
          peers.add(peersTemp[i].substring(1,peersTemp[i].length()));
        }
      }
      else if(i == peersTemp.length - 1){
        peers.add(peersTemp[i].substring(0,peersTemp[i].length()-2)); //dernier élément
      }
      else{
        peers.add(peersTemp[i]);
      }
    }
    String[] res = new String[peers.size()];
    for (int i = 0; i < peers.size(); i++) {
      res[i] = peers.get(i);
    }
    return res;
  }

  // retourne le buffermap dans la requête interested
  public String parseInterested(String interestedRes){
    return interestedRes.split(" ")[2];
  }

  public void clearPieces(String key){
    DLPieces.remove(key);
  }

}
