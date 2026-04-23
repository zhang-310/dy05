package cn.gaifan.douyinOperations.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;

@Component
public class CustomHealthIndicator implements HealthIndicator {
    private final DataSource dataSource;
    public CustomHealthIndicator(DataSource dataSource) { this.dataSource = dataSource; }
    @Override
    public Health health() {
        try {
            boolean dbHealthy = checkDatabase();
            boolean diskHealthy = checkDiskSpace();
            if (dbHealthy && diskHealthy) {
                return Health.up().withDetail("database", "connected").withDetail("disk", "sufficient").build();
            } else {
                return Health.down().withDetail("database", dbHealthy ? "connected" : "disconnected").withDetail("disk", diskHealthy ? "sufficient" : "low").build();
            }
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection()) { return conn.isValid(3); } catch (Exception e) { return false; }
    }
    private boolean checkDiskSpace() {
        File root = new File("/");
        long freeSpace = root.getFreeSpace();
        long totalSpace = root.getTotalSpace();
        double freePercent = (double) freeSpace / totalSpace * 100;
        return freePercent > 10;
    }
}
