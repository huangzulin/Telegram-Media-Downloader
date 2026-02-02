package fun.zulin.tmd.data.item;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DownloadItemService extends IService<DownloadItem> {


    DownloadItem getByUniqueId(String uniqueId);

    List<DownloadItem> getDownloadedItem();

    List<DownloadItem> getDownloadingItemsFromDB();

    List<DownloadItem> getDownloading();
    
    boolean removeByUniqueId(String uniqueId);
    
    /**
     * 根据标签搜索下载项
     * @param tag 标签名称
     * @return 匹配的下载项列表
     */
    List<DownloadItem> searchByTag(String tag);
    
    /**
     * 获取所有唯一的标签
     * @return 标签列表
     */
    List<String> getAllUniqueTags();
}
