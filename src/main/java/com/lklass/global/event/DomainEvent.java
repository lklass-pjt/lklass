package com.lklass.global.event;

import java.util.Objects;
import java.util.UUID;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public record DomainEvent<T extends DomainEventPayload>(
        UUID eventId,
        DomainEventType eventType,
        T payload
) implements ResolvableTypeProvider {

    public DomainEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public static <T extends DomainEventPayload> DomainEvent<T> of(DomainEventType eventType, T payload) {
        return new DomainEvent<>(UUID.randomUUID(), eventType, payload);
    }

    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(payload));
    }
}
