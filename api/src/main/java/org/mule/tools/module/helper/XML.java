package org.mule.tools.module.helper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XML {

    public static Document load(final File file) throws SAXException, ParserConfigurationException, IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setNamespaceAware(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    public static List<String> extract(final Document document, final String expression) throws XPathExpressionException {
        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        final XPathExpression xpathExpression = xpath.compile(expression);
        final Object result = xpathExpression.evaluate(document, XPathConstants.NODESET);
        final NodeList nodes = (NodeList) result;
        final List<String> results = new LinkedList<String>();
        for (int i = 0; i < nodes.getLength(); i++) {
            results.add(nodes.item(i).getTextContent()); 
        }
        return results;
    }

}