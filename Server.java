import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    public static void main(String[] args) throws IOException {
//Создаем новый серверный Socket на порту 2048
        ServerSocket s = new ServerSocket(2048);
        TreeSet<String> userlist = new TreeSet<>();
        //File chat = new File("chat.txt");
        ArrayDeque<DataOutputStream> usersOutput = new ArrayDeque<>();
        ArrayDeque<Document> messages = new ArrayDeque<>();

        OutputProcessor outputProcessor = new OutputProcessor(usersOutput, messages);
        outputProcessor.start();
        while (true) {
            Socket clientSocket = s.accept(); //принимаем соединение
            System.out.println("Получено соединение от:" + clientSocket.getInetAddress()
                    + ":" + clientSocket.getPort());
            DataOutputStream fromClientObjectsOut = new DataOutputStream(clientSocket.getOutputStream());
            usersOutput.add(fromClientObjectsOut);
            //Создаем и запускаем поток для обработки запроса
            Thread t = new Thread(new RequestProcessor(clientSocket, userlist, messages, fromClientObjectsOut, usersOutput));
            System.out.println("Запуск обработчика...");
            t.start();
        }
    }
}

class OutputProcessor extends Thread {//todo
    ArrayDeque<DataOutputStream> userOutput;
    ArrayDeque<Document> messages;

    public OutputProcessor(ArrayDeque<DataOutputStream> userOutput, ArrayDeque<Document> messages) {
        this.userOutput = userOutput;
        this.messages = messages;
    }
    public void addUser(DataOutputStream user) {
        userOutput.add(user);
    }

    @Override
    public void run() {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = tf.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        //transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        while (!isInterrupted()) {
            synchronized (messages) {
                try {
                    messages.wait();
                    for (DataOutputStream iter : userOutput) {
                        try {
                            StringWriter stringWriter = new StringWriter();
                            transformer.transform(new DOMSource(messages.getLast()), new StreamResult(stringWriter));
                            String output = stringWriter.toString();
                            iter.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                            iter.writeBytes(output);
                            iter.flush();
                        }
                        catch (IOException | TransformerException e) {
                            e.printStackTrace();
                            userOutput.remove(iter);
                        }
                    }
                }
                catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }
    }
}

class RequestProcessor implements Runnable {

    Socket s; //Точка установленного соединения
    TreeSet<String> userlist;
    String username;
    ArrayDeque<Document> messages;
    DataOutputStream writer;
    ArrayDeque<DataOutputStream> usersOutput;

    public static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }


    public RequestProcessor(Socket s, TreeSet<String> userlist, ArrayDeque<Document> messages) {
        this.s = s;
        this.userlist = userlist;
        this.messages = messages;
    }

    public RequestProcessor(Socket s, TreeSet<String> userlist, ArrayDeque<Document> messages, DataOutputStream out) {
        this.s = s;
        this.userlist = userlist;
        this.messages = messages;
        writer = out;
    }

    public RequestProcessor(Socket s, TreeSet<String> userlist, ArrayDeque<Document> messages, DataOutputStream writer, ArrayDeque<DataOutputStream> usersOutput) {
        this.s = s;
        this.userlist = userlist;
        this.messages = messages;
        this.writer = writer;
        this.usersOutput = usersOutput;
    }

    private static void printInfoAboutAllChildNodes(NodeList list) {
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);

            // У элементов есть два вида узлов - другие элементы или текстовая информация. Потому нужно разбираться две ситуации отдельно.
            if (node.getNodeType() == Node.TEXT_NODE) {
                // Фильтрация информации, так как пробелы и переносы строчек нам не нужны. Это не информация.
                String textInformation = node.getNodeValue().replace("\n", "").trim();

                if (!textInformation.isEmpty())
                    System.out.println("Внутри элемента найден текст: " + node.getNodeValue());
            }
            // Если это не текст, а элемент, то обрабатываем его как элемент.
            else {
                System.out.println("Найден элемент: " + node.getNodeName() + ", его атрибуты:");

                // Получение атрибутов
                NamedNodeMap attributes = node.getAttributes();

                // Вывод информации про все атрибуты
                for (int k = 0; k < attributes.getLength(); k++)
                    System.out.println("Имя атрибута: " + attributes.item(k).getNodeName() + ", его значение: " + attributes.item(k).getNodeValue());
            }

            // Если у данного элемента еще остались узлы, то вывести всю информацию про все его узлы.
            if (node.hasChildNodes())
                printInfoAboutAllChildNodes(node.getChildNodes());
        }
    }

    RequestProcessor(Socket s) {
        this.s = s;
    }

    public void run() {
        try {
            InputStream input = s.getInputStream();
            DataInputStream reader = new DataInputStream(input);//TODO

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            //transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            String line = "";
            System.out.println("Ожидаем имя...");

            int XMLSize = reader.readInt();
            System.out.println(XMLSize);
            byte[] xml = reader.readNBytes(XMLSize);
            String xmlString = new String(xml);
            System.out.println(xmlString);
            Document command = loadXMLFromString(xmlString);
            NodeList nameFromCommand = command.getDocumentElement().getElementsByTagName("name");
            //printInfoAboutAllChildNodes(nameFromCommand);
            System.out.println(nameFromCommand.getLength());
            //System.out.println(nameFromCommand.item(0).getChildNodes().item(0).getNodeValue());
            username = nameFromCommand.item(0).getChildNodes().item(0).getNodeValue();
            //username = comand.parameter;
            userlist.add(username);
            System.out.println(username);


            //writer.writeObject(new ChatComand("success", "login"));

            for (Document iter : messages) {
                StringWriter stringWriter = new StringWriter();
                transformer.transform(new DOMSource(iter), new StreamResult(stringWriter));
                String output = stringWriter.toString();
                writer.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                writer.writeBytes(output);
                writer.flush();
            }
            Timer timer = new Timer();
            AtomicBoolean isChecked = new AtomicBoolean();
            AtomicBoolean isAlive = new AtomicBoolean();
            //boolean isChecked = false;
            //boolean isAlive = false;
            TimerTask checkAlive = new TimerTask() {
                @Override
                public void run() {
                    System.out.println("check");
                    Document chatCommand = DOMxmlWriter.makeXML("check");
                    //Element message = chatCommand.createElement("message");
                    Element session = chatCommand.createElement("session");
                    //message.appendChild(chatCommand.createTextNode(forMessage.getText()));
                    session.appendChild(chatCommand.createTextNode(username));
                    //chatCommand.getDocumentElement().appendChild(message);
                    chatCommand.getDocumentElement().appendChild(session);

                    StringWriter stringWriter = new StringWriter();
                    try {
                        if (!isChecked.get()) {
                            isChecked.set(true);
                            transformer.transform(new DOMSource(chatCommand), new StreamResult(stringWriter));
                            String output = stringWriter.toString();
                            writer.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                            writer.writeBytes(output);
                            //writer.writeChars(output); //TODO
                            writer.flush();
                        }
                        else {
                            if (!isAlive.get()) {
                                System.out.println(username+": interrupted");
                                userlist.remove(username);
                                s.close();
                                timer.cancel();
                                usersOutput.remove(writer);
                                Thread.currentThread().interrupt();

                                Document logout = DOMxmlWriter.makeXML("logout");
                                Element message = logout.createElement("success");
                                logout.getDocumentElement().appendChild(message);

                                StringWriter stringWriter1 = new StringWriter();
                                transformer.transform(new DOMSource(logout), new StreamResult(stringWriter1));
                                String output = stringWriter1.toString();
                                writer.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                                writer.writeBytes(output);
                                writer.flush();
                            }
                            else {
                                isAlive.set(false);
                                isChecked.set(false);
                            }
                        }
                    } catch (TransformerException | IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            timer.scheduleAtFixedRate(checkAlive, 0, 5000);
            while (true) {
                System.out.println("Ожидаем команду...");
                XMLSize = reader.readInt();
                System.out.println(XMLSize);
                byte[] xmlBytes = reader.readNBytes(XMLSize);
                xmlString = new String(xmlBytes);
                System.out.println(xmlString);
                command = loadXMLFromString(xmlString);
                String commandType = command.getDocumentElement().getAttribute("name");
                System.out.println(commandType);

                if (commandType == null) break;
                if (commandType.equals("login")) {
                    //todo: login in server
                    System.out.println("login--> login another");
                    userlist.add(username);
                    usersOutput.add(writer);
                }
                if (commandType.equals("message")) {
                    //System.out.println(username+": "+comand.command+" "+ comand.parameter);
                    String message = command.getDocumentElement().getElementsByTagName("message").item(0).getFirstChild().getNodeValue();
                    System.out.println(message);
                    synchronized (messages) {
                        messages.add(command);
                        //writer.writeObject(new ChatComand("success", "message"));
                        messages.notify();
                    }
                }
                if (commandType.equals("list")) {
                    //System.out.println(username+": "+comand.command);
                    Document chatCommand = DOMxmlWriter.makeXML("list");
                    Element message = chatCommand.createElement("success");
                    Element listusers = chatCommand.createElement("listusers");
                    message.appendChild(listusers);
                    for (String user : userlist) {
                        Element userElement = chatCommand.createElement("user");
                        userElement.appendChild(chatCommand.createTextNode(user));
                        listusers.appendChild(userElement);
                    }
                    chatCommand.getDocumentElement().appendChild(message);

                    StringWriter stringWriter = new StringWriter();
                    transformer.transform(new DOMSource(chatCommand), new StreamResult(stringWriter));
                    String output = stringWriter.toString();
                    writer.writeInt(output.getBytes(StandardCharsets.UTF_8).length);
                    writer.writeBytes(output);
                    writer.flush();
                }
                if (commandType.equals("userlogout")) {
                    System.out.println(username+": "+commandType);
                    userlist.remove(username);
                    usersOutput.remove(writer);
                    s.close();
                    Thread.currentThread().interrupt();
                }
                if (commandType.equals("check")) {
                    System.out.println(xmlString);
                    isAlive.set(true);
                    Thread.currentThread().interrupt();
                }
            }

            writer.close();
            System.out.println("Обработчик завершил работу");
        }
        catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
