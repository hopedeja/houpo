package com.youruan.dentistry.core.enroll.service.impl;

import com.youruan.dentistry.core.activity.domain.Activity;
import com.youruan.dentistry.core.activity.service.ActivityService;
import com.youruan.dentistry.core.base.query.Pagination;
import com.youruan.dentistry.core.base.utils.HttpClientUtils;
import com.youruan.dentistry.core.base.utils.SnowflakeIdWorker;
import com.youruan.dentistry.core.base.wxpay.sdk.WXPayConstants;
import com.youruan.dentistry.core.base.wxpay.sdk.WXPayUtil;
import com.youruan.dentistry.core.enroll.WxPayProperties;
import com.youruan.dentistry.core.enroll.domain.Enroll;
import com.youruan.dentistry.core.enroll.mapper.EnrollMapper;
import com.youruan.dentistry.core.enroll.query.EnrollQuery;
import com.youruan.dentistry.core.enroll.service.EnrollService;
import com.youruan.dentistry.core.enroll.vo.ExtendedEnroll;
import com.youruan.dentistry.core.user.domain.RegisteredUser;
import com.youruan.dentistry.core.user.service.RegisteredUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.*;

@Service
public class BasicEnrollService implements EnrollService {

    private final EnrollMapper enrollMapper;
    private final ActivityService activityService;
    private final RegisteredUserService userService;
    private final WxPayProperties wxPayProperties;

    public BasicEnrollService(EnrollMapper enrollMapper, ActivityService activityService, RegisteredUserService userService, WxPayProperties wxPayProperties) {
        this.enrollMapper = enrollMapper;
        this.activityService = activityService;
        this.userService = userService;
        this.wxPayProperties = wxPayProperties;
    }

    @Override
    public Pagination<ExtendedEnroll> query(EnrollQuery qo) {
        int rows = enrollMapper.count(qo);
        List<ExtendedEnroll> datas = ( (rows == 0) ? new ArrayList<>() : enrollMapper.query(qo) );
        return new Pagination<>(rows,datas);
    }

    @Override
    @Transactional
    public Enroll create(RegisteredUser user, Long activityId, Integer type) {
        Assert.isTrue(userService.checkCompleteInfo(user),"请先完善信息");
        Assert.isTrue(this.checkEnrollStatus(activityId),"该活动暂未开启报名");
        Enroll enroll = new Enroll();
        // 除了职业百分百，其他默认已支付
        enroll.setOrderStatus(Enroll.ORDER_STATUS_OK);
        if(type==Enroll.TYPE_GENERAL) {
            // 普通活动
            Assert.isTrue(!checkEnroll(user.getId(), activityId),"该用户已经报名此活动");
            // 活动表更新报名人数
            activityService.updateNumberOfEntries(activityId);
        }else{
            // 职业百分百活动 付费
            if(type==Enroll.TYPE_WORKPLACE) {
                enroll.setOrderNo(SnowflakeIdWorker.getIdWorker());
                enroll.setPrice(new BigDecimal(1));
                enroll.setOrderStatus(Enroll.ORDER_STATUS_NOT);
            }
            // 就业直通车
            Assert.isTrue(!checkEnroll(user.getId(),type),"该用户已经报名此活动");
        }
        enroll.setType(type);
        enroll.setUserId(user.getId());
        enroll.setActivityId(activityId);
        return this.add(enroll);
    }

    /**
     * 校验活动是否开启报名
     */
    private boolean checkEnrollStatus(Long activityId) {
        Activity activity = activityService.get(activityId);
        return activity.getEnrollStatus() == 1;
    }

    /**
     * 检查用户是否报名职业百分百，或就业直通车
     * @param userId
     * @param type
     * @return
     */
    private boolean checkEnroll(Long userId, Integer type) {
        int count = enrollMapper.countByUserIdAndType(userId,type);
        return count > 0;
    }

    @Override
    public Enroll queryOne(Long userId, Integer type) {
        Assert.notNull(type,"类型不能为空");
        return enrollMapper.queryOne(userId,type);
    }

    @Override
    public String placeOrder(String orderNo, BigDecimal price, String openid, String ip) {
        try {
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("appid",wxPayProperties.getAppId());
            paramMap.put("mch_id",wxPayProperties.getMchid());
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paramMap.put("sign",WXPayUtil.generateSignature(paramMap,wxPayProperties.getPrivateKey()));
            paramMap.put("body","大苏打");
            paramMap.put("out_trade_no",orderNo);
            paramMap.put("total_fee",price.toString());
            paramMap.put("spbill_create_ip",ip);
            paramMap.put("notify_url",wxPayProperties.getNotifyUrl());
            paramMap.put("trade_type","JSAPI");
            paramMap.put("openid",openid);
            String xml = HttpClientUtils.doPostXml(WXPayConstants.UNIFIED_ORDER_URL,
                    WXPayUtil.generateSignedXml(paramMap, wxPayProperties.getPrivateKey()));
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            System.out.println("resultMap:" + resultMap);
            return resultMap.get("prepay_id");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String,String> payHandle(String prepayId) {
        try {
            Map<String,String> resultMap = new HashMap<>();
            resultMap.put("appId",wxPayProperties.getAppId());
            resultMap.put("timeStamp",String.valueOf(System.currentTimeMillis() / 1000));
            resultMap.put("signType","MD5");
            resultMap.put("nonceStr",WXPayUtil.generateNonceStr());
            resultMap.put("package","prepay_id="+prepayId);
            resultMap.put("paySign",WXPayUtil.generateSignature(resultMap,wxPayProperties.getPrivateKey()));
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int countByUserIdAndActivityId(Long userId, Long activityId) {
        return enrollMapper.countByUserIdAndActivityId(userId,activityId);
    }

    @Override
    public Enroll get(Long id) {
        return enrollMapper.get(id);
    }

    @Override
    public void setOrderStatus(Enroll enroll) {
        Assert.notNull(enroll,"必须提供报名信息");
        enrollMapper.setOrderStatus(enroll);
    }

    @Override
    public Enroll getByOrderNo(String orderNo) {
        Assert.notNull(orderNo,"必须提供订单号");
        return enrollMapper.getByOrderNo(orderNo);
    }

    @Override
    public List<ExtendedEnroll> list() {
        return enrollMapper.list();
    }

    @Override
    public List<Long> getActivityIdsByUserId(Long userId) {
        Assert.notNull(userId,"用户id为空");
        return enrollMapper.getActivityIdsByUserId(userId);
    }

    /**
     * 用户报名
     */
    private Enroll add(Enroll enroll) {
        enroll.setCreatedDate(new Date());
        enrollMapper.add(enroll);
        return enroll;
    }

    /**
     * 检查用户是否已报过名
     */
    private boolean checkEnroll(Long userId, Long activityId) {
        int count = enrollMapper.countByUserIdAndActivityId(userId, activityId);
        return count > 0;
    }

}
