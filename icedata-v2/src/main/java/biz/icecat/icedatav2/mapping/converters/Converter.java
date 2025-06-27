package biz.icecat.icedatav2.mapping.converters;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface Converter<E, D, A> {

    E apiToEntity(A api);

    D entityToDomain(E entity);

    E domainToEntity(D domain);

    A entityToApi(E entity);

    default List<A> toListOfApis(List<@Valid E> entities) {
        return entities.stream()
                .map(this::entityToApi)
                .toList();
    }

    default List<E> toListOfEntities(List<@Valid A> apis) {
        return apis.stream()
                .map(this::apiToEntity)
                .toList();
    }

    default List<E> domainsListToListOfEntities(List<@Valid D> domains) {
        return domains.stream()
                .map(this::domainToEntity)
                .toList();
    }

    default List<D> entityListToDomainList(List<@Valid E> entities) {
        return entities.stream()
                .map(this::entityToDomain)
                .toList();
    }
}
