package org.jmqtt.broker.store.mem;

import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.Subscription;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: jmqtt
 * @description:
 * @author: Mr.Liu
 * @create: 2021-02-12 21:35
 **/
public abstract class AbstractMemStore {


    public void start(BrokerConfig brokerConfig) {
    }
    public void shutdown() {
    }
}
