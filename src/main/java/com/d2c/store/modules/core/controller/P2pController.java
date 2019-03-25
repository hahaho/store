package com.d2c.store.modules.core.controller;

import com.d2c.store.common.api.base.BaseCtrl;
import com.d2c.store.modules.core.model.P2pDO;
import com.d2c.store.modules.core.query.P2pQuery;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author BaiCai
 */
@Api(description = "P2P平台管理")
@RestController
@RequestMapping("/back/p2p")
public class P2pController extends BaseCtrl<P2pDO, P2pQuery> {

}
