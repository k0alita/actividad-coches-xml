package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.xml.bind.JAXBException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Properties;

public class GestorConsesionario {
    private final Properties config = new Properties();
    private final ObjectMapper json = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final Path xmlPath;
    private Concencionario concencionario;

    public GestorConsesionario() throws IOException, JAXBException, ParseException, SAXException {
        Path configPath = Path.of("config.properties");
        try(InputStream input = Files.newInputStream(configPath)) {
            config.load(input);

        }

        xmlPath = path("xml.path");

    }
    public Concencionario getConcencionario() {
        return concencionario;
    }

    private Path path(String clave) {
        String valor = config.getProperty(clave);
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("La clave " + clave + " no tiene valor");
        }
        return Path.of(valor.trim());
    }

    private void crearCarpeta(Path fichero) throws IOException {
        Path carpeta = fichero.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
    }

}
