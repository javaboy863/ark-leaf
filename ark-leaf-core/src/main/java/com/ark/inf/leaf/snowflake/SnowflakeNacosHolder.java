package com.ark.inf.leaf.snowflake;


import com.ark.inf.leaf.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.PreservedMetadataKeys;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ark.inf.leaf.common.Utils;
import com.ark.inf.leaf.nacos.NacosConfig;
import com.ark.inf.leaf.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SnowflakeNacosHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeNacosHolder.class);
    private static final String METADATA_KEY_LAST_UPDATE_TIME = "leaf.id.snowflake.matadata.lastUpdateTime";

    private int workerId = -1;
    private String ip  = Utils.getIp();
    private int port = Integer.parseInt(NacosConfig.getSpringConfig(Constants.LEAF_SNOWFLAKE_PORT, "true"));

    private String serviceName = "arch-leaf-snowflake";
    private String serviceGroup = "SNOWFLAKE_NODE_GROUP";

    private long lastUpdateTime;
    private NamingService namingService;

    public boolean init() {
        try {
            createNamingService();
            checkLocalTime();
            CountDownLatch latch = new CountDownLatch(1);
            namingService.subscribe(serviceName, serviceGroup, new EventListener() {
                @Override
                public void onEvent(Event event) {
                    NamingEvent namingEvent = (NamingEvent) event;
                    Instance myself = namingEvent.getInstances().stream().filter(instance -> Objects.equals(instance.getIp(), ip) && Objects.equals(instance.getPort(), port))
                            .findFirst().orElse(null);
                    if (myself != null) {
                        try {
                            namingService.unsubscribe(serviceName, serviceGroup, this);
                            latch.countDown();
                        } catch (NacosException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            register();
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Init workId failed: register lost");
            }

            initWorkerId();
            scheduledUploadData();
            return true;
        } catch (Exception  e) {
            LOGGER.error("init SnowflakeNacosHolder error："+e.getMessage(), e);
        }
        return false;
    }

    private void checkLocalTime() throws NacosException {
        List<Instance> allService = getAllService();
        Instance instance = findMyselfInstance(allService);
        if (instance == null){
            return;
        }
        String lutStr = instance.getMetadata().get(METADATA_KEY_LAST_UPDATE_TIME);
        if (null != lutStr) {
            long lut = Long.parseLong(lutStr);
            if (System.currentTimeMillis() < lut) {
                throw new IllegalStateException("Init workId failed: timestamp check error");
            }
        }
    }

    private void createNamingService() throws NacosException {
        Properties namingServiceProperties = new Properties();
        String activeProfile = NacosConfig.getActiveProfile();
        if (! NacosConfig.isProductEnv(activeProfile)){
            namingServiceProperties.put("namespace", activeProfile);
        }
        namingServiceProperties.put("serverAddr", NacosConfig.getServerAddr());
        namingService = NamingFactory.createNamingService(namingServiceProperties);
    }

    private void scheduledUploadData() {
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "snowflake-schedule-upload");
            thread.setDaemon(true);
            return thread;
        }).scheduleWithFixedDelay(this::updateMeta, 1L, 3L, TimeUnit.SECONDS); //每3S上报数据
    }

    private void updateMeta() {
        if (System.currentTimeMillis() < lastUpdateTime) {
            return;
        }
        try {
            register();
        } catch (NacosException e) {
            log.error(e.getMessage(),e);
        }
    }

    private void initWorkerId() throws NacosException {
        List<Instance> allLeafNacosService = getAllService();
        if (CollectionUtils.isEmpty(allLeafNacosService)){
            throw new IllegalStateException("get nacos leaf service error.");
        }
        Collections.sort(allLeafNacosService, new Comparator<Instance>() {
            @Override
            public int compare(Instance o1, Instance o2) {
                String o1Ip = o1.getIp();
                String o2Ip = o2.getIp();
                Integer o1Num = getIpNumber(o1Ip);
                Integer o2Num = getIpNumber(o2Ip);
                return o1Num.compareTo(o2Num);
            }
        });

        Pair<Integer, Instance> myselfIndex = findMyselfIndexInstance(allLeafNacosService);
        if (myselfIndex == null) {
            throw new IllegalStateException("Init workId failed: this instance may not register");
        }
        Instance instance = myselfIndex.getValue();
        String lutStr = instance.getMetadata().get(METADATA_KEY_LAST_UPDATE_TIME);
        if (null != lutStr) {
            long lut = Long.parseLong(lutStr);
            if ( System.currentTimeMillis() > lut) {
                workerId = myselfIndex.getKey();
                LOGGER.info("init workerId:{}", workerId);
                return;
            }
            throw new IllegalStateException("Init workId failed: timestamp check error");
        }
        throw new IllegalStateException("Init workId failed: lutStr not found");
    }

    private Integer getIpNumber(String o1Ip) {
        String[] arr = o1Ip.split("\\.");
        Integer sum = 0;
        for (String s : arr) {
            sum += Integer.valueOf(s);
        }
        return sum;
    }

    private void register() throws NacosException {
        Instance instance = initInstance();
        namingService.registerInstance(serviceName, serviceGroup, instance);
    }

    private Instance initInstance() {
        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(port);
        instance.setEphemeral(false); // Ephemeral: 短暂，临时
        instance.setHealthy(true);
        instance.setEnabled(true);
        Map<String, String> metaData = new HashMap<>();
        metaData.put(PreservedMetadataKeys.INSTANCE_ID_GENERATOR, com.alibaba.nacos.api.common.Constants.SNOWFLAKE_INSTANCE_ID_GENERATOR);
        lastUpdateTime = System.currentTimeMillis();
        metaData.put(METADATA_KEY_LAST_UPDATE_TIME, Objects.toString(lastUpdateTime));
        instance.setMetadata(metaData);
        instance.setServiceName(serviceName);
        return instance;
    }


    private Instance findMyselfInstance(List<Instance> instances)  {
        return instances.stream().filter(instance -> Objects.equals(instance.getIp(), ip) && Objects.equals(instance.getPort(), port))
                .findFirst().orElse(null);
    }

    private Pair<Integer,Instance> findMyselfIndexInstance(List<Instance> instances)  {
        for (int i = 0; i < instances.size(); i++) {
            Instance instance = instances.get(i);
            if (Objects.equals(instance.getIp(), ip) && Objects.equals(instance.getPort(), port)){
                Pair<Integer,Instance> pair = new Pair<>();
                pair.setKey(i+1);
                pair.setValue(instance);
                return pair;
            }
        }
        return null;
    }

    private List<Instance> getAllService() throws NacosException {
        List<Instance> instances = namingService.getAllInstances(serviceName, serviceGroup);
        return instances;
    }

    public int getWorkerId() {
        return workerId;
    }

    public void setWorkerId(int workerId) {
        this.workerId = workerId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }



    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }


}