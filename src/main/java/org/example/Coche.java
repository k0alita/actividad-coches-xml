package org.example;

import jakarta.xml.bind.annotation.*;
import org.glassfish.jaxb.runtime.XmlAccessorFactory;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)

public class Coche {
    @XmlAttribute(name = "id")
    private int id;

    @XmlElement(name = "matricula-coche", required = true)
    private String matricula;

    @XmlElement(name = "marca", required = true)
    private String marca;

    @XmlElement(name = "modelo", required = true)
    private String modelo;

    // Agrupa los extras dentro del elemento <equipamiento>.
    @XmlElementWrapper(name = "equipamiento")
    @XmlElement(name = "extra")
    private List<String> extras = new ArrayList<>();

    public Coche() {

    }

    /**
     * Constructor principal para la creacion de un coche.
     *
     * @param id identificador del coche
     * @param matricula matrícula del coche
     * @param marca marca del coche
     * @param modelo modelo del coche
     * @param extras extras del coche
     */
    public Coche(int id, String matricula, String marca, String modelo, List<String>extras){
        this.id = id;
        this.matricula = matricula;
        this.marca = marca;
        this.modelo = modelo;
        // Crea una copia de la lista recibida o una vacía si recibe null.
        this.extras = extras == null ? new ArrayList<>() : new ArrayList<>(extras);
    }

    // Getter para obtener la lista de extras
    public List<String> getExtras() {
        if (extras == null) {
            return new ArrayList<>();
        }
        return extras;
    }

    public void setExtras(List<String> extras) {
        this.extras = extras == null ? new ArrayList<>() : new ArrayList<>(extras);
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Devuelve los datos del coche como texto para mostrarlos por consola.
     *
     * @return texto con el ID, matrícula, marca, modelo y extras
     */
    @Override
    public String toString() {
        return "ID: " + id + " | Matrícula: " + matricula
                + " | Marca: " + marca + " | Modelo: " + modelo
                + " | Extras: " + getExtras();
    }
}
