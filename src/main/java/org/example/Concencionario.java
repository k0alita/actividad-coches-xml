package org.example;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "concesionario-de-coches")
@XmlAccessorType(XmlAccessType.FIELD)

public class Concencionario {
    @XmlElementWrapper(name = "listado-coches")
    @XmlElement(name = "coche")
    private List<Coche> coches = new ArrayList<>();

    public Concencionario() {

    }

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
