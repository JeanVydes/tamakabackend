package com.tamaka.comercios.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Servicio ofrecido por un comercio. Los atributos variables viven en JSONB (RNF Flexibilidad).
 */
@Entity
@Table(name = "servicio")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comercio_id", nullable = false)
    private Comercio comercio;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "min_personas", nullable = false)
    private int minPersonas;

    @Column(name = "max_personas", nullable = false)
    private int maxPersonas;

    @Column(nullable = false)
    private boolean activo;

    /** Clave del objeto en R2; null si no tiene portada. */
    @Column(name = "imagen_key", length = 512)
    private String imagenKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> atributos;

    protected Servicio() {
    }

    private Servicio(Comercio comercio, String nombre, BigDecimal precioBase,
                     int minPersonas, int maxPersonas, Map<String, Object> atributos) {
        this.comercio = comercio;
        this.nombre = nombre;
        this.precioBase = precioBase;
        this.minPersonas = minPersonas;
        this.maxPersonas = maxPersonas;
        this.atributos = atributos == null ? Map.of() : Map.copyOf(atributos);
        this.activo = true;
    }

    public static Servicio crear(Comercio comercio, String nombre, BigDecimal precioBase,
                                 int minPersonas, int maxPersonas, Map<String, Object> atributos) {
        return new Servicio(comercio, nombre, precioBase, minPersonas, maxPersonas, atributos);
    }

    /**
     * Actualizacion parcial: los valores nulos conservan el dato actual.
     * El rango de personas ya validado llega desde el servicio.
     */
    public void actualizar(String nombre, BigDecimal precioBase, Integer minPersonas, Integer maxPersonas,
                           Map<String, Object> atributos) {
        if (nombre != null) {
            this.nombre = nombre;
        }
        if (precioBase != null) {
            this.precioBase = precioBase;
        }
        if (minPersonas != null) {
            this.minPersonas = minPersonas;
        }
        if (maxPersonas != null) {
            this.maxPersonas = maxPersonas;
        }
        if (atributos != null) {
            this.atributos = Map.copyOf(atributos);
        }
    }

    public void cambiarImagen(String clave) {
        this.imagenKey = clave;
    }

    public String getImagenKey() {
        return imagenKey;
    }

    public void cambiarActivo(boolean nuevoEstado) {
        this.activo = nuevoEstado;
    }

    public Long getId() {
        return id;
    }

    public Comercio getComercio() {
        return comercio;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public int getMinPersonas() {
        return minPersonas;
    }

    public int getMaxPersonas() {
        return maxPersonas;
    }

    public boolean isActivo() {
        return activo;
    }

    public Map<String, Object> getAtributos() {
        return atributos;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Servicio that && id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
