package com.github.ecsoya.bear.framework.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.io.ResourceUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import com.github.ecsoya.bear.common.constant.Constants;
import com.github.ecsoya.bear.common.utils.StringUtils;
import com.github.ecsoya.bear.common.utils.security.CipherUtils;
import com.github.ecsoya.bear.common.utils.spring.SpringUtils;
import com.github.ecsoya.bear.framework.shiro.realm.UserRealm;
import com.github.ecsoya.bear.framework.shiro.session.OnlineDefaultSessionDAO;
import com.github.ecsoya.bear.framework.shiro.session.OnlineRedisSessionDAO;
import com.github.ecsoya.bear.framework.shiro.session.OnlineSessionDAO;
import com.github.ecsoya.bear.framework.shiro.session.OnlineSessionFactory;
import com.github.ecsoya.bear.framework.shiro.web.CustomShiroFilterFactoryBean;
import com.github.ecsoya.bear.framework.shiro.web.filter.LogoutFilter;
import com.github.ecsoya.bear.framework.shiro.web.filter.captcha.CaptchaValidateFilter;
import com.github.ecsoya.bear.framework.shiro.web.filter.kickout.KickoutSessionFilter;
import com.github.ecsoya.bear.framework.shiro.web.filter.online.OnlineSessionFilter;
import com.github.ecsoya.bear.framework.shiro.web.filter.sync.SyncOnlineSessionFilter;
import com.github.ecsoya.bear.framework.shiro.web.session.OnlineWebSessionManager;
import com.github.ecsoya.bear.framework.shiro.web.session.SpringSessionValidationScheduler;

import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;

/**
 * 权限配置加载
 * 
 * @author angryred
 */
@Configuration
public class ShiroConfig {
	private static final Logger log = LoggerFactory.getLogger(ShiroConfig.class);

	/** The Constant CACHE_KEY. */
	private static final String CACHE_KEY = "shiro:cache:";

	/** The Constant SESSION_KEY. */
	@Value("${shiro.session.key:'shiro:session:'}")
	private String sessionKey = "shiro:session:";
	/** The host. */
	// Redis配置
	@Value("${spring.redis.enabled: false}")
	private boolean redisEnabled;
	// Redis配置
	@Value("${spring.redis.host:localhost}")
	private String host;

	/** The port. */
	@Value("${spring.redis.port:6379}")
	private int port;

	/** The database. */
	@Value("${spring.redis.database:1}")
	private int database;

	/** The timeout. */
	@Value("${spring.redis.timeout:60000}")
	private int timeout;

	/** The password. */
	@Value("${spring.redis.password:}")
	private String password;
	/**
	 * Session超时时间，单位为毫秒（默认30分钟）
	 */
	@Value("${shiro.session.expireTime}")
	private int expireTime;

	/**
	 * 相隔多久检查一次session的有效性，单位毫秒，默认就是10分钟
	 */
	@Value("${shiro.session.validationInterval}")
	private int validationInterval;

	/**
	 * 同一个用户最大会话数
	 */
	@Value("${shiro.session.maxSession}")
	private int maxSession;

	/**
	 * 踢出之前登录的/之后登录的用户，默认踢出之前登录的用户
	 */
	@Value("${shiro.session.kickoutAfter}")
	private boolean kickoutAfter;

	/**
	 * 验证码开关
	 */
	@Value("${shiro.user.captchaEnabled}")
	private boolean captchaEnabled;

	/**
	 * 验证码类型
	 */
	@Value("${shiro.user.captchaType}")
	private String captchaType;

	/**
	 * 设置Cookie的域名
	 */
	@Value("${shiro.cookie.domain}")
	private String domain;

	/**
	 * 设置cookie的有效访问路径
	 */
	@Value("${shiro.cookie.path}")
	private String path;

	/**
	 * 设置HttpOnly属性
	 */
	@Value("${shiro.cookie.httpOnly}")
	private boolean httpOnly;

	/**
	 * 设置Cookie的过期时间，秒为单位
	 */
	@Value("${shiro.cookie.maxAge}")
	private int maxAge;

	/**
	 * 设置cipherKey密钥
	 */
	@Value("${shiro.cookie.cipherKey}")
	private String cipherKey;

	/**
	 * 登录地址
	 */
	@Value("${shiro.user.loginUrl}")
	private String loginUrl;

	/**
	 * 权限认证失败地址
	 */
	@Value("${shiro.user.unauthorizedUrl}")
	private String unauthorizedUrl;

	/**
	 * 是否开启记住我功能
	 */
	@Value("${shiro.rememberMe.enabled: false}")
	private boolean rememberMe;

	@Value("${shiro.anons: []}")
	private String[] anons;
	/**
	 * 是否开启记住我功能
	 */
	@Value("${shiro.user.restful: false}")
	private boolean restful;
	@Autowired
	private ApplicationContext context;

	/**
	 * Inits the.
	 */
	@PostConstruct
	public void init() {
		log.info("Init ShiroConfig");
		try {
			final Class<?> type = getClass().getClassLoader()
					.loadClass("at.pollux.thymeleaf.shiro.dialect.ShiroDialect");
			if (type != null && context instanceof AnnotationConfigServletWebServerApplicationContext) {
				((AnnotationConfigServletWebServerApplicationContext) context).registerBean("shiroDialect", type);
			}
			log.debug("Thymeleaf shiro extension started");
		} catch (final Exception e) {
			log.debug("Not support thymeleaf");
		}
	}

	/**
	 * 缓存管理器 使用Ehcache实现
	 */
	@Bean("shiroCacheManager")
	public CacheManager getCacheManager() {
		if (redisEnabled) {
			final RedisCacheManager redisCacheManager = new RedisCacheManager();
			redisCacheManager.setRedisManager(redisManager());
			redisCacheManager.setKeyPrefix(CACHE_KEY);
			// 配置缓存的话要求放在session里面的实体类必须有个id标识
			redisCacheManager.setPrincipalIdFieldName("userId");
			return redisCacheManager;
		}
		net.sf.ehcache.CacheManager cacheManager = net.sf.ehcache.CacheManager.getCacheManager("bear");
		EhCacheManager em = new EhCacheManager();
		if (StringUtils.isNull(cacheManager)) {
			em.setCacheManager(new net.sf.ehcache.CacheManager(getCacheManagerConfigFileInputStream()));
		} else {
			em.setCacheManager(cacheManager);
		}
		return em;
	}

	/**
	 * Redis manager.
	 *
	 * @return the redis manager
	 */
	@Bean
	@ConditionalOnProperty(prefix = "spring.redis", name = "enabled", havingValue = "true")
	public RedisManager redisManager() {
		log.info("redis={}:{}, p={}", host, port, StringUtils.isNotEmpty(password));
		final RedisManager redisManager = new RedisManager();
		redisManager.setHost(host + ":" + port);
		redisManager.setDatabase(database);
		redisManager.setTimeout(timeout);
		if (StringUtils.isNotEmpty(password)) {
			redisManager.setPassword(password);
		}
		return redisManager;
	}

	/**
	 * 返回配置文件流 避免ehcache配置文件一直被占用，无法完全销毁项目重新部署
	 */
	protected InputStream getCacheManagerConfigFileInputStream() {
		String configFile = "classpath:ehcache/ehcache-shiro.xml";
		InputStream inputStream = null;
		try {
			inputStream = ResourceUtils.getInputStreamForPath(configFile);
			byte[] b = IOUtils.toByteArray(inputStream);
			InputStream in = new ByteArrayInputStream(b);
			return in;
		} catch (IOException e) {
			throw new ConfigurationException(
					"Unable to obtain input stream for cacheManagerConfigFile [" + configFile + "]", e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	/**
	 * 自定义Realm
	 */
	@Bean
	public UserRealm userRealm(CacheManager cacheManager) {
		UserRealm userRealm = new UserRealm();
		userRealm.setAuthorizationCacheName(Constants.SYS_AUTH_CACHE);
		userRealm.setCacheManager(cacheManager);
		return userRealm;
	}

	/**
	 * 自定义sessionDAO会话
	 */
	@Bean("onlineSessionDAO")
	public OnlineSessionDAO sessionDAO(CacheManager cacheManager) {
		if (redisEnabled) {
			final OnlineRedisSessionDAO redisSessionDAO = new OnlineRedisSessionDAO();
			redisSessionDAO.setRedisManager(redisManager());
			redisSessionDAO.setKeyPrefix(sessionKey);
			if (expireTime == -1) {
				redisSessionDAO.setExpire(-1);
			} else {
				redisSessionDAO.setExpire(expireTime * 60);
			}
			return redisSessionDAO;
		}
		OnlineDefaultSessionDAO sessionDAO = new OnlineDefaultSessionDAO();
		sessionDAO.setCacheManager(cacheManager);
		return sessionDAO;
	}

	/**
	 * 自定义sessionFactory会话
	 */
	@Bean
	public OnlineSessionFactory sessionFactory() {
		OnlineSessionFactory sessionFactory = new OnlineSessionFactory();
		return sessionFactory;
	}

	/**
	 * 会话管理器
	 */
	@Bean
	public OnlineWebSessionManager sessionManager(CacheManager cacheManager, SessionDAO sessionDAO) {
		OnlineWebSessionManager manager = new OnlineWebSessionManager();
		// 加入缓存管理器
		manager.setCacheManager(cacheManager);
		// 删除过期的session
		manager.setDeleteInvalidSessions(true);
		// 设置全局session超时时间
		if (expireTime < 0) {
			manager.setGlobalSessionTimeout(expireTime);
		} else {
			manager.setGlobalSessionTimeout(expireTime * 60 * 1000);
		}
		// 去掉 JSESSIONID
		manager.setSessionIdUrlRewritingEnabled(false);
		// 定义要使用的无效的Session定时调度器
		manager.setSessionValidationScheduler(SpringUtils.getBean(SpringSessionValidationScheduler.class));
		// 是否定时检查session
		manager.setSessionValidationSchedulerEnabled(true);
		// 自定义SessionDao
		manager.setSessionDAO(sessionDAO);
		// 自定义sessionFactory
		manager.setSessionFactory(sessionFactory());
		return manager;
	}

	/**
	 * 安全管理器
	 */
	@Bean
	public SecurityManager securityManager(UserRealm userRealm, CacheManager cacheManager,
			SessionManager sessionManager) {
		DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
		// 设置realm.
		securityManager.setRealm(userRealm);
		// 记住我
		securityManager.setRememberMeManager(rememberMe ? rememberMeManager() : null);
		// 注入缓存管理器;
		securityManager.setCacheManager(cacheManager);
		// session管理器
		securityManager.setSessionManager(sessionManager);
		return securityManager;
	}

	/**
	 * 退出过滤器
	 */
	public LogoutFilter logoutFilter() {
		LogoutFilter logoutFilter = new LogoutFilter();
		logoutFilter.setLoginUrl(loginUrl);
		logoutFilter.setEnabled(!restful);
		return logoutFilter;
	}

	/**
	 * Shiro过滤器配置
	 */
	@Bean
	public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager, OnlineSessionDAO sessionDAO,
			SessionManager sessionManager) {
		CustomShiroFilterFactoryBean shiroFilterFactoryBean = new CustomShiroFilterFactoryBean();
		// Shiro的核心安全接口,这个属性是必须的
		shiroFilterFactoryBean.setSecurityManager(securityManager);
		// 身份认证失败，则跳转到登录页面的配置
		shiroFilterFactoryBean.setLoginUrl(loginUrl);
		// 权限认证失败，则跳转到指定页面
		shiroFilterFactoryBean.setUnauthorizedUrl(unauthorizedUrl);
		// Shiro连接约束配置，即过滤链的定义
		LinkedHashMap<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
		// 对静态资源设置匿名访问
		filterChainDefinitionMap.put("/favicon.ico**", "anon");
		filterChainDefinitionMap.put("/bear.png**", "anon");
		filterChainDefinitionMap.put("/html/**", "anon");
		filterChainDefinitionMap.put("/css/**", "anon");
		filterChainDefinitionMap.put("/docs/**", "anon");
		filterChainDefinitionMap.put("/fonts/**", "anon");
		filterChainDefinitionMap.put("/img/**", "anon");
		filterChainDefinitionMap.put("/ajax/**", "anon");
		filterChainDefinitionMap.put("/js/**", "anon");
		filterChainDefinitionMap.put("/main/**", "anon");
		filterChainDefinitionMap.put("/captcha/captchaImage**", "anon");
		// 退出 logout地址，shiro去清除session
		filterChainDefinitionMap.put("/logout", "logout");
		// 不需要拦截的访问
		filterChainDefinitionMap.put("/login", "anon,captchaValidate");
		// 注册相关
		filterChainDefinitionMap.put("/register", "anon,captchaValidate");
		// 系统权限列表
		// filterChainDefinitionMap.putAll(SpringUtils.getBean(IMenuService.class).selectPermsAll());
		// 加载自定义匿名路径
		if (!ObjectUtils.isEmpty(anons)) {
			Arrays.asList(anons).forEach(path -> {
				log.info("{}->anon", path);
				if (!filterChainDefinitionMap.containsKey(path)) {
					filterChainDefinitionMap.put(path, "anon");
				}
			});
		}

		Map<String, Filter> filters = new LinkedHashMap<String, Filter>();
		filters.put("onlineSession", onlineSessionFilter(sessionDAO));
		filters.put("syncOnlineSession", syncOnlineSessionFilter(sessionDAO));
		filters.put("captchaValidate", captchaValidateFilter());
		filters.put("kickout", kickoutSessionFilter(sessionManager));
		// 注销成功，则跳转到指定页面
		filters.put("logout", logoutFilter());
		shiroFilterFactoryBean.setFilters(filters);

		// 所有请求需要认证
		filterChainDefinitionMap.put("/**", "user,kickout,onlineSession,syncOnlineSession");
		shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

		return shiroFilterFactoryBean;
	}

	/**
	 * 自定义在线用户处理过滤器
	 */
	public OnlineSessionFilter onlineSessionFilter(OnlineSessionDAO sessionDAO) {
		OnlineSessionFilter onlineSessionFilter = new OnlineSessionFilter();
		onlineSessionFilter.setLoginUrl(loginUrl);
		onlineSessionFilter.setOnlineSessionDAO(sessionDAO);
		onlineSessionFilter.setEnabled(!restful);
		return onlineSessionFilter;
	}

	/**
	 * 自定义在线用户同步过滤器
	 */
	public SyncOnlineSessionFilter syncOnlineSessionFilter(OnlineSessionDAO sessionDAO) {
		SyncOnlineSessionFilter syncOnlineSessionFilter = new SyncOnlineSessionFilter();
		syncOnlineSessionFilter.setOnlineSessionDAO(sessionDAO);
		return syncOnlineSessionFilter;
	}

	/**
	 * 自定义验证码过滤器
	 */
	public CaptchaValidateFilter captchaValidateFilter() {
		CaptchaValidateFilter captchaValidateFilter = new CaptchaValidateFilter();
		captchaValidateFilter.setCaptchaEnabled(captchaEnabled);
		captchaValidateFilter.setCaptchaType(captchaType);
		return captchaValidateFilter;
	}

	/**
	 * cookie 属性设置
	 */
	public SimpleCookie rememberMeCookie() {
		SimpleCookie cookie = new SimpleCookie("rememberMe");
		cookie.setDomain(domain);
		cookie.setPath(path);
		cookie.setHttpOnly(httpOnly);
		cookie.setMaxAge(maxAge * 24 * 60 * 60);
		return cookie;
	}

	/**
	 * 记住我
	 */
	public CookieRememberMeManager rememberMeManager() {
		CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
		cookieRememberMeManager.setCookie(rememberMeCookie());
		if (StringUtils.isNotEmpty(cipherKey)) {
			cookieRememberMeManager.setCipherKey(Base64.decode(cipherKey));
		} else {
			cookieRememberMeManager.setCipherKey(CipherUtils.generateNewKey(128, "AES").getEncoded());
		}
		return cookieRememberMeManager;
	}

	/**
	 * 同一个用户多设备登录限制
	 */
	public KickoutSessionFilter kickoutSessionFilter(SessionManager sessionManager) {
		KickoutSessionFilter kickoutSessionFilter = new KickoutSessionFilter();
		kickoutSessionFilter.setCacheManager(getCacheManager());
		kickoutSessionFilter.setSessionManager(sessionManager);
		// 同一个用户最大的会话数，默认-1无限制；比如2的意思是同一个用户允许最多同时两个人登录
		kickoutSessionFilter.setMaxSession(maxSession);
		// 是否踢出后来登录的，默认是false；即后者登录的用户踢出前者登录的用户；踢出顺序
		kickoutSessionFilter.setKickoutAfter(kickoutAfter);
		// 被踢出后重定向到的地址；
		kickoutSessionFilter.setKickoutUrl("/login?kickout=1");
		return kickoutSessionFilter;
	}

	/**
	 * thymeleaf模板引擎和shiro框架的整合
	 */
	@Bean
	public ShiroDialect shiroDialect() {
		return new ShiroDialect();
	}

	/**
	 * 开启Shiro注解通知器
	 */
	@Bean
	public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(
			@Qualifier("securityManager") SecurityManager securityManager) {
		AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
		authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
		return authorizationAttributeSourceAdvisor;
	}
}
