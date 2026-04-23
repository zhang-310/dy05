package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDigitalHumanTask;

public interface DigitalHumanPollingService {
    void pollPendingTasks();
    void onTaskCompleted(SvDigitalHumanTask task);
}
