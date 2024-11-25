package biz.icecat.icedatav2.mapping.converters;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface Converter<E, A> {

    E apiToEntity(A api);

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
}
