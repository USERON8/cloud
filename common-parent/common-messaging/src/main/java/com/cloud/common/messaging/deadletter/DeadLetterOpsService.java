package com.cloud.common.messaging.deadletter;

import java.util.List;

public interface DeadLetterOpsService {

  long countPending();

  long countPendingByTopic(String topic);

  List<DeadLetterRecord> listPending(int limit);

  boolean markHandled(String topic, String msgId);
}
