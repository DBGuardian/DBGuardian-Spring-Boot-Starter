package com.erp.mapper.transport;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.transport.DispatchOrderNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 运输单明细 Mapper
 */
@Mapper
public interface DispatchOrderNoticeMapper extends BaseMapper<DispatchOrderNotice> {

    DispatchOrderNotice selectByDispatchCode(@Param("dispatchCode") String dispatchCode);

    DispatchOrderNotice selectByNoticeCode(@Param("noticeCode") String noticeCode);

    int deleteByDispatchCode(@Param("dispatchCode") String dispatchCode);
}


