package com.tamaka.recomendaciones.repository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Persistencia sobre Redis. Guarda proyecciones reconstruibles a partir de eventos:
 * no es fuente de verdad. Si se pierde, se reconstruye con los eventos siguientes.
 */
@Repository
public class RecomendacionRedisRepository {

    private static final String PREFIJO_ITEMS = "reco:items:";
    private static final String PREFIJO_CATEGORIA = "reco:cat:";
    private static final String PREFIJO_PREFERENCIAS = "reco:pref:";
    private static final String PREFIJO_CATEGORIA_DE_ITEM = "reco:itemcat:";

    private final StringRedisTemplate redis;

    public RecomendacionRedisRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void registrarItem(String tipo, Long itemId, String categoria, double marcaTiempo) {
        String id = itemId.toString();
        // Si el item cambio de categoria, sale del conjunto anterior.
        Object anterior = redis.opsForHash().get(PREFIJO_CATEGORIA_DE_ITEM + tipo, id);
        if (anterior != null && !anterior.toString().equals(categoria)) {
            redis.opsForSet().remove(PREFIJO_CATEGORIA + tipo + ":" + anterior, id);
        }
        redis.opsForZSet().add(PREFIJO_ITEMS + tipo, id, marcaTiempo);
        redis.opsForSet().add(PREFIJO_CATEGORIA + tipo + ":" + categoria, id);
        redis.opsForHash().put(PREFIJO_CATEGORIA_DE_ITEM + tipo, id, categoria);
    }

    /** Quita un item de la proyeccion (cerrado o desactivado). Idempotente. */
    public void retirarItem(String tipo, Long itemId) {
        String id = itemId.toString();
        Object categoria = redis.opsForHash().get(PREFIJO_CATEGORIA_DE_ITEM + tipo, id);
        if (categoria != null) {
            redis.opsForSet().remove(PREFIJO_CATEGORIA + tipo + ":" + categoria, id);
        }
        redis.opsForZSet().remove(PREFIJO_ITEMS + tipo, id);
        redis.opsForHash().delete(PREFIJO_CATEGORIA_DE_ITEM + tipo, id);
    }

    public void reemplazarPreferencias(Long turistaId, Collection<String> categorias) {
        String clave = PREFIJO_PREFERENCIAS + turistaId;
        redis.delete(clave);
        if (!categorias.isEmpty()) {
            redis.opsForSet().add(clave, categorias.toArray(String[]::new));
        }
    }

    public Set<String> categoriasDe(Long turistaId) {
        Set<String> categorias = redis.opsForSet().members(PREFIJO_PREFERENCIAS + turistaId);
        return categorias == null ? Set.of() : categorias;
    }

    /** Ids de items de un tipo que pertenecen a alguna de las categorias. */
    public Set<String> idsPorCategorias(String tipo, Collection<String> categorias) {
        Set<String> ids = new LinkedHashSet<>();
        for (String categoria : categorias) {
            Set<String> miembros = redis.opsForSet().members(PREFIJO_CATEGORIA + tipo + ":" + categoria);
            if (miembros != null) {
                ids.addAll(miembros);
            }
        }
        return ids;
    }

    /** Ids mas recientes de un tipo (orden descendente por marca de tiempo). */
    public List<String> masRecientes(String tipo, int limite) {
        Set<String> ids = redis.opsForZSet().reverseRange(PREFIJO_ITEMS + tipo, 0, limite - 1L);
        return ids == null ? List.of() : List.copyOf(ids);
    }
}
