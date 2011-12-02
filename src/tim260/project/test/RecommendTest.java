package tim260.project.test;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import tim260.project.Recommend;

public class RecommendTest {


	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void testGetHunks() {
//		fail("Not yet implemented");
	}

	@Test
	public void testIndexDated() throws Exception {
		SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Date date = formate.parse("2011-11-29 03:01:18");
		Recommend rec = new Recommend(date);
		date = formate.parse("2011-11-29 05:01:18");
		assertFalse(rec.indexDated(date));
	}

}
