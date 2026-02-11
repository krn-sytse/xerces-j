package org.apache.xerces.tools;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI for validating XML files (or directories recursively) against an XSD.
 *
 * Usage:
 *   java -jar xerces-cli.jar schema.xsd xmlfile|directory
 */
public final class XMLValidate {

    private static final List<String> failures = new ArrayList<>();

    private static class CollectingErrorHandler implements ErrorHandler {

        private final Path baseDir;
        private final Path file;

        CollectingErrorHandler(Path baseDir, Path file) {
            this.baseDir = baseDir;
            this.file = file;
        }

        private void record(String message, SAXParseException e) {
            Path relative = baseDir.relativize(file);
            String output =
                    "FAILED: " + relative.toString() + System.lineSeparator() +
                    e.getMessage() +
                    " (line " + e.getLineNumber() +
                    ", col " + e.getColumnNumber() + ")";
            failures.add(output);
        }

        public void warning(SAXParseException e) { }
        public void error(SAXParseException e) { record("error", e); }
        public void fatalError(SAXParseException e) { record("fatal", e); }
    }

    private static void validateFile(Schema schema, Path baseDir, Path xmlPath) {

        try {
            Validator validator = schema.newValidator();
            validator.setErrorHandler(new CollectingErrorHandler(baseDir, xmlPath));

            validator.validate(new StreamSource(xmlPath.toFile()));

        } catch (Exception e) {
            Path relative = baseDir.relativize(xmlPath);
            failures.add("FAILED: " + relative.toString() + System.lineSeparator() + e.getMessage());
        }
    }

    private static List<Path> collectXmlFiles(Path input) throws IOException {

        final List<Path> files = new ArrayList<Path>();

        if (Files.isRegularFile(input)) {
            if (input.toString().toLowerCase().endsWith(".xml")) {
                files.add(input);
            }
            return files;
        }

        Files.walkFileTree(input, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().toLowerCase().endsWith(".xml")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // ignore unreadable files but continue
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.println("Usage: java -jar xerces-cli.jar <schema.xsd> <xmlfile|directory>");
            System.exit(1);
        }

        File schemaFile = new File(args[0]);
        Path inputPath = Paths.get(args[1]).toAbsolutePath().normalize();

        if (!schemaFile.exists()) {
            System.err.println("Schema not found: " + schemaFile);
            System.exit(1);
        }

        if (!Files.exists(inputPath)) {
            System.err.println("Input path not found: " + inputPath);
            System.exit(1);
        }

        // Ensure Xerces is used
        System.setProperty(
                "javax.xml.validation.SchemaFactory:" + XMLConstants.W3C_XML_SCHEMA_NS_URI,
                "org.apache.xerces.jaxp.validation.XMLSchemaFactory"
        );

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(schemaFile);

        Path baseDir = Files.isDirectory(inputPath) ? inputPath : inputPath.getParent();

        List<Path> xmlFiles = collectXmlFiles(inputPath);

        if (xmlFiles.isEmpty()) {
            System.out.println("No XML files found.");
            System.exit(0);
        }

        for (Path xml : xmlFiles) {
            validateFile(schema, baseDir, xml);
        }

        // print all failures
        for (String f : failures) {
            System.err.println(f);
            System.err.println();
        }

        if (!failures.isEmpty()) {
            System.exit(2);
        }

        System.out.println("All XML files are valid (" + xmlFiles.size() + " checked).");
    }
}
