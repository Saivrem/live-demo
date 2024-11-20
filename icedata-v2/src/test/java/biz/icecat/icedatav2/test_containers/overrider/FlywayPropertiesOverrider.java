package biz.icecat.icedatav2.test_containers.overrider;

import biz.icecat.icedatav2.test_containers.ResourceRegistry;
import biz.icecat.icedatav2.test_containers.mysql.MySqlContainer;
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
        FlywayProperties properties = (FlywayProperties) propertiesBean;
        MySqlContainer mySqlContainer = ResourceRegistry.findByType(MySqlContainer.class);
        if (mySqlContainer != null) {
            String connectionString = mySqlContainer.getMySqlConnectionString();
            log.info("Overriding Datasource URL. Previous value: {}, current value {}.", properties.getUrl(), connectionString);
            properties.setUrl(connectionString);
            mySqlContainer.createUser(properties.getUser(), properties.getPassword());
        }
    }
}
