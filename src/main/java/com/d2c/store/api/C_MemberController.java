package com.d2c.store.api;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.api.R;
import com.d2c.store.api.base.BaseController;
import com.d2c.store.common.api.Asserts;
import com.d2c.store.common.api.Response;
import com.d2c.store.common.api.ResultCode;
import com.d2c.store.common.utils.RequestUtil;
import com.d2c.store.config.security.constant.SecurityConstant;
import com.d2c.store.modules.core.model.P2PDO;
import com.d2c.store.modules.core.service.P2PService;
import com.d2c.store.modules.logger.service.SmsService;
import com.d2c.store.modules.member.model.MemberDO;
import com.d2c.store.modules.member.service.MemberService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * @author Cai
 */
@Api(description = "会员业务")
@RestController
@RequestMapping("/api/member")
public class C_MemberController extends BaseController {

    @Autowired
    private P2PService p2PService;
    @Autowired
    private SmsService smsService;
    @Autowired
    private MemberService memberService;

    @ApiOperation(value = "登录信息")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public R<MemberDO> info() {
        return Response.restResult(loginMemberHolder.getLoginMember(), ResultCode.SUCCESS);
    }

    @ApiOperation(value = "验证码登录")
    @RequestMapping(value = "/code/login", method = RequestMethod.POST)
    public R<MemberDO> login(String account, String code, String appId) {
        Asserts.notNull("参数不能为空", account, code, appId);
        Asserts.eq(smsService.doCheck(account, code), true, "验证码不正确");
        P2PDO p2pDO = p2PService.getById(Long.valueOf(appId));
        Asserts.notNull("appId不正确，请仔细检查", p2pDO);
        MemberDO member = memberService.findLogin(account, Long.valueOf(appId));
        Asserts.notNull("账户信息异常，请联系管理员", member);
        Date accessExpired = DateUtil.offsetDay(new Date(), 7).toJdkDate();
        String accessToken = SecurityConstant.TOKEN_PREFIX + Jwts.builder()
                .setSubject(member.getAccount())
                .claim(SecurityConstant.AUTHORITIES, member.getAccountInfo().getP2pId())
                .setExpiration(accessExpired)
                .signWith(SignatureAlgorithm.HS512, SecurityConstant.JWT_SIGN_KEY)
                .compact();
        member = memberService.doLogin(member, RequestUtil.getRequestIp(request), accessToken, accessExpired);
        member.setLoginToken(accessToken);
        return Response.restResult(member, ResultCode.SUCCESS);
    }

}
