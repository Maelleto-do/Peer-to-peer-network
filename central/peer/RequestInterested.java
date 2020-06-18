import java.util.*;
import java.io.IOException; // Import the IOException class to handle errors
import java.net.*;
import java.io.*;

class RequestInterested{

    private String key;
    private int portNumberPeerToContact;
    private Command command;

    public RequestInterested(Command command, String key, int portNumberPeerToContact) {
        this.command = command;
        this.key = key;
        this.portNumberPeerToContact = portNumberPeerToContact;
    }

    // Retourne la requête announce à envoyer sous forme de String
    public String buildRequest() {
        return "interested " + key;
    }

    // Méthode pour la requête interested
    public String interested() {
        String request = buildRequest();
        return command.sendToPeer(request,"localhost",portNumberPeerToContact);
    }
}
