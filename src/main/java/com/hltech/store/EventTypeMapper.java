package com.hltech.store;

import java.util.Collection;
import java.util.Objects;

public interface EventTypeMapper<E> {

    String toName(Class<? extends E> eventType);

    int toVersion(Class<? extends E> eventType);

    Class<? extends E> toType(String eventName, int eventVersion);

    void registerMapping(TypeNameAndVersion<? extends E> mapping);

    default void registerMapping(Class<? extends E> eventType, String eventName, int eventVersion) {
        registerMapping(new TypeNameAndVersion<E>(eventType, eventName, eventVersion));
    }

    default void registerMappings(Collection<TypeNameAndVersion<E>> mappings) {
        mappings.forEach(this::registerMapping);
    }

    class TypeNameAndVersion<E> {

        private final Class<? extends E> type;
        private final NameAndVersion nameAndVersion;

        public TypeNameAndVersion(Class<? extends E> type, NameAndVersion nameAndVersion) {
            this.type = type;
            this.nameAndVersion = nameAndVersion;
        }

        public TypeNameAndVersion(Class<? extends E> type, String name, int version) {
            this.type = type;
            this.nameAndVersion = new NameAndVersion(name, version);
        }

        public Class<? extends E> getType() {
            return this.type;
        }

        public String getName() {
            return this.nameAndVersion.name;
        }

        public int getVersion() {
            return this.nameAndVersion.version;
        }

        public NameAndVersion getNameAndVersion() {
            return this.nameAndVersion;
        }

        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof TypeNameAndVersion)) {
                return false;
            }
            final TypeNameAndVersion other = (TypeNameAndVersion) obj;
            final Object this$name = this.type;
            final Object other$name = other.type;
            if (!Objects.equals(this$name, other$name)) {
                return false;
            }
            final Object this$nameAndVersion = this.getNameAndVersion();
            final Object other$nameAndVersion = other.getNameAndVersion();
            if (!Objects.equals(this$nameAndVersion, other$nameAndVersion)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            final int prime = 59;
            int result = 1;
            final Object $name = this.type;
            result = result * prime + ($name == null ? 43 : $name.hashCode());
            final Object $nameAndVersion = this.getNameAndVersion();
            result = result * prime + ($nameAndVersion == null ? 43 : $nameAndVersion.hashCode());
            return result;
        }

    }

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
