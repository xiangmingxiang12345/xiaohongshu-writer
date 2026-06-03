Page({
  data: {
    content: '',
    sceneName: ''
  },

  onLoad() {
    const app = getApp()
    const content = app.globalData.generatedContent
    const sceneName = app.globalData.sceneName || ''
    if (content) {
      this.setData({ content, sceneName })
    } else {
      wx.showToast({ title: '没有内容', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
    }
  },

  copyContent() {
    const { content } = this.data
    wx.setClipboardData({
      data: content,
      success: () => {
        wx.showToast({ title: '已复制到剪贴板', icon: 'success' })
      }
    })
  },

  goBack() {
    wx.navigateBack()
  }
})
