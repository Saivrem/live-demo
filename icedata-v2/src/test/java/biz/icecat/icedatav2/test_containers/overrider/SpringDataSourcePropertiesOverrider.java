package biz.icecat.icedatav2.test_containers.overrider;

import biz.icecat.icedatav2.test_containers.ResourceRegistry;
import biz.icecat.icedatav2.test_containers.mysql.MySqlContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringDataSourcePropertiesOverrider implements PropertiesOverrider {

    @Override
    public boolean isApplicable(Object propertiesBean) {
        return propertiesBean instanceof DataSourceProperties;
    }

    @Override
    public void override(Object propertiesBean) {
        MySqlContainer mySqlContainer = ResourceRegistry.findByType(MySqlContainer.class);
        if (mySqlContainer == null) {
            return;
        }

        DataSourceProperties properties = (DataSourceProperties) propertiesBean;
        mySqlContainer.createUser(properties.getUsername(), properties.getPassword());
        String connectionString = mySqlContainer.getMySqlConnectionString();
        log.info("Overriding Datasource URL. Previous value: {}, current value {}.", properties.getUrl(), connectionString);
        properties.setUrl(connectionString);
    }
}
