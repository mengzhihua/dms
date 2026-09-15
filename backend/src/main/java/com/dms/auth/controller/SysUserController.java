package com.dms.auth.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.dms.auth.LoginUser;
import com.dms.auth.Role;
import com.dms.auth.UserContext;
import com.dms.auth.entity.SysUser;
import com.dms.auth.mapper.SysUserMapper;
import com.dms.auth.service.AuthService;
import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
import com.dms.common.R;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/user")
public class SysUserController extends BaseCrudController<SysUser, SysUserMapper> {
    private final AuthService authService;

    public SysUserController(AuthService authService) {
        super(SysUser.class);
        this.authService = authService;
    }

    protected String[] keywordColumns() {
        return new String[] {"username", "real_name"};
    }

    @Override
    protected void beforeSave(SysUser entity) {
        LoginUser me = UserContext.get();
        if (me != null && me.getId() != null && me.getId().equals(entity.getId())) {
            if (Boolean.FALSE.equals(entity.getEnabled())) {
                throw new BizException("不能禁用当前登录账号");
            }
        }
        if (Role.of(entity.getRole()) == null) {
            throw new BizException("角色不合法");
        }
        if (StringUtils.isNotBlank(entity.getPassword())) {
            entity.setPasswordHash(authService.hashPassword(entity.getPassword()));
        }
        entity.setPassword(null);
        if (entity.getId() == null && entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
    }

    @Override
    public R<Void> delete(@PathVariable Long id) {
        LoginUser me = UserContext.get();
        if (me != null && me.getId() != null && me.getId().equals(id)) {
            throw new BizException("不能删除当前登录账号");
        }
        return super.delete(id);
    }
}
