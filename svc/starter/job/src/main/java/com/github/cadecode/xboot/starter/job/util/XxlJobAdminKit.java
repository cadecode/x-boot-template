package com.github.cadecode.xboot.starter.job.util;

import com.github.cadecode.xboot.common.exception.GeneralException;
import com.xxl.job.core.openapi.admin.AdminJobBiz;
import com.xxl.job.core.openapi.admin.dto.JobAddRequest;
import com.xxl.job.core.openapi.admin.dto.JobOperateRequest;
import com.xxl.job.core.openapi.admin.dto.JobTriggerRequest;
import com.xxl.job.core.openapi.admin.dto.JobUpdateRequest;
import com.xxl.tool.response.Response;
import lombok.RequiredArgsConstructor;

/**
 * 调度中心任务管理，注入使用，需要调度中心 3.5.0 及以上
 *
 * @author Cade Li
 * @since 2026/11/7
 */
@RequiredArgsConstructor
public class XxlJobAdminKit {

    private final AdminJobBiz adminJobBiz;

    public void addJob(JobAddRequest request) {
        check(adminJobBiz.addJob(request), "addJob");
    }

    public void updateJob(JobUpdateRequest request) {
        check(adminJobBiz.updateJob(request), "updateJob");
    }

    public void removeJob(int jobId) {
        check(adminJobBiz.removeJob(operate(jobId)), "removeJob");
    }

    public void startJob(int jobId) {
        check(adminJobBiz.startJob(operate(jobId)), "startJob");
    }

    public void stopJob(int jobId) {
        check(adminJobBiz.stopJob(operate(jobId)), "stopJob");
    }

    public void triggerJob(int jobId, String param) {
        JobTriggerRequest request = new JobTriggerRequest();
        request.setId(jobId);
        request.setExecutorParam(param);
        check(adminJobBiz.triggerJob(request), "triggerJob");
    }

    private static JobOperateRequest operate(int jobId) {
        JobOperateRequest request = new JobOperateRequest();
        request.setId(jobId);
        return request;
    }

    private static void check(Response<String> response, String action) {
        if (response == null) {
            throw GeneralException.of("job {} fail, response is null", action);
        }
        if (!response.isSuccess()) {
            throw GeneralException.of("job {} fail, code: {}, msg: {}", action, response.getCode(), response.getMsg());
        }
    }

}
