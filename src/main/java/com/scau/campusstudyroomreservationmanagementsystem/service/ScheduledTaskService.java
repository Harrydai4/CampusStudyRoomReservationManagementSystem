package com.scau.campusstudyroomreservationmanagementsystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 定时任务入口：每分钟执行设计文档 10.5 节定义的维护逻辑。
 */
@Service
public class ScheduledTaskService {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);
    private final AppService appService;

    public ScheduledTaskService(AppService appService) {
        this.appService = appService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void runMaintenanceTasks() {
        try {
            appService.scheduledProcessInvalidCheckin();
            appService.scheduledProcessNoShow();
            appService.scheduledProcessAutoCheckout();
            appService.scheduledProcessBlacklistRelease();
        } catch (Exception ex) {
            log.warn("定时任务执行异常: {}", ex.getMessage());
        }
    }
}
