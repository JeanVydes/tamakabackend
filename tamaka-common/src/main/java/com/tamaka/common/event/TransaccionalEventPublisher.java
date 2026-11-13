package com.tamaka.common.event;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Decorador que difiere la publicacion hasta el commit de la transaccion en curso.
 * Asi un consumidor nunca recibe un evento de un cambio que luego se revirtio.
 * Sin transaccion activa (p. ej., servicios MongoDB) publica de inmediato.
 * Limitacion conocida: si el proceso cae entre el commit y la entrega, el evento se pierde (outbox pendiente).
 */
public class TransaccionalEventPublisher implements EventPublisher, AutoCloseable {

    private final EventPublisher delegado;

    public TransaccionalEventPublisher(EventPublisher delegado) {
        this.delegado = delegado;
    }

    @Override
    public void publicar(DomainEvent evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delegado.publicar(evento);
                }
            });
            return;
        }
        delegado.publicar(evento);
    }

    public EventPublisher delegado() {
        return delegado;
    }

    @Override
    public void close() throws Exception {
        if (delegado instanceof AutoCloseable cerrable) {
            cerrable.close();
        }
    }
}
