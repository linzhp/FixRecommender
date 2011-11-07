package tim260.project;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.service.IssueService;

import com.google.gson.reflect.TypeToken;

import static org.eclipse.egit.github.core.client.IGitHubConstants.*;

public class MyIssueService extends IssueService{

	private static final long serialVersionUID = 2772168428829362454L;
	
	@SuppressWarnings("unchecked")
	public List<RepositoryCommit> getCommits(IRepositoryIdProvider repo, int issueNum) throws IOException{
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repo.generateId());
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(issueNum);
		uri.append(SEGMENT_COMMITS);
		GitHubRequest request = new GitHubRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<RepositoryCommit>>() {
		}.getType());
		GitHubClient client = new GitHubClient();
		return (List<RepositoryCommit>)client.get(request).getBody();
	}
}
