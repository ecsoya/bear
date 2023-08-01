package com.github.ecsoya.bear.framework.controller.system;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.ecsoya.bear.common.core.controller.BaseController;
import com.github.ecsoya.bear.common.core.domain.AjaxResult;
import com.github.ecsoya.bear.common.core.text.Convert;
import com.github.ecsoya.bear.common.utils.ServletUtils;
import com.github.ecsoya.bear.common.utils.StringUtils;
import com.github.ecsoya.bear.framework.controller.adapter.ControllerAdapter;
import com.github.ecsoya.bear.framework.web.service.ConfigService;

/**
 * 登录验证
 * 
 * @author angryred
 */
@Controller
public class SysLoginController extends BaseController {
	/**
	 * 是否开启记住我功能
	 */
	@Value("${shiro.rememberMe.enabled: false}")
	private boolean rememberMe;

	@Autowired
	private ConfigService configService;

	@Autowired(required = false)
	private ControllerAdapter controllerAdapter;

	@GetMapping("/login")
	public String login(HttpServletRequest request, HttpServletResponse response, ModelMap mmap) {
		if (controllerAdapter != null && controllerAdapter.hasPage("main")) {
			return controllerAdapter.getPage(getRequest(), "main", mmap);
		}
		// 如果是Ajax请求，返回Json字符串。
		if (ServletUtils.isAjaxRequest(request)) {
			return ServletUtils.renderString(response, "{\"code\":\"1\",\"msg\":\"未登录或登录超时。请重新登录\"}");
		}
		// 是否开启记住我
		mmap.put("isRemembered", rememberMe);
		// 是否开启用户注册
		mmap.put("isAllowRegister", Convert.toBool(configService.getKey("sys.account.registerUser"), false));
		return "login";
	}

	@PostMapping("/login")
	@ResponseBody
	public AjaxResult ajaxLogin(String username, String password, Boolean rememberMe) {
		UsernamePasswordToken token = new UsernamePasswordToken(username, password, rememberMe);
		Subject subject = SecurityUtils.getSubject();
		try {
			subject.login(token);
			return success();
		} catch (AuthenticationException e) {
			String msg = "用户或密码错误";
			if (StringUtils.isNotEmpty(e.getMessage())) {
				msg = e.getMessage();
			}
			return error(msg);
		}
	}

	@GetMapping("/unauth")
	public String unauth() {
		return "error/unauth";
	}
}
