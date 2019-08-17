package tech.codestory.zookeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

/**
 * 为 ZooKeeper测试代码创建一个基类，封装建立连接的过程
 * 
 * @author junyongliao
 * @date 2019/8/16
 */
public class ZooKeeperBase implements Watcher {
    /** 日志，不使用 @Slf4j ，是要使用子类的log */
    Logger log = null;

    /** 等待连接建立成功的信号 */
    private CountDownLatch connectedSemaphore = new CountDownLatch(1);
    /** ZooKeeper 客户端 */
    private ZooKeeper zooKeeper = null;
    /** 避免重复根节点 */
    static Integer rootNodeInitial = Integer.valueOf(1);

    /** 构造函数 */
    public ZooKeeperBase(String address) throws IOException {
        log = LoggerFactory.getLogger(getClass());

        Profiler profiler = new Profiler(this.getClass().getName() + " 连接到ZooKeeper");
        profiler.start("开始链接");
        zooKeeper = new ZooKeeper(address, 3000, this);
        try {
            profiler.start("等待连接成功的Event");
            connectedSemaphore.await();
        } catch (InterruptedException e) {
            log.error("InterruptedException", e);
        }
        profiler.stop();
        profiler.setLogger(log);
        profiler.log();
    }

    /**
     * 创建测试需要的根节点
     * 
     * @param rootNodeName
     * @return
     */
    public String createRootNode(String rootNodeName) {
        synchronized (rootNodeInitial) {
            // 创建 tableSerial 的zNode
            try {
                Stat existsStat = getZooKeeper().exists(rootNodeName, false);
                if (existsStat == null) {
                    rootNodeName = getZooKeeper().create(rootNodeName, new byte[0],
                            ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } catch (KeeperException e) {
                log.error("KeeperException", e);
            } catch (InterruptedException e) {
                log.error("InterruptedException", e);
            }
        }
        return rootNodeName;
    }

    /** 读取ZooKeeper对象，供子类调用 */
    protected ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    @Override
    final public void process(WatchedEvent event) {
        if (Event.EventType.None.equals(event.getType())) {
            // 连接状态发生变化
            if (Event.KeeperState.SyncConnected.equals(event.getState())) {
                // 连接建立成功
                connectedSemaphore.countDown();
            }
        } else if (Event.EventType.NodeCreated.equals(event.getType())) {
            processNodeCreated(event);
        } else if (Event.EventType.NodeDeleted.equals(event.getType())) {
            processNodeDeleted(event);
        } else if (Event.EventType.NodeDataChanged.equals(event.getType())) {
            processNodeDataChanged(event);
        } else if (Event.EventType.NodeChildrenChanged.equals(event.getType())) {
            processNodeChildrenChanged(event);
        }
    }

    /**
     * 处理事件: NodeCreated
     * 
     * @param event
     */
    protected void processNodeCreated(WatchedEvent event) {}

    /**
     * 处理事件: NodeDeleted
     *
     * @param event
     */
    protected void processNodeDeleted(WatchedEvent event) {}

    /**
     * 处理事件: NodeDataChanged
     *
     * @param event
     */
    protected void processNodeDataChanged(WatchedEvent event) {}

    /**
     * 处理事件: NodeChildrenChanged
     *
     * @param event
     */
    protected void processNodeChildrenChanged(WatchedEvent event) {}
}
