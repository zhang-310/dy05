/**
 * 直播管理模块（Live Management Module）
 *
 * 该模块提供直播场次、直播产品、直播话术和监控数据的完整管理功能。
 *
 * 主要组件：
 * - entity: 数据实体（LiveSession、LiveProduct、LiveScript、LiveMonitor）
 * - repository: 数据访问层
 * - service: 业务逻辑层
 * - controller: REST API 接口
 * - vo: 数据传输对象
 *
 * 关键特性：
 * - 支持动态查询和分页
 * - 完整的异常处理
 * - 事务管理
 * - 认证权限控制
 *
 * @author gaifan
 */
package cn.gaifan.douyinOperations.module.live;
