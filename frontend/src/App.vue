<template>
  <el-container>
    <el-header>AI 数据库自然语言问答平台</el-header>
    <el-container>
      <el-aside width="200px">
        <el-menu :router="true">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/query">查询</el-menu-item>
          <el-menu-item index="/history">历史</el-menu-item>
          <el-menu-item index="/datasource">数据源</el-menu-item>
          <el-menu-item index="/schema">Schema</el-menu-item>
          <el-menu-item index="/statistics">统计</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.el-header {
  background-color: #409eff;
  color: white;
  line-height: 60px;
  font-size: 20px;
  font-weight: bold;
}
</style>
