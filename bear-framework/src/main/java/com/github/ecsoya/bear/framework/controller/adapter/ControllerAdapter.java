package com.github.ecsoya.bear.framework.controller.adapter;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.ModelMap;

public interface ControllerAdapter {

	String getPage(HttpServletRequest request, String name, ModelMap mmap);

	boolean hasPage(String name);
}
