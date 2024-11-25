package biz.icecat.icedatav2.utils.containers.overrider;

import biz.icecat.icedatav2.utils.containers.ResourceRegistry;
import biz.icecat.icedatav2.utils.containers.mysql.MySqlContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlywayPropertiesOverrider implements PropertiesOverrider {
    @Override
    public boolean isApplicable(Object propertiesBean) {
        return propertiesBean instanceof FlywayProperties;
    }

    @Override
    public void override(Object propertiesBean) {
        MySqlContainer mySqlContainer = ResourceRegistry.findByType(MySqlContainer.class);
        if (mySqlContainer == null) {
            return;
        }

        FlywayProperties flywayProperties = (FlywayProperties) propertiesBean;
        mySqlContainer.createUser(flywayProperties.getUser(), flywayProperties.getPassword());
        String connectionString = mySqlContainer.getMySqlConnectionString();
        log.info("Overriding Flyway URL. Previous value: {}, current value: {}.", flywayProperties.getUrl(), connectionString);
        flywayProperties.setUrl(connectionString);
    }
}
