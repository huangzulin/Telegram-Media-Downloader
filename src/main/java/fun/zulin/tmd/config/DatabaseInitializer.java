package fun.zulin.tmd.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化和修复工具
 * 用于处理表结构初始化和修复
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始数据库初始化...");
        
        try {
            // 初始化或修复 download_item 表
            initializeDownloadItemTable();
            
            log.info("数据库初始化完成");
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw e;
        }
    }
    
    /**
     * 初始化或修复 download_item 表
     */
    private void initializeDownloadItemTable() {
        try {
            // 检查表是否存在
            if (!tableExists("download_item")) {
                log.info("download_item 表不存在，创建新表");
                createDownloadItemTable();
                return;
            }
            
            // 检查 description 字段是否存在
            if (!columnExists("download_item", "description")) {
                log.info("description 字段不存在，添加字段");
                addDescriptionColumn();
            } else {
                log.info("description 字段已存在");
            }
            
            // 检查并添加tags字段
            if (!columnExists("download_item", "tags")) {
                String addTagsColumnSql = "ALTER TABLE download_item ADD COLUMN tags TEXT";
                jdbcTemplate.execute(addTagsColumnSql);
                log.info("已添加 tags 字段到 download_item 表");
            } else {
                log.info("tags 字段已存在");
            }
            
            // 检查并添加thumbnail字段
            if (!columnExists("download_item", "thumbnail")) {
                String addThumbnailColumnSql = "ALTER TABLE download_item ADD COLUMN thumbnail TEXT";
                jdbcTemplate.execute(addThumbnailColumnSql);
                log.info("已添加 thumbnail 字段到 download_item 表");
            } else {
                log.info("thumbnail 字段已存在");
            }
            
            // 检查并添加其他新字段
            checkAndAddNewColumns();
            
        } catch (Exception e) {
            log.error("初始化 download_item 表失败", e);
            throw e;
        }
    }
    
    /**
     * 检查并添加其他新增字段
     */
    private void checkAndAddNewColumns() {
        try {
            // 添加complete_time字段
            if (!columnExists("download_item", "complete_time")) {
                String addCompleteTimeColumnSql = "ALTER TABLE download_item ADD COLUMN complete_time TEXT";
                jdbcTemplate.execute(addCompleteTimeColumnSql);
                log.info("已添加 complete_time 字段到 download_item 表");
            } else {
                log.info("complete_time 字段已存在");
            }
            
            // 添加update_time字段
            if (!columnExists("download_item", "update_time")) {
                String addUpdateTimeColumnSql = "ALTER TABLE download_item ADD COLUMN update_time TEXT";
                jdbcTemplate.execute(addUpdateTimeColumnSql);
                log.info("已添加 update_time 字段到 download_item 表");
            } else {
                log.info("update_time 字段已存在");
            }
            
            // 添加download_byte_per_sec字段
            if (!columnExists("download_item", "download_byte_per_sec")) {
                String addDownloadBytePerSecColumnSql = "ALTER TABLE download_item ADD COLUMN download_byte_per_sec INTEGER";
                jdbcTemplate.execute(addDownloadBytePerSecColumnSql);
                log.info("已添加 download_byte_per_sec 字段到 download_item 表");
            } else {
                log.info("download_byte_per_sec 字段已存在");
            }
            
            // 添加download_update_time字段
            if (!columnExists("download_item", "download_update_time")) {
                String addDownloadUpdateTimeColumnSql = "ALTER TABLE download_item ADD COLUMN download_update_time TEXT";
                jdbcTemplate.execute(addDownloadUpdateTimeColumnSql);
                log.info("已添加 download_update_time 字段到 download_item 表");
            } else {
                log.info("download_update_time 字段已存在");
            }
            
            // 添加download_count字段
            if (!columnExists("download_item", "download_count")) {
                String addDownloadCountColumnSql = "ALTER TABLE download_item ADD COLUMN download_count INTEGER DEFAULT 0";
                jdbcTemplate.execute(addDownloadCountColumnSql);
                log.info("已添加 download_count 字段到 download_item 表");
            } else {
                log.info("download_count 字段已存在");
            }
            

            
        } catch (Exception e) {
            log.error("添加新字段时发生错误", e);
            // 不抛出异常，继续执行其他操作
        }
    }
    
    /**
     * 检查表是否存在
     */
    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
            String result = jdbcTemplate.queryForObject(sql, String.class, tableName);
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查字段是否存在
     */
    private boolean columnExists(String tableName, String columnName) {
        try {
            String sql = """
                SELECT COUNT(*) 
                FROM pragma_table_info(?) 
                WHERE name = ?
                """;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 创建 download_item 表
     */
    private void createDownloadItemTable() {
        String createTableSql = """
            CREATE TABLE download_item (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                description TEXT NULL,
                filename TEXT NULL,
                file_id INTEGER NULL,
                massage_id INTEGER NULL,
                unique_id TEXT NULL,
                file_size INTEGER NULL,
                downloaded_size INTEGER NULL,
                caption TEXT NULL,
                tags TEXT NULL,
                thumbnail TEXT NULL,
                state TEXT NULL,
                create_time TEXT NULL,
                complete_time TEXT NULL,
                update_time TEXT NULL,
                download_byte_per_sec INTEGER NULL,
                download_update_time TEXT NULL,
                download_count INTEGER DEFAULT 0
            )
            """;
        jdbcTemplate.execute(createTableSql);
        log.info("成功创建 download_item 表");
    }
    
    /**
     * 添加 description 字段
     */
    private void addDescriptionColumn() {
        try {
            String addColumnSql = "ALTER TABLE download_item ADD COLUMN description TEXT";
            jdbcTemplate.execute(addColumnSql);
            log.info("成功添加 description 字段");
        } catch (Exception e) {
            log.error("添加 description 字段失败", e);
            // 如果是字段已存在的错误，忽略
            if (e.getMessage().contains("duplicate column name") || 
                e.getMessage().contains("already exists")) {
                log.info("description 字段已存在，忽略错误");
            } else {
                throw e;
            }
        }
    }
}