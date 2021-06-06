import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
class SwingChat {
    Socket s;
    DataOutputStream writer;
    DataInputStream reader;
    AtomicReference<String> username;

    public SwingChat(Socket s, DataOutputStream writer, DataInputStream reader, AtomicReference<String> username) {
        this.s = s;
        this.writer = writer;
        this.reader = reader;
        this.username = username;
    }

    void run() throws TransformerConfigurationException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        AtomicBoolean isExit = new AtomicBoolean();
        isExit.set(false);
        JTextArea messages = new JTextArea();
        ArrayDeque<String> otherClients = new ArrayDeque<>();
        final ClientsReadThread[] readThread = {new ClientsReadThread(otherClients, reader, messages, writer)};
        readThread[0].setUsername(username);
        readThread[0].start();

        JFrame chat = new JFrame();
        messages.setEditable(false);
        chat.setSize(400, 400);
        JTextField forMessage = new JTextField("qwerty");
        forMessage.setSize(100, 40);
        JButton checkUserList = new JButton();
        checkUserList.addActionListener(e -> {
            try {
                /*ChatComand chatComand = new ChatComand("list");
                chatComand.setClientName("username");*/
                Document chatCommand = DOMxmlWriter.makeXML("list");
                Element session = chatCommand.createElement("session");
                session.appendChild(chatCommand.createTextNode(username.get()));
                chatCommand.getDocumentElement().appendChild(session);

                StringWriter stringWriter = new StringWriter();
                transformer.transform(new DOMSource(chatCommand), new StreamResult(stringWriter));
                String output = stringWriter.toString();
                writer.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                writer.writeBytes(output);
                writer.flush();
                synchronized (readThread[0]) {
                    readThread[0].wait();
                }

                JDialog dialog = new JDialog();
                dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                dialog.setSize(180, 90);
                dialog.setTitle("a");
                dialog.setVisible(true);
                JTextArea forDialog = new JTextArea();
                forDialog.setEditable(false);
                dialog.add(forDialog);
                StringBuffer usersList = new StringBuffer();
                for (String i : otherClients)
                    usersList.append(i).append("\n");
                System.out.println(usersList.toString());
                forDialog.setText(usersList.toString());

                forMessage.setText("");
            } catch (IOException | TransformerConfigurationException ioException) {
                ioException.printStackTrace();
            } catch (TransformerException transformerException) {
                transformerException.printStackTrace();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        });

        forMessage.setToolTipText("Введите сообщение");
        forMessage.addActionListener(e -> {
            try {
                /*String command = "";
                command = "send";
                //if (command.equalsIgnoreCase("exit")) break;
                ChatComand chatComand = new ChatComand(command);
                chatComand.setParameter(forMessage.getText());
                chatComand.setClientName(username.get());*/
                //String text = " ";
                if (!forMessage.getText().equals("")) {

                    Document chatCommand = DOMxmlWriter.makeXML("message");
                    Element message = chatCommand.createElement("message");
                    Element session = chatCommand.createElement("session");
                    message.appendChild(chatCommand.createTextNode(forMessage.getText()));
                    session.appendChild(chatCommand.createTextNode(username.get()));
                    chatCommand.getDocumentElement().appendChild(message);
                    chatCommand.getDocumentElement().appendChild(session);

                    StringWriter stringWriter = new StringWriter();
                    transformer.transform(new DOMSource(chatCommand), new StreamResult(stringWriter));
                    String output = stringWriter.toString();
                    writer.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                    writer.writeBytes(output);
                    //writer.writeChars(output); //TODO
                    writer.flush();
                    forMessage.setText("");
                    forMessage.setSize(40, 20);
                }
            } catch (IOException | TransformerException ioException) {
                ioException.printStackTrace();
            }
        });
        Container container = chat.getContentPane();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(messages);
        container.add(forMessage);
        container.add(checkUserList);
        chat.setVisible(true);

        Timer timer = new Timer();
        TimerTask checkIsAlive = new TimerTask() {
            @Override
            public void run() {
                if (readThread[0].isInterrupted() && !isExit.get()) {
                    try {
                        s.close();
                        s = new Socket("127.0.0.1", 2048);
                        writer = new DataOutputStream(s.getOutputStream());
                        reader = new DataInputStream(s.getInputStream());
                        readThread[0] = new ClientsReadThread(otherClients, reader, messages, writer);

                        Document chatCommand = DOMxmlWriter.makeXML("login");
                        Element message = chatCommand.createElement("name");
                        message.appendChild(chatCommand.createTextNode(username.get()));
                        chatCommand.getDocumentElement().appendChild(message);

                        StringWriter stringWriter = new StringWriter();
                        transformer.transform(new DOMSource(chatCommand), new StreamResult(stringWriter));
                        String output = stringWriter.toString();
                        writer.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                        writer.writeBytes(output);
                        System.out.println(output.getBytes(StandardCharsets.UTF_8).length+output);
                        writer.flush();

                        chat.dispose();

                        SwingChat.this.run();
                        return;
                    } catch (IOException | TransformerException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(checkIsAlive, 0, 15000);

        chat.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                try {
                    Document chatCommand = DOMxmlWriter.makeXML("userlogout");
                    Element message = chatCommand.createElement("name");
                    message.appendChild(chatCommand.createTextNode(username.get()));
                    chatCommand.getDocumentElement().appendChild(message);
                    StringWriter stringWriter = new StringWriter();
                    transformer.transform(new DOMSource(chatCommand), new StreamResult(stringWriter));
                    String output = stringWriter.toString();
                    writer.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                    writer.writeBytes(output);
                    System.out.println(output.getBytes(StandardCharsets.UTF_8).length+output);
                    writer.flush();

                    timer.cancel();
                    isExit.set(true);
                    readThread[0].interrupt();
                    Thread.currentThread().interrupt();
                    chat.dispose();
                    return;
                } catch (TransformerException | IOException transformerException) {
                    transformerException.printStackTrace();
                }
            }
        });
    }

}
public class SwingClient {
    Socket s;

    public void run() throws IOException, TransformerConfigurationException {
        s = new Socket("25.51.144.161", 2048); //todo new host
        DataOutputStream writer = new DataOutputStream(s.getOutputStream());

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        //transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        //BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        DataInputStream reader = new DataInputStream(s.getInputStream());
        AtomicReference<String> username = new AtomicReference<>();


        JFrame registration = new JFrame();
        registration.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        registration.setSize(400, 400);

        JTextField forName = new JTextField("Enter name");


        forName.addActionListener(e -> {
            try {
                username.set(forName.getText());

                Document chatCommand = DOMxmlWriter.makeXML("login");
                Element message = chatCommand.createElement("name");
                message.appendChild(chatCommand.createTextNode(username.get()));
                chatCommand.getDocumentElement().appendChild(message);

                StringWriter stringWriter = new StringWriter();
                transformer.transform(new DOMSource(chatCommand), new StreamResult(stringWriter));
                String output = stringWriter.toString();
                writer.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                writer.writeBytes(output); //TODO
                //System.out.println(output);
                System.out.println(output.getBytes(StandardCharsets.UTF_8).length+output);
                writer.flush();

                //line = reader.readLine();
                //ChatComand fromServer = (ChatComand) reader.readObject();
                //System.out.println("Получен ответ:" + line);
                registration.getContentPane().remove(forName);
                //registration.getContentPane().add(messages);
                //registration.getContentPane().add(forMessage);
                /*JPanel panel = new JPanel();*/
                //Container container = registration.getContentPane();

                registration.dispose();
                //registration.setVisible(true);
                SwingChat chat = new SwingChat(s, writer, reader, username);
                chat.run();

            } catch (TransformerException | IOException ioException) {
                ioException.printStackTrace();
            }
        });
        registration.getContentPane().add(forName);
        //registration
        registration.setVisible(true);
    }
}
