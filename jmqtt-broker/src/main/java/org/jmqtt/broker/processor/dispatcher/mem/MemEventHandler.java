package org.jmqtt.broker.processor.dispatcher.mem;

import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.EventConsumeHandler;
import org.jmqtt.broker.processor.dispatcher.event.Event;
import org.jmqtt.broker.store.mem.AbstractMemStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @program: jmqtt
 * @description:
 * @author: Mr.Liu
 * @create: 2021-02-14 21:59
 **/
public class MemEventHandler extends AbstractMemStore implements ClusterEventHandler {
    private int oneMax = 10; // 一次去的event最大数量
    private ConcurrentLinkedDeque<Event> events = new ConcurrentLinkedDeque<Event>();
    @Override
    public void start(BrokerConfig brokerConfig) {
        super.start(brokerConfig);
        oneMax = brokerConfig.getMaxPollEventNum();
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public boolean sendEvent(Event event) {
        return events.offerLast(event);
    }

    @Override
    public void setEventConsumeHandler(EventConsumeHandler eventConsumeHandler) {

    }

    @Override
    public List<Event> pollEvent(int maxPollNum) {
        List<Event> e = new ArrayList<>(oneMax/2);
        for (int i = 0; i < oneMax; i++) {
            Event e1 = events.pollFirst();
            if(e1 == null){
                break;
            }
            e.add(e1);
        }
        return e;
    }
}
