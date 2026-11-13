package com.tamaka.recomendaciones.service;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tamaka.recomendaciones.repository.RecomendacionRedisRepository;

/**
 * Regla: prioriza items de las categorias preferidas por el turista; si no hay preferencias o no alcanzan,
 * completa con los mas recientes. Sin preferencias no se favorece a ningun operador (criterio de no sesgo).
 */
@Service
public class RecomendacionServiceImpl implements RecomendacionService {

    static final String TIPO_ATRACTIVO = "ATRACTIVO";
    static final String ESTADO_CERRADO = "CERRADO";
    static final String CATEGORIA_POR_DEFECTO = "GENERAL";

    private final RecomendacionRedisRepository repository;
    private final int limite;

    public RecomendacionServiceImpl(RecomendacionRedisRepository repository,
                                    @Value("${tamaka.recomendaciones.limite:10}") int limite) {
        this.repository = repository;
        this.limite = limite;
    }

    @Override
    public List<Long> recomendar(String tipo, Long turistaId) {
        Set<String> ids = new LinkedHashSet<>();
        Set<String> categorias = repository.categoriasDe(turistaId);
        if (!categorias.isEmpty()) {
            ids.addAll(repository.idsPorCategorias(tipo, categorias));
        }
        if (ids.size() < limite) {
            ids.addAll(repository.masRecientes(tipo, limite));
        }
        return ids.stream()
                .limit(limite)
                .map(Long::valueOf)
                .toList();
    }

    @Override
    public void registrarItem(String tipo, Long itemId, String categoria) {
        String cat = categoria == null || categoria.isBlank() ? CATEGORIA_POR_DEFECTO : categoria.trim();
        repository.registrarItem(tipo, itemId, cat, Instant.now().toEpochMilli());
    }

    @Override
    public void registrarAtractivo(Long atractivoId, String categoria, String estado) {
        if (ESTADO_CERRADO.equals(estado)) {
            repository.retirarItem(TIPO_ATRACTIVO, atractivoId);
            return;
        }
        registrarItem(TIPO_ATRACTIVO, atractivoId, categoria);
    }

    @Override
    public void retirarItem(String tipo, Long itemId) {
        repository.retirarItem(tipo, itemId);
    }

    @Override
    public void registrarPreferencias(Long turistaId, Collection<String> categorias) {
        repository.reemplazarPreferencias(turistaId, categorias);
    }
}
