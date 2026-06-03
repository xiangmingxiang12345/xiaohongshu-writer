const SCENES = [
  { type: 'xiaohongshu', name: '小红书笔记', icon: '📕', desc: '种草、测评、日常分享', placeholder: '如：出租屋改造、自用好物推荐...' },
  { type: 'douyin',       name: '抖音文案', icon: '🎬', desc: '短视频脚本、口播文案', placeholder: '如：探店视频、产品测评、剧情...' },
  { type: 'moments',      name: '朋友圈文案', icon: '💬', desc: '生活分享、心情记录', placeholder: '如：旅游打卡、美食晒图、心情...' },
  { type: 'weibo',        name: '微博文案', icon: '📢', desc: '热门话题、日常吐槽', placeholder: '如：今日趣事、热点评论、感悟...' },
  { type: 'gongzhonghao', name: '公众号文章', icon: '📰', desc: '干货长文、观点输出', placeholder: '如：行业分析、经验总结、教程...' },
  { type: 'taobao',       name: '商品描述', icon: '🛒', desc: '淘宝/拼多多带货文案', placeholder: '如：纯棉T恤、家用加湿器...' },
]

Page({
  data: {
    scenes: SCENES,
    selectedScene: '',
    sceneName: '',
    placeholder: '选择场景后输入主题...',
    topic: '',
    images: [],
    loading: false,
    canGenerate: false
  },

  selectScene(e) {
    const type = e.currentTarget.dataset.type
    const scene = SCENES.find(s => s.type === type)
    this.setData({
      selectedScene: type,
      sceneName: scene?.name || '',
      placeholder: scene?.placeholder || '输入主题...',
      canGenerate: this.data.topic.trim().length > 0 && !this.data.loading
    })
  },

  onTopicInput(e) {
    const topic = e.detail.value
    this.setData({
      topic,
      canGenerate: topic.trim().length > 0 && !!this.data.selectedScene && !this.data.loading
    })
  },

  chooseFromAlbum() {
    this._chooseImage('album')
  },

  takePhoto() {
    const remaining = 9 - this.data.images.length
    if (remaining <= 0) {
      wx.showToast({ title: '最多 9 张图片', icon: 'none' })
      return
    }
    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      sourceType: ['camera'],
      success: (res) => {
        const paths = res.tempFiles.map(f => f.tempFilePath)
        this.setData({ images: this.data.images.concat(paths) })
      }
    })
  },

  _chooseImage(sourceType) {
    const remaining = 9 - this.data.images.length
    if (remaining <= 0) {
      wx.showToast({ title: '最多 9 张图片', icon: 'none' })
      return
    }
    wx.chooseImage({
      count: remaining,
      sizeType: ['compressed'],
      sourceType: [sourceType],
      success: (res) => {
        this.setData({ images: this.data.images.concat(res.tempFilePaths) })
      }
    })
  },

  removeImage(e) {
    const index = e.currentTarget.dataset.index
    const images = this.data.images.filter((_, i) => i !== index)
    this.setData({ images })
  },

  generate() {
    const { topic, images, loading, selectedScene } = this.data
    if (loading || !topic.trim() || !selectedScene) return

    this.setData({ loading: true, canGenerate: false })

    wx.request({
      url: getApp().globalData.apiBaseUrl + '/api/generate',
      method: 'POST',
      data: {
        type: selectedScene,
        topic: topic.trim(),
        images: images
      },
      success: (res) => {
        const data = res.data
        if (data && data.content) {
          getApp().globalData.generatedContent = data.content
          getApp().globalData.sceneName = SCENES.find(s => s.type === selectedScene)?.name || ''
          wx.navigateTo({ url: '/pages/result/result' })
        } else {
          wx.showToast({ title: data?.error || '生成失败，请重试', icon: 'none' })
        }
      },
      fail: (err) => {
        console.error(err)
        wx.showToast({ title: '网络异常，请检查后端是否启动', icon: 'none' })
      },
      complete: () => {
        const topic = this.data.topic
        this.setData({
          loading: false,
          canGenerate: topic.trim().length > 0 && !!this.data.selectedScene
        })
      }
    })
  }
})
