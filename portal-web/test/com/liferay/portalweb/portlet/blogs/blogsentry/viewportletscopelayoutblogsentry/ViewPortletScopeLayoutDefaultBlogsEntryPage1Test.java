/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portalweb.portlet.blogs.blogsentry.viewportletscopelayoutblogsentry;

import com.liferay.portalweb.portal.BaseTestCase;
import com.liferay.portalweb.portal.util.RuntimeVariables;

/**
 * @author Brian Wing Shun Chan
 */
public class ViewPortletScopeLayoutDefaultBlogsEntryPage1Test
	extends BaseTestCase {
	public void testViewPortletScopeLayoutDefaultBlogsEntryPage1()
		throws Exception {
		selenium.open("/web/guest/home/");
		selenium.clickAt("link=Blogs Test Page1",
			RuntimeVariables.replace("Blogs Test Page1"));
		selenium.waitForPageToLoad("30000");
		assertEquals(RuntimeVariables.replace("Blogs"),
			selenium.getText("//span[@class='portlet-title-text']"));
		assertFalse(selenium.isTextPresent("Blogs Entry Title"));
		assertFalse(selenium.isTextPresent("Blogs Entry Content"));
		selenium.clickAt("//strong/a", RuntimeVariables.replace("Options Icon"));

		for (int second = 0;; second++) {
			if (second >= 90) {
				fail("timeout");
			}

			try {
				if (selenium.isVisible(
							"//div[@class='lfr-component lfr-menu-list']/ul/li[2]/a")) {
					break;
				}
			}
			catch (Exception e) {
			}

			Thread.sleep(1000);
		}

		assertEquals(RuntimeVariables.replace("Configuration"),
			selenium.getText(
				"//div[@class='lfr-component lfr-menu-list']/ul/li[2]/a"));
		selenium.click("//div[@class='lfr-component lfr-menu-list']/ul/li[2]/a");

		for (int second = 0;; second++) {
			if (second >= 90) {
				fail("timeout");
			}

			try {
				if (selenium.isVisible(
							"//iframe[@id='_33_configurationIframeDialog']")) {
					break;
				}
			}
			catch (Exception e) {
			}

			Thread.sleep(1000);
		}

		selenium.selectFrame("//iframe[@id='_33_configurationIframeDialog']");

		for (int second = 0;; second++) {
			if (second >= 90) {
				fail("timeout");
			}

			try {
				if (selenium.isVisible("link=Scope")) {
					break;
				}
			}
			catch (Exception e) {
			}

			Thread.sleep(1000);
		}

		selenium.clickAt("link=Scope", RuntimeVariables.replace("Scope"));
		selenium.waitForPageToLoad("30000");

		for (int second = 0;; second++) {
			if (second >= 90) {
				fail("timeout");
			}

			try {
				if (selenium.isVisible("//select[@id='_86_scopeType']")) {
					break;
				}
			}
			catch (Exception e) {
			}

			Thread.sleep(1000);
		}

		assertEquals("Default",
			selenium.getSelectedLabel("//select[@id='_86_scopeType']"));
		selenium.selectFrame("relative=top");
	}
}