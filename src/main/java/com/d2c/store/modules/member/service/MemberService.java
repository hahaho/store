package com.d2c.store.modules.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.d2c.store.modules.core.model.P2PDO;
import com.d2c.store.modules.member.model.MemberDO;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author BaiCai
 */
public interface MemberService extends IService<MemberDO> {

    MemberDO doOauth(String account, BigDecimal amount, String loginIp, P2PDO p2pDO);

    MemberDO doLogin(MemberDO member, String loginIp, String accessToken, Date accessExpired);

    boolean doLogout(String account);

    boolean updatePassword(String account, String password);

}
