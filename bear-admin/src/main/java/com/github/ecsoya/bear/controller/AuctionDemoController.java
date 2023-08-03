package com.github.ecsoya.bear.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.github.ecsoya.bear.common.core.controller.BaseController;

@Controller
public class AuctionDemoController extends BaseController {

	@GetMapping("/auction/{name}.html")
	public String page(@PathVariable("name") String name) {
		return "auction/" + name;

	}
}
