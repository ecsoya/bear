package com.github.ecsoya.bear.framework.shiro.session;

import java.util.Date;

import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.springframework.beans.factory.annotation.Value;

import com.github.ecsoya.bear.framework.manager.AsyncManager;
import com.github.ecsoya.bear.framework.manager.factory.AsyncFactory;

public interface OnlineSessionDAO extends SessionDAO {
	/**
	 * 同步session到数据库的周期 单位为毫秒（默认1分钟）
	 */
	@Value("${shiro.session.dbSyncPeriod}")
	public int dbSyncPeriod = 1;

	/**
	 * 上次同步数据库的时间戳
	 */
	public static final String LAST_SYNC_DB_TIMESTAMP = OnlineDefaultSessionDAO.class.getName()
			+ "LAST_SYNC_DB_TIMESTAMP";

	/**
	 * 更新会话；如更新会话最后访问时间/停止会话/设置超时时间/设置移除属性等会调用
	 */
	public default void syncToDb(OnlineSession onlineSession) {
		Date lastSyncTimestamp = (Date) onlineSession.getAttribute(LAST_SYNC_DB_TIMESTAMP);
		if (lastSyncTimestamp != null) {
			boolean needSync = true;
			long deltaTime = onlineSession.getLastAccessTime().getTime() - lastSyncTimestamp.getTime();
			if (deltaTime < dbSyncPeriod * 60 * 1000) {
				// 时间差不足 无需同步
				needSync = false;
			}
			// isGuest = true 访客
			boolean isGuest = onlineSession.getUserId() == null || onlineSession.getUserId() == 0L;

			// session 数据变更了 同步
			if (!isGuest && onlineSession.isAttributeChanged()) {
				needSync = true;
			}

			if (!needSync) {
				return;
			}
		}
		// 更新上次同步数据库时间
		onlineSession.setAttribute(LAST_SYNC_DB_TIMESTAMP, onlineSession.getLastAccessTime());
		// 更新完后 重置标识
		if (onlineSession.isAttributeChanged()) {
			onlineSession.resetAttributeChanged();
		}
		AsyncManager.me().execute(AsyncFactory.syncSessionToDb(onlineSession));
	}

}
