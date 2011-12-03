package tim260.project.test;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.junit.Before;
import org.junit.Test;

import tim260.project.Index;
import tim260.project.Recommend;

public class RecommendTest {


	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void testGetComment() {
		String[] content = new String[3];
		content[0] = "self.protected_instance_variables = %w(@_action_has_layout)";
		content[1] = "#   be turned off to aid in functional testing.";
		content[2] = "include AbstractController::Logger # Note that the proc is evaluated right";
		String comment = Recommend.getComment(content, 3, 3);
		assertEquals(
				"   be turned off to aid in functional testing. Note that the proc is evaluated right",
				comment);
	}

	@Test
	public void testGetLatestCommit() throws Exception {
		Date date1 = format.parse("2011-11-29 03:01:18");
//		Recommend rec = new Recommend(date);
		Date date2 = format.parse("2011-11-29 05:01:18");
		Recommend.repoID = "6";
		int commit1 = Recommend.getLatestCommit(date1);
		int commit2 = Recommend.getLatestCommit(date2);
		assertEquals(commit1, commit2);
	}

	@Test
	public void testAddDeleteFile() throws Exception {
		Recommend rec = new Recommend(format.parse("2004-11-24 00:04:44"));
		rec.addFile(47772, 39182);
		Index index = Index.getInstace();
		IndexReader reader = index.getReader();
		int freq = reader.docFreq(new Term("file_id", "47772"));
		assertEquals(269, freq);
		rec.deleteFile(47772);
		reader = index.getReader();
		assertTrue(reader.isCurrent());
		freq = reader.docFreq(new Term("file_id", "47772"));
		assertEquals(0, freq);
	}
}
