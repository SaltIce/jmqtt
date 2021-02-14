package org.jmqtt.broker.store.mem;

import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.SessionStore;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @program: jmqtt
 * @description:
 * @author: Mr.Liu
 * @create: 2021-02-12 21:42
 **/
public class MemSessionStore extends AbstractMemStore implements SessionStore {
    private static final Object o = new Object();
    /**
     * 离线消息
     */
    private Map<String,/*clientId*/ BlockingQueue<Message>> offlineTable = new ConcurrentHashMap<>();
    // 离线消息最大数量
    private int msgMaxNum = 1000;
    /**
     * 过程消息
     */
    private Map<String,/*clientId*/ ConcurrentHashMap<Integer,/*msgId*/Message>> recCache = new ConcurrentHashMap<>();
    private Map<String,/*clientId*/ ConcurrentHashMap<Integer,/*msgId*/Message>> sendCache = new ConcurrentHashMap<>();
    private Map<String,/*clientId*/ ConcurrentHashMap<Integer,/*msgId*/Object>> secTwoCache = new ConcurrentHashMap<>();

    /**
     * session
     */
    private Map<String,SessionState> sessionTable = new ConcurrentHashMap<>();
    /**
     * sub
     */
    private Map<String, ConcurrentHashMap<String, Subscription>> subscriptionCache = new ConcurrentHashMap<>();

    @Override
    public void start(BrokerConfig brokerConfig) {
        super.start(brokerConfig);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public SessionState getSession(String clientId) {
        SessionState s = sessionTable.get(clientId);
        if(s==null){
            // 从未连接过
            return new SessionState(SessionState.StateEnum.NULL);
        }
        return s;
    }

    @Override
    public boolean storeSession(String clientId, SessionState sessionState) {
        // notifyClearOtherSession在单机模式是无用的
        sessionTable.put(clientId,sessionState);
        return true;
    }

    @Override
    public void clearSession(String clientId, boolean clearOfflineMsg) {
        sessionTable.remove(clientId);
        if (clearOfflineMsg)
            offlineTable.remove(clientId);
    }

    @Override
    public boolean storeSubscription(String clientId, Subscription subscription) {
        ConcurrentHashMap<String,Subscription> v = subscriptionCache.get(clientId);
        if(v == null){
            v = new ConcurrentHashMap<>();
            v.put(subscription.getTopic(),subscription);
            subscriptionCache.put(clientId,v);
        }else{
            v.put(subscription.getTopic(), subscription);
        }
        return true;
    }

    @Override
    public boolean delSubscription(String clientId, String topic) {
        ConcurrentHashMap<String,Subscription> v = subscriptionCache.get(clientId);
        if(v != null){
            v.remove(topic);
        }
        return true;
    }

    @Override
    public boolean clearSubscription(String clientId) {
        subscriptionCache.remove(clientId);
        return true;
    }

    @Override
    public Set<Subscription> getSubscriptions(String clientId) {
        ConcurrentHashMap<String,Subscription> v = subscriptionCache.get(clientId);
        if(v == null){
            return new HashSet<Subscription>();
        }
        Collection<Subscription> sub  = v.values();
        return new HashSet<>(sub);
    }

    @Override
    public boolean cacheInflowMsg(String clientId, Message message) {
        ConcurrentHashMap<Integer,Message> v =  recCache.get(clientId);
        if(v == null){
            v = new ConcurrentHashMap<>(16);
        }
        v.put(message.getMsgId(),message);
        recCache.put(clientId,v);
        return true;
    }

    @Override
    public Message releaseInflowMsg(String clientId, int msgId) {
        ConcurrentHashMap<Integer,Message> v =  recCache.get(clientId);
        if(v == null){
            return null;
        }
        return v.remove(msgId);
    }

    @Override
    public Collection<Message> getAllInflowMsg(String clientId) {
        ConcurrentHashMap<Integer,Message> v =  recCache.get(clientId);
        if(v == null || v.isEmpty()){
            return new ArrayList<Message>();
        }
        return v.values();
    }

    @Override
    public boolean cacheOutflowMsg(String clientId, Message message) {
        ConcurrentHashMap<Integer,Message> v =  sendCache.get(clientId);
        if(v == null){
            v = new ConcurrentHashMap<>(16);
        }
        v.put(message.getMsgId(),message);
        sendCache.put(clientId,v);
        return true;
    }

    @Override
    public Collection<Message> getAllOutflowMsg(String clientId) {
        ConcurrentHashMap<Integer,Message> v =  sendCache.get(clientId);
        if(v == null || v.isEmpty()){
            return new ArrayList<Message>();
        }
        return v.values();
    }

    @Override
    public Message releaseOutflowMsg(String clientId, int msgId) {
        ConcurrentHashMap<Integer,Message> v =  sendCache.get(clientId);
        if(v == null){
            return null;
        }
        return v.remove(msgId);
    }

    @Override
    public boolean cacheOutflowSecMsgId(String clientId, int msgId) {
        ConcurrentHashMap<Integer,Object> v =  secTwoCache.get(clientId);
        if(v == null){
            v = new ConcurrentHashMap<>(16);
        }
        v.put(msgId,o);
        secTwoCache.put(clientId,v);
        return true;
    }

    @Override
    public boolean releaseOutflowSecMsgId(String clientId, int msgId) {
        ConcurrentHashMap<Integer,Object> v =  secTwoCache.get(clientId);
        if(v == null || v.isEmpty()){
            return false;
        }
        Object os =  v.remove(msgId);
        return os != null;
    }

    @Override
    public List<Integer> getAllOutflowSecMsgId(String clientId) {
        ConcurrentHashMap<Integer,Object> v =  secTwoCache.get(clientId);
        if(v == null){
            return new ArrayList<Integer>();
        }
        ArrayList<Integer> ret = new ArrayList<>(v.size());
        v.forEach((k,v2)->{
            ret.add(k);
        });
        return ret;
    }

    @Override
    public boolean storeOfflineMsg(String clientId, Message message) {
        BlockingQueue<Message> off = offlineTable.get(clientId);
        if(off == null){
            off = new LinkedBlockingQueue<>();
        }
        off.add(message);
        offlineTable.put(clientId,off);
        return true;
    }

    @Override
    public Collection<Message> getAllOfflineMsg(String clientId) {
        BlockingQueue<Message> off = offlineTable.get(clientId);
        if(off == null){
            return new ArrayList<Message>();
        }
        return off;
    }

    @Override
    public boolean clearOfflineMsg(String clientId) {
        offlineTable.remove(clientId);
        return true;
    }
}
