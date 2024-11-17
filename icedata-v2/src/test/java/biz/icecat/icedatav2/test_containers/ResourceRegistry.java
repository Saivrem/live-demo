package biz.icecat.icedatav2.test_containers;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@UtilityClass
public class ResourceRegistry {

    private static final Map<String, Object> COMPONENTS = new HashMap<>();
    private static final ThreadLocal<Class<?>> TEST_GROUPS = new ThreadLocal<>();

    public static <T> T findNonNullByType(Class<T> type) {
        T resource = findByType(type);
        assertThat(resource)
                .describedAs("Expected %s to be initialized.", type.getSimpleName())
                .isNotNull();
        return resource;
    }

    public static <T> T findByType(Class<T> type) {
        String componentKey = getComponentKey(TEST_GROUPS.get(), type);
        if (StringUtils.isBlank(componentKey)) {
            return null;
        }
        return type.cast(COMPONENTS.get(componentKey));
    }

    public static <T> RegisterResourceStep<T> getByType(Class<T> type) {
        return getByType(type, TEST_GROUPS.get());
    }

    public static <T> RegisterResourceStep<T> getByType(Class<T> type, Class<?> testGroup) {
        return new RegisterResourceStep<>(type, testGroup);
    }

    private static <T> String getComponentKey(Class<?> testGroup, Class<T> type) {
        if (testGroup == null) {
            return null;
        }
        return testGroup.getTypeName() + "-" + type.getTypeName();
    }

    public static class RegisterResourceStep<T> {
        private final Class<T> type;
        private final Class<?> testGroup;
        private Supplier<T> supplier;

        public RegisterResourceStep(Class<T> type, Class<?> testGroup) {
            this.type = type;
            this.testGroup = testGroup;
        }

        public RegisterResourceStep<T> orElseGet(@NonNull Supplier<T> supplier) {
            this.supplier = supplier;
            return this;
        }

        public T andRegister() {
            TEST_GROUPS.set(testGroup);
            String componentKey = getComponentKey(testGroup, type);
            Object existingComponent = COMPONENTS.get(componentKey);

            if (existingComponent != null) {
                return castAndReturn(existingComponent);
            }
            synchronized (componentKey.intern()) {
                existingComponent = COMPONENTS.get(componentKey);
                if (existingComponent != null) {
                    castAndReturn(existingComponent);
                }
                T newComponent = supplier.get();
                if (newComponent == null) {
                    throw new IllegalStateException("Supplier returned null.");
                }
                COMPONENTS.put(componentKey, newComponent);
                return newComponent;
            }
        }

        private T castAndReturn(Object existingComponent) {
            log.debug("[{}] using existing component of type {}.", testGroup.getSimpleName(), type.getTypeName());
            return type.cast(existingComponent);
        }
    }
}
