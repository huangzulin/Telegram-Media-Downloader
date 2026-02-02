package fun.zulin.tmd.data.item;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.zulin.tmd.telegram.DownloadManage;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

        var wrapper = new LambdaQueryWrapper<DownloadItem>();
        wrapper.in(DownloadItem::getState, DownloadState.Created.name(), DownloadState.Downloading.name(), DownloadState.Pause.name())
                .orderByAsc(DownloadItem::getId);
        var items = this.baseMapper.selectList(wrapper);
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


}
