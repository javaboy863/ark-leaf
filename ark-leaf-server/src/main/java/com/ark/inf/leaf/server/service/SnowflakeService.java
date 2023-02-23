package com.ark.inf.leaf.server.service;

import com.ark.inf.leaf.IDGen;
import com.ark.inf.leaf.common.ZeroIDGen;
import com.ark.inf.leaf.nacos.NacosConfig;
import com.ark.inf.leaf.server.exception.InitException;
import com.ark.inf.leaf.common.Constants;
import com.ark.inf.leaf.common.Result;
import com.ark.inf.leaf.snowflake.SnowflakeIDGenImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("SnowflakeService")
public class SnowflakeService {
    private Logger logger = LoggerFactory.getLogger(SnowflakeService.class);

    private IDGen idGen;

    public SnowflakeService() throws InitException {
        boolean flag = Boolean.parseBoolean(NacosConfig.getSpringConfig(Constants.LEAF_SNOWFLAKE_ENABLE, "true"));
        if (flag) {
            idGen = new SnowflakeIDGenImpl();
            if(idGen.init()) {
                logger.info("Snowflake Service Init Successfully");
            } else {
                throw new InitException("Snowflake Service Init Fail");
            }
        } else {
            idGen = new ZeroIDGen();
            logger.info("Zero ID Gen Service Init Successfully");
        }
    }

    public Result getId(String key) {
        return idGen.get(key);
    }
}
