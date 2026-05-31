-- payment verify：user-1 绑定组织 + 订单 org 冗余
UPDATE auth_user SET organization_id = COALESCE(organization_id, 1) WHERE id = 1;
