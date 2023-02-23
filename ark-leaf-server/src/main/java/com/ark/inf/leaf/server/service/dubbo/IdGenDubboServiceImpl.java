package com.ark.inf.leaf.server.service.dubbo;

import com.ark.inf.leaf.IIdGenService;
import com.ark.inf.leaf.server.exception.LeafServerException;
import com.ark.inf.leaf.server.exception.NoKeyException;
import com.ark.inf.leaf.common.Result;
import com.ark.inf.leaf.common.Status;
import com.ark.inf.leaf.server.service.SegmentService;
import com.ark.inf.leaf.server.service.SnowflakeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@DubboService(version = "1.0.0")
@Slf4j
public class IdGenDubboServiceImpl  implements IIdGenService {

    @Autowired
    private SegmentService segmentService;

    @Autowired
    private SnowflakeService snowflakeService;

    @Override
    public Long getSegmentId(String key) {
        return get(key, segmentService.getId(key));
    }

    @Override
    public Long getSnowflakeId(String key) {
        return get(key, snowflakeService.getId(key));
    }
    private Long get(String key, Result id) {
        Result result;
        if (key == null || key.isEmpty()) {
            throw new NoKeyException();
        }
        result = id;
        if (result.getStatus().equals(Status.EXCEPTION)) {
            log.error("KEY={}不存在",key);
            throw new LeafServerException(result.toString());
        }
        return result.getId();
    }
}
