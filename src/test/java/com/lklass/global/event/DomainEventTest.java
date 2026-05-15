package com.lklass.global.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;

class DomainEventTest {

    @Test
    @DisplayName("DomainEvent는 eventId, 이벤트 타입, payload를 함께 보존한다")
    void preserveEventIdEventTypeAndPayload() {
        // given
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        TestPayload payload = new TestPayload(1L, "message");

        // when
        DomainEvent<TestPayload> event = new DomainEvent<>(eventId, DomainEventType.ENROLLMENT_CREATED, payload);

        // then
        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.eventType()).isEqualTo(DomainEventType.ENROLLMENT_CREATED);
        assertThat(event.payload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("DomainEvent.of는 호출자가 eventId를 만들지 않아도 새로운 UUID를 생성한다")
    void createEventIdWithFactoryMethod() {
        // given
        TestPayload payload = new TestPayload(1L, "message");

        // when
        DomainEvent<TestPayload> event = DomainEvent.of(DomainEventType.ENROLLMENT_CREATED, payload);

        // then
        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo(DomainEventType.ENROLLMENT_CREATED);
        assertThat(event.payload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("DomainEvent는 Spring 이벤트 리스너가 payload 제네릭 타입을 해석할 수 있도록 ResolvableType을 제공한다")
    void provideResolvableTypeWithPayloadGeneric() {
        // given
        TestPayload payload = new TestPayload(1L, "message");
        DomainEvent<TestPayload> event = DomainEvent.of(DomainEventType.ENROLLMENT_CREATED, payload);

        // when
        ResolvableType resolvableType = event.getResolvableType();

        // then
        assertThat(resolvableType.resolve()).isEqualTo(DomainEvent.class);
        assertThat(resolvableType.getGeneric(0).resolve()).isEqualTo(TestPayload.class);
    }

    @Test
    @DisplayName("DomainEvent는 eventId, 이벤트 타입, payload 중 하나라도 null이면 생성할 수 없다")
    void rejectNullRequiredFields() {
        // given
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        TestPayload payload = new TestPayload(1L, "message");

        // when & then
        assertThatThrownBy(() -> new DomainEvent<>(null, DomainEventType.ENROLLMENT_CREATED, payload))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("eventId must not be null");
        assertThatThrownBy(() -> new DomainEvent<>(eventId, null, payload))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("eventType must not be null");
        assertThatThrownBy(() -> new DomainEvent<TestPayload>(eventId, DomainEventType.ENROLLMENT_CREATED, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("payload must not be null");
    }

    private record TestPayload(Long id, String message) implements DomainEventPayload {
    }
}
