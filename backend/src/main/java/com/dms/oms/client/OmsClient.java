package com.dms.oms.client;

import java.util.List;
import java.util.Map;

/** OMS 渠道开放接口(/api/open/channel/**)客户端抽象;返回值为 OMS 响应中的 data 部分。 */
public interface OmsClient {
    /** 渠道下单(OMS 按 shopCode+channelOrderNo 幂等) */
    Map<String, Object> createOrder(Map<String, Object> request);

    /** 按渠道单号查询订单,不存在返回 null */
    Map<String, Object> getOrder(String shopCode, String channelOrderNo);

    Map<String, Object> cancelOrder(String shopCode, String channelOrderNo, String reason);

    /** 渠道可售库存 [{sku,qty}] */
    List<Map<String, Object>> inventory(String shopCode, List<String> skus);
}
