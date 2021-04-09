package com.hltech.store.versioning;

import java.util.Objects;

public interface EventVersioningStrategy<E> {

    E toEvent(String eventJson, String eventName, int eventVersion);

    String toName(Class<? extends E> eventType);

    int toVersion(Class<? extends E> eventType);

    String toJson(E event);

    class NameAndVersion {

        private final String name;
        private final int version;

        public NameAndVersion(
                String name,
                int version
        ) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return this.name;
        }

        public int getVersion() {
            return this.version;
        }

        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof NameAndVersion)) {
                return false;
            }
            final NameAndVersion other = (NameAndVersion) obj;
            final Object this$name = this.name;
            final Object other$name = other.name;
            if (!Objects.equals(this$name, other$name)) {
                return false;
            }
            if (this.version != other.version) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            final int prime = 59;
            int result = 1;
            final Object $name = this.name;
            result = result * prime + ($name == null ? 43 : $name.hashCode());
            result = result * prime + this.version;
            return result;
        }

    }

    class EventTypeMappingException extends RuntimeException {

        EventTypeMappingException(String message) {
            super(message);
        }

    }

    class NonUniqueMappingException extends RuntimeException {

        NonUniqueMappingException(String message) {
            super(message);
        }

    }

}
