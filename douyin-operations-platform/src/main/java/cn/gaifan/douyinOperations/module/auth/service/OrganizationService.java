package cn.gaifan.douyinOperations.module.auth.service;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.constant.RoleCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.auth.entity.AuthOrgMember;
import cn.gaifan.douyinOperations.module.auth.entity.AuthOrganization;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrgMemberRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrganizationRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * 机构管理：CRUD、邀请达人、成员管理
 */
@Service
public class OrganizationService {

    @Resource
    private AuthOrganizationRepository orgRepository;
    @Resource
    private AuthOrgMemberRepository memberRepository;
    @Resource
    private AuthUserRepository userRepository;

    /** 创建机构（institution 角色用户调用） */
    @Transactional(rollbackFor = Exception.class)
    public AuthOrganization createOrg(Long ownerId, String orgName, String orgCode, String contactName, String contactPhone) {
        if (orgCode != null && !orgCode.isEmpty() && orgRepository.existsByOrgCodeAndDeleted(orgCode, 0)) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "机构编码已存在");
        }
        AuthOrganization org = new AuthOrganization();
        org.setOrgName(orgName);
        org.setOrgCode(orgCode);
        org.setContactName(contactName);
        org.setContactPhone(contactPhone);
        org.setOwnerId(ownerId);
        org = orgRepository.save(org);

        // 将 owner 自身也加入成员表
        AuthOrgMember ownerMember = new AuthOrgMember();
        ownerMember.setOrgId(org.getId());
        ownerMember.setUserId(ownerId);
        ownerMember.setRoleInOrg("owner");
        ownerMember.setStatus(1);
        ownerMember.setJoinedAt(new Timestamp(System.currentTimeMillis()));
        memberRepository.save(ownerMember);

        return org;
    }

    /** 获取用户所属机构（institution 角色通过 owner_id 查找） */
    public AuthOrganization getOrgByOwnerId(Long ownerId) {
        return orgRepository.findByOwnerIdAndDeleted(ownerId, 0).orElse(null);
    }

    /** 获取机构详情 */
    public AuthOrganization getOrgById(Long orgId) {
        return orgRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "机构不存在"));
    }

    /** 更新机构信息 */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrg(Long orgId, Long operatorId, String orgName, String contactName, String contactPhone) {
        AuthOrganization org = getOrgById(orgId);
        if (!org.getOwnerId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改该机构");
        }
        if (orgName != null) org.setOrgName(orgName);
        if (contactName != null) org.setContactName(contactName);
        if (contactPhone != null) org.setContactPhone(contactPhone);
        orgRepository.save(org);
    }

    /** 邀请达人加入机构 */
    @Transactional(rollbackFor = Exception.class)
    public AuthOrgMember inviteTalent(Long orgId, Long operatorId, Long talentUserId) {
        AuthOrganization org = getOrgById(orgId);
        if (!org.getOwnerId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权邀请成员");
        }
        AuthUser talent = userRepository.findById(talentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (!RoleCode.TALENT.equals(talent.getRoleCode())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "只能邀请达人角色用户");
        }
        Optional<AuthOrgMember> existing = memberRepository.findByOrgIdAndUserIdAndDeleted(orgId, talentUserId, 0);
        if (existing.isPresent()) {
            AuthOrgMember m = existing.get();
            if (m.getStatus() == 1) {
                throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "该达人已是机构成员");
            }
            if (m.getStatus() == 0) {
                throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "已发送邀请，待达人确认");
            }
            // 之前拒绝过，重新邀请
            m.setStatus(0);
            m.setInvitedAt(new Timestamp(System.currentTimeMillis()));
            m.setJoinedAt(null);
            return memberRepository.save(m);
        }
        AuthOrgMember member = new AuthOrgMember();
        member.setOrgId(orgId);
        member.setUserId(talentUserId);
        member.setRoleInOrg("member");
        member.setStatus(0);
        return memberRepository.save(member);
    }

    /** 达人接受邀请 */
    @Transactional(rollbackFor = Exception.class)
    public void acceptInvitation(Long memberId, Long talentUserId) {
        AuthOrgMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "邀请记录不存在"));
        if (!member.getUserId().equals(talentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
        if (member.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "邀请状态不正确");
        }
        member.setStatus(1);
        member.setJoinedAt(new Timestamp(System.currentTimeMillis()));
        memberRepository.save(member);

        // 更新达人的 organization_id
        AuthUser user = userRepository.findById(talentUserId).orElse(null);
        if (user != null) {
            user.setOrganizationId(member.getOrgId());
            userRepository.save(user);
        }
    }

    /** 达人拒绝邀请 */
    @Transactional(rollbackFor = Exception.class)
    public void rejectInvitation(Long memberId, Long talentUserId) {
        AuthOrgMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "邀请记录不存在"));
        if (!member.getUserId().equals(talentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
        if (member.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "邀请状态不正确");
        }
        member.setStatus(2);
        memberRepository.save(member);
    }

    /** 移除成员 */
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long orgId, Long operatorId, Long memberUserId) {
        AuthOrganization org = getOrgById(orgId);
        if (!org.getOwnerId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权移除成员");
        }
        if (operatorId.equals(memberUserId)) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "不能移除自己");
        }
        AuthOrgMember member = memberRepository.findByOrgIdAndUserIdAndDeleted(orgId, memberUserId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "成员不存在"));
        member.setDeleted(1);
        memberRepository.save(member);

        // 清除达人的 organization_id
        AuthUser user = userRepository.findById(memberUserId).orElse(null);
        if (user != null && orgId.equals(user.getOrganizationId())) {
            user.setOrganizationId(null);
            userRepository.save(user);
        }
    }

    /** 获取机构成员列表 */
    public List<AuthOrgMember> getMembers(Long orgId) {
        return memberRepository.findByOrgIdAndDeleted(orgId, 0);
    }

    /** 获取达人的待处理邀请 */
    public List<AuthOrgMember> getPendingInvitations(Long talentUserId) {
        return memberRepository.findByUserIdAndDeletedAndStatus(talentUserId, 0, 0);
    }
}
