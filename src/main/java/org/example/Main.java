package org.example;

import jakarta.xml.bind.JAXBException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Punto de entrada y menú de texto de la aplicación.
 */
public class Main {

    private static final Scanner SCANNER = new Scanner(System.in);

    /**
     * Inicia la aplicación.
     *
     * @param args argumentos de consola, no utilizados
     */
    public static void main(String[] args) {
        try {
            GestorConcesionario gestor = new GestorConcesionario();
            ejecutarMenu(gestor);
        } catch (IOException e) {
            System.err.println("Error de lectura/escritura o configuración: " + e.getMessage());
        } catch (JAXBException e) {
            System.err.println("Error de mapeo XML/JAXB: " + e.getMessage());
        } catch (ParserConfigurationException e) {
            System.err.println("Error al configurar el parser XML: " + e.getMessage());
        } catch (SAXException e) {
            System.err.println("El XML no es válido: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Datos inválidos: " + e.getMessage());
        } catch (IllegalStateException | ParseException e) {
            System.err.println("Configuración incorrecta: " + e.getMessage());
        }
    }

    private static void ejecutarMenu(GestorConcesionario gestor) {
        boolean continuar = true;

        while (continuar) {
            mostrarMenu();
            String opcion = leer("Elige una opción: ");

            try {
                switch (opcion) {
                    case "1" -> importarCsv(gestor);
                    case "2" -> insertarCoche(gestor);
                    case "3" -> ordenar(gestor);
                    case "4" -> borrar(gestor);
                    case "5" -> modificar(gestor);
                    case "6" -> exportarJson(gestor);
                    case "7" -> importarJson(gestor);
                    case "8" -> informe(gestor);
                    case "9" -> mostrarCoches(gestor);
                    case "0" -> continuar = false;
                    default -> System.out.println("Opción no válida.");
                }
            } catch (NoSuchFileException e) {
                System.out.println("No se encontró el coche o fichero: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error de archivo: " + e.getMessage());
            } catch (JAXBException e) {
                System.out.println("Error al guardar el XML: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Datos inválidos: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Error inesperado: " + e.getMessage());
            }
        }

        System.out.println("Programa finalizado.");
    }

    private static void mostrarMenu() {
        System.out.println("""
                
                ===== CONCESIONARIO =====
                1. Importar coches desde CSV
                2. Insertar coche nuevo
                3. Ordenar por matrícula
                4. Borrar coche por matrícula
                5. Modificar coche por matrícula
                6. Exportar a JSON
                7. Importar desde JSON
                8. Generar informe resumen
                9. Mostrar coches
                0. Salir
                """);
    }

    private static void importarCsv(GestorConcesionario gestor)
            throws IOException, JAXBException {
        gestor.importarCsv();
    }

    private static void insertarCoche(GestorConcesionario gestor)
            throws JAXBException {
        Coche coche = leerCoche();
        if (gestor.insertar(coche)) {
            System.out.println("Coche insertado y guardado en XML.");
        } else {
            System.out.println("Ya existe esa matrícula; no se ha insertado.");
        }
    }

    private static Coche leerCoche() {
        String matricula = leer("Matrícula: ");
        String marca = leer("Marca: ");
        String modelo = leer("Modelo: ");
        List<String> extras = leerExtras("Extras separados por | (vacío si no tiene): ");
        return new Coche(0, matricula, marca, modelo, extras);
    }

    private static void ordenar(GestorConcesionario gestor) throws JAXBException {
        gestor.ordenar();
        System.out.println("Coches ordenados y guardados en XML.");
    }

    private static void borrar(GestorConcesionario gestor) throws JAXBException {
        String matricula = leer("Matrícula a borrar: ");
        System.out.println(gestor.borrar(matricula)
                ? "Coche borrado y XML actualizado."
                : "No se encontró esa matrícula.");
    }

    private static void modificar(GestorConcesionario gestor) throws JAXBException {
        String matricula = leer("Matrícula del coche que quieres modificar: ");
        String marca = leer("Nueva marca: ");
        String modelo = leer("Nuevo modelo: ");
        List<String> extras = leerExtras("Nuevos extras separados por |: ");

        System.out.println(gestor.modificar(matricula, marca, modelo, extras)
                ? "Coche modificado y XML actualizado. La matrícula no se ha cambiado."
                : "No se encontró esa matrícula.");
    }

    private static void exportarJson(GestorConcesionario gestor) throws IOException {
        System.out.println("1. Exportar todo el concesionario");
        System.out.println("2. Exportar un coche por matrícula");
        String opcion = leer("Opción: ");

        if (opcion.equals("1")) {
            gestor.exportarConcesionarioJson();
            System.out.println("Concesionario exportado.");
        } else if (opcion.equals("2")) {
            gestor.exportarCocheJson(leer("Matrícula: "));
            System.out.println("Coche exportado.");
        } else {
            System.out.println("Opción no válida.");
        }
    }

    private static void importarJson(GestorConcesionario gestor)
            throws IOException, JAXBException {
        System.out.println("El fichero de importación configurado es: json.import.path");
        System.out.println("1. Importar un concesionario completo y reemplazar la base de datos");
        System.out.println("2. Importar un coche y añadirlo");
        String opcion = leer("Opción: ");

        if (opcion.equals("1")) {
            gestor.importarConcesionarioJson();
            System.out.println("Base de datos reemplazada y guardada en XML.");
        } else if (opcion.equals("2")) {
            gestor.importarCocheJson();
            System.out.println("Coche importado y guardado en XML.");
        } else {
            System.out.println("Opción no válida.");
        }
    }

    private static void informe(GestorConcesionario gestor) throws IOException {
        gestor.generarInforme();
        System.out.println("Informe generado en la ruta configurada.");
    }

    private static void mostrarCoches(GestorConcesionario gestor) {
        if (gestor.getConcesionario().getCoches().isEmpty()) {
            System.out.println("El concesionario está vacío.");
            return;
        }
        gestor.getConcesionario().getCoches().forEach(System.out::println);
    }

    private static List<String> leerExtras(String mensaje) {
        String texto = leer(mensaje);
        if (texto.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(texto.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String leer(String mensaje) {
        System.out.print(mensaje);
        return SCANNER.nextLine().trim();
    }
}