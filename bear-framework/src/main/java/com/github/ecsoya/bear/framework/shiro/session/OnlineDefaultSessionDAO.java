package com.github.ecsoya.bear.framework.shiro.session;

import java.io.Serializable;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.ecsoya.bear.common.enums.OnlineStatus;
import com.github.ecsoya.bear.framework.shiro.service.SysShiroService;

/**
 * 针对自定义的ShiroSession的db操作
 * 
 * @author angryred
 */
public class OnlineDefaultSessionDAO extends EnterpriseCacheSessionDAO implements OnlineSessionDAO {

	@Autowired
	private SysShiroService sysShiroService;

	public OnlineDefaultSessionDAO() {
		super();
	}

	public OnlineDefaultSessionDAO(long expireTime) {
		super();
	}

	/**
	 * 根据会话ID获取会话
	 *
	 * @param sessionId 会话ID
	 * @return ShiroSession
	 */
	@Override
	protected Session doReadSession(Serializable sessionId) {
		return sysShiroService.getSession(sessionId);
	}

	@Override
	public void update(Session session) throws UnknownSessionException {
		super.update(session);
	}

	/**
	 * 当会话过期/停止（如用户退出时）属性等会调用
	 */
	@Override
	protected void doDelete(Session session) {
		OnlineSession onlineSession = (OnlineSession) session;
		if (null == onlineSession) {
			return;
		}
		onlineSession.setStatus(OnlineStatus.off_line);
		sysShiroService.deleteSession(onlineSession);
	}
}
