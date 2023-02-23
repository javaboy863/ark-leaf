package com.ark.inf.leaf;


public interface IIdGenService {

    /**
     * 根据key获取以号段方式生成的ID
     */
    Long getSegmentId(String key);

    /**
     * 根据key获取以雪花算法方式生成的ID
     */
    Long getSnowflakeId(String key);
}
