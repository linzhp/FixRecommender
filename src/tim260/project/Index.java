package tim260.project;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import tim260.project.MyAnalyzer;

public class Index {
	
	private static Index instance;
	public int commidID;
	private Directory dir;
	private IndexWriter writer;
	private IndexReader reader;
	private static Version ver = Version.LUCENE_34;
	
	private Index() throws Exception{
		reset();
	}
	
	public static Index getInstace() throws Exception{
		if(instance == null){
			instance = new Index();
		}
		return instance;
	}
	
	public void reset() throws Exception{
		File indexPath = new File("index");
		if(indexPath.exists()){
			FileUtils.deleteDirectory(indexPath);
		}
		dir = new SimpleFSDirectory(indexPath);
		
	}
	
	public IndexWriter getWriter() throws Exception{
		if(writer == null){
			IndexWriterConfig config = new IndexWriterConfig(ver, new MyAnalyzer(ver));
			writer = new IndexWriter(dir, config);
		}
		return writer;
	}
	
	public IndexReader getReader() throws Exception{
		reader = IndexReader.open(getWriter(), true);
		return reader;
	}
}
