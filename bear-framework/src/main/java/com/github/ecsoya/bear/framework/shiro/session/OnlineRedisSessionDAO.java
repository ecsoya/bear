package com.github.ecsoya.bear.framework.shiro.session;

import java.io.Serializable;
import java.util.Date;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.crazycake.shiro.RedisSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.github.ecsoya.bear.common.core.text.Convert;
import com.github.ecsoya.bear.common.enums.OnlineStatus;
import com.github.ecsoya.bear.framework.manager.AsyncManager;
import com.github.ecsoya.bear.framework.manager.factory.AsyncFactory;
import com.github.ecsoya.bear.framework.shiro.service.SysShiroService;

/**
 * 针对自定义的ShiroSession的db操作
 * 
 * @author angryred
 */
public class OnlineRedisSessionDAO extends RedisSessionDAO implements OnlineSessionDAO {
	private static final Logger log = LoggerFactory.getLogger(OnlineDefaultSessionDAO.class);

	@Value("${shiro.session.dbSyncPeriod}")
	private int dbSyncPeriod;

	/**
	 * 上次同步数据库的时间戳
	 */
	private static final String LAST_SYNC_DB_TIMESTAMP = "LAST_SYNC_DB_TIMESTAMP_%s";

	@Autowired
	private SysShiroService sysShiroService;

	public OnlineRedisSessionDAO() {
		super();
	}

	public OnlineRedisSessionDAO(long expireTime) {
		super();
	}

	@Override
	public void update(Session session) throws UnknownSessionException {
		super.update(session);
		syncToDb(session.getId());
	}

	@Override
	protected Serializable doCreate(Session session) {
		Serializable sessionId = super.doCreate(session);
		syncToDb(sessionId);
		return sessionId;
	}

	private void syncToDb(Serializable sessionKey) {
		if (sessionKey == null) {
			return;
		}
		AsyncManager.me().execute(() -> {
			try {
				Session session = doReadSession(sessionKey);
				if (session instanceof OnlineSession) {
					syncToDb((OnlineSession) session);
				}
			} catch (Exception e) {
			}
		});
	}

	private Long getLastSyncTime(Serializable sessionId) {
		if (sessionId == null) {
			return null;
		}
		String key = String.format(LAST_SYNC_DB_TIMESTAMP, sessionId);
		try {
			byte[] bytes = getRedisManager().get(key.getBytes());
			if (bytes != null) {
				return Convert.toLong(new String(bytes));
			}
		} catch (Exception e) {
		}
		return null;
	}

	private void rememberLastSyncTime(Serializable sessionId) {
		if (sessionId == null) {
			return;
		}
		String key = String.format(LAST_SYNC_DB_TIMESTAMP, sessionId);
		Long value = new Date().getTime();
		getRedisManager().set(key.getBytes(), value.toString().getBytes(), dbSyncPeriod * 60);
	}

	/**
	 * 更新会话；如更新会话最后访问时间/停止会话/设置超时时间/设置移除属性等会调用
	 */
	public void syncToDb(OnlineSession onlineSession) {
		if (onlineSession == null) {
			return;
		}
		boolean isGuest = onlineSession.getUserId() == null || onlineSession.getUserId() == 0L;
		if (isGuest) {
			return;
		}
		Serializable sessionId = onlineSession.getId();
		Long now = new Date().getTime();
		Long lastSyncTimestamp = getLastSyncTime(sessionId);
		if (lastSyncTimestamp != null) {
			boolean needSync = (now - lastSyncTimestamp) > dbSyncPeriod * 60 * 1000;
			if (!needSync) {
				return;
			}
		}
		log.info("OnlineSession.sync={}", sessionId);
		// 更新上次同步数据库时间
		rememberLastSyncTime(sessionId);
		// 更新完后 重置标识
		if (onlineSession.isAttributeChanged()) {
			onlineSession.resetAttributeChanged();
		}
		AsyncManager.me().execute(AsyncFactory.syncSessionToDb(onlineSession));
	}

	@Override
	public void delete(Session session) {
		super.delete(session);
		if (session instanceof OnlineSession) {
			OnlineSession onlineSession = (OnlineSession) session;
			onlineSession.setStatus(OnlineStatus.off_line);
			sysShiroService.deleteSession(onlineSession);
		}
	}

	@Override
	protected Session doReadSession(Serializable sessionId) {
		Session session = super.doReadSession(sessionId);
		if (session == null) {
			session = sysShiroService.getSession(sessionId);
		}
		return session;
	}
}
