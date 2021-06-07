package com.youruan.dentistry.core.banner.domain;

import com.youruan.dentistry.core.base.domain.BasicDomain;
import lombok.Getter;
import lombok.Setter;

/**
 * 轮播图
 */
@Getter
@Setter
public class Banner extends BasicDomain {

    /**
     * 轮播图名称
     */
    private String bannerName;
    /**
     * 缩略图
     */
    private String imageUrl;
    /**
     * 链接
     */
    private String linkUrl;
    /**
     * 序号
     */
    private Integer no;
    /**
     * 状态 0-停用 1-启用
     */
    private Integer status;


}