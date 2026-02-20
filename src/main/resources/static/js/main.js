class TelegramMediaDownloader {
    constructor() {
        this.ws = null;
        this.isConnected = false;
        this.isAuthenticated = false;
        this.downloadingItems = [];
        this.completedItems = []; // 不再加载已完成的数据
        this.currentFilter = 'downloading';
        this.autoRefreshInterval = null; // 自动刷新定时器
        this.lastStats = { total: 0, active: 0, completed: 0 }; // 初始化统计信息
        this.lastDataHash = ''; // 初始化数据哈希
        
        this.initializeElements();
        this.bindEvents();
        this.checkInitialAuthStatus(); // 进入页面时首先检查认证状态
        this.connectWebSocket();
        this.loadInitialData();
        this.startAutoRefresh(); // 启动自动刷新
        this.bindTelegramLinkEvents(); // 绑定Telegram链接事件
        this.bindBatchDownloadEvents(); // 绑定批量下载事件
        this.bindBatchDownloadToggleEvents(); // 绑定批量下载显示/隐藏事件
    }
    initializeElements() {
        // 状态指示器
        this.connectionStatus = document.getElementById('connection-status');
        this.authStatus = document.getElementById('auth-status');
        this.downloadStatus = document.getElementById('download-status');

        // 各个区域
        this.loginSection = document.getElementById('login-section');
        // this.controlPanel = document.getElementById('control-panel'); // 已移除控制面板
        this.downloadsList = document.getElementById('downloads-list');
        this.emptyState = document.getElementById('empty-state');

        // 头部统计数字（已移除）
        // this.totalDownloadsHeader = document.getElementById('total-downloads-header');
        // this.activeDownloadsHeader = document.getElementById('active-downloads-header');
        // this.completedDownloadsHeader = document.getElementById('completed-downloads-header');


    }

    bindEvents() {
        // 标签过滤事件
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
                e.target.classList.add('active');
                this.currentFilter = e.target.dataset.filter;
                this.renderDownloads();
            });
        });

        // 页面可见性变化时重新连接
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden && !this.isConnected) {
                this.connectWebSocket();
            }
        });
    }

    connectWebSocket() {
        console.log('开始连接WebSocket');
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/websocket`;
        
        try {
            // 检查必要的库是否加载
            if (typeof SockJS === 'undefined') {
                console.error('SockJS库未加载');
                console.error('WebSocket库加载失败');
                return;
            }
            
            // 使用SockJS和STOMP协议
            // 支持多种STOMP库版本
            const Stomp = window.Stomp || window['@stomp/stompjs'] || window['stomp'];
            console.log('STOMP对象:', Stomp);
            console.log('STOMP类型:', typeof Stomp);
            
            // 检查STOMP库是否可用
            if (!Stomp) {
                console.error('STOMP库不可用');
                console.error('STOMP库加载失败');
                return;
            }
            
            // 检查over方法是否存在
            const overMethod = Stomp.over || Stomp.client;
            if (overMethod && typeof overMethod === 'function') {
                // 使用SockJS
                const socket = new SockJS('/websocket');
                this.stompClient = overMethod(socket);
                console.log('STOMP客户端创建成功');
                
                this.stompClient.connect({}, (frame) => {
                    console.log('WebSocket连接已建立:', frame);
                    this.isConnected = true;
                    this.updateConnectionStatus(true);
                    
                    // 订阅各个主题
                    this.subscribeToTopics();
                }, (error) => {
                    console.error('WebSocket连接错误:', error);
                    this.isConnected = false;
                    this.updateConnectionStatus(false);
                    this.scheduleReconnect();
                });
            } else {
                console.error('STOMP库方法不可用，降级到普通WebSocket');
                console.log('STOMP对象详情:', Stomp);
                console.log('可用方法:', Object.getOwnPropertyNames(Stomp));
                
                // 降级到普通WebSocket
                this.ws = new WebSocket(wsUrl);
                
                this.ws.onopen = () => {
                    console.log('WebSocket连接已建立');
                    this.isConnected = true;
                    this.updateConnectionStatus(true);
                };

                this.ws.onmessage = (event) => {
                    this.handleWebSocketMessage(event.data);
                };

                this.ws.onclose = () => {
                    console.log('WebSocket连接已关闭');
                    this.isConnected = false;
                    this.updateConnectionStatus(false);
                    this.scheduleReconnect();
                };

                this.ws.onerror = (error) => {
                    console.error('WebSocket错误:', error);
                    console.error('连接出现错误');
                };
            }
        } catch (error) {
            console.error('WebSocket连接失败:', error);
            this.scheduleReconnect();
        }
    }

    scheduleReconnect() {
        setTimeout(() => {
            if (!this.isConnected) {
                console.log('尝试重新连接...');
                this.connectWebSocket();
            }
        }, 5000);
    }

    subscribeToTopics() {
        if (this.stompClient) {
            // 订阅认证状态
            this.stompClient.subscribe('/topic/auth', (message) => {
                try {
                    const data = JSON.parse(message.body);
                    this.handleAuthMessage(data);
                } catch (error) {
                    console.error('解析认证消息失败:', error);
                    // 如果不是JSON，可能是简单的状态消息
                    if (message.body === 'ok') {
                        this.handleAuthMessage({status: 'ok'});
                    }
                }
            });
            
            // 订阅二维码消息
            this.stompClient.subscribe('/topic/qrcode', (message) => {
                try {
                    console.log('收到二维码消息:', message);
                    console.log('消息内容:', message.body);
                    this.handleQRCodeMessage(message);
                } catch (error) {
                    console.error('处理二维码消息失败:', error);
                }
            });
            
            // 订阅下载状态
            this.stompClient.subscribe('/topic/downloading', (message) => {
                try {
                    const data = JSON.parse(message.body);
                    this.handleDownloadingMessage(data);
                } catch (error) {
                    console.error('解析下载中消息失败:', error);
                }
            });
        }
    }

    handleWebSocketMessage(data) {
        // 处理原始WebSocket消息
        if (typeof data === 'string') {
            try {
                const parsedData = JSON.parse(data);
                this.handleStructuredMessage(parsedData);
            } catch (e) {
                // 可能是简单的文本消息
                console.log('收到文本消息:', data);
                if (data.startsWith('tg://')) {
                    this.handleQRCodeMessage(data);
                } else if (data === 'ok') {
                    this.handleAuthMessage({status: 'ok'});
                }
            }
        } else {
            this.handleStructuredMessage(data);
        }
    }

    handleStructuredMessage(data) {
        switch (data.type) {
            case 'auth':
                this.handleAuthMessage(data);
                break;
            case 'downloading':
                this.handleDownloadingMessage(data.items);
                break;
            case 'downloaded':
                this.handleDownloadedMessage(data.items);
                break;
            case 'directory_status':
                this.handleDirectoryStatusChange(data.payload);
                break;
            default:
                console.log('未知消息类型:', data.type);
        }
    }

    handleQRCodeMessage(qrCodeLink) {
        console.log('处理二维码链接:', qrCodeLink);
        console.log('链接类型:', typeof qrCodeLink);
        console.log('链接值:', qrCodeLink);
        
        // 处理可能的不同数据类型
        let link = '';
        
        if (typeof qrCodeLink === 'object') {
            // 如果是STOMP消息对象
            if (qrCodeLink.body) {
                link = qrCodeLink.body;
            } else if (qrCodeLink.destination) {
                // 可能是其他格式的对象
                link = JSON.stringify(qrCodeLink);
            }
        } else if (typeof qrCodeLink === 'string') {
            // 如果是纯字符串
            link = qrCodeLink;
        } else if (qrCodeLink && typeof qrCodeLink.toString === 'function') {
            // 尝试转换为字符串
            link = qrCodeLink.toString();
        }
        
        console.log('处理后的链接:', link);
        console.log('处理后链接类型:', typeof link);
        
        // 确保link是字符串类型后再调用startsWith
        if (link && typeof link === 'string' && (link.startsWith('tg://') || link.includes('t.me'))) {
            // 生成二维码图片
            this.generateQRCodeImage(link);
        } else {
            console.error('无效的二维码链接:', link);
            console.error('链接类型:', typeof link);
            console.error('接收到无效的二维码链接: ' + (link || '空值'));
        }
    }

    async generateQRCodeFallback(link) {
        try {
            console.log('使用外部服务生成二维码');
            
            // 使用多个二维码生成服务作为备选
            const qrServices = [
                `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(link)}`, 
                `https://quickchart.io/qr?text=${encodeURIComponent(link)}&size=250`,
                `https://chart.googleapis.com/chart?cht=qr&chs=250x250&chl=${encodeURIComponent(link)}`
            ];
            
            // 尝试第一个服务
            const qrUrl = qrServices[0];
            console.log('使用的二维码URL:', qrUrl);
            
            // 预加载图片确保可用
            const img = new Image();
            img.onload = () => {
                console.log('外部二维码图片加载成功');
                this.displayQRCode(qrUrl);
                console.log('二维码已生成（使用外部服务）');
            };
            img.onerror = (error) => {
                console.error('外部二维码图片加载失败:', error);
                // 尝试备用服务
                this.tryBackupQRService(qrServices.slice(1), link);
            };
            img.src = qrUrl;
        } catch (error) {
            console.error('外部服务生成二维码失败:', error);
            this.displayQRCode(null);
            console.error('所有二维码生成服务都不可用');
        }
    }

    async tryBackupQRService(services, link) {
        if (services.length === 0) {
            console.error('所有二维码服务都不可用');
            this.displayQRCode(null);
            console.error('所有二维码生成服务都不可用');
            return;
        }
        
        const backupService = services[0];
        console.log('尝试备用二维码服务:', backupService);
        
        const img = new Image();
        img.onload = () => {
            console.log('备用服务二维码生成成功');
            this.displayQRCode(backupService);
            console.log('二维码已生成（使用备用服务）');
        };
        img.onerror = () => {
            console.error('备用服务失败，尝试下一个');
            this.tryBackupQRService(services.slice(1), link);
        };
        img.src = backupService;
    }

    async generateQRCodeImage(link) {
        try {
            console.log('开始生成二维码图片，链接:', link);
            
            // 使用本地API生成二维码
            const response = await fetch('/api/qrcode/generate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    content: link
                })
            });
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const result = await response.json();
            
            if (result.success && result.data && result.data.image) {
                console.log('本地二维码生成成功');
                this.displayQRCode(result.data.image);
                console.log('二维码已生成');
            } else {
                throw new Error(result.message || '二维码生成失败');
            }
            
        } catch (error) {
            console.error('生成二维码失败:', error);
            
            // 降级到外部服务
            console.log('本地生成失败，尝试外部服务...');
            this.generateQRCodeFallback(link);
        }
    }

    handleAuthMessage(data) {
        if (data.status === 'ok') {
            this.isAuthenticated = true;
            this.updateAuthStatus(true);
            this.loginSection.style.display = 'none';
            // controlPanel已被移除，无需显示
        } else if (data.qrCode) {
            this.displayQRCode(data.qrCode);
        }
    }

    handleDownloadingMessage(items) {
        // 确保传入的是数组
        if (Array.isArray(items)) {
            this.downloadingItems = items;
        } else {
            console.warn('handleDownloadingMessage 接收到非数组数据:', items);
            this.downloadingItems = [];
        }
        this.updateDownloadStats();
        this.renderDownloads();
    }

    handleDownloadedMessage(items) {
        // 确保传入的是数组
        if (Array.isArray(items)) {
            this.completedItems = items;
        } else {
            console.warn('handleDownloadedMessage 接收到非数组数据:', items);
            this.completedItems = [];
        }
        this.updateDownloadStats();
        this.renderDownloads();
    }

    displayQRCode(qrCodeData) {
        const qrPlaceholder = document.getElementById('qr-placeholder');
        const qrCodeImg = document.getElementById('qr-code');
        
        console.log('显示二维码数据类型:', typeof qrCodeData);
        
        if (qrCodeData) {
            if (typeof qrCodeData === 'string') {
                if (qrCodeData.startsWith('http')) {
                    // 如果是URL，则直接使用
                    qrCodeImg.src = qrCodeData;
                    console.log('使用URL显示二维码');
                } else if (qrCodeData.startsWith('data:image')) {
                    // 如果已经是完整的data URL
                    qrCodeImg.src = qrCodeData;
                    console.log('使用data URL显示二维码');
                } else {
                    // 如果是Base64数据
                    qrCodeImg.src = `data:image/png;base64,${qrCodeData}`;
                    console.log('使用Base64数据显示二维码');
                }
            } else {
                console.error('二维码数据格式不正确:', qrCodeData);
                qrPlaceholder.style.display = 'flex';
                qrCodeImg.style.display = 'none';
                return;
            }
            
            qrPlaceholder.style.display = 'none';
            qrCodeImg.style.display = 'block';
            console.log('二维码显示成功');
        } else {
            qrPlaceholder.style.display = 'flex';
            qrCodeImg.style.display = 'none';
            console.log('显示占位符');
        }
    }

    async checkInitialAuthStatus() {
        try {
            console.log('开始检查初始认证状态');
            
            // 调用健康检查API获取认证状态
            const healthResponse = await fetch('/api/health');
            if (healthResponse && healthResponse.ok) {
                const healthData = await healthResponse.json();
                console.log('健康检查响应:', healthData);
                
                // 检查认证状态
                const userLoggedIn = healthData.data?.userLoggedIn || false;
                console.log('用户认证状态:', userLoggedIn);
                
                if (userLoggedIn) {
                    // 用户已认证
                    this.isAuthenticated = true;
                    this.loginSection.style.display = 'none';
                    this.updateAuthStatus(true);
                    console.log('用户已认证，隐藏登录区域');
                } else {
                    // 用户未认证
                    this.isAuthenticated = false;
                    this.loginSection.style.display = 'block';
                    this.updateAuthStatus(false);
                    console.log('用户未认证，显示登录区域');
                }
            } else {
                console.warn('健康检查API调用失败，状态码:', healthResponse?.status);
                // API调用失败时，默认显示登录区域
                this.isAuthenticated = false;
                this.loginSection.style.display = 'block';
                this.updateAuthStatus(false);
            }
        } catch (error) {
            console.error('检查初始认证状态失败:', error);
            // 发生错误时，默认显示登录区域
            this.isAuthenticated = false;
            this.loginSection.style.display = 'block';
            this.updateAuthStatus(false);
        }
    }

    async loadInitialData() {
        try {
            // 只加载进行中的下载项，不加载已完成的数据
            const downloadingResponse = await fetch('/api/downloads/downloading');
            if (downloadingResponse && downloadingResponse.ok) {
                const response = await downloadingResponse.json();
                // 确保数据是数组格式
                if (response && response.data && Array.isArray(response.data)) {
                    this.downloadingItems = response.data;
                } else if (Array.isArray(response)) {
                    this.downloadingItems = response;
                } else {
                    console.warn('进行中下载项数据格式不正确:', response);
                    this.downloadingItems = [];
                }
            } else {
                this.downloadingItems = [];
            }

            // 不再加载已完成的下载项
            this.completedItems = [];

            console.log('加载的下载数据:', {
                downloadingItems: this.downloadingItems,
                completedItems: this.completedItems
            });

            this.updateDownloadStats();
            this.renderDownloads();

        } catch (error) {
            console.error('加载初始数据失败:', error);
            // 发生错误时确保数组初始化
            this.downloadingItems = [];
            this.completedItems = [];
            this.updateDownloadStats();
        }
    }

    async checkAuthStatus(healthData) {
        try {
            const userLoggedIn = healthData.data?.userLoggedIn || false;
            console.log('检查认证状态:', userLoggedIn);
            
            if (userLoggedIn) {
                // 用户已认证
                this.isAuthenticated = true;
                this.loginSection.style.display = 'none';
                // this.controlPanel.style.display = 'block'; // 已移除控制面板
                this.updateAuthStatus(true);
                console.log('用户认证成功');
            } else {
                // 用户未认证
                this.isAuthenticated = false;
                this.loginSection.style.display = 'block';
                // this.controlPanel.style.display = 'none'; // 已移除控制面板
                this.updateAuthStatus(false);
                console.log('用户未认证');
            }
        } catch (error) {
            console.error('检查认证状态失败:', error);
        }
    }

    updateHealthStatus(healthData) {
        const status = healthData.status || 'unknown';
        this.updateConnectionStatus(status === 'UP' || status === 'healthy');
        // 修正认证状态的读取路径
        const userLoggedIn = healthData.data?.userLoggedIn || false;
        this.updateAuthStatus(userLoggedIn);
        
        // 如果用户已登录，隐藏登录区域
        // 注意：controlPanel已被移除，只处理loginSection
        if (userLoggedIn && this.loginSection) {
            this.loginSection.style.display = 'none';
            this.isAuthenticated = true;
        }
    }

    updateConnectionStatus(connected) {
        if (!this.connectionStatus) {
            console.warn('connectionStatus 元素未找到');
            return;
        }
        
        const dot = this.connectionStatus.querySelector('.status-dot');
        const text = this.connectionStatus.querySelector('span:last-child');
        
        if (dot && text) {
            if (connected) {
                dot.className = 'status-dot connected';
                text.textContent = '已连接';
            } else {
                dot.className = 'status-dot disconnected';
                text.textContent = '未连接';
            }
        }
    }

    updateAuthStatus(authenticated) {
        if (!this.authStatus) {
            console.warn('authStatus 元素未找到');
            return;
        }
        
        const dot = this.authStatus.querySelector('.status-dot');
        const text = this.authStatus.querySelector('span:last-child');
        
        if (dot && text) {
            if (authenticated) {
                dot.className = 'status-dot authenticated';
                text.textContent = '已认证';
            } else {
                dot.className = 'status-dot disconnected';
                text.textContent = '未认证';
            }
        }
    }

    updateDownloadStats() {
        // 确保数组类型安全
        if (!Array.isArray(this.downloadingItems)) {
            console.warn('downloadingItems 不是数组，重置为空数组:', this.downloadingItems);
            this.downloadingItems = [];
        }
        if (!Array.isArray(this.completedItems)) {
            console.warn('completedItems 不是数组，重置为空数组:', this.completedItems);
            this.completedItems = [];
        }
        
        const total = this.downloadingItems.length + this.completedItems.length;
        const active = this.downloadingItems.length;
        const completed = this.completedItems.length;
        
        // 更新头部统计数据（安全检查元素是否存在）
        if (this.totalDownloadsHeader) this.totalDownloadsHeader.textContent = total;
        if (this.activeDownloadsHeader) this.activeDownloadsHeader.textContent = active;
        if (this.completedDownloadsHeader) this.completedDownloadsHeader.textContent = completed;

        // 重新渲染下载列表
        this.renderDownloads();
    }

    formatSpeed(bytesPerSecond) {
        if (bytesPerSecond === 0) return '0 KB/s';
        
        const units = ['B/s', 'KB/s', 'MB/s', 'GB/s'];
        let unitIndex = 0;
        let speed = bytesPerSecond;
        
        while (speed >= 1024 && unitIndex < units.length - 1) {
            speed /= 1024;
            unitIndex++;
        }
        
        return `${speed.toFixed(1)} ${units[unitIndex]}`;
    }

    renderDownloads() {

        if (this.downloadingItems.length === 0) {
            this.downloadsList.style.display = 'none';
            this.emptyState.style.display = 'block';
            return;
        }

        this.emptyState.style.display = 'none';
        this.downloadsList.style.display = 'block';
        
        this.downloadsList.innerHTML = this.downloadingItems.map(item =>
            this.createDownloadItemElement(item)
        ).join('');
    }

    filterItems(items, filter) {
        // 只显示下载中的项目
        return items.filter(item => 
            item.state === 'Downloading' || item.state === 'Created'
        );
    }

    createDownloadItemElement(item) {
        const isCompleted = item.state === 'Complete';
        const progress = item.progress || 0;
        const fileSize = this.formatFileSize(item.fileSize || 0);
        const downloadedSize = this.formatFileSize(item.downloadedSize || 0);
        const speed = this.formatSpeed(item.downloadBytePerSec || 0);
        
        // 判断是否为视频文件（根据文件扩展名）
        const isVideo = isCompleted && this.isVideoFile(item.filename);
        
        // 下载完成时不显示速度
        const showSpeed = !isCompleted;
        
        return `
            <div class="download-item ${isCompleted ? 'completed' : ''}" data-unique-id="${item.uniqueId}">
                <div class="download-item-header">

                    <div class="download-title">${this.escapeHtml(item.description || item.filename || '未知文件')}</div>
                    <div class="download-meta">
                        <div class="download-size">
                            <i class="fas fa-weight-hanging"></i>
                            ${isCompleted ? fileSize : `${downloadedSize} / ${fileSize}`}
                        </div>
                        ${showSpeed ? `
                        <div class="download-speed">
                            <i class="fas fa-tachometer-alt"></i>
                            ${speed}
                        </div>
                        ` : ''}
                        <!-- 下载操作按钮 -->
                        <div class="download-actions">
                            ${!isCompleted ? `
                            <button class="action-btn" onclick="app.pauseDownload('${item.uniqueId}')" title="暂停">
                                <i class="fas fa-pause"></i>
                            </button>
                            ` : ''}
                            <button class="action-btn delete" onclick="app.deleteDownload('${item.uniqueId}')" title="删除">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>
                
                ${!isCompleted ? `
                <div class="progress-container">
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: ${progress}%"></div>
                    </div>
                    <div class="progress-text">
                        <span>${progress.toFixed(1)}%</span>
                    </div>
                </div>
                ` : ''}
            </div>
        `;
    }

    getStateIcon(state) {
        switch (state) {
            case 'Complete': return 'check-circle';
            case 'Downloading': return 'download';
            case 'Created': return 'clock';
            case 'Failed': return 'exclamation-circle';
            case 'Pause': return 'pause-circle';
            default: return 'question-circle';
        }
    }

    getStateText(state, filename) {
        const isVideo = this.isVideoFile(filename);
        
        switch (state) {
            case 'Complete': 
                return isVideo ? '' : '已完成'; // 视频文件不显示状态文字
            case 'Downloading': return '下载中';
            case 'Created': return '排队中';
            case 'Failed': return '失败';
            case 'Pause': return '已暂停';
            default: return state;
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

    // 标准化文件名用于比较
    normalizeFilename(filename) {
        if (!filename) return '';
        // 移除特殊符号，统一空格，转换为小写进行比较
        return filename
            .replace(/[\【\】\,\，]/g, ' ')  // 替换括号和逗号为空格
            .replace(/\s+/g, ' ')            // 统一多个空格为单个空格
            .trim()
            .toLowerCase();
    }
    
    // 模糊匹配文件名
    fuzzyMatchFilenames(requestedFilename, actualFilename) {
        const normalizedRequested = this.normalizeFilename(requestedFilename);
        const normalizedActual = this.normalizeFilename(actualFilename);
        
        // 完全匹配
        if (normalizedRequested === normalizedActual) {
            return true;
        }
        
        // 包含关系匹配
        if (normalizedRequested.includes(normalizedActual) || 
            normalizedActual.includes(normalizedRequested)) {
            return true;
        }
        
        // 计算相似度
        const similarity = this.calculateStringSimilarity(normalizedRequested, normalizedActual);
        return similarity > 0.8; // 80%相似度阈值
    }
    
    // 计算字符串相似度
    calculateStringSimilarity(str1, str2) {
        if (str1 === str2) return 1.0;
        if (!str1 || !str2) return 0.0;
        
        // 使用编辑距离算法
        const len1 = str1.length;
        const len2 = str2.length;
        const matrix = Array(len1 + 1).fill().map(() => Array(len2 + 1).fill(0));
        
        for (let i = 0; i <= len1; i++) matrix[i][0] = i;
        for (let j = 0; j <= len2; j++) matrix[0][j] = j;
        
        for (let i = 1; i <= len1; i++) {
            for (let j = 1; j <= len2; j++) {
                const cost = str1[i - 1] === str2[j - 1] ? 0 : 1;
                matrix[i][j] = Math.min(
                    matrix[i - 1][j] + 1,      // 删除
                    matrix[i][j - 1] + 1,      // 插入
                    matrix[i - 1][j - 1] + cost // 替换
                );
            }
        }
        
        const distance = matrix[len1][len2];
        return 1 - (distance / Math.max(len1, len2));
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    // 判断是否为视频文件
    isVideoFile(filename) {
        if (!filename) return false;
        const videoExtensions = ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.webm', '.mkv'];
        const lowerFilename = filename.toLowerCase();
        return videoExtensions.some(ext => lowerFilename.endsWith(ext));
    }
    
    // 渲染下载项标签
    renderItemTags(tags) {
        if (!tags || !Array.isArray(tags) || tags.length === 0) {
            return '';
        }
        
        return tags.map(tag => `
            <span class="tag">${this.escapeHtml(tag)}</span>
        `).join('');
    }
    
    // 视频播放功能已移除
    // 视频关闭功能已移除
    // 全屏播放功能已移除

    async refreshData() {
        this.showLoading(this.refreshBtn);
        try {
            await this.loadInitialData();
            console.log('数据已刷新');
        } catch (error) {
            console.error('刷新失败');
        } finally {
            this.hideLoading(this.refreshBtn);
        }
    }

    // 启动自动刷新
    startAutoRefresh() {
        // 每秒刷新一次状态
        this.autoRefreshInterval = setInterval(async () => {
            try {
                // 使用智能刷新，无变化时不操作DOM
                await this.smartRefreshDownloadStatus();
            } catch (error) {
                console.error('智能刷新失败:', error);
            }
        }, 1000); // 1000ms = 1秒
        
        console.log('自动刷新已启动，每秒刷新一次');
    }

    // 停止自动刷新
    stopAutoRefresh() {
        if (this.autoRefreshInterval) {
            clearInterval(this.autoRefreshInterval);
            this.autoRefreshInterval = null;
            console.log('自动刷新已停止');
        }
    }
    
    // 智能刷新下载状态 - 无数据变动时完全不操作DOM
    async smartRefreshDownloadStatus() {
        try {
            // 并行获取下载状态，提高效率
            const [, downloadingResponse] = await Promise.all([
                fetch('/api/downloads/downloading')
            ]);

            let newDownloadingItems = [];


            // 处理进行中的下载项
            if (downloadingResponse && downloadingResponse.ok) {
                const response = await downloadingResponse.json();
                if (response && response.data && Array.isArray(response.data)) {
                    newDownloadingItems = response.data;
                } else if (Array.isArray(response)) {
                    newDownloadingItems = response;
                } else {
                    newDownloadingItems = [];
                }
            }

            // 精确检查数据是否发生变化
            const newDataHash = this.calculatePreciseDataHash(newDownloadingItems);
            
            if (newDataHash !== this.lastDataHash) {
                console.log('检测到数据变化，更新UI');
                this.lastDataHash = newDataHash;
                
                // 更新数据
                this.downloadingItems = newDownloadingItems;
                
                // 智能更新UI
                this.smartUpdateUI();
            }
            // 完全无变化时不执行任何DOM操作

        } catch (error) {
            console.error('智能刷新下载状态失败:', error);
        }
    }
    
    // 计算精确数据哈希值用于比较
    calculatePreciseDataHash(downloadingItems) {
        // 确保参数是数组
        if (!Array.isArray(downloadingItems)) downloadingItems = [];
        
        // 考虑所有关键字段：uniqueId, state, progress, fileSize, downloadedSize
        const keyFields = (items) => 
            items.map(item => 
                `${item.uniqueId}|${item.state}|${Math.floor(item.progress || 0)}|${item.fileSize}|${item.downloadedSize}`
            ).sort().join(';;');
        
        return keyFields(downloadingItems);
    }
    
    // 智能更新UI - 只更新有变化的部分
    smartUpdateUI() {
        // 直接重新渲染下载列表
        this.renderDownloads();
    }



    async pauseDownload(uniqueId) {
        try {
            const response = await fetch(`/api/downloads/${uniqueId}/pause`, {
                method: 'POST'
            });
            
            if (response.ok) {
                console.log('下载已暂停');
            } else {
                throw new Error('操作失败');
            }
        } catch (error) {
            console.error('暂停失败: ' + error.message);
        }
    }

    async openFolder(uniqueId) {
        try {
            // 获取下载项信息
            const item = [...this.downloadingItems, ...this.completedItems]
                .find(item => item.uniqueId === uniqueId);
            
            if (!item) {
                console.error('未找到下载项');
                return;
            }
            
            // 构造文件路径并打开
            const filePath = `/downloads/videos/${encodeURIComponent(item.filename)}`;
            window.open(filePath, '_blank');
            
        } catch (error) {
            console.error('打开文件夹失败:', error);
            console.error('打开文件夹失败: ' + error.message);
        }
    }

    async pauseDownload(uniqueId) {
        try {
            const response = await fetch(`/api/downloads/${uniqueId}/pause`, {
                method: 'POST'
            });
            
            if (response.ok) {
                console.log('下载已暂停');
            } else {
                throw new Error('暂停失败');
            }
        } catch (error) {
            console.error('暂停失败: ' + error.message);
        }
    }

    async deleteDownload(uniqueId) {
        if (!confirm('确定要删除这个下载任务吗？')) {
            return;
        }

        try {
            const response = await fetch(`/api/downloads/${uniqueId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                // 从本地数组中移除
                this.downloadingItems = this.downloadingItems.filter(item => item.uniqueId !== uniqueId);
                this.completedItems = this.completedItems.filter(item => item.uniqueId !== uniqueId);
                this.updateDownloadStats();
                this.renderDownloads();
                console.log('任务已删除');
            } else {
                throw new Error('删除失败');
            }
        } catch (error) {
            console.error('删除失败: ' + error.message);
        }
    }

    bindTelegramLinkEvents() {
        const downloadBtn = document.getElementById('download-link-btn');
        const linkInput = document.getElementById('telegram-link-input');
        const resultDiv = document.getElementById('download-result');
        
        if (downloadBtn && linkInput) {
            // 点击下载按钮
            downloadBtn.addEventListener('click', () => {
                this.handleTelegramLinkDownload();
            });
            
            // 回车键触发下载
            linkInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.handleTelegramLinkDownload();
                }
            });
            
            // 输入框内容变化时隐藏结果提示
            linkInput.addEventListener('input', () => {
                if (resultDiv.style.display !== 'none') {
                    resultDiv.style.display = 'none';
                }
            });
        }
    }
    
    async handleTelegramLinkDownload() {
        const linkInput = document.getElementById('telegram-link-input');
        const downloadBtn = document.getElementById('download-link-btn');
        const resultDiv = document.getElementById('download-result');
        
        const link = linkInput.value.trim();
        
        // 验证输入
        if (!link) {
            this.showDownloadResult('请输入Telegram链接', 'error');
            linkInput.focus();
            return;
        }
        
        // 验证链接格式
        if (!link.toLowerCase().startsWith('https://t.me')) {
            this.showDownloadResult('请输入有效的Telegram链接 (以 https://t.me 开头)', 'error');
            return;
        }
        
        // 显示加载状态
        downloadBtn.disabled = true;
        downloadBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i><span>处理中...</span>';
        resultDiv.style.display = 'none';
        
        try {
            // 调用API
            const response = await fetch('/api/downloads/telegram-link', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ link: link })
            });
            
            const result = await response.json();
            
            if (response.ok && result.code === 200) {
                this.showDownloadResult('下载请求已提交，正在处理中...', 'success');
                linkInput.value = ''; // 清空输入框
                
                // 5秒后自动隐藏成功提示
                setTimeout(() => {
                    resultDiv.style.display = 'none';
                }, 5000);
            } else {
                this.showDownloadResult(result.message || '下载请求失败', 'error');
            }
            
        } catch (error) {
            console.error('下载请求失败:', error);
            this.showDownloadResult('网络错误，请稍后重试', 'error');
        } finally {
            // 恢复按钮状态
            downloadBtn.disabled = false;
            downloadBtn.innerHTML = '<i class="fas fa-download"></i><span>下载</span>';
        }
    }
    
    showDownloadResult(message, type) {
        const resultDiv = document.getElementById('download-result');
        if (resultDiv) {
            resultDiv.className = `download-result ${type}`;
            resultDiv.innerHTML = `
                <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i>
                <span>${message}</span>
            `;
            resultDiv.style.display = 'flex';
        }
    }

    showLoading(button) {
        const originalContent = button.innerHTML;
        button.innerHTML = '<span class="loading"></span> 处理中...';
        button.disabled = true;
        button.dataset.originalContent = originalContent;
    }

    hideLoading(button) {
        if (button.dataset.originalContent) {
            button.innerHTML = button.dataset.originalContent;
            button.disabled = false;
            delete button.dataset.originalContent;
        }
    }
    
    /**
     * 检查目录状态
     */
    async checkDirectoryStatus() {
        try {
            const response = await fetch('/api/directory/status');
            const result = await response.json();
            
            if (result.code === 200) {
                this.handleDirectoryStatusChange(result.data);
            }
        } catch (error) {
            console.error('检查目录状态失败:', error);
        }
    }
    
    /**
     * 处理目录状态变化
     */
    handleDirectoryStatusChange(status) {
        console.log('目录状态变化:', status);
        
        const directoryAlert = document.getElementById('directory-alert');
        
        if (!status.available) {
            // 目录不可用
            if (directoryAlert) {
                directoryAlert.style.display = 'block';
                directoryAlert.querySelector('.alert-message').textContent = 
                    status.message || '下载目录不可访问，请检查U盘或移动硬盘连接';
            }
            
            // 禁用下载按钮
            const downloadButtons = document.querySelectorAll('[onclick*="download"]');
            downloadButtons.forEach(btn => {
                btn.disabled = true;
                btn.title = '下载目录不可用';
            });
        } else {
            // 目录可用
            if (directoryAlert) {
                directoryAlert.style.display = 'none';
            }
            
            // 启用下载按钮
            const downloadButtons = document.querySelectorAll('[onclick*="download"]');
            downloadButtons.forEach(btn => {
                btn.disabled = false;
                btn.title = '';
            });
        }
    }

    bindBatchDownloadToggleEvents() {
        const showBtn = document.getElementById('show-batch-download-btn');
        const hideBtn = document.getElementById('hide-batch-download-btn');
        const batchSection = document.getElementById('batch-download-section');
        
        if (showBtn) {
            showBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.showBatchDownloadForm();
            });
        }
        
        if (hideBtn && batchSection) {
            hideBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.hideBatchDownloadForm();
            });
        }
        
        // 点击表单外部区域也隐藏表单（移动端体验优化）
        if (batchSection) {
            batchSection.addEventListener('click', (e) => {
                // 如果点击的是表单区域本身，则不隐藏
                if (e.target === batchSection) {
                    this.hideBatchDownloadForm();
                }
            });
        }
    }

    showBatchDownloadForm() {
        const batchSection = document.getElementById('batch-download-section');
        const showBtn = document.getElementById('show-batch-download-btn');
        
        if (batchSection) {
            batchSection.style.display = 'block';
            // 检测是否为移动端
            const isMobile = window.innerWidth <= 768;
            
            if (isMobile) {
                // 移动端：表单从底部滑入
                batchSection.style.transform = 'translateY(0)';
                batchSection.style.opacity = '1';
            } else {
                // 桌面端：滚动到表单位置
                batchSection.scrollIntoView({ behavior: 'smooth' });
            }
            
            if (showBtn) showBtn.style.display = 'none';
        }
    }
    
    hideBatchDownloadForm() {
        const batchSection = document.getElementById('batch-download-section');
        const showBtn = document.getElementById('show-batch-download-btn');
        
        if (batchSection) {
            const isMobile = window.innerWidth <= 768;
            
            if (isMobile) {
                // 移动端：表单向下滑出
                batchSection.style.transform = 'translateY(100%)';
                batchSection.style.opacity = '0';
                // 延迟隐藏以完成动画
                setTimeout(() => {
                    batchSection.style.display = 'none';
                }, 300);
            } else {
                // 桌面端：直接隐藏
                batchSection.style.display = 'none';
            }
            
            if (showBtn) showBtn.style.display = 'inline-block';
        }
    }

    bindBatchDownloadEvents() {
        const batchDownloadBtn = document.getElementById('batch-download-btn');
        const resultDiv = document.getElementById('batch-download-result');
        
        if (batchDownloadBtn) {
            batchDownloadBtn.addEventListener('click', () => {
                this.handleBatchDownload();
            });
        }
        
        // 隐藏结果提示的事件
        const chatIdInput = document.getElementById('chat-id');
        const startMessageIdInput = document.getElementById('start-message-id');
        const endMessageIdInput = document.getElementById('end-message-id');
        const concurrentInput = document.getElementById('concurrent');
        const intervalInput = document.getElementById('interval');
        
        [chatIdInput, startMessageIdInput, endMessageIdInput, concurrentInput, intervalInput].forEach(input => {
            if (input) {
                input.addEventListener('input', () => {
                    if (resultDiv && resultDiv.style.display !== 'none') {
                        resultDiv.style.display = 'none';
                    }
                });
            }
        });
    }

    async handleBatchDownload() {
        const chatIdInput = document.getElementById('chat-id');
        const startMessageIdInput = document.getElementById('start-message-id');
        const endMessageIdInput = document.getElementById('end-message-id');
        const concurrentInput = document.getElementById('concurrent');
        const intervalInput = document.getElementById('interval');
        const batchDownloadBtn = document.getElementById('batch-download-btn');
        const resultDiv = document.getElementById('batch-download-result');
        
        const chatId = chatIdInput.value.trim();
        const startMessageId = startMessageIdInput.value.trim();
        const endMessageId = endMessageIdInput.value.trim();
        const concurrent = concurrentInput.value || '3';
        const interval = intervalInput.value || '1000';
        
        // 验证输入
        if (!chatId || !startMessageId || !endMessageId) {
            this.showBatchResult('请填写所有必填字段', 'error');
            return;
        }
        
        if (isNaN(parseInt(chatId)) && !/^[a-zA-Z0-9_-]+$/.test(chatId)) {
            this.showBatchResult('频道/群组名称格式不正确，请使用英文、数字、下划线或连字符', 'error');
            return;
        }
        
        if (isNaN(parseInt(startMessageId)) || isNaN(parseInt(endMessageId))) {
            this.showBatchResult('消息ID必须是数字', 'error');
            return;
        }
        
        const startId = parseInt(startMessageId);
        const endId = parseInt(endMessageId);
        
        if (startId > endId) {
            this.showBatchResult('起始消息ID不能大于结束消息ID', 'error');
            return;
        }
        
        if (startId < 1 || endId < 1) {
            this.showBatchResult('消息ID必须大于0', 'error');
            return;
        }
        
        // 显示加载状态
        batchDownloadBtn.disabled = true;
        batchDownloadBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i><span>处理中...</span>';
        resultDiv.style.display = 'none';
        
        try {
            // 调用批量下载API
            const response = await fetch('/api/downloads/batch', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    chatId: chatId,
                    startMessageId: startId,
                    endMessageId: endId,
                    concurrent: parseInt(concurrent),
                    interval: parseInt(interval)
                })
            });
            
            const result = await response.json();
            
            if (response.ok && result.code === 200) {
                this.showBatchResult(result.data.message || '批量下载任务已提交', 'success');
                
                // 5秒后自动隐藏成功提示和表单
                setTimeout(() => {
                    resultDiv.style.display = 'none';
                    const batchSection = document.getElementById('batch-download-section');
                    const showBtn = document.getElementById('show-batch-download-btn');
                    if (batchSection) batchSection.style.display = 'none';
                    if (showBtn) showBtn.style.display = 'inline-block';
                }, 5000);
                
                                // 清空输入框
                chatIdInput.value = '';
                startMessageIdInput.value = '';
                endMessageIdInput.value = '';
                minDurationInput.value = '10';
                
                // 刷新下载列表
                await this.loadInitialData();
            } else {
                this.showBatchResult(result.message || '批量下载请求失败', 'error');
            }
        } catch (error) {
            console.error('处理批量下载时发生错误:', error);
            this.showBatchResult('网络错误，请稍后重试: ' + error.message, 'error');
        } finally {
            // 恢复按钮状态
            batchDownloadBtn.disabled = false;
            batchDownloadBtn.innerHTML = '<i class="fas fa-download"></i><span>开始批量下载</span>';
        }
    }

    showBatchResult(message, type) {
        const resultDiv = document.getElementById('batch-download-result');
        if (resultDiv) {
            resultDiv.className = `batch-download-result alert-${type}`;
            resultDiv.innerHTML = message;
            resultDiv.style.display = 'block';
        }
    }
}

// 页面加载完成后初始化应用
document.addEventListener('DOMContentLoaded', () => {
    console.log('页面DOM加载完成');
    
    // 页面卸载时清理资源
    window.addEventListener('beforeunload', () => {
        if (window.app) {
            window.app.stopAutoRefresh();
        }
    });
    
    // 等待必要的库加载完成
    let attempts = 0;
    const maxAttempts = 50; // 最多等待5秒
    
    const checkLibraries = () => {
        attempts++;
        console.log(`检查库加载情况 - 尝试 ${attempts}/${maxAttempts}`);
        console.log('SockJS状态:', typeof SockJS);
        console.log('STOMP状态:', typeof Stomp, typeof window['@stomp/stompjs']);
        
        // 检查SockJS
        if (typeof SockJS === 'undefined') {
            if (attempts >= maxAttempts) {
                console.error('SockJS库加载超时');
                alert('WebSocket库加载失败，请刷新页面重试');
                return;
            }
            console.log('SockJS未加载，继续等待...');
            setTimeout(checkLibraries, 100);
            return;
        }
        
        // 检查STOMP库
        const stompAvailable = window.Stomp || window['@stomp/stompjs'] || window['stomp'];
        if (!stompAvailable) {
            if (attempts >= maxAttempts) {
                console.error('STOMP库加载超时');
                alert('STOMP库加载失败，请刷新页面重试');
                return;
            }
            console.log('STOMP未加载，继续等待...');
            setTimeout(checkLibraries, 100);
            return;
        }
        
        console.log('所有必需库已加载完成');
        window.app = new TelegramMediaDownloader();
    };
    
    // 开始检查
    checkLibraries();
});

// 添加一些实用的工具函数
window.utils = {
    formatDate: (date) => {
        return new Date(date).toLocaleString('zh-CN');
    },
    
    debounce: (func, wait) => {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
};