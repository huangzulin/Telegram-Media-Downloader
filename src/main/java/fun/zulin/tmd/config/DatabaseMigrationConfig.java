package fun.zulin.tmd.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库迁移配置类
 * 处理数据库表结构的自动更新，专门解决 chat_id 列缺失问题
 */
@Slf4j
@Component
public class DatabaseMigrationConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateDatabase() {
        try {
            log.info("开始数据库迁移检查...");
            
            // 检查并添加 chat_id 列
            addChatIdColumnIfNotExists();
            
            log.info("数据库迁移检查完成");
        } catch (Exception e) {
            log.error("数据库迁移过程中发生严重错误", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    /**
     * 检查并添加 chat_id 列（如果不存在）
     */
    private void addChatIdColumnIfNotExists() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            
            // 检查 download_item 表是否存在
            if (!tableExists(connection, "download_item")) {
                log.info("download_item 表不存在，AutoTable 应该会自动创建");
                return;
            }
            
            // 检查 chat_id 列是否存在
            boolean columnExists = columnExists(connection, "download_item", "chat_id");
            
            if (!columnExists) {
                log.info("发现缺失的 chat_id 列，正在添加...");
                String sql = "ALTER TABLE download_item ADD COLUMN chat_id BIGINT";
                jdbcTemplate.execute(sql);
                log.info("chat_id 列添加成功");
            } else {
                log.info("chat_id 列已存在，无需添加");
            }
            
        } catch (SQLException e) {
            log.error("数据库连接检查时发生错误", e);
            throw new RuntimeException("数据库连接失败", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.warn("关闭数据库连接时发生错误", e);
                }
            }
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE"});
        boolean exists = tables.next();
        tables.close();
        return exists;
    }

    /**
     * 检查列是否存在
     */
    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet columns = metaData.getColumns(null, null, tableName, columnName);
        boolean exists = columns.next();
        columns.close();
        return exists;
    }
}