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
        repository.registrarItem(tipo, itemId, categoria, Instant.now().toEpochMilli());
    }

    @Override
    public void registrarPreferencias(Long turistaId, Collection<String> categorias) {
        repository.reemplazarPreferencias(turistaId, categorias);
    }
}
