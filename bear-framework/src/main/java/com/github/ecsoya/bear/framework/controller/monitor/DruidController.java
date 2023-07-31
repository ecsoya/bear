package com.github.ecsoya.bear.framework.controller.monitor;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.github.ecsoya.bear.common.core.controller.BaseController;

/**
 * druid 监控
 * 
 * @author angryred
 */
@Controller
@RequestMapping("/monitor/data")
public class DruidController extends BaseController {
	private String prefix = "/druid";

	@RequiresPermissions("monitor:data:view")
	@GetMapping()
	public String index() {
		return redirect(prefix + "/index.html");
	}
}
