package fun.zulin.tmd.data.item;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.zulin.tmd.telegram.DownloadManage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Setter
@Service
@Transactional(rollbackFor = Exception.class)
public class DownloadItemServiceImpl extends ServiceImpl<DownloadItemMapper, DownloadItem> implements DownloadItemService {

    @Override
    public DownloadItem getByUniqueId(String uniqueId) {
        var wrapper = new LambdaQueryWrapper<DownloadItem>();
        return this.getOne(wrapper.eq(DownloadItem::getUniqueId, uniqueId));
    }

    @Override
    public List<DownloadItem> getDownloadedItem() {

        var wrapper = new LambdaQueryWrapper<DownloadItem>();

        wrapper.eq(DownloadItem::getState, DownloadState.Complete.name()).orderByDesc(DownloadItem::getCreateTime);
        var items = this.baseMapper.selectList(wrapper);
        return CollectionUtil.emptyIfNull(items);
    }


    @Override
    public List<DownloadItem> getDownloadingItemsFromDB() {
        log.info("=== 执行getDownloadingItemsFromDB查询 ===");

        var wrapper = new LambdaQueryWrapper<DownloadItem>();
        wrapper.in(DownloadItem::getState, DownloadState.Created.name(),
                DownloadState.Downloading.name(),
                DownloadState.Pause.name(),
                DownloadState.Failed.name()
        ).orderByAsc(DownloadItem::getId);

        log.info("查询条件: 状态 in ('Created', 'Downloading', 'Pause','Failed')");

        var items = this.baseMapper.selectList(wrapper);
        log.info("查询结果数量: {}", items != null ? items.size() : 0);

        if (items != null && !items.isEmpty()) {
            items.forEach(item ->
                    log.info("未完成任务 - ID: {}, 文件名: {}, 状态: {}, UniqueId: {}",
                            item.getId(), item.getFilename(), item.getState(), item.getUniqueId()));
        }

        return CollectionUtil.emptyIfNull(items);
    }

    @Override
    public List<DownloadItem> getDownloading() {
        return DownloadManage.getItems();
    }

    @Override
    public boolean removeByUniqueId(String uniqueId) {
        var wrapper = new LambdaQueryWrapper<DownloadItem>();
        wrapper.eq(DownloadItem::getUniqueId, uniqueId);
        return this.remove(wrapper);
    }

    @Override
    public List<DownloadItem> searchByTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return new ArrayList<>();
        }

        var wrapper = new LambdaQueryWrapper<DownloadItem>();
        wrapper.like(DownloadItem::getTags, tag.trim())
                .eq(DownloadItem::getState, DownloadState.Complete.name())
                .orderByDesc(DownloadItem::getCreateTime);

        return this.list(wrapper);
    }

    @Override
    public List<String> getAllUniqueTags() {
        var wrapper = new LambdaQueryWrapper<DownloadItem>();
        wrapper.isNotNull(DownloadItem::getTags)
                .ne(DownloadItem::getTags, "");

        List<DownloadItem> items = this.list(wrapper);
        return items.stream()
                .flatMap(item -> Arrays.stream(item.getTags().split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * 获取失败的下载项
     */
    public List<DownloadItem> getFailedItemsFromDB() {
        var wrapper = new LambdaQueryWrapper<DownloadItem>();
        wrapper.eq(DownloadItem::getState, DownloadState.Failed.name())
                .orderByAsc(DownloadItem::getId);
        
        var items = this.baseMapper.selectList(wrapper);
        log.info("查询到失败任务数量: {}", items != null ? items.size() : 0);
        
        return CollectionUtil.emptyIfNull(items);
    }


}
