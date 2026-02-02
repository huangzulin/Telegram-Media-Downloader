class VideoGallery {
    constructor() {
        this.videos = [];
        this.filteredVideos = [];
        this.allTags = [];
        this.currentSort = 'date-desc';
        this.currentTagFilter = '';
        this.popularTags = ['教程', '娱乐', '学习', '生活', '科技', '游戏', '音乐', '电影'];
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadVideos();
    }

    bindEvents() {
        // 统一搜索事件（同时支持视频和标签搜索）
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.handleSearchInput(e.target.value);
            });
            
            // 回车键搜索
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.performSearch();
                }
            });
        }

        // 排序事件
        const sortSelect = document.getElementById('sortSelect');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                this.currentSort = e.target.value;
                this.sortVideos();
                this.renderVideos();
            });
        }

        // 模态框关闭事件
        window.addEventListener('click', (e) => {
            const modal = document.getElementById('videoModal');
            const tagModal = document.getElementById('tagModal');
            if (e.target === modal) {
                this.closeVideoModal();
            }
            if (e.target === tagModal) {
                this.closeTagModal();
            }
        });

        // ESC键关闭模态框
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.closeVideoModal();
                this.closeTagModal();
            }
        });

        // 新标签输入框回车事件
        const newTagInput = document.getElementById('newTagInput');
        if (newTagInput) {
            newTagInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.addTag();
                }
            });
        }
    }

    async loadVideos() {
        this.showLoading(true);
        
        try {
            const response = await fetch('/api/downloads/completed');
            if (response.ok) {
                const data = await response.json();
                this.videos = Array.isArray(data) ? data : (data.data || []);
                
                // 过滤出视频文件
                this.videos = this.videos.filter(item => 
                    this.isVideoFile(item.filename)
                );
                
                this.sortVideos();
                this.updateStats();
                this.renderVideos();
                await this.loadAllTags();
            } else {
                throw new Error('获取视频列表失败');
            }
        } catch (error) {
            console.error('加载视频失败:', error);
            this.showError('加载视频列表失败，请稍后重试');
        } finally {
            this.showLoading(false);
        }
    }

    async loadAllTags() {
        try {
            const response = await fetch('/api/downloads/tags');
            if (response.ok) {
                const tags = await response.json();
                this.allTags = Array.isArray(tags) ? tags : (tags.data || []);
                this.renderTagsCloud();
                this.renderPopularTags();
            }
        } catch (error) {
            console.error('加载标签失败:', error);
        }
    }

    async searchByTag(tag) {
        try {
            const response = await fetch(`/api/downloads/search/tag/${encodeURIComponent(tag)}`);
            if (response.ok) {
                const videos = await response.json();
                return Array.isArray(videos) ? videos : (videos.data || []);
            }
        } catch (error) {
            console.error('按标签搜索失败:', error);
        }
        return [];
    }

    async addTagsToVideo(uniqueId, tags) {
        try {
            const response = await fetch(`/api/downloads/${uniqueId}/tags`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(tags)
            });
            
            if (response.ok) {
                return true;
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || '添加标签失败');
            }
        } catch (error) {
            console.error('添加标签失败:', error);
            throw error;
        }
    }

    async removeTagFromVideo(uniqueId, tag) {
        try {
            const response = await fetch(`/api/downloads/${uniqueId}/tags/${encodeURIComponent(tag)}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                return true;
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || '删除标签失败');
            }
        } catch (error) {
            console.error('删除标签失败:', error);
            throw error;
        }
    }

    isVideoFile(filename) {
        if (!filename) return false;
        const videoExtensions = ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.webm', '.mkv'];
        const lowerFilename = filename.toLowerCase();
        return videoExtensions.some(ext => lowerFilename.endsWith(ext));
    }

    // 处理搜索输入
    handleSearchInput(searchTerm) {
        const clearBtn = document.getElementById('clearSearchBtn');
        const searchTips = document.getElementById('searchTips');
        
        if (searchTerm.trim()) {
            clearBtn.style.display = 'inline-flex';
            // 显示搜索提示
            if (searchTips) {
                searchTips.style.display = 'block';
                setTimeout(() => {
                    if (searchTips) {
                        searchTips.style.display = 'none';
                    }
                }, 3000);
            }
        } else {
            clearBtn.style.display = 'none';
            if (searchTips) {
                searchTips.style.display = 'none';
            }
        }
        
        // 实时搜索（防抖）
        clearTimeout(this.searchTimeout);
        this.searchTimeout = setTimeout(() => {
            this.performUnifiedSearch(searchTerm);
        }, 300);
    }

    // 执行搜索
    performSearch() {
        const searchInput = document.getElementById('searchInput');
        const searchTerm = searchInput.value.trim();
        this.performUnifiedSearch(searchTerm);
    }

    // 清除搜索
    clearSearch() {
        const searchInput = document.getElementById('searchInput');
        const clearBtn = document.getElementById('clearSearchBtn');
        
        searchInput.value = '';
        clearBtn.style.display = 'none';
        
        // 显示所有视频
        this.filteredVideos = [...this.videos];
        this.sortVideos();
        this.renderVideos();
    }
    performUnifiedSearch(searchTerm) {
        const term = searchTerm.trim().toLowerCase();
        
        if (!term) {
            // 如果搜索词为空，显示所有视频
            this.filteredVideos = [...this.videos];
            this.sortVideos();
            this.renderVideos();
            return;
        }
        
        // 首先尝试按标签搜索
        const tagFilteredVideos = this.videos.filter(video => {
            if (!video.tags) return false;
            const videoTags = video.tags.split(',').map(t => t.trim().toLowerCase());
            return videoTags.includes(term);
        });
        
        // 然后尝试按视频内容搜索
        const contentFilteredVideos = this.videos.filter(video => 
            (video.description && video.description.toLowerCase().includes(term)) ||
            (video.filename && video.filename.toLowerCase().includes(term)) ||
            (video.caption && video.caption.toLowerCase().includes(term))
        );
        
        // 合并两种搜索结果（去重）
        const uniqueVideos = new Map();
        [...tagFilteredVideos, ...contentFilteredVideos].forEach(video => {
            uniqueVideos.set(video.uniqueId, video);
        });
        
        this.filteredVideos = Array.from(uniqueVideos.values());
        this.sortVideos();
        this.renderVideos();
    }

    sortVideos() {
        const videosToSort = this.filteredVideos.length > 0 ? this.filteredVideos : this.videos;
        
        switch (this.currentSort) {
            case 'date-desc':
                videosToSort.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
                break;
            case 'date-asc':
                videosToSort.sort((a, b) => new Date(a.createTime) - new Date(b.createTime));
                break;
            case 'name-asc':
                videosToSort.sort((a, b) => (a.description || a.filename).localeCompare(b.description || b.filename));
                break;
            case 'name-desc':
                videosToSort.sort((a, b) => (b.description || b.filename).localeCompare(a.description || a.filename));
                break;
            case 'size-desc':
                videosToSort.sort((a, b) => b.fileSize - a.fileSize);
                break;
            case 'size-asc':
                videosToSort.sort((a, b) => a.fileSize - b.fileSize);
                break;
        }
        
        this.filteredVideos = [...videosToSort];
    }

    updateStats() {
        const totalVideos = this.videos.length;
        const totalSize = this.videos.reduce((sum, video) => sum + video.fileSize, 0);
        
        document.getElementById('totalVideos').textContent = totalVideos;
        document.getElementById('totalSize').textContent = this.formatFileSize(totalSize);
    }

    renderVideos() {
        const list = document.getElementById('videosList');
        const emptyState = document.getElementById('emptyState');
        
        // 安全检查：确保必需的DOM元素存在
        if (!list) return;
        
        if (this.filteredVideos.length === 0) {
            if (emptyState) {
                emptyState.style.display = 'block';
            }
            list.innerHTML = '';
            if (emptyState) {
                list.appendChild(emptyState);
            }
            return;
        }
        
        if (emptyState) {
            emptyState.style.display = 'none';
        }
        list.innerHTML = this.filteredVideos.map(video => this.createVideoListItem(video)).join('');
    }

    renderTagsCloud() {
        const container = document.getElementById('tagsContainer');
        if (!container) return;
        
        if (this.allTags.length === 0) {
            container.innerHTML = '<p class="no-tags">暂无标签</p>';
            return;
        }
        
        container.innerHTML = this.allTags.map(tag => {
            const colorClass = this.getTagColorClass(tag);
            return `
                <span class="tag-item ${colorClass}" onclick="gallery.selectTag('${this.escapeJSString(tag)}')">
                    ${this.escapeHtml(tag)}
                </span>
            `;
        }).join('');
    }

    renderPopularTags() {
        const popularTagsList = document.getElementById('popularTagsList');
        if (!popularTagsList) return;
        
        // 获取常用的标签（出现频率高的标签）
        const tagFrequency = {};
        this.videos.forEach(video => {
            if (video.tags) {
                const tags = video.tags.split(',').map(t => t.trim()).filter(t => t);
                tags.forEach(tag => {
                    tagFrequency[tag] = (tagFrequency[tag] || 0) + 1;
                });
            }
        });
        
        // 按频率排序，取前10个
        const sortedTags = Object.entries(tagFrequency)
            .sort(([,a], [,b]) => b - a)
            .slice(0, 10)
            .map(([tag,]) => tag);
        
        if (sortedTags.length === 0) {
            popularTagsList.innerHTML = '<p class="no-popular-tags">暂无常用标签</p>';
            return;
        }
        
        popularTagsList.innerHTML = sortedTags.map(tag => {
            const colorClass = this.getTagColorClass(tag);
            return `
                <span class="popular-tag ${colorClass}" onclick="gallery.selectPopularTag('${this.escapeJSString(tag)}')">
                    ${this.escapeHtml(tag)}
                </span>
            `;
        }).join('');
    }

    selectTag(tag) {
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = tag;
            this.performUnifiedSearch(tag);
        }
    }

    selectPopularTag(tag) {
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = tag;
            this.performUnifiedSearch(tag);
        }
    }

    openTagModal(uniqueId, videoTitle) {
        this.currentVideoId = uniqueId;
        this.currentVideoTitle = videoTitle;
        
        const modal = document.getElementById('tagModal');
        if (modal) {
            modal.style.display = 'block';
        }
        
        this.loadCurrentVideoTags();
    }

    closeTagModal() {
        const modal = document.getElementById('tagModal');
        if (modal) {
            modal.style.display = 'none';
        }
        this.currentVideoId = null;
        this.currentVideoTitle = null;
    }

    async loadCurrentVideoTags() {
        if (!this.currentVideoId) return;
        
        const video = this.videos.find(v => v.uniqueId === this.currentVideoId);
        if (!video) return;
        
        const currentTagsDisplay = document.getElementById('currentTagsDisplay');
        if (!currentTagsDisplay) return;
        
        if (!video.tags) {
            currentTagsDisplay.innerHTML = '<p class="no-tags">暂无标签</p>';
            return;
        }
        
        const tags = video.tags.split(',').map(t => t.trim()).filter(t => t);
        currentTagsDisplay.innerHTML = tags.map(tag => `
            <span class="current-tag">
                ${this.escapeHtml(tag)}
                <button class="remove-tag-btn" onclick="gallery.removeTag('${this.escapeJSString(tag)}')">
                    &times;
                </button>
            </span>
        `).join('');
    }

    async addTag() {
        const input = document.getElementById('newTagInput');
        if (!input) return;
        
        const tag = input.value.trim();
        
        if (!tag) {
            this.showToast('请输入标签名称', 'error');
            return;
        }
        
        if (!this.currentVideoId) return;
        
        try {
            await this.addTagsToVideo(this.currentVideoId, [tag]);
            this.showToast('标签添加成功', 'success');
            input.value = '';
            
            // 重新加载视频列表和标签
            await this.loadVideos();
            this.loadCurrentVideoTags();
        } catch (error) {
            this.showToast(`添加标签失败: ${error.message}`, 'error');
        }
    }

    async removeTag(tag) {
        if (!this.currentVideoId || !tag) return;
        
        try {
            await this.removeTagFromVideo(this.currentVideoId, tag);
            this.showToast('标签删除成功', 'success');
            
            // 重新加载视频列表和标签
            await this.loadVideos();
            this.loadCurrentVideoTags();
        } catch (error) {
            this.showToast(`删除标签失败: ${error.message}`, 'error');
        }
    }

    createVideoListItem(video) {
        const formattedSize = this.formatFileSize(video.fileSize);
        const formattedDate = this.formatDate(video.createTime);
        const displayName = video.description || video.filename;
        
        // 处理封面图片
        let thumbnailHtml = '';
        if (video.thumbnail) {
            thumbnailHtml = `
                <div class="video-thumbnail">
                    <img src="/downloads/thumbnails/${video.thumbnail}" 
                         alt="${this.escapeHtml(displayName)}" 
                         class="thumbnail-image"
                         onerror="this.parentElement.innerHTML='<div class=\\'thumbnail-placeholder\\'><i class=\\'fas fa-video\\'></i></div>'">
                </div>
            `;
        } else {
            thumbnailHtml = `
                <div class="video-thumbnail">
                    <div class="thumbnail-placeholder">
                        <i class="fas fa-video"></i>
                    </div>
                </div>
            `;
        }
        
        // 处理标签显示
        let tagsHtml = '';
        if (video.tags) {
            const tags = video.tags.split(',').map(t => t.trim()).filter(t => t);
            if (tags.length > 0) {
                tagsHtml = `
                    <div class="video-tags">
                        ${tags.map(tag => {
                            const colorClass = this.getTagColorClass(tag);
                            return `
                                <span class="video-tag ${colorClass}" onclick="gallery.selectTag('${this.escapeHtml(tag)}')">
                                    ${this.escapeHtml(tag)}
                                </span>
                            `;
                        }).join('')}
                    </div>
                `;
            }
        }
        
        return `
            <div class="video-list-item" data-video-id="${video.uniqueId}">
                ${thumbnailHtml}
                <div class="video-details">
                    <div class="video-title">${this.escapeHtml(displayName)}</div>
                    <div class="video-tag-meta">
                        ${tagsHtml}
                        <div class="video-meta">
                            <span class="meta-item">
                                <i class="fas fa-weight-hanging"></i>
                                ${formattedSize}
                            </span>
                            <span class="meta-item">
                                <i class="fas fa-calendar"></i>
                                ${formattedDate}
                            </span>
                        </div>
                    </div>
                </div>
                <div class="video-actions">
                    <button class="action-btn tag" onclick="gallery.openTagModal('${video.uniqueId}', '${encodeURIComponent(displayName)}')" title="管理标签">
                        <i class="fas fa-tags"></i>
                    </button>
                    <button class="action-btn play" onclick="gallery.playVideo('${encodeURIComponent(video.filename)}', '${encodeURIComponent(displayName)}')" title="播放视频">
                        <i class="fas fa-play"></i>
                    </button>
                    <button class="action-btn download" onclick="gallery.downloadVideo('${encodeURIComponent(video.filename)}')" title="下载视频">
                        <i class="fas fa-download"></i>
                    </button>
                    <button class="action-btn delete" onclick="gallery.deleteVideo('${video.uniqueId}', '${encodeURIComponent(displayName)}')" title="删除视频">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    }

    getThumbnailUrl(filename) {
        // 首先尝试获取对应的缩略图
        const video = this.videos.find(v => v.filename === filename);
        if (video && video.thumbnail) {
            return `/downloads/thumbnails/${encodeURIComponent(video.thumbnail)}`;
        }
        
        // 如果没有缩略图，返回默认占位图
        return '/images/video-thumbnail.png';
    }

    playVideo(encodedFilename, title) {
        const filename = decodeURIComponent(encodedFilename);
        const videoUrl = `/downloads/videos/${encodedFilename}`;
        
        const modal = document.getElementById('videoModal');
        const modalVideo = document.getElementById('modalVideo');
        const modalTitle = document.getElementById('modalTitle');
        
        if (!modal || !modalVideo || !modalTitle) {
            console.error('视频播放模态框元素未找到');
            return;
        }
        
        modalTitle.textContent = decodeURIComponent(title);
        
        const modalFilename = document.getElementById('modalFilename');
        if (modalFilename) {
            modalFilename.textContent = filename;
        }
        
        // 查找对应的视频信息来显示详情
        const video = this.videos.find(v => v.filename === filename);
        if (video) {
            const modalFileSize = document.getElementById('modalFileSize');
            const modalDownloadTime = document.getElementById('modalDownloadTime');
            
            if (modalFileSize) {
                modalFileSize.textContent = this.formatFileSize(video.fileSize);
            }
            if (modalDownloadTime) {
                modalDownloadTime.textContent = this.formatDate(video.createTime);
            }
        }
        
        modalVideo.src = videoUrl;
        modal.style.display = 'block';
        
        // 预加载视频元数据
        modalVideo.load();
    }

    closeVideoModal() {
        const modal = document.getElementById('videoModal');
        const modalVideo = document.getElementById('modalVideo');
        
        if (!modal || !modalVideo) return;
        
        modalVideo.pause();
        modalVideo.src = '';
        modal.style.display = 'none';
    }

    downloadVideo(encodedFilename) {
        const filename = decodeURIComponent(encodedFilename);
        const downloadUrl = `/downloads/videos/${encodedFilename}`;
        
        // 创建临时链接进行下载
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = filename;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    async deleteVideo(uniqueId, displayName) {
        if (!confirm(`确定要删除视频 "${displayName}" 吗？此操作不可撤销。`)) {
            return;
        }
        
        try {
            const response = await fetch(`/api/downloads/${uniqueId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                this.showToast('视频删除成功', 'success');
                // 重新加载视频列表
                await this.loadVideos();
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || '删除失败');
            }
        } catch (error) {
            console.error('删除视频失败:', error);
            this.showToast(`删除失败: ${error.message}`, 'error');
        }
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 B';
        const units = ['B', 'KB', 'MB', 'GB', 'TB'];
        let unitIndex = 0;
        let size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return `${size.toFixed(1)} ${units[unitIndex]}`;
    }

    formatDate(dateString) {
        if (!dateString) return '未知时间';
        const date = new Date(dateString);
        return date.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // 专门用于JavaScript字符串转义的方法
    escapeJSString(text) {
        if (!text) return '';
        return text
            .replace(/\\/g, '\\')  // 转义反斜杠
            .replace(/'/g, "\'")   // 转义单引号
            .replace(/"/g, '\"')  // 转义双引号
            .replace(/\n/g, '\\n') // 转义换行符
            .replace(/\r/g, '\\r') // 转义回车符
            .replace(/\t/g, '\\t'); // 转义制表符
    }

    getTagColorClass(tag) {
        // 预定义一些常见标签的颜色映射
        const tagColorMap = {
            '教程': 'tag-tutorial',
            '学习': 'tag-study',
            '教育': 'tag-education',
            '娱乐': 'tag-entertainment',
            '搞笑': 'tag-funny',
            '生活': 'tag-life',
            '日常': 'tag-daily',
            '美食': 'tag-food',
            '旅游': 'tag-travel',
            '科技': 'tag-tech',
            '编程': 'tag-programming',
            '游戏': 'tag-game',
            '电竞': 'tag-esports',
            '音乐': 'tag-music',
            'MV': 'tag-mv',
            '电影': 'tag-movie',
            '电视剧': 'tag-tv',
            '动漫': 'tag-anime',
            '体育': 'tag-sports',
            '健身': 'tag-fitness',
            '新闻': 'tag-news',
            '时事': 'tag-current',
            '财经': 'tag-finance'
        };
        
        // 如果标签在预定义映射中，返回对应的颜色类
        if (tagColorMap[tag]) {
            return tagColorMap[tag];
        }
        
        // 否则根据标签名称的哈希值生成颜色类
        const hashCode = this.hashCode(tag);
        const colorIndex = Math.abs(hashCode) % 8; // 8种随机颜色
        return `tag-color-${colorIndex}`;
    }

    hashCode(str) {
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            const char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // 转换为32位整数
        }
        return hash;
    }

    showToast(message, type = 'info') {
        // 创建toast通知
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
            <span>${message}</span>
        `;
        
        // 添加样式
        toast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 15px 20px;
            border-radius: 5px;
            color: white;
            font-weight: 500;
            z-index: 10000;
            display: flex;
            align-items: center;
            gap: 10px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            transform: translateX(100%);
            transition: transform 0.3s ease;
            ${type === 'success' ? 'background: #28a745;' : type === 'error' ? 'background: #dc3545;' : 'background: #17a2b8;'}
        `;
        
        document.body.appendChild(toast);
        
        // 显示动画
        setTimeout(() => {
            toast.style.transform = 'translateX(0)';
        }, 100);
        
        // 自动隐藏
        setTimeout(() => {
            toast.style.transform = 'translateX(100%)';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, 3000);
    }

    showError(message) {
        this.showToast(message, 'error');
    }

    showLoading(show) {
        const loadingIndicator = document.getElementById('loadingIndicator');
        if (loadingIndicator) {
            loadingIndicator.style.display = show ? 'block' : 'none';
        }
    }
}

// 全局函数供HTML调用
function performSearch() {
    if (gallery) {
        gallery.performSearch();
    }
}

function clearSearch() {
    if (gallery) {
        gallery.clearSearch();
    }
}

function refreshGallery() {
    if (gallery) {
        gallery.loadVideos();
    }
}

// 初始化画廊
let gallery;
document.addEventListener('DOMContentLoaded', () => {
    gallery = new VideoGallery();
    window.gallery = gallery; // 使gallery对象全局可访问
});