import java.util.*;

class RequestUpdate{

    private String request;
    private Command command;

    public RequestUpdate(Command command) {
        this.command = command;
    }

    // Retourne la requête look sous forme de String
    String buildRequest(String[] keyS, String[] keyL) {
        String req = "update seed [";
        if(keyS.length>0){
          for (int i = 0; i < keyS.length - 1; i++) {
              req += keyS[i];
              req += " ";
          }
          req += keyS[keyS.length - 1];
        }
        req += "] leech [";
        if(keyL.length>0){
          for (int i = 0; i < keyL.length - 1; i++) {
              req += keyL[i];
              req += " ";
          }
          req += keyL[keyL.length - 1];
        }
        req += "]\n";
        return req;
    }

    // Méthode pour la requête Update
    public void update() {
      Object[] objSeed = command.buffermaps.keySet().toArray();
      String[] keySeed = Arrays.copyOf(objSeed, objSeed.length, String[].class);
      Object[] objLeech = command.buffermaps_leech.keySet().toArray();
      String[] keyLeech = Arrays.copyOf(objLeech, objLeech.length, String[].class);
      request = buildRequest(keySeed,keyLeech);
      command.sendToTracker(request);
    }
}
