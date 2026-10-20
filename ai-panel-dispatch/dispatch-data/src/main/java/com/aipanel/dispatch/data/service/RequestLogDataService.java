package com.aipanel.dispatch.data.service;

import com.aipanel.dispatch.data.mapper.RequestLogMapper;
import com.aipanel.dispatch.data.po.RequestLogPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Request Log Data Service
 * Handles async batch insertion of request logs
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RequestLogDataService {

    private final RequestLogMapper requestLogMapper;
    private final ConcurrentLinkedQueue<RequestLogPO> logQueue = new ConcurrentLinkedQueue<>();

    /**
     * Queue a request log for batch insertion
     */
    public void queueLog(RequestLogPO log) {
        logQueue.offer(log);

        // Flush if queue gets too large
        if (logQueue.size() >= 100) {
            flushLogs();
        }
    }

    /**
     * Flush queued logs to database
     */
    @Async
    public void flushLogs() {
        List<RequestLogPO> batch = new ArrayList<>();
        RequestLogPO logEntry;

        while ((logEntry = logQueue.poll()) != null && batch.size() < 500) {
            batch.add(logEntry);
        }

        if (!batch.isEmpty()) {
            try {
                requestLogMapper.batchInsert(batch);
                log.info("Flushed {} request logs to database", batch.size());
            } catch (Exception e) {
                log.error("Failed to flush request logs: {}", e.getMessage());
                // Re-queue failed logs
                logQueue.addAll(batch);
            }
        }
    }

    /**
     * Direct insert for single log
     */
    public void saveLog(RequestLogPO log) {
        requestLogMapper.insert(log);
    }

    /**
     * Get user daily usage statistics
     */
    public List<Map<String, Object>> getUserDailyStats(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return requestLogMapper.getUserDailyStats(userId, startTime, endTime);
    }

    /**
     * Get model usage statistics
     */
    public List<Map<String, Object>> getModelStats(LocalDateTime startTime, LocalDateTime endTime) {
        return requestLogMapper.getModelStats(startTime, endTime);
    }

    /**
     * Get provider error rates
     */
    public List<Map<String, Object>> getProviderErrorRates(LocalDateTime since) {
        return requestLogMapper.getProviderErrorRates(since);
    }

    public List<Map<String, Object>> getModelHealthStats(LocalDateTime since) {
        return requestLogMapper.getModelHealthStats(since);
    }

    public List<Map<String, Object>> getRecentStatuses(String modelId, int limit) {
        return requestLogMapper.getRecentStatuses(modelId, limit);
    }

    /**
     * Create request log from request details
     */
    public RequestLogPO createLog(String requestId, Long userId, Long apiKeyId,
                                   String requestedModel, String actualModel,
                                   String providerCode, String requestType) {
        RequestLogPO log = new RequestLogPO();
        log.setRequestId(requestId);
        log.setUserId(userId);
        log.setApiKeyId(apiKeyId);
        log.setRequestedModel(requestedModel);
        log.setActualModel(actualModel);
        log.setProviderCode(providerCode);
        log.setRequestType(requestType);
        log.setCreateTime(LocalDateTime.now());
        return log;
    }
}
