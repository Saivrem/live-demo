package biz.icecat.icedatav2.test_containers.overrider;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PropertiesOverridingPostProcessor implements BeanPostProcessor {

    private List<PropertiesOverrider> propertiesOverriders;

    @Override
    public Object postProcessBeforeInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        CollectionUtils.emptyIfNull(propertiesOverriders)
                .stream().filter(overrider -> overrider.isApplicable(bean))
                .forEach(overrider -> overrider.override(bean));
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        return bean;
    }

    @Autowired(required = false)
    public void setPropertiesOverriders(List<PropertiesOverrider> propertiesOverriders) {
        this.propertiesOverriders = propertiesOverriders;
        CollectionUtils.emptyIfNull(propertiesOverriders)
                .forEach(propertiesOverrider -> {
                    log.info("Loaded property overrider: {}", propertiesOverrider.getClass().getSimpleName());
                });
    }
}
