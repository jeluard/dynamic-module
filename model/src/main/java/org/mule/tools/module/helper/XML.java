/**
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright 2012 Julien Eluard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     [http://www.apache.org/licenses/LICENSE-2.0]
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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