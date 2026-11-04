package org.example;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

// Representa el concesionario, e indica como se representará en el XML
@XmlRootElement(name = "concesionario-de-coches")
@XmlAccessorType(XmlAccessType.FIELD)

public class Concesionario {

    // Agrupa los coches dentro de una lista
    @XmlElementWrapper(name = "listado-coches")

    // Cada objeto de la lista es un elemento <coche>
    @XmlElement(name = "coche")
    private List<Coche> coches = new ArrayList<>();

    // Constructor vacio necesario para JAXB
    public Concesionario() {}

    // Getter para obtener la lista de coches
    public List<Coche> getCoches() {
        if (coches == null) {
            coches = new ArrayList<>();
        }
        return coches;
    }

    public void setCoches(List<Coche> coches) {
        this.coches = coches == null ? new ArrayList<>() : new ArrayList<>(coches);
    }

}
