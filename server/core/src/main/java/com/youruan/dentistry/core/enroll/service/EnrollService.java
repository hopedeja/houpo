package com.youruan.dentistry.core.enroll.service;

import com.youruan.dentistry.core.base.query.Pagination;
import com.youruan.dentistry.core.enroll.domain.Enroll;
import com.youruan.dentistry.core.enroll.domain.pay.PayParam;
import com.youruan.dentistry.core.enroll.query.EnrollQuery;
import com.youruan.dentistry.core.enroll.vo.ExtendedEnroll;

public interface EnrollService {

    /**
     * 条件分页查询-报名信息
     */
    Pagination<ExtendedEnroll> query(EnrollQuery qo);
    /**
     * 根据用户id和活动id
     * 查询是否有报名信息
     */
    int countByUserIdAndActivityId(Long userId, Long activityId);

    /**
     * 用户报名
     */
    Enroll create(Long userId, Long activityId, Integer type);

    /**
     * 查询用户订单状态
     */
    Enroll queryOne(Long userId, Integer type);

    /**
     * 下单
     */
    String placeOrder(String orderNo, String openid);

    /**
     * 返回JSAPI调起支付所需参数
     */
    PayParam payHandle(String prepayId);
}
