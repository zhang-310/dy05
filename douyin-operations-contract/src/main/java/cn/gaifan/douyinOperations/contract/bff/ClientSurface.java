package cn.gaifan.douyinOperations.contract.bff;

/**
 * 客户端入口类型。
 *
 * <p>Web、App、小程序和开放 API 的展示结构、权限粒度、网络环境都不同。
 * MVP 阶段先用枚举明确边界，后续可以分别演进 BFF 返回结构。</p>
 */
public enum ClientSurface {
    WEB,
    APP,
    MINIAPP,
    OPENAPI
}
