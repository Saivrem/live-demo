package biz.icecat.icedatav2.it;

import biz.icecat.icedatav2.IcedataV2Application;
import biz.icecat.icedatav2.test_containers.mysql.MySqlContainer;
import org.junit.ClassRule;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.Network;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@SpringBootTest
@Import(BaseIT.BaseTestConfiguration.class)
@ContextConfiguration(classes = {IcedataV2Application.class}, loader = SpringBootContextLoader.class)
public class BaseIT {

    private static final Class<?> TEST_CLASS = BaseIT.class;

    @ClassRule
    public static MySqlContainer mySqlContainer = MySqlContainer.create(TEST_CLASS);

    @ClassRule
    public static Network network = Network.newNetwork();

    static {
        mySqlContainer.withNetwork(network).start();
    }

    @TestConfiguration
    public static class BaseTestConfiguration {

        @Bean
        @Primary
        public Clock testClock() {
            return Clock.fixed(Instant.ofEpochMilli(1731870252907L), ZoneOffset.UTC);
        }
    }
}


