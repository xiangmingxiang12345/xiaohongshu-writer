const SCENES = [
  { type: 'xiaohongshu', name: '小红书笔记', icon: '📕', desc: '种草、测评、日常分享', placeholder: '如：出租屋改造、自用好物推荐...' },
  { type: 'douyin',       name: '抖音文案', icon: '🎬', desc: '短视频脚本、口播文案', placeholder: '如：探店视频、产品测评、剧情...' },
  { type: 'moments',      name: '朋友圈文案', icon: '💬', desc: '生活分享、心情记录', placeholder: '如：旅游打卡、美食晒图、心情...' },
  { type: 'weibo',        name: '微博文案', icon: '📢', desc: '热门话题、日常吐槽', placeholder: '如：今日趣事、热点评论、感悟...' },
  { type: 'gongzhonghao', name: '公众号文章', icon: '📰', desc: '干货长文、观点输出', placeholder: '如：行业分析、经验总结、教程...' },
  { type: 'english',      name: '英语写作', icon: '🇬🇧', desc: 'English essay / 英语范文', placeholder: '如：My dream, The importance of reading, 议论文：Should students use phones...' },
  { type: 'zuowen',       name: '作文写作', icon: '📝', desc: '学生范文、记叙文/议论文', placeholder: '如：难忘的一天、我的梦想、议论文：奋斗...' },
  { type: 'mortgage',     name: '房贷计算器', icon: '🏠', desc: '等额本息/等额本金', placeholder: '' },
  { type: 'taobao',       name: '商品描述', icon: '🛒', desc: '淘宝/拼多多带货文案', placeholder: '如：纯棉T恤、家用加湿器...' },
]

Page({
  data: {
    scenes: SCENES,
    selectedScene: SCENES[0].type,
    sceneName: SCENES[0].name,
    placeholder: SCENES[0].placeholder,
    topic: '',
    images: [],
    loading: false,
    // 房贷计算器
    mortgageAmount: '1000000',
    mortgageRate: '3.85',
    mortgageYears: '30',
    mortgageType: 'debx',
    mortgageResult: null
  },

  selectScene(e) {
    const type = e.currentTarget.dataset.type
    const scene = SCENES.find(s => s.type === type)
    this.setData({
      selectedScene: type,
      sceneName: scene?.name || '',
      placeholder: scene?.placeholder || '输入主题...'
    })
  },

  onTopicInput(e) {
    this.setData({ topic: e.detail.value })
  },

  // 房贷计算器输入
  onMortgageInput(e) {
    const { field } = e.currentTarget.dataset
    this.setData({ [field]: e.detail.value, mortgageResult: null })
  },

  onMortgageTypeChange(e) {
    this.setData({ mortgageType: e.detail.value, mortgageResult: null })
  },

  calculateMortgage() {
    const { mortgageAmount, mortgageRate, mortgageYears, mortgageType } = this.data
    const total = parseFloat(mortgageAmount) * 10000
    const yearRate = parseFloat(mortgageRate) / 100
    const monthRate = yearRate / 12
    const months = parseInt(mortgageYears) * 12

    if (!total || !monthRate || !months) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' })
      return
    }

    if (mortgageType === 'debx') {
      // 等额本息
      const power = Math.pow(1 + monthRate, months)
      const monthlyPayment = total * monthRate * power / (power - 1)
      const totalPayment = monthlyPayment * months
      const totalInterest = totalPayment - total
      const result = {
        title: '等额本息',
        monthlyPayment: Math.round(monthlyPayment),
        totalPayment: Math.round(totalPayment),
        totalInterest: Math.round(totalInterest),
        total,
        months,
        yearRate: yearRate * 100
      }
      this.setData({ mortgageResult: result })
    } else {
      // 等额本金
      const principalPerMonth = total / months
      let totalPayment = 0
      const details = []
      for (let i = 0; i < months; i++) {
        const remaining = total - principalPerMonth * i
        const interest = remaining * monthRate
        const payment = principalPerMonth + interest
        totalPayment += payment
        if (i === 0 || i === months - 1) {
          details.push({ month: i + 1, payment: Math.round(payment), interest: Math.round(interest), principal: Math.round(principalPerMonth) })
        }
      }
      const result = {
        title: '等额本金',
        firstPayment: Math.round(principalPerMonth + total * monthRate),
        lastPayment: Math.round(principalPerMonth + principalPerMonth * monthRate),
        totalPayment: Math.round(totalPayment),
        totalInterest: Math.round(totalPayment - total),
        total,
        months,
        yearRate: yearRate * 100,
        details
      }
      this.setData({ mortgageResult: result })
    }

    // 跳转到结果页
    const app = getApp()
    app.globalData.generatedContent = this.formatMortgageResult()
    app.globalData.sceneName = '房贷计算器'
    wx.navigateTo({ url: '/pages/result/result' })
  },

  formatMortgageResult() {
    const r = this.data.mortgageResult
    if (!r) return ''
    if (this.data.mortgageType === 'debx') {
      return `【等额本息】\n\n贷款总额：${(r.total / 10000).toFixed(0)} 万元\n贷款年限：${r.months / 12} 年\n年利率：${r.yearRate.toFixed(2)}%\n\n月供：${r.monthlyPayment.toLocaleString()} 元\n总利息：${r.totalInterest.toLocaleString()} 元\n还款总额：${r.totalPayment.toLocaleString()} 元`
    } else {
      let text = `【等额本金】\n\n贷款总额：${(r.total / 10000).toFixed(0)} 万元\n贷款年限：${r.months / 12} 年\n年利率：${r.yearRate.toFixed(2)}%\n\n首月月供：${r.firstPayment.toLocaleString()} 元\n末月月供：${r.lastPayment.toLocaleString()} 元\n总利息：${r.totalInterest.toLocaleString()} 元\n还款总额：${r.totalPayment.toLocaleString()} 元`
      if (r.details) {
        text += `\n\n还款明细：\n第1期：${r.details[0].payment.toLocaleString()} 元\n第${r.details[1].month}期：${r.details[1].payment.toLocaleString()} 元`
      }
      return text
    }
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

    // 房贷计算器本地处理
    if (selectedScene === 'mortgage') {
      this.calculateMortgage()
      return
    }

    if (loading || !topic.trim() || !selectedScene) return

    this.setData({ loading: true })

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
        this.setData({ loading: false })
      }
    })
  }
})
