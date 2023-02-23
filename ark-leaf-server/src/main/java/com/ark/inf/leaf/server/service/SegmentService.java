package com.ark.inf.leaf.server.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.ark.inf.leaf.IDGen;
import com.ark.inf.leaf.common.ZeroIDGen;
import com.ark.inf.leaf.nacos.NacosConfig;
import com.ark.inf.leaf.segment.dao.IDAllocDao;
import com.ark.inf.leaf.segment.dao.impl.IDAllocDaoImpl;
import com.ark.inf.leaf.server.exception.InitException;
import com.ark.inf.leaf.common.Constants;
import com.ark.inf.leaf.common.Result;
import com.ark.inf.leaf.segment.SegmentIDGenImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service("SegmentService")
public class SegmentService {
    private Logger logger = LoggerFactory.getLogger(SegmentService.class);

    private IDGen idGen;
    private DruidDataSource dataSource;
    public SegmentService() throws SQLException, InitException {
        boolean flag = Boolean.parseBoolean(NacosConfig.getSpringConfig(Constants.LEAF_SEGMENT_ENABLE, "true"));
        if (flag) {
            // CogetdataSource
            dataSource = new DruidDataSource();
            dataSource.setUrl(NacosConfig.getSpringConfig(Constants.LEAF_JDBC_URL));
            dataSource.setUsername(NacosConfig.getSpringConfig(Constants.LEAF_JDBC_USERNAME));
            dataSource.setPassword(NacosConfig.getSpringConfig(Constants.LEAF_JDBC_PASSWORD));
            dataSource.setDriverClassName(NacosConfig.getSpringConfig(Constants.LEAF_JDBC_DRIVER_CLASS_NAME));
            dataSource.setMaxActive(30);
            dataSource.setInitialSize(10);
            dataSource.init();

            // Config Dao
            IDAllocDao dao = new IDAllocDaoImpl(dataSource);
            // Config ID Gen
            idGen = new SegmentIDGenImpl();
            ((SegmentIDGenImpl) idGen).setDao(dao);
            if (idGen.init()) {
                logger.info("Segment Service Init Successfully");
            } else {
                throw new InitException("Segment Service Init Fail");
            }
        } else {
            idGen = new ZeroIDGen();
            logger.info("Zero ID Gen Service Init Successfully");
        }
    }

    public Result getId(String key) {
        return idGen.get(key);
    }

    public SegmentIDGenImpl getIdGen() {
        if (idGen instanceof SegmentIDGenImpl) {
            return (SegmentIDGenImpl) idGen;
        }
        return null;
    }
}
