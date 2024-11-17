package biz.icecat.icedatav2.test_containers.mysql;

import biz.icecat.icedatav2.test_containers.TestDockerContainer;
import biz.icecat.icedatav2.utils.TestUtils;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.testcontainers.shaded.com.github.dockerjava.core.command.ExecStartResultCallback;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static biz.icecat.icedatav2.utils.TestUtils.isBuildRunningInDockerContainer;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Slf4j
public class MySqlContainer extends TestDockerContainer<MySqlContainer> {

    private final int port;
    private final String dbName;
    private final List<String> createdUsers = new ArrayList<>();

    public static MySqlContainer create(Class<?> testClass) {
        return create(() -> new MySqlContainer(testClass), MySqlContainer.class, testClass);
    }

    private MySqlContainer(Class<?> testClass) {
        super(testClass, "mysql:8.4.3", "mysql-");
        this.port = TestUtils.randomPort();
        withFixedExposedPort(this.port, 3306);
        withCreateContainerCmdModifier(cmd -> cmd.withAttachStderr(true).withAttachStdout(true));
        dbName = RandomStringUtils.randomAlphabetic(10).toLowerCase();
        withEnv("MYSQL_DATABASE", dbName);
        withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "true");
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        long currentTime = System.currentTimeMillis();
        await().pollInterval(2, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(() -> {
            ExecCreateCmdResponse execCommand = dockerClient.execCreateCmd(getContainerId())
                    .withAttachStdout(true)
                    .withCmd("mysqladmin", "-h", "localhost")
                    .exec();
            try (OutputStream stdout = new ByteArrayOutputStream();
                 OutputStream stderr = new ByteArrayOutputStream()) {
                dockerClient.execStartCmd(execCommand.getId())
                        .exec(new ExecStartResultCallback(stdout, stderr))
                        .awaitStarted()
                        .awaitCompletion();
                if (stdout.toString().contains("mysqld is alive")) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Error executing isReady", e);
            }
            return false;
        });
        log.info("MySQL is ready. Startup time: {}ms", (System.currentTimeMillis() - currentTime));
    }

    public String getMySqlConnectionString() {
        String host = isBuildRunningInDockerContainer() ? getContainerName() : "127.0.0.1";
        int port = isBuildRunningInDockerContainer() ? 3306 : this.port;
        return String.format("jdbc:mysql://%s/%s", host + ":" + port, dbName);
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

    private boolean userDoesNotExist(String username) {
        if (createdUsers.contains(username)) {
            log.debug("MySQL user {} already exists.", username);
            return false;
        }
        return true;
    }

    private void createSingleUser(String username, String password) {
        await().pollInterval(2, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(() -> {
            try (OutputStream createOut = new ByteArrayOutputStream(); OutputStream grantOut = new ByteArrayOutputStream()) {
                String create = String.format("CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s';", username, password);
                ExecCreateCmdResponse createCommand = dockerClient.execCreateCmd(getContainerId())
                        .withAttachStdout(true)
                        .withCmd("mysql", "-uroot", "-e", create)
                        .exec();

                dockerClient.execStartCmd(createCommand.getId())
                        .exec(new ExecStartResultCallback(createOut, null))
                        .awaitStarted()
                        .awaitCompletion();
                if (StringUtils.isBlank(createOut.toString())) {
                    log.info("MySQL user '{}' has been created.", username);
                    String grant = String.format("GRANT ALL PRIVILEGES ON *.* TO '%s'@'%%';", username);
                    ExecCreateCmdResponse grantCommand = dockerClient.execCreateCmd(getContainerId())
                            .withAttachStdout(true)
                            .withCmd("mysql", "-uroot", "-e", grant)
                            .exec();
                    dockerClient.execStartCmd(grantCommand.getId())
                            .exec(new ExecStartResultCallback(grantOut, null))
                            .awaitStarted()
                            .awaitCompletion();
                    if (StringUtils.isBlank(grantOut.toString())) {
                        return true;
                    } else {
                        throw new IllegalStateException("Failed to grant permissions to MySQL user. Output: " + grantOut);
                    }
                } else {
                    throw new IllegalStateException("Failed to create MySQL user. Output: " + createOut);
                }
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException("Failed to create MySQL user. Reason: " + e.getMessage(), e);
            }
        });
    }

    private void checkConnectionReadiness(String username, String password) {
        AtomicInteger retryCount = new AtomicInteger(0);
        await().pollInterval(2, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(() -> {
            int currentRetryCount = retryCount.get();
            String retryInfo = currentRetryCount == 0 ? "" : String.format("Retry %s/%s.", currentRetryCount, currentRetryCount);
            log.debug("Testing database connection for user {}. {}", username, retryInfo);
            ExecCreateCmdResponse execCommand =
                    dockerClient.execCreateCmd(getContainerId())
                            .withAttachStdout(true)
                            .withAttachStderr(true)
                            .withCmd("mysql", "-u", username, String.format("-p%s", password), "-e", "SHOW DATABASES")
                            .exec();
            try (OutputStream stdout = new ByteArrayOutputStream();
                 OutputStream stderr = new ByteArrayOutputStream()) {
                dockerClient.execStartCmd(execCommand.getId())
                        .exec(new ExecStartResultCallback(stdout, stderr))
                        .awaitStarted()
                        .awaitCompletion();
                String stdoutString = stdout.toString();
                if (stdoutString.contains(dbName)) {
                    try (Connection ignored = DriverManager.getConnection(getMySqlConnectionString(), username, password)) {
                        log.info("MySQL database {} is accepting connections from user {}.", dbName, username);
                        return true;
                    } catch (Exception ignored) {
                    }
                } else {
                    print(stdoutString, stderr.toString());
                }
                retryCount.incrementAndGet();
                return false;
            }
        });
    }

    private void print(String stdoutString, String stderrString) {
        if (StringUtils.isNotBlank(stdoutString)) {
            log.debug("--- {}", stdoutString);
        }
        if (StringUtils.isNotBlank(stderrString)) {
            log.debug("--- {}", stderrString);
        }
    }

    @Override
    protected Logger logger() {
        return log;
    }

    @Override
    protected Class<MySqlContainer> selfClass() {
        return MySqlContainer.class;
    }
}
