package com.github.ecsoya.bear.framework.manager;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeansException;

import com.github.ecsoya.bear.common.utils.Threads;
import com.github.ecsoya.bear.common.utils.spring.SpringUtils;

/**
 * 异步任务管理器
 * 
 * @author liuhulu
 */
public class AsyncManager {
	/**
	 * 操作延迟10毫秒
	 */
	private final int OPERATE_DELAY_TIME = 10;

	/**
	 * 单例模式
	 */
	private AsyncManager() {
	}

	private static AsyncManager me = new AsyncManager();

	public static AsyncManager me() {
		return me;
	}

	/**
	 * 执行任务
	 * 
	 * @param task 任务
	 */
	public void execute(TimerTask task) {
		ScheduledExecutorService executor = getExecutor();
		if (executor != null) {
			executor.schedule(task, OPERATE_DELAY_TIME, TimeUnit.MILLISECONDS);
		} else {
			task.run();
		}
	}

	public void execute(Runnable task) {
		ScheduledExecutorService executor = getExecutor();
		if (executor != null) {
			executor.schedule(task, OPERATE_DELAY_TIME, TimeUnit.MILLISECONDS);
		} else {
			task.run();
		}
	}

	public void schedule(Runnable task, long delay, TimeUnit timeUnit) {
		ScheduledExecutorService executor = getExecutor();
		if (executor != null) {
			executor.schedule(task, delay, timeUnit);
		} else {
			task.run();
		}
	}

	private ScheduledExecutorService getExecutor() {
		try {
			return SpringUtils.getBean("scheduledExecutorService");
		} catch (BeansException e) {
			return null;
		}
	}

	/**
	 * 停止任务线程池
	 */
	public void shutdown() {
		ScheduledExecutorService executor = getExecutor();
		if (executor != null) {
			Threads.shutdownAndAwaitTermination(executor);
		}
	}
}
