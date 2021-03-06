package tim260.project;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;


public class MyAnalyzer extends ReusableAnalyzerBase {
	private Set<Object> englishStopSet;
	private Set<Object> rubyStopSet;
	private Version matchVersion;

	public MyAnalyzer(Version matchVersion) {
		this.matchVersion = matchVersion;
		englishStopSet = StopFilter.makeStopSet(matchVersion, getEnglishStopwords());
		rubyStopSet = StopFilter.makeStopSet(matchVersion, getRubyStopwords());
	}

	private static List<String> getEnglishStopwords() {
		List<String> stopwords = new ArrayList<String>();
		// Stop words from
		// http://www.textfixer.com/resources/common-english-words.txt
		List<String> stopList1 = Arrays.asList("a", "able", "about", "across",
				"after", "all", "almost", "also", "am", "among", "an", "and",
				"any", "are", "as", "at", "be", "because", "been", "but", "by",
				"can", "cannot", "could", "dear", "did", "do", "does",
				"either", "else", "ever", "every", "for", "from", "get", "got",
				"had", "has", "have", "he", "her", "hers", "him", "his", "how",
				"however", "i", "if", "in", "into", "is", "it", "its", "just",
				"least", "let", "like", "likely", "may", "me", "might", "most",
				"must", "my", "neither", "no", "nor", "not", "of", "off",
				"often", "on", "only", "or", "other", "our", "own", "rather",
				"said", "say", "says", "she", "should", "since", "so", "some",
				"than", "that", "the", "their", "them", "then", "there",
				"these", "they", "this", "tis", "to", "too", "twas", "us",
				"wants", "was", "we", "were", "what", "when", "where", "which",
				"while", "who", "whom", "why", "will", "with", "would", "yet",
				"you", "your");
		// Stop words from http://www.ranks.nl/resources/stopwords.html
		List<String> stopList2 = Arrays.asList("a", "about", "above", "after",
				"again", "against", "all", "am", "an", "and", "any", "are",
				"aren't", "as", "at", "be", "because", "been", "before",
				"being", "below", "between", "both", "but", "by", "can't",
				"cannot", "could", "couldn't", "did", "didn't", "do", "does",
				"doesn't", "doing", "don't", "down", "during", "each", "few",
				"for", "from", "further", "had", "hadn't", "has", "hasn't",
				"have", "haven't", "having", "he", "he'd", "he'll", "he's",
				"her", "here", "here's", "hers", "herself", "him", "himself",
				"his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if",
				"in", "into", "is", "isn't", "it", "it's", "its", "itself",
				"let's", "me", "more", "most", "mustn't", "my", "myself", "no",
				"nor", "not", "of", "off", "on", "once", "only", "or", "other",
				"ought", "our", "ours ", "ourselves", "out", "over", "own",
				"same", "shan't", "she", "she'd", "she'll", "she's", "should",
				"shouldn't", "so", "some", "such", "than", "that", "that's",
				"the", "their", "theirs", "them", "themselves", "then",
				"there", "there's", "these", "they", "they'd", "they'll",
				"they're", "they've", "this", "those", "through", "to", "too",
				"under", "until", "up", "very", "was", "wasn't", "we", "we'd",
				"we'll", "we're", "we've", "were", "weren't", "what", "what's",
				"when", "when's", "where", "where's", "which", "while", "who",
				"who's", "whom", "why", "why's", "with", "won't", "would",
				"wouldn't", "you", "you'd", "you'll", "you're", "you've",
				"your", "yours", "yourself", "yourselves");
		stopwords.addAll(stopList1);
		stopwords.addAll(stopList2);
		return stopwords;
	}

	private static List<String> getRubyStopwords() {
		return Arrays.asList("alias", "and", " BEGIN", "begin", "break",
				"case", "class", "def", " defined?", "do", "", "else", "elsif",
				"END", " end", " ensure", "false", "for", " if", "in", "",
				"module", "next", "nil", " not", "or", "", "redo", "rescue",
				"retry", "return", "self", "super", "then", "true", "undef",
				"unless", "until", "when", "while", "yield");
	}
	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
		final StandardTokenizer src = new StandardTokenizer(matchVersion,
				reader);
		TokenStream tok = new StandardFilter(matchVersion, src);
		tok = new LowerCaseFilter(matchVersion, tok);
		if(fieldName.equals("text")){
			tok = new StopFilter(matchVersion, tok, englishStopSet);			
			tok = new PorterStemFilter(tok);
		}else if(fieldName.equals("code")){
			tok = new StopFilter(matchVersion, tok, rubyStopSet);					
		}
		return new TokenStreamComponents(src, tok) {
			@Override
			protected boolean reset(final Reader reader) throws IOException {
				return super.reset(reader);
			}
		};
	}

}
