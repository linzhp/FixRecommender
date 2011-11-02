package tim260.project.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.junit.Test;

import tim260.project.MyIssueService;

public class IssueTest {

	@Test
	public void testGetCommits() throws IOException {
		IRepositoryIdProvider repo = RepositoryId.create("rails","rails");
		MyIssueService icService = new MyIssueService();
		List<RepositoryCommit> commits = icService.getCommits(repo, 3486);
		assertEquals("5f401d796977b7525edd1429c838720a412dfd30",commits.get(0).getSha());
		
		commits = icService.getCommits(repo, 3477);
		assertEquals("ff9c2799c1a065fb132df64da6d19683c647b5b4", commits.get(0).getSha());
	}

}
