package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

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
     */

    public GestorConcesionario() throws IOException, JAXBException {
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
     * Importa coches desde CSV, comprueba que no existan duplicados
     *
     * @throws IOException si falla la lectura del CSV
     * @throws JAXBException si falla la persistencia XML
     * @throws IllegalArgumentException si los datos son inválidos
     */
    public void importarCsv() throws IOException, JAXBException {
        Path csvPath = path("csv.path");
        int importados = 0;
        int duplicados = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {

            // Descarta la primera línea, que contiene los nombres de los campos.
            reader.readLine();

            String linea;

            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }

                // El -1 conserva el campo de extras cuando está vacío.
                String[] campos = linea.split(";", -1);

                if (campos.length != 4) {
                    System.out.println(
                            "Fila ignorada: debe tener exactamente 4 campos.");
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

                Coche coche = new Coche(
                        siguienteId(),
                        matricula,
                        campos[1].trim(),
                        campos[2].trim(),
                        extras
                );

                try {
                    validarCoche(coche);
                    concesionario.getCoches().add(coche);
                    importados++;

                } catch (IllegalArgumentException e) {
                    System.out.println("Fila ignorada: " + e.getMessage());
                }
            }
        }

        // Guarda los coches añadidos cuando termina correctamente la lectura.
        if (importados > 0) {
            guardarXml();
        }

        System.out.println("Coches importados: " + importados);
        System.out.println("Matrículas duplicadas omitidas: " + duplicados);
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

        // Asigna el siguente id disponible al coche
        coche.setId(siguienteId());

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
                // Compara las matrículas sin distinguir entre mayúsculas y minúsculas
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
        // Busca el coche por la matricula indicada
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
    public boolean modificar(String matricula, String marca, String modelo, List<String> extras) throws JAXBException {
        Coche coche = buscar(matricula);

        if (coche == null) {
            return false;
        }

        // Crea un coche temporal con los nuevos datos
        Coche temporal = new Coche(
                coche.getId(),
                coche.getMatricula(),
                marca,
                modelo,
                extras
        );

        validarCoche(temporal);

        // Aplica los cambios al coche original
        coche.setMarca(temporal.getMarca());
        coche.setModelo(temporal.getModelo());
        coche.setExtras(temporal.getExtras());

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
        // Convierte el path a un archivo y escribbe el resultado en el archivo
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
            throw new IllegalArgumentException("No existe un coche con matrícula " + matricula);
        }

        Path destino = path("json.coche.path");
        crearCarpeta(destino);
        json.writeValue(destino.toFile(), coche);
    }

    /**
     * Importa/reemplaza un concesionario JSON.
     *
     * @throws IOException si falla la lectura JSON
     * @throws JAXBException si falla la escritura XML
     */
    public void importarConcesionarioJson() throws IOException, JAXBException {
        // Lee el archivo JSON y convierte el contenido en un objeto Concesionario
        Concesionario nuevo = json.readValue(path("json.import.path").toFile(), Concesionario.class);

        if (nuevo == null) {
            throw new IllegalArgumentException("El JSON debe contener un concesionario, no null.");
        }

        validarMatriculas(nuevo.getCoches());

        concesionario = nuevo;
        // Asignar los nuevos id de forma consecutiva
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
        // Lee el archivo JSON y convierte el contenido en un objeto Coche
        Coche coche = json.readValue(path("json.import.path").toFile(), Coche.class);

        // guardarXml() se llama dentro de insertar()
        if (!insertar(coche)) {
            throw new IllegalArgumentException("Ya existe un coche con matrícula " + coche.getMatricula());
        }
    }

    /**
     * Genera un informe resumen agrupado por marca y con el extra más repetido.
     *
     * METODO REALIZADO CON AYUDA DE IA
     *
     * @throws IOException si falla la escritura del informe
     */
    /**
     * Genera un informe con el total de coches, los coches agrupados
     * por marca y los extras más repetidos.
     *
     * @throws IOException si falla la escritura del informe
     */
    public void generarInforme() throws IOException {
        Path destino = path("informe.path");
        crearCarpeta(destino);

        //Mapas para agrupar los coches por marca y contar los extras.
        Map<String, List<Coche>> porMarca = new TreeMap<>();
        Map<String, Integer> frecuenciaExtras = new TreeMap<>();

        for (Coche coche : concesionario.getCoches()) {
            String marca = coche.getMarca();

            // Si la marca aún no está registrada, crea su lista.
            if (!porMarca.containsKey(marca)) {
                porMarca.put(marca, new ArrayList<>());
            }

            // Añade el coche a su lista correspondiente.
            porMarca.get(marca).add(coche);

            // Recorre los extras del coche.
            for (String extra : coche.getExtras()) {
                extra = extra.trim();

                if (!extra.isEmpty()) {
                    // Consulta la cantidad de apariciones del extra.
                    int cantidad = frecuenciaExtras.getOrDefault(extra, 0);
                    // Suma 1 al contador de apariciones.
                    frecuenciaExtras.put(extra, cantidad + 1);
                }
            }
        }

        // Busca la cantidad máxima de apariciones de un extra.
        int maximo = 0;

        for (int cantidad : frecuenciaExtras.values()) {
            if (cantidad > maximo) {
                maximo = cantidad;
            }
        }

        // Abre el archivo para escribir el informe.
        try (BufferedWriter writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {

            writer.write("INFORME DEL CONCESIONARIO\n");
            writer.write("=========================\n");
            writer.write("Número total de coches: " + concesionario.getCoches().size() + "\n\n");
            writer.write("COCHES AGRUPADOS POR MARCA\n");

            // Recorre las marcas y los coches de cada una.
            for (String marca : porMarca.keySet()) {
                writer.write("\n" + marca + "\n");

                for (Coche coche : porMarca.get(marca)) {
                    writer.write("  - " + coche.getMatricula()
                            + " | " + coche.getModelo()
                            + " | Extras: " + coche.getExtras() + "\n");
                }
            }

            writer.write("\nEXTRAS MÁS REPETIDOS\n");

            if (frecuenciaExtras.isEmpty()) {
                writer.write("No hay extras registrados.\n");
            } else {
                // Muestra todos los extras empatados con la frecuencia máxima.
                for (String extra : frecuenciaExtras.keySet()) {
                    int cantidad = frecuenciaExtras.get(extra);

                    if (cantidad == maximo) {
                        writer.write(extra + " (" + cantidad + " apariciones)\n");
                    }
                }
            }
        }
    }

    /**
     * Busca un coche por su matrícula.
     *
     * @param matricula La matrícula del coche a buscar.
     * @return El coche encontrado o null si no se encuentra.
     */

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

    /**
     * Valida los campos de un coche.
     *
     * @param coche El coche a validar.
     */

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

            throw new IllegalArgumentException("La matrícula, marca o modelo no pueden estar vacíos.");
        }

        List<String> extrasLimpios = new ArrayList<>();

        for (String extra : coche.getExtras()) {
            if (extra == null) {
                throw new IllegalArgumentException("Los extras no pueden contener valores nulos.");
            }

            extra = extra.trim();

            if (!extra.isEmpty()) {
                extrasLimpios.add(extra);
            }
        }

        coche.setExtras(extrasLimpios);
    }

    /**
     * Valida las matrículas de una lista de coches.
     *
     * @param coches La lista de coches a validar.
     */

    private void validarMatriculas(List<Coche> coches) {
        Set<String> matriculas = new HashSet<>();

        for (Coche coche : coches) {
            validarCoche(coche);

            String matricula = coche.getMatricula().toUpperCase(Locale.ROOT);

            if (!matriculas.add(matricula)) {
                throw new IllegalArgumentException("La matrícula " + coche.getMatricula() + " ya existe.");
            }
        }
    }

    /**
     * Obtiene el siguiente ID disponible para un coche.
     *
     * @return El siguiente ID disponible.
     */
    private int siguienteId() {
        int mayorId = 0;
        for (Coche coche : concesionario.getCoches()) {
            if (coche.getId() > mayorId) {
                mayorId = coche.getId();
            }
        }
        return mayorId + 1;
    }

    /**
     * Normaliza los IDs de los coches en el concesionario.
     */
    private void normalizarIds() {
        int id = 1;
        for (Coche coche : concesionario.getCoches()) {
            coche.setId(id++);
        }
    }

    /**
     * Limpia el valor de una cadena, eliminando espacios en blanco al inicio y al final.
     *
     * @param valor El valor a limpiar.
     * @return Devuelve el valor limpio.
     */
    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /**
     * Obtiene la ruta absoluta de un fichero a partir de una clave en el archivo de configuración.
     *
     * @param clave La clave del archivo de configuración.
     * @return La ruta absoluta del fichero.
     * @throws IllegalStateException Si la clave no tiene valor.
     */

    private Path path(String clave) {
        String valor = config.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("La clave " + clave + " no tiene valor");
        }
        return Paths.get(valor.trim());
    }

    /**
     * Crea la carpeta padre del archivo indicado y las carpetas intermedias
     * que sean necesarias. Si ya existen, no realiza cambios.
     *
     * @param fichero El fichero para el que se quiere crear la carpeta.
     * @throws IOException Si ocurre un error al crear la carpeta.
     */
    private void crearCarpeta(Path fichero) throws IOException {
        Path carpeta = fichero.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
    }

    /**
     * Lee un archivo XML y devuelve un objeto Concesionario.
     *
     * @param fichero El archivo XML a leer.
     * @return El objeto Concesionario leído.
     * @throws JAXBException Si ocurre un error al leer el archivo XML.
     */

    private Concesionario leerXml(Path fichero) throws JAXBException {
        Unmarshaller unmarshaller = contextoXml.createUnmarshaller();
        return (Concesionario) unmarshaller.unmarshal(fichero.toFile());
    }

    /**
     * Guarda el objeto Concesionario en un archivo XML.
     *
     * @throws JAXBException Si ocurre un error al guardar el archivo XML.
     */
    private void guardarXml() throws JAXBException {
        try {
            crearCarpeta(xmlPath);
            Marshaller marshaller = contextoXml.createMarshaller();
            // Activar el formato de salida formateado
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            // Establecer la codificación UTF-8
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            // Escribe el objeto Concesionario en el archivo XML
            marshaller.marshal(concesionario, xmlPath.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Error al crear la carpeta del archivo XML", e);
        }
    }

}
