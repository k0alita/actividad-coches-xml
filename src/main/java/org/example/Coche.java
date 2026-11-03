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

    @XmlAttribute(name = "marca", required = true)
    private String marca;

    @XmlElement(name = "modelo", required = true)
    private String modelo;

    @XmlElementWrapper(name = "equipamiento")
    @XmlElement(name = "extra")
    private List<String> extras = new ArrayList<>();

    public Coche() {

    }

    public Coche(int id, String matricula, String marca, String modelo, List<String>extras){
        this.id = id;
        this.matricula = matricula;
        this.marca = marca;
        this.modelo = modelo;
        this.extras = extras == null ? new ArrayList<>() : new ArrayList<>(extras);
    }

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
}
