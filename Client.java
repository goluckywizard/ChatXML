import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ClientsReadThread extends Thread {
    AtomicReference<String> username;
    private boolean success;
    private ArrayDeque<String> otherClients;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    JTextArea chat;
    SwingClient client;

    public void setClient(SwingClient client) {
        this.client = client;
    }

    ClientsReadThread(ArrayDeque<String> otherClients, DataInputStream fromServer, JTextArea chat, DataOutputStream toServer) {
        this.fromServer = fromServer;
        this.otherClients = otherClients;
        this.chat = chat;
        this.toServer = toServer;
    }

    public void setUsername(AtomicReference<String> username) {
        this.username = username;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setFromServer(DataInputStream fromServer) {
        this.fromServer = fromServer;
    }

    public void setToServer(DataOutputStream toServer) {
        this.toServer = toServer;
    }

    @Override
    public void run() {
        Document command;
        while (!isInterrupted()) {//TODO
            try {
                int XMLSize = fromServer.readInt();
                System.out.println(XMLSize);
                byte[] xml = fromServer.readNBytes(XMLSize);
                String xmlString = new String(xml);
                System.out.println(xmlString);
                command = loadXMLFromString(xmlString);

                String commandType = command.getDocumentElement().getAttribute("name");
                switch (commandType) {
                    case "check" -> {
                        toServer.writeInt(XMLSize);
                        toServer.writeBytes(xmlString);
                    }
                    case "message" -> {
                        chat.append(command.getDocumentElement().getElementsByTagName("session").item(0).getFirstChild().getNodeValue()
                                +": "
                                +command.getDocumentElement().getElementsByTagName("message").item(0).getFirstChild().getNodeValue()
                                +"\n");
                        /*System.out.println(command.clientName+":"+command.parameter);
                        chat.append(command.clientName+":"+command.parameter+"\n");*/
                    }
                    case "list" -> {
                        synchronized (this) {
                            otherClients.clear();
                            NodeList userlist = command.getDocumentElement().getElementsByTagName("user");
                            System.out.println("size userlist:"+userlist.getLength());
                            for (int i = 0; i < userlist.getLength(); ++i) {
                                otherClients.add(userlist.item(i).getFirstChild().getNodeValue());
                            }
                            success = true;
                            notify();
                            /*for (String a : otherClients) {
                                System.out.println(a);
                                chat.append(a+"\n");
                            }*/
                        }
                    }
                    case "logout" -> {
                        //client.relogin();
                        Thread.currentThread().interrupt();
                        /*TransformerFactory tf = TransformerFactory.newInstance();
                        Transformer transformer = tf.newTransformer();
                        //transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                        Document chatCommand = DOMxmlWriter.makeXML("login");
                        Element message = chatCommand.createElement("name");
                        message.appendChild(chatCommand.createTextNode(username.get()));
                        chatCommand.getDocumentElement().appendChild(message);

                        StringWriter stringWriter = new StringWriter();
                        transformer.transform(new DOMSource(chatCommand), new StreamResult(stringWriter));
                        String output = stringWriter.toString();
                        toServer.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                        toServer.writeBytes(output); //TODO
                        //System.out.println(output);
                        System.out.println(output.getBytes(StandardCharsets.UTF_8).length+output);
                        toServer.flush();*/
                    }
                    /*
                    case ERROR -> {
                        synchronized (this) {
                            success = false;
                            notify();
                        }
                    }
                    case LOGIN -> {
                        System.out.println("Добро пожаловать!");
                    }*/
                }
            }
            catch (IOException | ParserConfigurationException | SAXException err) {
                interrupt();
                err.printStackTrace();
            }
        }
    }

    private Document loadXMLFromString(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }
}
/*class ClientsWrite {
    public void Run() throws IOException, ClassNotFoundException {
    }
}*/

public class Client {

    public static void main(String[] args) throws IOException, TransformerConfigurationException {
        SwingClient swingClient = new SwingClient();
        swingClient.run();

    }
}