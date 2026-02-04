package fun.zulin.tmd.data.item;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.tangzc.autotable.annotation.AutoTable;
import com.tangzc.autotable.annotation.Ignore;
import com.tangzc.autotable.annotation.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@Builder
@TableName("download_item")
@AutoTable("download_item")
@NoArgsConstructor
@AllArgsConstructor
public class DownloadItem {

    @TableId(type = IdType.AUTO)
    @PrimaryKey(true)
    private Long id;


    /**
     * 原始文件描述，可能包含特殊字符
     * 用于显示和用户界面展示
     */
    private String description;

    /**
     * 实际文件系统中的文件名
     * 已经过处理，去除特殊字符，适合作为文件名使用
     */
    private String filename;

    private Integer fileId;

    private Long massageId;


    private String uniqueId;


    private long fileSize;



    private long downloadedSize;


    private String caption;

    /**
     * 视频标签，用于分类和搜索
     * 存储格式：tag1,tag2,tag3
     */
    private String tags;

    /**
     * 视频封面图片文件名
     * 存储本地生成的视频截图
     */
    private String thumbnail;

    @Ignore
    @TableField(exist = false)
    private Float progress;

    private String state;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 下载完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 下载速度（字节/秒）
     */
    private Long downloadBytePerSec;

    /**
     * 下载更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime downloadUpdateTime;

    /**
     * 下载计数器
     */
    @Builder.Default
    private Integer downloadCount = 0;
    
    // 添加getter方法确保不会返回null
    public Integer getDownloadCount() {
        return downloadCount != null ? downloadCount : 0;
    }

    public long getDownloadBytePerSec() {
        var timeDiff = Duration.between(this.getDownloadUpdateTime(), LocalDateTime.now(ZoneId.of("Asia/Shanghai"))).toSeconds();
        if (timeDiff > 2) {
            return 0;
        }
        return downloadBytePerSec != null ? downloadBytePerSec : 0;
    }

    public LocalDateTime getDownloadUpdateTime() {
        if (downloadUpdateTime == null) {
            return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        }
        return downloadUpdateTime;
    }

    public Float getProgress() {
        if (fileSize <= 0) return 0.0f;
        return (float) downloadedSize / (float) fileSize * 100;
    }

}
