package com.github.ecsoya.bear.quartz.util;

import org.quartz.JobExecutionContext;

import com.github.ecsoya.bear.quartz.domain.SysJob;

/**
 * 定时任务处理（允许并发执行）
 * 
 * @author angryred
 *
 */
public class QuartzJobExecution extends AbstractQuartzJob {
	@Override
	protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception {
		JobInvokeUtil.invokeMethod(sysJob);
	}
}
