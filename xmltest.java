import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class xmltest {
    private static Node setElements(Document doc, Element element, String name, String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }
    private static Node setParameter(Document doc, String id, String name, String age) {
        Element language = doc.createElement("Language");

        // устанавливаем атрибут id
        language.setAttribute("id", id);

        // создаем элемент name
        language.appendChild(setElements(doc, language, "name", name));

        // создаем элемент age
        language.appendChild(setElements(doc, language, "age", age));
        return language;
    }
    public static void main(String[] args) {
        Document doc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
            Element rootElement = doc.createElement("command");
            doc.appendChild(rootElement);
            rootElement.appendChild(setParameter(doc, "5", "hui", "10"));

            Document chatCommand = DOMxmlWriter.makeXML("message");
            Element message = chatCommand.createElement("message");
            message.appendChild(chatCommand.createTextNode("username.get()"));
            chatCommand.appendChild(message);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            // для красивого вывода в консоль
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(chatCommand);
            StreamResult console = new StreamResult(System.out);
            transformer.transform(source, console);
        }
        catch (ParserConfigurationException | TransformerConfigurationException err) {
            err.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}
