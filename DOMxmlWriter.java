import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;

public class DOMxmlWriter {
    private static Node setElements(Document doc, Element element, String name, String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }
    private static Node getParameter(Document doc, String id, String name, String age) {
        Element language = doc.createElement("Language");

        // устанавливаем атрибут id
        language.setAttribute("id", id);

        // создаем элемент name
        language.appendChild(setElements(doc, language, "name", name));

        // создаем элемент age
        language.appendChild(setElements(doc, language, "age", age));
        return language;
    }
    public static Document makeXML(String command) {
        Document doc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
            Element rootElement = doc.createElement("command");
            rootElement.setAttribute("name", command);
            doc.appendChild(rootElement);
        /*TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        // для красивого вывода в консоль
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);

        //печатаем в консоль или файл
        StreamResult console = new StreamResult(System.out);

        //записываем данные
        transformer.transform(source, console);*/
        }
        catch (ParserConfigurationException err) {
            err.printStackTrace();
        }
    /*catch (TransformerConfigurationException err) {
        err.printStackTrace();
    }
    catch (TransformerException e) {
        e.printStackTrace();
    }*/
        return doc;
    }
}
