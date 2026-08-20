package org.nana.testsupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * Test data helpers backed by Flyway and plain JDBC so tests never depend on the
 * reactive session factory of the application under test.
 */
@ApplicationScoped
public class TestDataSupport {

    @Inject
    Flyway flyway;

    @Inject
    DataSource dataSource;

    public void reset() {
        flyway.clean();
        flyway.migrate();
    }

    public long quotaCount() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("select count(*) from fastDownloadQuota");
                ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long createPending(String md5, String title, String author, String extension, String requestedBy) {
        String sql = "insert into download (id, md5, title, author, extension, requestedBy, status, requestedAt) "
                + "values (nextval('download_SEQ'), ?, ?, ?, ?, ?, 'PENDING', now()) returning id";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, md5);
            statement.setString(2, title);
            statement.setString(3, author);
            statement.setString(4, extension);
            statement.setString(5, requestedBy);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertDdosGuardCookies(String cookieHeader) {
        String sql = "insert into ddosGuardCookies (id, cookieHeader, updatedAt) "
                + "values (nextval('ddosGuardCookies_SEQ'), ?, now())";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cookieHeader);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String ddosGuardCookieHeader() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("select cookieHeader from ddosGuardCookies order by id desc");
                ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getString(1) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRequestedAt(long id, Instant requestedAt) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("update download set requestedAt = ? where id = ?")) {
            statement.setObject(1, OffsetDateTime.ofInstant(requestedAt, ZoneOffset.UTC));
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
