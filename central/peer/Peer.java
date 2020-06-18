
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Timer;
import java.util.Arrays;
import java.lang.Thread;
import java.lang.Runnable;
import java.io.IOException; // Import the IOException class to handle errors
import java.net.*;
import java.net.InetAddress;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class Peer {
  private static int portNumberPeer;
  private static String doneCmd = "DONE"; // commande à entrer par l'utilisateur quand il a terminé d'entrer des paramètres
  public static boolean IS_AVAILABLE = true; // permet d'éviter l'intrusion des update et have lorsque l'utilisateur entre une commande


  //////////////////////////////////////////////////// Fonctions utiles diverses //////////////////////////////////////////

  // Demande et lit la commande de l'utilisateur
  private static String nextCommand() {
    System.out.println(
        "Entrez le numéro d'une des commandes suivantes :\n1: announce\n2: look\n3: getfile\n4: interested\n5: getpieces\n6: download file\n7: exit");
    return readInput();
  }

  // Renvoie la chaine de caracteres entree par l'utilisateur
  public static String readInput() {
    Scanner scan = new Scanner(System.in);
    String s = scan.next();
    System.out.println("");
    return s;
  }

  /////////////////////////////////////////////// Prompt utilisateur ////////////////////////////////////////////////

  // Liste des commandes utilisateur sous forme de prompt
  public static void basicInputs(Command command){
    int commandNo;
    String key;
    do { // BOUCLER TANT QUE LA COMMANDE != exit
      try {
        commandNo = Integer.parseInt(nextCommand());
      } catch (Exception e) {
        System.out.print("Error : Entrez un NUMERO VALIDE\n");
        commandNo = Integer.parseInt(nextCommand());
      }

      switch (commandNo) {
        case 1: // (announce)
          IS_AVAILABLE = false;
          RequestAnnounce requestAnnounce = new RequestAnnounce(command);
          requestAnnounce.announce();
          IS_AVAILABLE = true;
          break;

        case 2:// (look)
          IS_AVAILABLE = false;
          System.out.println("Veuillez entrer le nom du fichier: ");
          String filename = readInput();
          System.out.println("Précisez la taille minimum : ");
          String filesize = readInput();
          RequestLook requestLook = new RequestLook(command,filename,filesize);
          requestLook.look();
          IS_AVAILABLE = true;
          break;

        case 3:// (getfile)
          IS_AVAILABLE = false;
          System.out.println("Entrez la clé du fichier souhaité");
          key = readInput();
          RequestGetfile requestGetfile = new RequestGetfile(command, key);
          requestGetfile.getfile();
          IS_AVAILABLE = true;
          break;

        case 4:// (interested)
          IS_AVAILABLE = false;
          System.out.println("Entrez la clé du fichier souhaité");
          key = readInput();
          System.out.println("Entrez le numéro de port du peer à contacter: ");
          int portNumberPeerToContact = Integer.parseInt(readInput());
          RequestInterested requestInterested = new RequestInterested(command, key, portNumberPeerToContact);
          requestInterested.interested();
          IS_AVAILABLE = true;
          break;

        case 5:// (getpieces)
          IS_AVAILABLE = false;
          List<String> indexes =  new ArrayList<String>();
          System.out.println("Entrez la clé du fichier souhaité");
          key = readInput();
          System.out.println("Entrez le nom du fichier");
          command.save_filename = command.readInput();
          System.out.println("Entrez une partie souhaitée du fichier (index) (ou " + command.doneCmd + " pour terminer)");
          String temp = command.readInput();
          while (!temp.equals(command.doneCmd)) {
              indexes.add(temp);
              System.out.println("Entrez une partie souhaitée du fichier (index) (ou " + command.doneCmd + " pour terminer)");
              temp = command.readInput();
          }
          System.out.println("Entrez le numéro de port cible :");
          int port = Integer.parseInt(command.readInput());
          RequestGetpieces requestGetpieces = new RequestGetpieces(command, key, indexes, port);
          requestGetpieces.getpieces();
          IS_AVAILABLE = true;
          break;

        case 6:// (download file)
          // Look part
          IS_AVAILABLE = false;
          System.out.println("Veuillez entrer le nom du fichier à l'enregistrement: ");
          command.save_filename = readInput();
          System.out.println("Veuillez entrer le nom du fichier: ");
          String filenameDL = readInput();
          System.out.println("Précisez la taille minimum : ");
          String filesizeDL = readInput();

          RequestLook requestLookDL = new RequestLook(command,filenameDL,filesizeDL);
          String lookRes = requestLookDL.look();
          String keyDL = command.parseLook(lookRes);
          if(keyDL.equals("")){
            System.out.println("Le fichier n'est pas connu du tracker !");
            break;
          }

          // Getfile part
          RequestGetfile requestGetfileDL = new RequestGetfile(command, keyDL);
          String getfileRes = requestGetfileDL.getfile();
          String[] peers = command.parseGetFile(getfileRes);
          IS_AVAILABLE = true;
          if(peers == null){
            System.out.println("Pas de peers avec le fichier !");
            break;
          }


          // Interested part
          Map<Integer, String> indexes_peer = new HashMap<Integer, String>();
          Map<String, List<String>> peer_indexes = new HashMap<String, List<String>>();
          for (int i = 0; i < peers.length; i++) {
            int portNumberPeerToContactDL = Integer.parseInt(peers[i].split(":")[1]);
            RequestInterested requestInterestedDL = new RequestInterested(command, keyDL, portNumberPeerToContactDL);
            String interestedRes = requestInterestedDL.interested();
            String buffermap = command.parseInterested(interestedRes);
            for (int j = 0; j < buffermap.length(); j++) {
              List<String> indexesDL = null;
              if(buffermap.charAt(j) == '1' && indexes_peer.get(j) == null){ // a le morceau + pas encore prévu pour un peer
                indexes_peer.put(j,peers[i]);
                if(peer_indexes.get(peers[i]) == null){ // pas d'indexs a demander au peer
                  indexesDL = new ArrayList<String>();
                  indexesDL.add(Integer.toString(j));
                  peer_indexes.put(peers[i],indexesDL);
                }
                else{ // ajouter un index à la liste des index a demander
                  indexesDL = peer_indexes.get(peers[i]);
                  indexesDL.add(Integer.toString(j));
                  peer_indexes.remove(peers[i]);
                  peer_indexes.put(peers[i],indexesDL);
                }
              }
            }
          }

          // Getpieces part
          Object[] objPeers = peer_indexes.keySet().toArray();
          String[] peersToContact = Arrays.copyOf(objPeers, objPeers.length, String[].class);
          int maxIndexes = (command.getMaxSize()/command.getPieceSize()) - 1; // Nombre max d'indices qu'on peut demander en une fois
          for (int i = 0; i < peersToContact.length; i++) {
            String peer = peersToContact[i];
            List <String> peerIndexes = peer_indexes.get(peer);
            int peerPort = Integer.parseInt(peer.split(":")[1]);
            RequestGetpieces requestGetpiecesDL = null;

            if(peerIndexes.size() > maxIndexes){ // fichier trop grand pour récuperer en un message
              int cpt = 0; // compteur pour savoir si on a atteint la taille max
              List <String> sublist = new ArrayList<String>();
              for(int j = 0; j < peerIndexes.size(); j++){ // on envoie plusieurs messages
                sublist.add(peerIndexes.get(j));
                cpt ++;
                if(cpt >= maxIndexes){
                  requestGetpiecesDL = new RequestGetpieces(command, keyDL, sublist, peerPort);
                  requestGetpiecesDL.getpieces();
                  sublist.clear();
                  cpt = 0;
                }
                else if(j == peerIndexes.size() - 1){ // fin d'indexes
                  requestGetpiecesDL = new RequestGetpieces(command, keyDL, sublist, peerPort);
                  requestGetpiecesDL.getpieces();
                }
              }
            }
            else{
              requestGetpiecesDL = new RequestGetpieces(command, keyDL, peerIndexes, peerPort);
              requestGetpiecesDL.getpieces();
            }
          }
          command.clearPieces(keyDL);
          break;

        case 7: //exit
          IS_AVAILABLE = false;
          for (int clientI = 0; clientI < command.clients.size(); clientI++) {
            ClientHandler client = command.clients.get(clientI);
            client.sendMessage("exit\n");
          }
          IS_AVAILABLE = true;
          System.exit(0);
      }
    } while (true);
  }


  ////////////////////////////// Main ///////////////////////////////////////////////////////////////////////
  public static void main(String[] args) throws Exception {
    System.out.print("Entrez le nouveau numéro de port du peer si besoin: ");
    portNumberPeer = Integer.parseInt(readInput());
    Command command = new Command(portNumberPeer);
    command.initPeerData();
    Timer timer = new Timer();
    File file = new File("log" + portNumberPeer + ".txt");

    // Requête announce pour éviter un bug côté tracker
    IS_AVAILABLE = false;
    System.out.println("Announce au tracker, entrez des noms de fichiers à déclarer au tracker ou DONE pour terminer");
    RequestAnnounce requestAnnounce = new RequestAnnounce(command);
    requestAnnounce.announce();
    IS_AVAILABLE = true;

    // --------- Timer pour la requête Have/Update ---------------
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        int portTracker = command.getTrackerPort();
        String IPTracker = command.getTrackerIP();
        Object[] objArray = command.buffermaps.keySet().toArray();
        String[] keysS = Arrays.copyOf(objArray, objArray.length, String[].class); // keys already download
        objArray = command.buffermaps_leech.keySet().toArray();
        String[] keysL = Arrays.copyOf(objArray, objArray.length, String[].class); // keys in downloading
        for (int clientI = 0; clientI < command.clients.size(); clientI++) {
          ClientHandler client = command.clients.get(clientI);
          // Si ce n'est pas le tracker
          if (!(client.sock.getInetAddress().getHostName().equals(IPTracker) && client.sock.getPort() == portTracker)) {
            // Requete en cours côté utilisateur
            while (!IS_AVAILABLE) {
              System.out.println("");
            }
            for (int keyI = 0; keyI < keysS.length; keyI++) {
              String key = keysS[keyI];
              String buffermap = command.buffermaps.get(key);
              RequestHave requestHave = new RequestHave(key, buffermap, client.sock.getInetAddress().getHostName(),
                  client.sock.getPort(), command);
              requestHave.have();
            }
            for (int keyI = 0; keyI < keysL.length; keyI++) {
              String key = keysL[keyI];
              String buffermap = command.buffermaps_leech.get(key);
              RequestHave requestHave = new RequestHave(key, buffermap, client.sock.getInetAddress().getHostName(),
                  client.sock.getPort(), command);
              requestHave.have();
            }
          } else {
            // Requete en cours côté utilisateur
            while (!IS_AVAILABLE) {
              System.out.print("");
            }
            RequestUpdate requestUpdate = new RequestUpdate(command);
            requestUpdate.update();
          }
        }
      }
    }, command.getTimeout() * 1000, command.getTimeout() * 1000);

    // --------------------- Thread mode serveur --------------------------
    Runnable r1 = new Runnable() {
      private ServerSocket server = null;
      String host = "localhost"; // a changer si nécéssaire

      public void run() {
        boolean isRunning = true;
        try {
          server = new ServerSocket(portNumberPeer);
        } catch (Exception e) {
          System.out.println("Error in peer server thread : cannot open socket on port " + portNumberPeer);
          // e.printStackTrace();
        }
        while (isRunning == true) {
          try {
            // On attend une connexion d'un client
            Socket client = server.accept();

            // Une fois reçue, on la traite dans un thread dans la pool de threads
            System.out.println("Connexion cliente reçue.");
            ClientHandler clientThread = new ClientHandler(command, client);

            boolean exists = false;
            for (int i = 0; i < command.clients.size(); i++) {
              ClientHandler thread = command.clients.get(i);
              if (thread.sock.getInetAddress().toString().equals(client.getInetAddress().toString())
                  && thread.sock.getPort() == client.getLocalPort()) {
                clientThread = thread;
                exists = true;
              }
            }
            if (!exists) {
              System.out.println("creating new thread on socket " + client);
              command.clients.add(clientThread);
              command.executor.execute(clientThread);
            }
          } catch (IOException e) {
            System.out.println("Error while accepting new connection");
            // e.printStackTrace();
            server = null;
            isRunning = false;
          }
        }
      }
    };

    Thread thr1 = new Thread(r1);
    thr1.start();

    basicInputs(command);

    System.out.println("Main done");
  }
}
