# 1.什么是ark-leaf？
ark-leaf是ark系列框架中的分布式ID服务，基于美团leaf组件开发，在leaf服务的基础上做了功能的增强。
# 2.使用场景
- 生成全局唯一ID：互联网应用中，某个表可能要占用很大的物理存储空间，为了解决该问题，使用数据库分片技术。将一个数据库进行拆分，通过数据库中间件连接。如果数据库中该表选用ID自增策略，则可能产生重复的ID，此时应该使用分布式ID生成策略来生成全局唯一的ID。
- 订单号生成场景
- 生成业务主键ID的场景
- 生成MQ message ID的场景
# 3.功能增强列表
- 增加了对dubbo协议的支持，可同时提供http和dubbo两种服务；
- fix官方原版读取DB失败的问题；
- snowflake部分不在依赖zookeeper，改为依赖nacos。
- 本地配置信息（如dubbo，mysql连接池，leaf自身配置等）从nacos配置中心读，不在读本地配置文件。
# 4.ark-leaf服务启动
> 1. 修改配置文件中config/application.properties，bootstrap.properties的spring.cloud.nacos.config.server-addr地址为nacos地址。
> 2. 复制script文件夹下的ark-leaf-dev.properties到，nacos的public namespace下面的ark-leaf-dev.properties。
> 3. 导入script文件下的sql到数据库。
> 4. LeafServerApplication.java 启动服务。
# 5.为什么leaf改造使用nacos生成workerID？
dubbo使用nacos注册中心，不需要为了生成workid而单独维护一套zookeeper。 

<br/>其他生成workerID的方式：
> 1. 使用redis
>   - a. 自增方式，使一个key初始化为0，每哥leaf服务启动都读取使其自增1，这样可以保证原子性，很好的规避了并发问题，在leaf服务重启后通过redis获取workerID还是不重复。
>     - 问题：雪花算法10bit的workerID一共可以部署 2^10 = 1024台，也就是workerID取值范围是 0 ~ 1023。当leaf服务多重启几次，redis中存储的值就有可能超过1023了，并且在超过1023后，不能很方便的知道哪些workId被占用了。且在服务停止时不能将该数值-1，因为当前服务workerid并不可能是最后一个占用的数字。
>   - b. 在redis中设置一个数组,在redis中初始化一个类型为boolean的长度为1024的数组，默认全部为true。在服务启动时循环数组，得到第一个为true的元素下标并将其设为false，这步操作是原子性的，并在服务停止之前使用@PreDestroy注解标注方法，在服务摧毁之前根据 workerID恢复数组对应的元素为true。
>     - 问题：该方案解决了上面workerid无法复用的问题，但在测试下如果服务进程是被kill时无法执行@PreDestroy注解标注方法，即无法恢复false为true。
> 2. 使用IP取余，当我们每台机器都仅部署一台leaf服务时，可以根据IP去计算workerID。
>   - 问题：因为workerid不能超过1023，所以对ip转换成byte后得到ascii码总和进行1024取余，还是可能导致workerid重复问题。
> 3. 使用nacos
>   - a. 获取nacos所有leaf实例，循环获取下标索引作为workerId
>     问题：
>      - 并发问题：
>        - 如果当前有服务A，workerId为0，服务B-1，服务C-2。此时服务A宕机，按照计算方法,服务B的workerId应该由1变0，服务C的workerId由2变1, 但由于网络波动，可能服务B并没有收到nacos的通知而导致workerid还是1，而此时服务C的workerId已经是1了,所以会导致workerid相同，两个服务生成的id重复。
>        - 如果两个新leaf服务同时启动，由于获取实例下标和注册进nacos的操作不是原子性的，很可能导致两个服务得到的实例列表是一样的，在等待nacos感知到新服务已注册进入nacos时的下一次广播这段时间内可能导致计算的workerId(下标)一致，服务生成id重复。
>      - 性能问题：只要有一个leaf实例状态发生变化，那么所有在线的leaf服务workerId就需要重新计算，造成的资源浪费严重。
>   - b. leaf服务注册nacos持久化实例：在leaf项目中配置spring.cloud.nacos.discovery.ephemeral=false，leaf服务注册nacos为持久性实例，并且取消监听代码，直接获取所有实例列表计算下标索引即可。
>      - 问题：当前方案可以解决方案a的并发问题和性能问题，但是在新服务第一次启动时还是会遇到获取的实例数组中不包含自身服务而导致的方案a并发的问题。
>   - c. 手动配置workerid，将其作为实例元数据一并注册进nacos
>      - 这个方案可以解决以上所有问题，适合小集群，但是如果集群服务多时，手动配置成本过高，且在出现workerid冲突时是很难排查出问题的。
>   - d. 手动注册nacos实例：由b可知，只需要解决新服务第一次注册nacos时在获取实例计算workerID这步骤之前就已经注册好实例后再去获取实例列表计算workerID，则不会出现并发的问题。
>      - 但是经过实践得知，nacos注册实例顺序是由ip决定，也就是说后注册进nacos的服务，很可能因为ip而显示排序在服务列表的第一位，所以不能使用下标来计算workerid


