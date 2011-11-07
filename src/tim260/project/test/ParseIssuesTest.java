package tim260.project.test;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Test;

import tim260.project.ParseIssues;

public class ParseIssuesTest {

	@Test
	public void testParseRev() {
		LinkedList<String> revs = ParseIssues.parseRev("Fixed in eb5391c added"+
				" 2354476f1e600d07fb0bbdeb9a70e57a00f8caab");
		assertEquals("eb5391c", revs.get(0));
		assertEquals("2354476f1e600d07fb0bbdeb9a70e57a00f8caab", revs.get(1));
	}

}
