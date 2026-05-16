<template>
  <div class="publish-select">
    <div class="header" >
      <h1>你想发布什么？</h1>
      <p>选择发布类型，轻松出售闲置或求购所需</p>
    </div>
    
    <div class="options">
      <div class="option-card product" @click="$router.push('/publish-product')">
        <div class="icon">
          <ShoppingBag />
        </div>
        <div class="content">
          <div class="title">
            发布商品
            <span class="badge">最常用</span>
          </div>
          <p class="desc">出售闲置物品，让物品流转起来</p>
        </div>
      </div>
      
      <div class="option-card wanted" @click="$router.push('/publish-wanted')">
        <div class="icon">
          <Star />
        </div>
        <div class="content">
          <div class="title">发布求购</div>
          <p class="desc">找不到想要的东西？发布求购等待好心人</p>
        </div>
      </div>
    </div>
    
    <div class="tips">
      <h3>💡 发布小贴士</h3>
      <ul>
        <li>图文并茂的商品页面成交率提升3倍</li>
        <li>价格设置合理是成交关键，参考市场价7折</li>
        <li>二手交易每单可获得减碳积分奖励</li>
        <li>分类填写准确，让买家更容易找到你</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ShoppingBag, Star } from "@element-plus/icons-vue";
</script>

<style scoped>
.publish-select {
  background: #f1f8e9;
  border-radius: 12px;
  max-width: 600px;
  margin: 40px auto;
  padding: 30px 20px;
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
}

.header p {
  color: #909399;
  font-size: 14px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 100px;
  height: 400px;
}

.option-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.icon {
  width: 100px;
  height: 100px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 16px;
}

.option-card.product {
  background: linear-gradient(135deg, #e8f5e9 0%, #ffffff 100%);
  border-color: #81c784;
}

.option-card.wanted {
  background: linear-gradient(135deg, #fff3e0 0%, #ffffff 100%);
  border-color: #ffb74d;
}

.option-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.product .icon {
  background: #c8e6c9;
  color: #2e7d32;
}

.wanted .icon {
  background: #ffcc80;
  color: #e65100;
}

.content {
  flex: 1;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.badge {
  font-size: 12px;
  padding: 2px 8px;
  background: #2e7d32;
  color: white;
  border-radius: 10px;
}

.desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.arrow {
  font-size: 20px;
  color: #c0c4cc;
}

.tips {
  margin-top: 32px;
  padding: 20px 24px;
  background: #f1f8e9;
  border-radius: 12px;
}

.tips h3 {
  font-size: 14px;
  font-weight: 600;
  color: #2e7d32;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tips ul {
  margin: 0;
  padding-left: 20px;
}

.tips li {
  font-size: 13px;
  color: #606266;
  line-height: 2;
}
</style>
