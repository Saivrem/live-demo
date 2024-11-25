package biz.icecat.icedatav2.utils.containers.mysql;

import biz.icecat.icedatav2.utils.TestUtils;
import biz.icecat.icedatav2.utils.containers.TestDockerContainer;
import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static biz.icecat.icedatav2.utils.TestUtils.isBuildRunningInDockerContainer;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Slf4j
public class MySqlContainer extends TestDockerContainer<MySqlContainer> {
    private static final String ROOT = "root";
    private static final String RETRY_PATTERN = "Retry %s/%s.";
    private static final int AWAIT_TIMEOUT = 120;
    private static final int POLL_INTERVAL = 2;
    private static final int MAX_RETRIES = AWAIT_TIMEOUT / POLL_INTERVAL;

    private final int port;
    private final String dbName;
    private final List<String> createdUsers = new ArrayList<>();

    public static MySqlContainer create(Class<?> testClass) {
        return create(() -> new MySqlContainer(testClass), MySqlContainer.class, testClass);
    }

    private MySqlContainer(Class<?> testClass) {
        super(testClass, "mysql:8.4", "mysql-");
        port = TestUtils.randomPort();
        dbName = RandomStringUtils.randomAlphabetic(10).toLowerCase();
        this.withFixedExposedPort(port, 3306)
                .withCreateContainerCmdModifier(cmd -> cmd.withAttachStderr(true).withAttachStdout(true))
                .withEnv("MYSQL_DATABASE", dbName)
                .withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "true");
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        long currentTime = System.currentTimeMillis();
        await().pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    ExecResult ping = execInContainer("mysqladmin", "-h", "localhost");
                    return ping.getStdout().contains("mysqld is alive");
                });
        log.info("MySQL is ready. Startup time: {}ms", (System.currentTimeMillis() - currentTime));
    }

    public void createUser(String username, String password) {
        if (userDoesNotExist(username)) {
            synchronized (this) {
                if (userDoesNotExist(username)) {
                    createSingleUser(username, password);
                    checkConnectionReadiness(username, password);
                    createdUsers.add(username);
                }
            }
        }
    }

    private void createSingleUser(String username, String password) {
        await().pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    String userQuery = createUserQuery(username, password);
                    String grantQuery = grantQuery(username);

                    ExecResult execResult = execInContainer(buildQuery(ROOT, null, userQuery));

                    if (isBlank(execResult.getStderr())) {
                        log.info("MySQL user '{}' has been created.", username);
                        execResult = execInContainer(buildQuery(ROOT, null, grantQuery));

                        if (isBlank(execResult.getStderr())) {
                            log.info("Permissions granted to {}", username);
                            return true;
                        }
                    }

                    return false;
                });
    }

    private void checkConnectionReadiness(String username, String password) {
        AtomicInteger retryCount = new AtomicInteger(0);
        await().pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    log.info(
                            "Testing database connection for user {}. {}",
                            username,
                            RETRY_PATTERN.formatted(retryCount.getAndIncrement(), MAX_RETRIES)
                    );

                    ExecResult showDatabases = execInContainer(buildQuery(username, password, showDatabasesQuery()));
                    log.debug("Exec Result OUT: {}", showDatabases.getStdout());
                    log.debug("Exec Result ERR: {}", showDatabases.getStderr());
                    return showDatabases.getStdout().contains(dbName) &&
                            verifyConnection(username, password);
                });
    }

    @Override
    protected Logger logger() {
        return log;
    }

    @Override
    protected Class<MySqlContainer> selfClass() {
        return MySqlContainer.class;
    }

    public String getMySqlConnectionString() {
        String host = isBuildRunningInDockerContainer() ? getContainerName() : "127.0.0.1";
        int port = isBuildRunningInDockerContainer() ? 3306 : this.port;
        return String.format("jdbc:mysql://%s/%s", host + ":" + port, dbName);
    }

    private boolean userDoesNotExist(String username) {
        if (createdUsers.contains(username)) {
            log.debug("MySQL user {} already exists.", username);
            return false;
        }
        return true;
    }

    private boolean verifyConnection(String username, String password) {
        try (Connection connection = DriverManager.getConnection(getMySqlConnectionString(), username, password)) {
            log.debug("MySQL database {} is accepting connections from user {}.", dbName, username);
            return true;
        } catch (Exception exception) {
            log.debug(exception.getMessage());
        }
        return false;
    }

    private String[] buildQuery(String user, String pass, String query) {
        List<String> result = new ArrayList<>();
        result.add("mysql");
        result.add("-u");
        result.add(user);
        if (pass != null) {
            result.add("-p%s".formatted(pass));
        }
        result.add("-e");
        result.add(query);
        return result.toArray(String[]::new);
    }

    private static String createUserQuery(String user, String pass) {
        return "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s';".formatted(user, pass);
    }

    private static String grantQuery(String user) {
        return "GRANT ALL PRIVILEGES ON *.* TO '%s'@'%%';".formatted(user);
    }

    private static String showDatabasesQuery() {
        return "SHOW DATABASES";
    }
}
