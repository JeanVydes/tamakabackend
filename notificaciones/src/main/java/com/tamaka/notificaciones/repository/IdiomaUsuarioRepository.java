package com.tamaka.notificaciones.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.tamaka.notificaciones.domain.IdiomaUsuario;

public interface IdiomaUsuarioRepository extends MongoRepository<IdiomaUsuario, Long> {
}
