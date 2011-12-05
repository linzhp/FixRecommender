package tim260.project.test;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.junit.Before;
import org.junit.Test;

import tim260.project.Index;
import tim260.project.Recommend;

public class RecommendTest {


	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	@Before
	public void setUp() throws Exception {

	}

//	@Test
	public void testGetComment() {
		String[] content = new String[3];
		content[0] = "self.protected_instance_variables = %w(@_action_has_layout)";
		content[1] = "#   be turned off to aid in functional testing.";
		content[2] = "include AbstractController::Logger # Note that the proc is evaluated right";
		String[] cAndT = Recommend.separateCodeAndComment(content, 3, 3);
		assertEquals(
				"be turned off to aid in functional testing.Note that the proc is evaluated right",
				cAndT[1]);
		assertEquals("include AbstractController::Logger", cAndT[0]);
	}

//	@Test
	public void testGetLatestCommit() throws Exception {
		Date date1 = format.parse("2011-11-29 03:01:18");
		Date date2 = format.parse("2011-11-29 05:01:18");
		Recommend.repoID = "6";
		int commit1 = Recommend.getLatestCommit(date1);
		int commit2 = Recommend.getLatestCommit(date2);
		assertEquals(commit1, commit2);
	}

//	@Test
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
	
	@Test
	public void testSeparateCodeAndText() {
		String[] cAndT = Recommend.separateCodeAndText("Opening a new issue which is still in rails 3.0.x releases.\r\noriginal lighthouse ticket: http://rails.lighthouseapp.com/projects/8994/tickets/6680\r\noriginal github issue: #1005\r\n\r\nHi everyone,\r\n\r\nFirst off, I love Rails. It's a fantastic framework.\r\n\r\nSo on to this possible bug...\r\n\r\nSay I have a Book model with many authors through an AuthorAssignment model. The AuthorAssignment model also has an additional boolean column named featured.\r\n\r\n```ruby\r\nclass Book < ActiveRecord::Base\r\n    has_many :author_assignments, :dependent => :destroy\r\n    has_many :featured_authors, :through => :author_assignments, :conditions => \"`author_assignments`.featured = 1\"\r\nend\r\n```\r\nHere is my problem:\r\n\r\nWhen I call:\r\n```\r\n@book.featured_author_ids\r\n```\r\nThe expected array of ids is incorrect. I get an array of all the author ids, not just the featured authors. The SQL query doesn't include the condition that 'author_assignments'.featured must be true.\r\n\r\n```\r\nSELECT `author_assignments`.author_id FROM `author_assignments` WHERE (`author_assignments`.book_id = 4)\r\n```\r\nOn the other hand, when I fetch the @book.featured_authors records:\r\n\r\n```\r\n@book.featured_authors\r\n```\r\nThe 'author_assignments'.featured condition is included in the SQL finder and the expected result for records is correct.\r\n\r\nThanks,\r\nChris");
		assertTrue(cAndT[0].startsWith("ruby"));
		assertTrue(cAndT[0].endsWith("@book.featured_authors"));
		assertTrue(cAndT[1].startsWith("Opening a new issue which"));
		assertTrue(cAndT[1].endsWith("Chris"));
		cAndT = Recommend.separateCodeAndText("@jmileham you can just call `ast` on the select manager.  ARel does not do \"relational algebra\", it merely manages an SQL ast.  Since one ast is a valid subtree of another ast, you *should* be able to pull the ast from one select manager, and pass it to another select manager.\n\nAs for aliasing your subquery, we can construct an `AS` node.  Off the top of my head, you could do something like this:\n\n    as = Arel::Nodes::As.new sm1.grouping(sm1.ast), Arel.sql('omg')\n    sm2.project('whatever').from(as)\n\nProbably we need a factory method for `As` nodes, but I see no reason why Arel cannot handle this use case today.");
		assertTrue(cAndT[0].startsWith("as = Arel::Nodes::As.new"));
		assertTrue(cAndT[0].endsWith("from(as)"));
		assertTrue(cAndT[1].startsWith("@jmileham you can just call `ast` on the select manager"));
		assertTrue(cAndT[1].endsWith("this use case today."));
	}
	
//	@Test
	public void testExtractFileNames() {
		String text = "seem that database.yml is not load on rake task.\r\nIf we've an additional database conection, for example for a legacy ddbb. When execute \"rake -T\" for example gets the next error:\r\n\r\n$> rake -T \r\nrake aborted!\r\nfoo database is not configured\r\n\r\n\r\n/home/myuser/.bundler/ruby/1.9.2-p180/rails-c6e513bab050/activerecord/lib/active_record/connection_adapters/abstract/connection_specification.rb:62:in `establish_connection'\r\n";
		LinkedList<String> files = Recommend.extractFileNames(text);
		assertEquals("activerecord/lib/active_record/connection_adapters/abstract/connection_specification.rb", files.get(0));
	}
}
