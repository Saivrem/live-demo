package biz.icecat.icedatav2.utils.containers;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.model.Container;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class TestDockerContainer <SELF extends TestDockerContainer<SELF>> extends FixedHostPortGenericContainer<SELF> {

    public static final String DEFAULT_TEST_NETWORK = "icedata-test-network";

    @Getter
    private final String containerName;

    protected abstract Logger logger();

    protected abstract Class<SELF> selfClass();

    private final AtomicInteger usagesCount = new AtomicInteger(0);

    protected TestDockerContainer(Class<?> testOrTestSuiteClass, @NonNull String dockerImageName, @NonNull String containerPrefix) {
        super(dockerImageName);

        this.containerName = generateContainerName(containerPrefix, testOrTestSuiteClass.getSimpleName());
        String hostName = generateHostName();
        withCreateContainerCmdModifier(cmd -> {
            cmd.withName(containerName);
            cmd.withHostName(hostName);
            cmd.withAliases(hostName);
        });
        waitingFor(new NoOpWaitingStrategy());
        withNetworkMode(DEFAULT_TEST_NETWORK);
        withEnv("MODE", "dev");
        withEnv("DEV", "true");
    }

    public static <T extends TestDockerContainer> T create(Supplier<T> supplier, Class<T> type, Class<?> testGroup) {
        return ResourceRegistry.getByType(type, testGroup).orElseGet(supplier).andRegister();
    }

    public void start() {
        if (usagesCount.incrementAndGet() == 1) {
            removeContainerWithTheSameNameIfExists(containerName);
            super.start();
        }
    }

    @Override
    public void stop() {
        int currentUsagesCount = usagesCount.decrementAndGet();
        if (currentUsagesCount == 0) {
            logger().info("Stopping {}.", selfClass().getTypeName());
            super.stop();
        }
    }

    @NonNull
    private String generateHostName() {
        String hostName = containerName.toLowerCase();
        assertThat(hostName.length())
                .describedAs("Generated hostname should be less than or equal to 64. Got %s(%d)", hostName, hostName.length())
                .isLessThan(64);
        return hostName;
    }

    public static String generateContainerName(String prefix, String name) {
        return prefix.toLowerCase() + name;
    }

    private void removeContainerWithTheSameNameIfExists(String containerName) {
        List<Container> containers = dockerClient.listContainersCmd().withNameFilter(Collections.singletonList(containerName)).exec();
        int containersSize = CollectionUtils.size(containers);
        if (containersSize > 1) {
            throw new IllegalStateException("Several containers found by name " + containerName);
        }
        if (containersSize == 0) {
            return;
        }

        Container container = containers.get(0);
        try {
            logger().info("Removing previous container with the same name '{}': {}", containerName, container.getId());
            try {
                dockerClient.removeContainerCmd(container.getId()).withRemoveVolumes(true).withForce(true).exec();
            } catch (InternalServerErrorException e) {
                logger().warn("Exception when removing container (due to {})", e.getMessage());
            }
        } catch (DockerException e) {
            logger().warn("Error encountered shutting down container (ID: {}) - it may not been stopped, or may already be stopped: {}",
                    container.getId(), e.getMessage());
        }
    }

    private static class NoOpWaitingStrategy implements WaitStrategy {
        @Override
        public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {

        }

        @Override
        public WaitStrategy withStartupTimeout(Duration startupTimeout) {
            return this;
        }
    }
}
