package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GestorConcesionario {
    private final Properties config = new Properties();
    private final ObjectMapper json = new ObjectMapper();
    private final Path xmlPath;
    private final JAXBContext contextoXml;
    private Concesionario concesionario;

    /**
     * Carga la configuración y la base de datos XML.
     *
     * @throws IOException si no se puede leer la configuración o el XML
     * @throws JAXBException si el XML no puede deserializarse
     * @throws ParserConfigurationException si falla la configuración XML segura
     * @throws SAXException si el XML no es válido
     */

    public GestorConcesionario() throws IOException, JAXBException, SAXException, ParserConfigurationException {
        try (InputStream input = GestorConcesionario.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new FileNotFoundException(
                        "No se encuentra config.properties en src/main/resources");
            }

            config.load(input);
        }

        contextoXml = JAXBContext.newInstance(Concesionario.class);
        json.enable(SerializationFeature.INDENT_OUTPUT);


        xmlPath = path("xml.path");
        crearCarpeta(xmlPath);

        if (Files.exists(xmlPath)) {
            concesionario = leerXml(xmlPath);
        } else {
            concesionario = new Concesionario();
            guardarXml();
        }

        validarMatriculas(concesionario.getCoches());
        normalizarIds();

    }

    /**
     * Obtiene el concesionario cargado en memoria.
     *
     * @return concesionario actual
     */
    public Concesionario getConcesionario() {
        return concesionario;
    }

    /**
     * Importa coches desde CSV separado por punto y coma.
     * El campo extras debe separar los elementos con barra vertical.
     *
     * @throws IOException si falla la lectura del CSV
     * @throws JAXBException si falla la persistencia XML
     */
    public void importarCsv() throws IOException, JAXBException {
        Path csvPath = path("csv.path");
        int importados = 0;
        int duplicados = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String linea;
            boolean primera = true;

            while ((linea = reader.readLine()) != null) {
                if (primera) {
                    primera = false;
                    if (linea.toLowerCase(Locale.ROOT).contains("matricula")) {
                        continue;
                    }
                }

                if (linea.isBlank()) {
                    continue;
                }

                String[] campos = linea.split(";", -1);
                if (campos.length < 4) {
                    System.out.println("Línea CSV ignorada por formato incorrecto: " + linea);
                    continue;
                }

                String matricula = campos[0].trim();
                if (buscar(matricula) != null) {
                    duplicados++;
                    continue;
                }

                List<String> extras = new ArrayList<>();

                for (String extra : campos[3].split("\\|")) {
                    extra = extra.trim();

                    if (!extra.isEmpty()) {
                        extras.add(extra);
                    }
                }

                Coche coche = new Coche(siguenteId(), matricula,
                        campos[1].trim(), campos[2].trim(), extras);

                try {
                    validarCoche(coche);
                    concesionario.getCoches().add(coche);
                    importados++;
                } catch (IllegalArgumentException e) {
                    System.out.println("Fila ignorada: " + e.getMessage());
                }
            }
        }

        if (importados > 0) {
            guardarXml();
        }
        System.out.printf("Importados: %d | Matrículas duplicadas omitidas: %d%n",
                importados, duplicados);
    }

    /**
     * Inserta un coche si su matrícula no existe.
     *
     * @param coche coche que se quiere añadir
     * @return true si se insertó; false si ya existía la matrícula
     * @throws JAXBException si falla la persistencia XML
     */

    public boolean insertar(Coche coche) throws JAXBException {
        validarCoche(coche);
        if (buscar(coche.getMatricula()) != null) {
            return false;
        }

        coche.setId(siguenteId());
        concesionario.getCoches().add(coche);
        guardarXml();

        return true;
    }

    /**
     * Ordena los coches alfabéticamente por matrícula y guarda el resultado.
     *
     * @throws JAXBException si falla la persistencia XML
     */

    public void ordenar() throws JAXBException {
        concesionario.getCoches().sort(
                Comparator.comparing(Coche::getMatricula, String.CASE_INSENSITIVE_ORDER));
        guardarXml();
    }

    /**
     * Borra un coche por matrícula.
     *
     * @param matricula matrícula a buscar
     * @return true si se borró un coche
     * @throws JAXBException si falla la persistencia XML
     */

    public boolean borrar(String matricula) throws JAXBException {
        Coche coche = buscar(matricula);
        if (coche == null) {
            return false;
        }
        concesionario.getCoches().remove(coche);
        guardarXml();
        return true;
    }

    /**
     * Modifica los datos de un coche sin cambiar su matrícula ni su ID.
     *
     * @param matricula matrícula del coche
     * @param marca nueva marca
     * @param modelo nuevo modelo
     * @param extras nuevos extras
     * @return true si se encontró el coche; false si no existe
     * @throws IllegalArgumentException si los datos son inválidos
     * @throws JAXBException si falla el guardado XML
     */

    public boolean modificar(String matricula, String marca, String modelo,
                             List<String> extras) throws JAXBException {

        Coche coche = buscar(matricula);

        if (coche == null) {
            return false;
        }

        // Comprueba los datos sin modificar el registro original.
        Coche candidato = new Coche(
                coche.getId(),
                coche.getMatricula(),
                marca,
                modelo,
                extras
        );

        validarCoche(candidato);

        coche.setMarca(candidato.getMarca());
        coche.setModelo(candidato.getModelo());
        coche.setExtras(candidato.getExtras());

        guardarXml();
        return true;
    }

    /**
     * Exporta toda la base de datos a JSON.
     *
     * @throws IOException si falla la escritura JSON
     */

    public void exportarConcesionarioJson() throws IOException {
        Path destino = path("json.concesionario.path");
        crearCarpeta(destino);
        json.writeValue(destino.toFile(), concesionario);
    }

    /**
     * Busca un coche y lo exporta como objeto JSON.
     *
     * @param matricula matrícula buscada
     * @throws IOException si falla la escritura JSON
     * @throws IllegalArgumentException si no existe el coche
     */
    public void exportarCocheJson(String matricula) throws IOException {
        Coche coche = buscar(matricula);
        if (coche == null) {
            throw new IllegalArgumentException(
                    "No existe un coche con matrícula " + matricula);
        }

        Path destino = path("json.coche.path");
        crearCarpeta(destino);
        json.writeValue(destino.toFile(), coche);
    }

    /**
     * Importa un concesionario JSON como nueva base de datos y lo persiste en XML.
     *
     * @throws IOException si falla la lectura JSON
     * @throws JAXBException si falla la escritura XML
     */
    public void importarConcesionarioJson() throws IOException, JAXBException {
        Concesionario nuevo = json.readValue(
                path("json.import.path").toFile(),
                Concesionario.class
        );

        if (nuevo == null) {
            throw new IllegalArgumentException(
                    "El JSON debe contener un concesionario, no null.");
        }

        validarMatriculas(nuevo.getCoches());

        concesionario = nuevo;
        normalizarIds();
        guardarXml();
    }

    /**
     * Importa un coche JSON y lo añade si su matrícula no existe.
     *
     * @throws IOException si falla la lectura JSON
     * @throws JAXBException si falla la persistencia XML
     */
    public void importarCocheJson() throws IOException, JAXBException {
        Coche coche = json.readValue(path("json.import.path").toFile(), Coche.class);
        if (!insertar(coche)) {
            throw new IllegalArgumentException(
                    "Ya existe un coche con matrícula " + coche.getMatricula());
        }
    }

    /**
     * Genera un informe resumen agrupado por marca y con el extra más repetido.
     *
     * @throws IOException si falla la escritura del informe
     */
    public void generarInforme() throws IOException {
        Path destino = path("informe.path");
        crearCarpeta(destino);

        Map<String, List<Coche>> porMarca = concesionario.getCoches().stream()
                .collect(Collectors.groupingBy(
                        Coche::getMarca, TreeMap::new, Collectors.toList()));

        Map<String, Long> frecuenciaExtras = concesionario.getCoches().stream()
                .flatMap(c -> c.getExtras().stream())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.groupingBy(
                        s -> s, TreeMap::new, Collectors.counting()));

        String resultadoExtra;
        if (frecuenciaExtras.isEmpty()) {
            resultadoExtra = "No hay extras registrados.";
        } else {
            long max = Collections.max(frecuenciaExtras.values());
            resultadoExtra = frecuenciaExtras.entrySet().stream()
                    .filter(e -> e.getValue() == max)
                    .map(e -> e.getKey() + " (" + e.getValue() + " veces)")
                    .collect(Collectors.joining(", "));
        }

        try (BufferedWriter writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            writer.write("INFORME DEL CONCESIONARIO\n");
            writer.write("=========================\n");
            writer.write("Número total de coches: " + concesionario.getCoches().size() + "\n\n");
            writer.write("COCHES AGRUPADOS POR MARCA\n");

            for (Map.Entry<String, List<Coche>> entrada : porMarca.entrySet()) {
                writer.write("\n" + entrada.getKey() + "\n");
                for (Coche coche : entrada.getValue()) {
                    writer.write("  - " + coche.getMatricula() + " | "
                            + coche.getModelo() + " | Extras: "
                            + coche.getExtras() + "\n");
                }
            }

            writer.write("\nEXTRA MÁS REPETIDO\n");
            writer.write(resultadoExtra + "\n");
        }
    }

    private Coche buscar(String matricula) {
        if (matricula == null) {
            return null;
        }

        matricula = matricula.trim();

        for (Coche coche : concesionario.getCoches()) {
            if (coche.getMatricula().equalsIgnoreCase(matricula)) {
                return coche;
            }
        }

        return null;
    }

    private void validarCoche(Coche coche) {
        if (coche == null) {
            throw new IllegalArgumentException("El coche no puede ser nulo.");
        }

        coche.setMatricula(limpiar(coche.getMatricula()));
        coche.setMarca(limpiar(coche.getMarca()));
        coche.setModelo(limpiar(coche.getModelo()));

        if (coche.getMatricula().isEmpty()
                || coche.getMarca().isEmpty()
                || coche.getModelo().isEmpty()) {

            throw new IllegalArgumentException(
                    "La matrícula, marca y modelo no pueden estar vacíos.");
        }

        List<String> extrasLimpios = new ArrayList<>();

        for (String extra : coche.getExtras()) {
            if (extra == null) {
                throw new IllegalArgumentException(
                        "Los extras no pueden contener valores nulos.");
            }

            extra = extra.trim();

            if (!extra.isEmpty()) {
                extrasLimpios.add(extra);
            }
        }

        coche.setExtras(extrasLimpios);
    }

    private void validarMatriculas(List<Coche> coches) {
        Set<String> matriculas = new HashSet<>();

        for (Coche coche : coches) {
            validarCoche(coche);

            String matricula = coche.getMatricula().toUpperCase(Locale.ROOT);

            if (!matriculas.add(matricula)) {
                throw new IllegalArgumentException(
                        "La matrícula " + coche.getMatricula() + " ya existe.");
            }
        }
    }

    private int siguenteId() {
        int mayorId = 0;
        for (Coche coche : concesionario.getCoches()) {
            if (coche.getId() > mayorId) {
                mayorId = coche.getId();
            }
        }
        return mayorId + 1;
    }

    private void normalizarIds() {
        int id = 1;
        for (Coche coche : concesionario.getCoches()) {
            coche.setId(id++);
        }
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private Path path(String clave) {
        String valor = config.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("La clave " + clave + " no tiene valor");
        }
        return Paths.get(valor.trim());
    }

    private void crearCarpeta(Path fichero) throws IOException {
        Path carpeta = fichero.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
    }

    private Concesionario leerXml(Path fichero) throws JAXBException {
        Unmarshaller unmarshaller = contextoXml.createUnmarshaller();
        return (Concesionario) unmarshaller.unmarshal(fichero.toFile());
    }

    private void guardarXml() throws JAXBException {
        try {
            crearCarpeta(xmlPath);
            Marshaller marshaller = contextoXml.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.marshal(concesionario, xmlPath.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Error al crear la carpeta del archivo XML", e);
        }
    }

}
