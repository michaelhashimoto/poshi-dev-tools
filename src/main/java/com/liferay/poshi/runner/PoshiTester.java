/*
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.poshi.runner;

import com.liferay.poshi.runner.selenium.SeleniumUtil;
import junit.framework.TestCase;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

/**
 * @author Kenji Heigel
 */
public class PoshiTester extends PoshiRunnerTestCase {

	@Before
	public void setUp() throws Exception {
		setUpPoshiRunner(_TEST_BASE_DIR_NAME);
	}

	@Test
	public void testPoshiTest() throws Exception {
		PoshiRunner poshiRunner = new PoshiRunner("PoshiTester#Testing");

		poshiRunner.setUp();

		poshiRunner.test();
	}

	private static final String _TEST_BASE_DIR_NAME =
		"/Users/kenji/Projects/github/poshi-runner-tester/src/main/resources/poshiFiles";

}
