package org.apache.xerces.tools;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;

/**
 * Simple CLI for validating XML against an XSD.
 * Usage:
 *   java -jar xerces-cli.jar path/to/schema.xsd path/to/xmlfile.xml
 */
public final class XMLValidate {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java -jar xerces-cli.jar <schema.xsd> <xmlfile.xml>");
            System.exit(1);
        }

        File schemaFile = new File(args[0]);
        File xmlFile = new File(args[1]);

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(schemaFile);
        Validator validator = schema.newValidator();

        try {
            validator.validate(new StreamSource(xmlFile));
            System.out.println("XSD validation successful: " + xmlFile.getName());
        } catch (Exception e) {
            System.err.println("XSD validation failed: " + e.getMessage());
            System.exit(2);
        }
    }
}
