# auth 模块 API 文档

## 文件结构
```
config/OAuthProviderProperties.java
controller/AuthController.java
controller/AuthResourceController.java
controller/AuthRoleController.java
controller/AuthUserController.java
controller/DashboardController.java
controller/OrganizationController.java
controller/package-info.java
dto/AuthUserDTO.java
entity/AuthLoginLog.java
entity/AuthOrgMember.java
entity/AuthOrganization.java
entity/AuthResource.java
entity/AuthRole.java
entity/AuthRoleResource.java
entity/AuthThirdPartyBind.java
entity/AuthUser.java
entity/AuthVerifyCode.java
entity/package-info.java
package-info.java
repository/AuthLoginLogRepository.java
repository/AuthOrgMemberRepository.java
repository/AuthOrganizationRepository.java
repository/AuthResourceRepository.java
repository/AuthRoleRepository.java
repository/AuthRoleResourceRepository.java
repository/AuthThirdPartyBindRepository.java
repository/AuthUserRepository.java
repository/AuthVerifyCodeRepository.java
repository/package-info.java
service/AuthLoginLogService.java
service/AuthLoginService.java
service/AuthMenuService.java
service/AuthOAuthService.java
service/AuthPermissionService.java
service/AuthResourceService.java
service/AuthRoleService.java
service/AuthTokenStore.java
service/AuthUserService.java
service/CaptchaService.java
service/OrganizationService.java
service/impl/AuthLoginServiceImpl.java
service/impl/AuthMenuServiceImpl.java
service/impl/AuthOAuthServiceImpl.java
service/impl/AuthPermissionServiceImpl.java
service/impl/AuthResourceServiceImpl.java
service/impl/AuthRoleServiceImpl.java
service/impl/AuthUserServiceImpl.java
service/impl/CaptchaServiceImpl.java
service/impl/InMemoryAuthTokenStore.java
service/impl/RedisAuthTokenStore.java
service/impl/package-info.java
service/package-info.java
util/DevDataInit.java
util/package-info.java
vo/AuthResourceSaveVO.java
vo/AuthResourceSearchVO.java
vo/AuthResourceVO.java
vo/AuthRoleResourceSaveVO.java
vo/AuthRoleSaveVO.java
vo/AuthRoleSearchVO.java
vo/AuthRoleVO.java
vo/AuthUserBanVO.java
vo/AuthUserGetVO.java
vo/AuthUserSaveVO.java
vo/AuthUserSearchVO.java
vo/AuthUserVO.java
vo/CaptchaVO.java
vo/ChangePasswordVO.java
vo/ForgotPasswordVO.java
vo/LoginLogQueryVO.java
vo/LoginLogVO.java
vo/LoginResultVO.java
vo/LoginVO.java
vo/MenuItemVO.java
vo/OAuthUserInfo.java
vo/OnlineUserVO.java
vo/ProfileUpdateVO.java
vo/ProfileVO.java
vo/ResourceCodeVO.java
vo/SmsSendVO.java
vo/package-info.java
```

## API 接口

### AuthController
```
@RequestMapping("/api/v1/auth")
@PostMapping("/captcha")
public RESTResult<CaptchaVO> captcha(@RequestBody(required = false) java.util.Map<String, Object> body) {
@PostMapping("/login")
public RESTResult<LoginResultVO> login(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/sms/send")
public RESTResult<Void> smsSend(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/forgot-password")
public RESTResult<Void> forgotPassword(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/profile")
public RESTResult<ProfileVO> profile(HttpServletRequest request) {
@PostMapping("/profile/update")
public RESTResult<Void> profileUpdate(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/profile/change-password")
public RESTResult<Void> changePassword(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/menu/search")
public RESTResult<List<MenuItemVO>> menuSearch(HttpServletRequest request) {
@PostMapping("/resource/search")
public RESTResult<ResourceCodeVO> resourceSearch(HttpServletRequest request) {
@GetMapping("/oauth/authorize")
public RESTResult<java.util.Map<String, String>> oauthAuthorize(HttpServletRequest request,
@GetMapping("/oauth/callback")
@PostMapping("/oauth/bindings")
public RESTResult<java.util.List<String>> oauthBindings(HttpServletRequest request) {
@PostMapping("/oauth/bind")
public RESTResult<OAuthUserInfo> oauthBind(HttpServletRequest request,
@PostMapping("/oauth/unbind")
public RESTResult<Void> oauthUnbind(HttpServletRequest request,
@PostMapping("/logout")
public RESTResult<Void> logout(HttpServletRequest request) {
```

### AuthResourceController
```
@RequestMapping("/api/v1/auth/resource")
@PostMapping("/list")
public RESTResult<PageResultVO<AuthResourceVO>> list(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/tree")
public RESTResult<List<MenuItemVO>> tree(HttpServletRequest request) {
@PostMapping("/tree-full")
public RESTResult<List<MenuItemVO>> treeFull(HttpServletRequest request) {
@PostMapping("/get")
public RESTResult<AuthResourceVO> get(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
```

### AuthRoleController
```
@RequestMapping("/api/v1/auth/role")
@PostMapping("/search")
public RESTResult<PageResultVO<AuthRoleVO>> search(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/list")
public RESTResult<List<AuthRoleVO>> listAll(HttpServletRequest request) {
@PostMapping("/get")
public RESTResult<AuthRoleVO> get(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/resources")
public RESTResult<List<Long>> getResources(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/resources/save")
public RESTResult<Void> saveResources(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
```

### AuthUserController
```
@RequestMapping("/api/v1/auth/user")
@PostMapping("/search")
public RESTResult<PageResultVO<AuthUserVO>> search(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/get")
public RESTResult<AuthUserVO> get(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/ban")
public RESTResult<Void> ban(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/login-logs")
public RESTResult<List<LoginLogVO>> loginLogs(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/online")
public RESTResult<List<OnlineUserVO>> online(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
```

### DashboardController
```
@RequestMapping("/api/v1/dashboard")
@PostMapping("/admin")
public RESTResult<Map<String, Object>> adminDashboard(HttpServletRequest request) {
@PostMapping("/org")
public RESTResult<Map<String, Object>> orgDashboard(HttpServletRequest request) {
@PostMapping("/talent")
public RESTResult<Map<String, Object>> talentDashboard(HttpServletRequest request) {
```

### OrganizationController
```
@RequestMapping("/api/v1/organization")
@PostMapping("/my")
public RESTResult<Map<String, Object>> getMyOrg(HttpServletRequest request,
@PostMapping("/create")
public RESTResult<Map<String, Object>> createOrg(HttpServletRequest request, @RequestBody Map<String, String> body) {
@PostMapping("/update")
public RESTResult<Void> updateOrg(HttpServletRequest request, @RequestBody Map<String, String> body) {
@PostMapping("/members")
public RESTResult<List<Map<String, Object>>> getMembers(HttpServletRequest request,
@PostMapping("/invite")
public RESTResult<Void> invite(HttpServletRequest request, @RequestBody Map<String, Long> body) {
@PostMapping("/remove")
public RESTResult<Void> remove(HttpServletRequest request, @RequestBody Map<String, Long> body) {
@PostMapping("/invitations")
public RESTResult<List<Map<String, Object>>> getInvitations(HttpServletRequest request,
@PostMapping("/invitation/accept")
public RESTResult<Void> acceptInvitation(HttpServletRequest request, @RequestBody Map<String, Long> body) {
@PostMapping("/invitation/reject")
public RESTResult<Void> rejectInvitation(HttpServletRequest request, @RequestBody Map<String, Long> body) {
@PostMapping("/search-talents")
public RESTResult<List<Map<String, Object>>> searchTalents(HttpServletRequest request,
```

### package-info
```
```

## Entity 字段

### AuthLoginLog
```
@Id
private Long id;
@Column(name = "user_id")
private Long userId;
@Column(name = "username", length = 64)
private String username;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "fail_reason", length = 256)
private String failReason;
@Column(name = "login_type", nullable = false, length = 32)
private String loginType;
@Column(name = "device_type", nullable = false, length = 16)
private String deviceType;
@Column(name = "ip", length = 64)
private String ip;
@Column(name = "user_agent", length = 256)
private String userAgent;
@Column(name = "login_time", nullable = false)
private Timestamp loginTime;
```

### AuthOrgMember
```
@Id
private Long id;
@Column(name = "org_id", nullable = false)
private Long orgId;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "role_in_org", length = 32)
private String roleInOrg = "member";
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "invited_at")
private Timestamp invitedAt;
@Column(name = "joined_at")
private Timestamp joinedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AuthOrganization
```
@Id
private Long id;
@Column(name = "org_name", nullable = false, length = 128)
private String orgName;
@Column(name = "org_code", length = 64)
private String orgCode;
@Column(name = "contact_name", length = 64)
private String contactName;
@Column(name = "contact_phone", length = 20)
private String contactPhone;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AuthResource
```
@Id
private Long id;
@Column(name = "resource_type", nullable = false, length = 16)
private String resourceType;
@Column(name = "resource_code", nullable = false, length = 256)
private String resourceCode;
@Column(name = "request_method", length = 16)
private String requestMethod;
@Column(name = "module", length = 64)
private String module;
@Column(name = "resource_name", length = 128)
private String resourceName;
@Column(name = "parent_id")
private Long parentId = 0L;
@Column(name = "sort_order")
private Integer sortOrder = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AuthRole
```
@Id
private Long id;
@Column(name = "role_code", nullable = false, length = 64)
private String roleCode;
@Column(name = "role_name", nullable = false, length = 64)
private String roleName;
@Column(name = "sort_order")
private Integer sortOrder = 0;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AuthRoleResource
```
@Id
private Long id;
@Column(name = "role_id", nullable = false)
private Long roleId;
@Column(name = "resource_id", nullable = false)
private Long resourceId;
@Column(name = "create_time")
private Timestamp createTime;
```

### AuthThirdPartyBind
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "provider", nullable = false, length = 32)
private String provider;
@Column(name = "open_id", nullable = false, length = 256)
private String openId;
@Column(name = "union_id", length = 256)
private String unionId;
@Column(name = "nickname", length = 64)
private String nickname;
@Column(name = "avatar", length = 512)
private String avatar;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AuthUser
```
@Id
private Long id;
@Column(name = "username", nullable = false, length = 64)
private String username;
@Column(name = "password_hash", nullable = false, length = 128)
private String passwordHash;
@Column(name = "mobile", length = 20)
private String mobile;
@Column(name = "email", length = 128)
private String email;
@Column(name = "nickname", length = 64)
private String nickname;
@Column(name = "avatar_url", length = 256)
private String avatarUrl;
@Column(name = "role_code", nullable = false, length = 32)
private String roleCode = "user";
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "banned_at")
private Timestamp bannedAt;
@Column(name = "banned_reason", length = 256)
private String bannedReason;
@Column(name = "last_login_at")
private Timestamp lastLoginAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "organization_id")
private Long organizationId;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AuthVerifyCode
```
@Id
private Long id;
@Column(name = "target", nullable = false, length = 128)
private String target;
@Column(name = "code", nullable = false, length = 16)
private String code;
@Column(name = "type", nullable = false, length = 32)
private String type;
@Column(name = "expire_at", nullable = false)
private Timestamp expireAt;
@Column(name = "used", nullable = false)
private Integer used = 0;
@Column(name = "try_count", nullable = false)
private Integer tryCount = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### package-info
```
```

